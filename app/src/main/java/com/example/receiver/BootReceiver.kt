package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.service.SoundboxForegroundService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootReceiver", "Device rebooted: initializing Live Voice Alert soundbox services")
            try {
                SoundboxForegroundService.start(context)
            } catch (e: Exception) {
                Log.e("BootReceiver", "Failed starting foreground service after boot", e)
            }
        }
    }
}
