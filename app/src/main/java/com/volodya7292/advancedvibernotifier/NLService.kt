package com.volodya7292.advancedvibernotifier

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.lifecycle.MutableLiveData

const val AVN_NOTIFICATION_ID = 1001
const val NOTIFICATION_CHANNEL_GENERAL = "General"

class NLService : NotificationListenerService() {
    lateinit var prefs: SharedPreferences
    var lastMsgTime: Long = 0
    var mediaPlayer: MediaPlayer? = null

    companion object {
        var instance: NLService? = null
        var lastNotificationTextData = MutableLiveData("")

        fun isInstanceCreated(): Boolean {
            return instance != null
        }
    }

    fun defaultNotification(): Notification.Builder {
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_GENERAL)
            .setContentTitle("Service enabled").setContentText("Service is running")
            .setSmallIcon(R.drawable.baseline_notifications_active_24)
            .setOngoing(true)
            .setContentIntent(
                PendingIntent.getActivity(
                    this,
                    0,
                    Intent(this, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE
                )
            )
            .setVisibility(Notification.VISIBILITY_PUBLIC)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            notification.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }

        return notification
    }

    override fun onCreate() {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_GENERAL,
            NOTIFICATION_CHANNEL_GENERAL,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.setShowBadge(false)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)

        super.onCreate()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onListenerConnected() {
        Log.i(TAG, "Notification listener connected")
        super.onListenerConnected()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        startForeground(AVN_NOTIFICATION_ID, defaultNotification().build())
        Log.i(TAG, "Notification listener service started")

        return START_REDELIVER_INTENT
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
//        if (prefs.getBoolean(PREF_RETAIN_SERVICE, true)) {
//            val serviceIntent = Intent(this, NLService::class.java)
//            startForegroundService(serviceIntent)
//        }

        super.onTaskRemoved(rootIntent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (sbn.packageName != "com.viber.voip") {
            return
        }
        if (sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) {
            return
        }
        if (sbn.notification.`when` <= lastMsgTime) {
            return
        }
        lastMsgTime = sbn.notification.`when`

        val sbnTitle = sbn.notification.extras.getCharSequence(Notification.EXTRA_TITLE)
        val sbnText = sbn.notification.extras.getCharSequence(Notification.EXTRA_TEXT)
        val sbnTitleLower = sbnTitle.toString().lowercase()

        val chatNames = listOf(
            prefs.getString(PREF_CHAT1, "")!!, prefs.getString(PREF_CHAT2, "")!!,
            prefs.getString(PREF_CHAT3, "")!!
        )
        val chatTones = listOf(
            prefs.getString(PREF_CHAT1_RINGTONE_URI, "")!!,
            prefs.getString(PREF_CHAT2_RINGTONE_URI, "")!!,
            prefs.getString(PREF_CHAT3_RINGTONE_URI, "")!!
        )

        for (pair in chatNames.zip(chatTones)) {
            val name = pair.first
            val tone = pair.second

            if (name.isBlank()) {
                continue
            }
            if (!sbnTitleLower.contains(name.lowercase())) {
                continue
            }

            val lastNotificationText = "[${sbnTitle}]: \"${sbnText}\""
            prefs.edit().putString(PREF_LAST_NOTIFICATION_TEXT, lastNotificationText).apply()
            lastNotificationTextData.value = lastNotificationText

            val notification = defaultNotification()
                .setContentText("Tap to stop the ringtone")
                .setContentIntent(
                    PendingIntent.getService(
                        this,
                        0,
                        Intent(this, NotificationActionService::class.java).setAction(
                            NotificationActionService.STOP_RINGTONE
                        ),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
                    )
                )
                .build()

            postNewNotification(notification, tone)
            break
        }
    }

    private fun postNewNotification(notification: Notification, toneUriStr: String) {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                return
            }
        } catch (_: Exception) {
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        val toneUri = Uri.parse(toneUriStr)
        notificationManager.notify(AVN_NOTIFICATION_ID, notification)

        this.mediaPlayer = if (toneUriStr.isNotBlank() && toneUri != null) {
            MediaPlayer.create(this, toneUri)
        } else {
            MediaPlayer.create(this, R.raw.default_notification_tone)
        }
        mediaPlayer?.setOnCompletionListener {
            notificationManager.notify(AVN_NOTIFICATION_ID, defaultNotification().build())
        }
        mediaPlayer?.start()
    }
}