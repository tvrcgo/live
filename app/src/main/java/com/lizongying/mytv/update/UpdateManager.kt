package com.lizongying.mytv.update

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import androidx.core.content.FileProvider
import com.google.gson.JsonParser
import com.lizongying.mytv.appVersionName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request as HttpRequest
import java.io.File
import java.util.concurrent.TimeUnit

object UpdateManager {

    data class RemoteVersion(val name: String, val apkUrl: String)

    sealed class State {
        object Idle : State()
        object Checking : State()
        object Latest : State()
        object CheckFailed : State()
        class Available(val remote: RemoteVersion) : State()
        class Downloading(val percent: Int) : State()
        class Downloaded(val remote: RemoteVersion, val apk: File) : State()
        class DownloadFailed(val remote: RemoteVersion) : State()
    }

    var state: State = State.Idle
        private set

    // SettingFragment 在 onCreateView 注册、onDestroyView 注销；下载生命周期独立于设置页
    var listener: ((State) -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var downloadJob: Job? = null

    fun check(context: Context) {
        // 已有结果或进行中时不重复请求，避免频繁触发 API 限流
        when (state) {
            is State.Checking, is State.Available,
            is State.Downloading, is State.Downloaded -> return
            else -> Unit
        }
        setState(State.Checking)
        val current = context.appVersionName
        scope.launch {
            setState(
                try {
                    val remote = fetchLatest()
                    when {
                        remote == null -> State.CheckFailed
                        VersionCompare.compare(current, remote.name) > 0 -> State.Available(remote)
                        else -> State.Latest
                    }
                } catch (e: Exception) {
                    State.CheckFailed
                }
            )
        }
    }

    fun download(context: Context) {
        val remote = when (val s = state) {
            is State.Available -> s.remote
            is State.DownloadFailed -> s.remote
            else -> return
        }
        if (downloadJob?.isActive == true) {
            return
        }
        setState(State.Downloading(0))
        // 下载生命周期独立于设置页，协程内只持有 applicationContext，避免泄漏 Activity
        val appContext = context.applicationContext
        downloadJob = scope.launch {
            val apk = File(updateDir(appContext), "live-${remote.name}.apk")
            try {
                downloadTo(remote.apkUrl, apk) { percent ->
                    setState(State.Downloading(percent))
                }
                setState(State.Downloaded(remote, apk))
                install(appContext)
            } catch (e: Exception) {
                apk.delete()
                setState(State.DownloadFailed(remote))
            }
        }
    }

    fun install(context: Context) {
        val downloaded = state as? State.Downloaded ?: return
        val uri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", downloaded.apk
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MIME_TYPE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    private fun fetchLatest(): RemoteVersion? {
        val response = client.newCall(
            HttpRequest.Builder()
                .url(LATEST_RELEASE_URL)
                .header("User-Agent", API_USER_AGENT)
                .build()
        ).execute()
        response.use {
            if (!it.isSuccessful) {
                return null
            }
            val root = JsonParser.parseString(it.body()?.string() ?: return null).asJsonObject
            val tag = root.get("tag_name")?.asString?.removePrefix("v") ?: return null
            val assets = root.getAsJsonArray("assets") ?: return null
            for (element in assets) {
                val asset = element.asJsonObject
                val name = asset.get("name")?.asString ?: continue
                if (name.startsWith(APK_NAME_PREFIX) && name.endsWith(APK_NAME_SUFFIX)) {
                    val url = asset.get("browser_download_url")?.asString ?: continue
                    return RemoteVersion(tag, url)
                }
            }
            return null
        }
    }

    private fun downloadTo(url: String, target: File, onProgress: (Int) -> Unit) {
        target.parentFile?.mkdirs()
        val response = client.newCall(
            HttpRequest.Builder()
                .url(url)
                .header("User-Agent", API_USER_AGENT)
                .build()
        ).execute()
        response.use {
            if (!it.isSuccessful) {
                throw java.io.IOException("download failed: ${it.code()}")
            }
            val body = it.body() ?: throw java.io.IOException("empty body")
            val total = body.contentLength()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DOWNLOAD_BUFFER_SIZE)
                    var read: Int
                    var fetched = 0L
                    var lastPercent = -1
                    while (input.read(buffer).also { n -> read = n } != -1) {
                        output.write(buffer, 0, read)
                        fetched += read
                        if (total > 0) {
                            val percent = (fetched * 100 / total).toInt()
                            if (percent != lastPercent) {
                                lastPercent = percent
                                onProgress(percent)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateDir(context: Context) = File(context.cacheDir, UPDATE_DIR_NAME)

    private fun setState(next: State) {
        state = next
        val l = listener ?: return
        mainHandler.post { l(next) }
    }

    private const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/tvrcgo/live/releases/latest"
    private const val API_USER_AGENT = "tvrcgo-live"
    private const val APK_NAME_PREFIX = "live-"
    private const val APK_NAME_SUFFIX = ".apk"
    private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    private const val UPDATE_DIR_NAME = "updates"
    private const val CONNECT_TIMEOUT_SECONDS = 10L
    private const val READ_TIMEOUT_SECONDS = 60L
    private const val DOWNLOAD_BUFFER_SIZE = 64 * 1024
}
