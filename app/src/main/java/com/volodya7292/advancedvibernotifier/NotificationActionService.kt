package com.volodya7292.advancedvibernotifier

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class NotificationActionService : Service() {
    companion object {
        const val STOP_RINGTONE = "STOP_RINGTONE"
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == STOP_RINGTONE) {
            try {
                NLService.instance?.mediaPlayer?.stop()
            } catch (_: Exception) {
            }
        }

        NLService.instance?.let {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(AVN_NOTIFICATION_ID, it.defaultNotification().build())
        }

        stopSelf()
        return START_NOT_STICKY
    }
}