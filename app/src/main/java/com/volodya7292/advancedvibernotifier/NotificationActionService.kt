package com.volodya7292.advancedvibernotifier

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.IBinder

class NotificationActionService : Service() {
    lateinit var prefs: SharedPreferences

    companion object {
        const val STOP_RINGTONE = "STOP_RINGTONE"
    }

    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        NLService.instance?.let {
            if (intent.action == STOP_RINGTONE) {
                try {
                    it.mediaPlayer?.stop()
                } catch (_: Exception) {
                }
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(AVN_NOTIFICATION_ID, it.notificationServiceRunning().build())
        }

        if (prefs.getBoolean(PREF_STOP_SECOND_SERVICE, true)) {
            stopSelf()
        }
        return START_NOT_STICKY
    }
}