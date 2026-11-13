package com.chacha.live.logo

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.chacha.live.SP
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File

/**
 * 台标解析与加载：优先用上次的解析结果，没有才按频道名推导候选地址；
 * 缓存的地址失效（404）时丢弃缓存重新推导，全部失败则回退源 tvg-logo。
 */
object Logos {

    private const val TAG = "Logos"
    private const val FILE_NAME = "logo-resolve.json"
    private const val SAVE_DELAY_MILLIS = 3000L

    private val cache = LogoCache()
    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var storeFile: File? = null
    private var version = 0L
    private var store: LogoStore? = null
    private var storeHost = ""

    private val failed = HashSet<String>()

    private val saveTask = Runnable { save() }

    fun init(context: Context) {
        storeFile = File(context.filesDir, FILE_NAME)
        version = versionOf(context)
        cache.load(read(), version)
    }

    fun load(view: ImageView, title: String, sourceLogo: String, width: Int = 0, height: Int = 0) {
        view.setImageDrawable(null)
        if (title.isBlank()) {
            showFallback(view, sourceLogo, width, height)
            return
        }
        val now = System.currentTimeMillis()
        val cached = cache.file(title, now)
        if (cached != null) {
            attempt(view, title, listOf(cached), 0, sourceLogo, width, height, true)
            return
        }
        if (cache.isFailed(title, now)) {
            showFallback(view, sourceLogo, width, height)
            return
        }
        attempt(view, title, LogoName.candidates(title), 0, sourceLogo, width, height, false)
    }

    fun clearFailures() {
        cache.clearFailures()
        scheduleSave()
    }

    private fun attempt(
        view: ImageView,
        title: String,
        names: List<String>,
        index: Int,
        sourceLogo: String,
        width: Int,
        height: Int,
        cached: Boolean,
    ) {
        val url = store().url(names[index])
        if (failed.contains(url)) {
            proceed(view, title, names, index, sourceLogo, width, height, cached)
            return
        }
        request(view, url, width, height, object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                failed.add(url)
                // Glide 禁止在回调内对同一 view 发起新请求，post 到回调返回之后再继续
                view.post { proceed(view, title, names, index, sourceLogo, width, height, cached) }
                return true
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean {
                cache.recordSuccess(title, names[index], System.currentTimeMillis())
                scheduleSave()
                return false
            }
        })
    }

    private fun proceed(
        view: ImageView,
        title: String,
        names: List<String>,
        index: Int,
        sourceLogo: String,
        width: Int,
        height: Int,
        cached: Boolean,
    ) {
        if (index + 1 < names.size) {
            attempt(view, title, names, index + 1, sourceLogo, width, height, cached)
            return
        }
        if (cached) {
            val fresh = LogoName.candidates(title).filter { it != names[0] }
            if (fresh.isNotEmpty()) {
                attempt(view, title, fresh, 0, sourceLogo, width, height, false)
                return
            }
        }
        cache.recordFailure(title, System.currentTimeMillis())
        scheduleSave()
        showFallback(view, sourceLogo, width, height)
    }

    private fun showFallback(view: ImageView, sourceLogo: String, width: Int, height: Int) {
        if (sourceLogo.isEmpty() || failed.contains(sourceLogo)) {
            return
        }
        request(view, sourceLogo, width, height, object : RequestListener<Drawable> {
            override fun onLoadFailed(
                e: GlideException?,
                model: Any?,
                target: Target<Drawable>,
                isFirstResource: Boolean,
            ): Boolean {
                failed.add(sourceLogo)
                return false
            }

            override fun onResourceReady(
                resource: Drawable,
                model: Any,
                target: Target<Drawable>,
                dataSource: DataSource,
                isFirstResource: Boolean,
            ): Boolean = false
        })
    }

    private fun request(
        view: ImageView,
        url: String,
        width: Int,
        height: Int,
        listener: RequestListener<Drawable>,
    ) {
        val builder = Glide.with(view).load(url)
        if (width > 0 && height > 0) {
            builder.override(width, height)
        }
        builder.centerInside().listener(listener).into(view)
    }

    private fun store(): LogoStore {
        val host = SP.logoHost.ifEmpty { LogoStore.defaultHost() }
        val current = store
        if (current == null || storeHost != host) {
            return LogoStore(host).also {
                store = it
                storeHost = host
            }
        }
        return current
    }

    private fun versionOf(context: Context): Long = try {
        context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
    } catch (e: Exception) {
        0L
    }

    private fun read(): LogoCache.Stored? {
        val file = storeFile ?: return null
        if (!file.exists()) {
            return null
        }
        return try {
            gson.fromJson(file.readText(), LogoCache.Stored::class.java)
        } catch (e: Exception) {
            file.delete()
            null
        }
    }

    private fun scheduleSave() {
        handler.removeCallbacks(saveTask)
        handler.postDelayed(saveTask, SAVE_DELAY_MILLIS)
    }

    private fun save() {
        val file = storeFile ?: return
        val json = gson.toJson(cache.toStored(version))
        scope.launch {
            try {
                file.writeText(json)
            } catch (e: Exception) {
                Log.i(TAG, "save failed ${e.message}")
            }
        }
    }
}
