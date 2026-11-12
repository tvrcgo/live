package com.chacha.live

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // Android 12+ 禁止后台启动 Activity，开机自启不可用
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return
        }
        if (SP.bootStartup) {
            context.startActivity(
                Intent(context, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

}
