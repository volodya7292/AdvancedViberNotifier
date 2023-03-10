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
    lateinit var currentNotification: Notification.Builder

    companion object {
        var instance: NLService? = null
        var lastNotificationTextData = MutableLiveData("")

        fun isInstanceCreated(): Boolean {
            return instance != null
        }
    }

    fun notificationServiceStarting(): Notification.Builder {
        val notification = Notification.Builder(this, NOTIFICATION_CHANNEL_GENERAL)
            .setContentTitle("Service enabled").setContentText("Service is starting...")
            .setSmallIcon(R.drawable.baseline_notifications_24)
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

    fun notificationServiceRunning(): Notification.Builder {
        return notificationServiceStarting().setContentText("Service is running")
    }

    fun notificationListenerDisconnected(): Notification.Builder {
        return notificationServiceStarting().setContentText("Notification listener is disconnected")
    }

    fun notificationViberMessageReceived(): Notification.Builder {
        return notificationServiceRunning()
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
    }

    override fun onCreate() {
        val notificationChannel = NotificationChannel(
            NOTIFICATION_CHANNEL_GENERAL,
            NOTIFICATION_CHANNEL_GENERAL,
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationChannel.setSound(null, null)
        notificationChannel.setShowBadge(false)

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(notificationChannel)

        super.onCreate()
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        instance = this
        prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        currentNotification = notificationServiceStarting()
        startForeground(AVN_NOTIFICATION_ID, currentNotification.build())
        Log.i(TAG, "Notification listener service started")

        return START_REDELIVER_INTENT
    }

    override fun onListenerConnected() {
        currentNotification = notificationServiceRunning()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(AVN_NOTIFICATION_ID, currentNotification.build())
        super.onListenerConnected()
    }

    override fun onListenerDisconnected() {
        currentNotification = notificationListenerDisconnected()
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(AVN_NOTIFICATION_ID, currentNotification.build())
        super.onListenerConnected()
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
        val sbnTitleLower = sbnTitle.toString().trim().lowercase()

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
            if (!sbnTitleLower.contains(name.trim().lowercase())) {
                continue
            }

            val lastNotificationText = "[${sbnTitle}]: \"${sbnText}\""
            prefs.edit().putString(PREF_LAST_NOTIFICATION_TEXT, lastNotificationText).apply()
            lastNotificationTextData.value = lastNotificationText

            val notification = notificationViberMessageReceived().build()
            postNewNotification(notification, tone)

            break
        }
    }

    fun postNewNotification(notification: Notification, toneUriStr: String) {
        try {
            if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
                return
            }
        } catch (_: Exception) {
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(AVN_NOTIFICATION_ID, notification)

        val toneUri = Uri.parse(toneUriStr)
        this.mediaPlayer = MediaPlayer.create(this, toneUri)

        mediaPlayer?.setOnCompletionListener {
            notificationManager.notify(AVN_NOTIFICATION_ID, notificationServiceRunning().build())
        }

        try {
            mediaPlayer?.start()
        } catch (_: Exception) {
        }
    }
}