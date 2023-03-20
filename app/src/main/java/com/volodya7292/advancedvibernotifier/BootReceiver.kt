package com.volodya7292.advancedvibernotifier

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

            if (prefs.getBoolean(PREF_ACTIVE, true)) {
                val serviceIntent = Intent(context, NLService::class.java)
                context.startForegroundService(serviceIntent)
            }
        }
    }
}