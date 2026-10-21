package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("hey_gandhi_prefs", Context.MODE_PRIVATE)
            val autoStartOnBoot = prefs.getBoolean("auto_start_wake_service", false)
            if (autoStartOnBoot) {
                GandhiWakeWordService.startService(context)
            }
        }
    }
}
