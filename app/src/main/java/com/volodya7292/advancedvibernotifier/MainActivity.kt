package com.volodya7292.advancedvibernotifier

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener

const val TAG = "AVN"
const val PREFS_NAME = "PREFS"
const val PREF_CHAT1 = "chatName1"
const val PREF_CHAT2 = "chatName2"
const val PREF_CHAT3 = "chatName3"
const val PREF_STOP_SECOND_SERVICE = "doStopSecondServiceImmediately"
const val PREF_LAST_NOTIFICATION_TEXT = "lastNotificationText"
const val PREF_CHAT1_RINGTONE_URI = "chat1_ringtone_uri"
const val PREF_CHAT2_RINGTONE_URI = "chat2_ringtone_uri"
const val PREF_CHAT3_RINGTONE_URI = "chat3_ringtone_uri"

class MainActivity : AppCompatActivity() {
    private val permissionReadAudio by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
    private val permissions by lazy {
        val list = mutableListOf(permissionReadAudio)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        list
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var statusText: TextView
    private lateinit var lastNotificationText: TextView
    private lateinit var fixPowerOptimizationsB: Button
    private lateinit var testRingtoneB: Button
    private lateinit var ring1ShortSelectB: Button
    private lateinit var ring1LongSelectB: Button
    private lateinit var ring2ShortSelectB: Button
    private lateinit var ring2LongSelectB: Button
    private lateinit var ring3ShortSelectB: Button
    private lateinit var ring3LongSelectB: Button
    private lateinit var restartB: Button
    private var started = false
    private var notificationListenerPermissionToast: Toast? = null
    private var currentRingtonePrefName = ""

    private var ringtonePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val toneUri =
                    data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)

                val prefName =
                    currentRingtonePrefName//data?.getStringExtra(RING_PICKER_EXTRA_RINGTONE_PREF_NAME)!!
                prefs.edit().putString(prefName, toneUri.toString()).apply()
            }
        }

    private var optimizationsResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkPowerOptimizations()
        }

    private fun launchRingtonePicker(resultPrefName: String, ringtoneType: Int) {
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, ringtoneType)
        val currRingtoneUri = prefs.getString(resultPrefName, "")
        intent.putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            Uri.parse(currRingtoneUri)
        )
        intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        intent.putExtra(
            RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
            Uri.Builder().scheme(ContentResolver.SCHEME_ANDROID_RESOURCE).authority(packageName)
                .path("/raw/default_notification_tone").build()
        )
        currentRingtonePrefName = PREF_CHAT1_RINGTONE_URI
        ringtonePickerLauncher.launch(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val versionText = findViewById<TextView>(R.id.versionText)
        val chatName1ET = findViewById<EditText>(R.id.chatName1)
        val chatName2ET = findViewById<EditText>(R.id.chatName2)
        val chatName3ET = findViewById<EditText>(R.id.chatName3)
        val stopSecondServiceSwitch = findViewById<SwitchCompat>(R.id.stopSecondServiceSwitch)

        ring1ShortSelectB = findViewById(R.id.ring1ShortSelectB)
        ring1LongSelectB = findViewById(R.id.ring1LongSelectB)
        ring2ShortSelectB = findViewById(R.id.ring2ShortSelectB)
        ring2LongSelectB = findViewById(R.id.ring2LongSelectB)
        ring3ShortSelectB = findViewById(R.id.ring3ShortSelectB)
        ring3LongSelectB = findViewById(R.id.ring3LongSelectB)
        restartB = findViewById(R.id.restartB)
        statusText = findViewById(R.id.statusText)
        lastNotificationText = findViewById(R.id.lastNotificationText)
        fixPowerOptimizationsB = findViewById(R.id.fixPowerOptimizationsB)
        testRingtoneB = findViewById(R.id.testRingtoneB)

        stopSecondServiceSwitch.isChecked = prefs.getBoolean(PREF_STOP_SECOND_SERVICE, true)
        stopSecondServiceSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean(PREF_STOP_SECOND_SERVICE, isChecked).apply()
        }
        versionText.text = "v${BuildConfig.VERSION_NAME}"

        chatName1ET.setText(prefs.getString(PREF_CHAT1, ""))
        chatName2ET.setText(prefs.getString(PREF_CHAT2, ""))
        chatName3ET.setText(prefs.getString(PREF_CHAT3, ""))

        chatName1ET.addTextChangedListener {
            prefs.edit().putString(PREF_CHAT1, it.toString()).apply()
        }
        chatName2ET.addTextChangedListener {
            prefs.edit().putString(PREF_CHAT2, it.toString()).apply()
        }
        chatName3ET.addTextChangedListener {
            prefs.edit().putString(PREF_CHAT3, it.toString()).apply()
        }

        ring1ShortSelectB.setOnClickListener {
            launchRingtonePicker(PREF_CHAT1_RINGTONE_URI, RingtoneManager.TYPE_NOTIFICATION)
        }
        ring1LongSelectB.setOnClickListener {
            launchRingtonePicker(PREF_CHAT1_RINGTONE_URI, RingtoneManager.TYPE_RINGTONE)
        }
        ring2ShortSelectB.setOnClickListener {
            launchRingtonePicker(PREF_CHAT2_RINGTONE_URI, RingtoneManager.TYPE_NOTIFICATION)
        }
        ring2LongSelectB.setOnClickListener {
            launchRingtonePicker(PREF_CHAT2_RINGTONE_URI, RingtoneManager.TYPE_RINGTONE)
        }
        ring3ShortSelectB.setOnClickListener {
            launchRingtonePicker(PREF_CHAT3_RINGTONE_URI, RingtoneManager.TYPE_NOTIFICATION)
        }
        ring3LongSelectB.setOnClickListener {
            launchRingtonePicker(PREF_CHAT3_RINGTONE_URI, RingtoneManager.TYPE_RINGTONE)
        }

        restartB.setOnClickListener {
            restart()
        }
        testRingtoneB.setOnClickListener {
            NLService.instance?.let {
                it.postNewNotification(
                    it.notificationViberMessageReceived().build(),
                    prefs.getString(PREF_CHAT1_RINGTONE_URI, "")!!
                )
            }
        }

        NLService.lastNotificationTextData.observe(this) {
            lastNotificationText.text = it
        }

        startCheckRuntimePermissions()
    }

    override fun onResume() {
        if (Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                .contains(packageName)
        ) {
            notificationListenerPermissionToast?.cancel()
        } else {
            notificationListenerPermissionToast = Toast.makeText(
                this,
                "Please grant permission to enable NOTIFICATION_LISTENER",
                Toast.LENGTH_LONG
            )
            notificationListenerPermissionToast?.show()

            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        lastNotificationText.text = prefs.getString(PREF_LAST_NOTIFICATION_TEXT, "")

        checkPowerOptimizations()

        super.onResume()
    }

    private fun checkPowerOptimizations() {
        val powerManager = getSystemService(PowerManager::class.java)
        val powerOptimizationsAreOff = powerManager.isIgnoringBatteryOptimizations(packageName)

        if (powerOptimizationsAreOff) {
            fixPowerOptimizationsB.text = "Configure power optimizations"
            fixPowerOptimizationsB.setBackgroundColor(getColor(R.color.gray))
            fixPowerOptimizationsB.setOnClickListener {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        } else {
            fixPowerOptimizationsB.text = "Disable power optimizations"
            fixPowerOptimizationsB.setBackgroundColor(getColor(R.color.yellow))
            fixPowerOptimizationsB.setOnClickListener {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                optimizationsResultLauncher.launch(intent)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { results: Map<String, Boolean> ->
            for ((perm, granted) in results) {
                if (granted) {
                    continue
                }

                if (perm == permissionReadAudio) {
                    Toast.makeText(
                        this,
                        "Reading external storage is needed to play custom sounds",
                        Toast.LENGTH_LONG
                    ).show()
                } else if (perm == Manifest.permission.POST_NOTIFICATIONS) {
                    Toast.makeText(
                        this,
                        "Please enable notifications to use the app properly",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

    private fun startCheckRuntimePermissions() {
        var allGranted = true

        for (perm in permissions) {
            if (ContextCompat.checkSelfPermission(this, perm)
                != PackageManager.PERMISSION_GRANTED
            ) {
                allGranted = false
                continue
            }
        }

        if (!allGranted) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }

        start()
    }

    private fun start() {
        if (started) {
            return
        }
        started = true

        if (!NLService.isInstanceCreated()) {
            val serviceIntent = Intent(this, NLService::class.java)
            startForegroundService(serviceIntent)
            statusText.text = "Service started"
        } else {
            statusText.text = "Service is already running"
        }
    }

    private fun restart() {
        stopService(Intent(this, NLService::class.java))

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val mainIntent = Intent.makeRestartActivityTask(intent!!.component)
        startActivity(mainIntent)

        Runtime.getRuntime().exit(0)
    }
}