package com.lizongying.mytv.logo

import android.content.Context
import android.util.Log
import com.lizongying.mytv.SP
import com.lizongying.mytv.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 台标的 CDN 域名与地址。首次启动探测一次可用域名并缓存，之后只做字符串拼接。
 */
class LogoStore(private val host: String) {

    fun urls(title: String): List<String> = LogoName.candidates(title).map { "https://$host/$PATH/$it.png" }

    companion object {
        private const val TAG = "LogoStore"
        private const val PATH = "gh/fanmingming/live@main/tv"
        private const val PROBE_FILE = "CCTV1.png"
        private const val PROBE_TIMEOUT_SECONDS = 5L
        private val HOSTS = listOf(
            "fastly.jsdelivr.net",
            "cdn.jsdelivr.net",
            "gcore.jsdelivr.net",
            "testingcf.jsdelivr.net",
        )
        private val client = OkHttpClient.Builder()
            .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

        fun load(context: Context): LogoStore = LogoStore(SP.logoHost.ifEmpty { HOSTS.first() })

        suspend fun probe() = withContext(Dispatchers.IO) {
            if (SP.logoHost.isNotEmpty()) {
                return@withContext
            }
            for (host in HOSTS) {
                if (reachable(host)) {
                    SP.logoHost = host
                    Log.i(TAG, "logo host $host")
                    return@withContext
                }
            }
            Log.i(TAG, "no logo host reachable")
        }

        private fun reachable(host: String): Boolean {
            val request = Request.Builder()
                .url("https://$host/$PATH/$PROBE_FILE")
                .header("User-Agent", Utils.USER_AGENT)
                .head()
                .build()
            return try {
                client.newCall(request).execute().use { it.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }
    }
}
