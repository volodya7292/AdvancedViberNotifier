package com.volodya7292.advancedvibernotifier

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
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
    lateinit var prefs: SharedPreferences
    lateinit var statusText: TextView
    lateinit var lastNotificationText: TextView
    lateinit var fixPowerOptimizationsB: Button
    lateinit var ring1SelectB: ImageButton
    lateinit var ring2SelectB: ImageButton
    lateinit var ring3SelectB: ImageButton
    var started = false
    var notificationListenerPermissionToast: Toast? = null
    var currentRingtonePrefName = ""

    private var resultLauncher =
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val versionText = findViewById<TextView>(R.id.versionText)
        val chatName1ET = findViewById<EditText>(R.id.chatName1)
        val chatName2ET = findViewById<EditText>(R.id.chatName2)
        val chatName3ET = findViewById<EditText>(R.id.chatName3)
        val stopSecondServiceSwitch = findViewById<SwitchCompat>(R.id.stopSecondServiceSwitch)
        ring1SelectB = findViewById(R.id.ring1SelectB)
        ring2SelectB = findViewById(R.id.ring2SelectB)
        ring3SelectB = findViewById(R.id.ring3SelectB)
        statusText = findViewById(R.id.statusText)
        lastNotificationText = findViewById(R.id.lastNotificationText)
        fixPowerOptimizationsB = findViewById(R.id.fixPowerOptimizationsB)

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

        ring1SelectB.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            currentRingtonePrefName = PREF_CHAT1_RINGTONE_URI
            resultLauncher.launch(intent)
        }
        ring2SelectB.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            currentRingtonePrefName = PREF_CHAT2_RINGTONE_URI
            resultLauncher.launch(intent)
        }
        ring3SelectB.setOnClickListener {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALL)
            currentRingtonePrefName = PREF_CHAT3_RINGTONE_URI
            resultLauncher.launch(intent)
        }


        NLService.lastNotificationTextData.observe(this) {
            lastNotificationText.text = it
        }
    }

    @SuppressLint("BatteryLife")
    override fun onResume() {
        if (Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
                .contains(packageName)
        ) {
            notificationListenerPermissionToast?.cancel()
            startCheckRuntimePermissions()
        } else {
            notificationListenerPermissionToast = Toast.makeText(
                this,
                "Please grant permission to enable NOTIFICATION_LISTENER",
                Toast.LENGTH_LONG
            )
            notificationListenerPermissionToast?.show()

            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }

        if (prefs.contains(PREF_CHAT1_RINGTONE_URI)) {
            ring1SelectB.setColorFilter(
                Color.rgb(0, 180, 0),
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
        }
        if (prefs.contains(PREF_CHAT2_RINGTONE_URI)) {
            ring2SelectB.setColorFilter(
                Color.rgb(0, 180, 0),
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
        }
        if (prefs.contains(PREF_CHAT3_RINGTONE_URI)) {
            ring3SelectB.setColorFilter(
                Color.rgb(0, 180, 0),
                android.graphics.PorterDuff.Mode.MULTIPLY
            )
        }

        val powerManager = getSystemService(PowerManager::class.java)
        val powerOptimizationsAreOff = powerManager.isIgnoringBatteryOptimizations(packageName)

        if (powerOptimizationsAreOff) {
            fixPowerOptimizationsB.text = "Configure power optimizations"
            fixPowerOptimizationsB.setOnClickListener {
                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                startActivity(intent)
            }
        } else {
            fixPowerOptimizationsB.text = "Disable power optimizations"
            fixPowerOptimizationsB.setBackgroundColor(getColor(R.color.red))
            fixPowerOptimizationsB.setOnClickListener {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
        }

        lastNotificationText.text = prefs.getString(PREF_LAST_NOTIFICATION_TEXT, "")

        super.onResume()
    }

    private fun startCheckRuntimePermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            == PackageManager.PERMISSION_GRANTED
        ) {
            start()
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Please enable notifications to use the app properly",
                    Toast.LENGTH_LONG
                ).show()
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
}