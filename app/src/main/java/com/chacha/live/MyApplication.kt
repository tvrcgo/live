package com.chacha.live

import android.app.Application
import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager
import com.chacha.live.logo.LogoStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {
    private lateinit var displayMetrics: DisplayMetrics

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        SP.init(this)
        scope.launch { LogoStore.probe() }

        displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
//        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        windowManager.defaultDisplay.getMetrics(displayMetrics)
    }

    fun getDisplayMetrics(): DisplayMetrics {
        return displayMetrics
    }
}