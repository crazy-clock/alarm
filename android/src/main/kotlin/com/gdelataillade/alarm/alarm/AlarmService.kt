package com.gdelataillade.alarm.alarm

import com.gdelataillade.alarm.services.AudioService
import com.gdelataillade.alarm.services.AlarmStorage
import com.gdelataillade.alarm.services.VibrationService
import com.gdelataillade.alarm.services.VolumeService
import com.gdelataillade.alarm.services.TTSService
import com.gdelataillade.alarm.services.FlashlightService

import android.app.Service
import android.app.PendingIntent
import android.app.ForegroundServiceStartNotAllowedException
import android.app.Notification
import android.content.Intent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.os.Build
import com.gdelataillade.alarm.generated.EditRingingAlarmSettingsWire
import com.gdelataillade.alarm.models.AlarmSettings
import com.gdelataillade.alarm.models.EditRingingAlarmSettings
import com.gdelataillade.alarm.services.AlarmRingingLiveData
import com.gdelataillade.alarm.services.NotificationHandler
import io.flutter.Log
import kotlinx.serialization.json.Json

class AlarmService : Service() {
    companion object {
        private const val TAG = "AlarmService"

        var instance: AlarmService? = null

        @JvmStatic
        var ringingAlarmIds: List<Int> = listOf()
    }

    private var audioService: AudioService? = null
    private var vibrationService: VibrationService? = null
    private var volumeService: VolumeService? = null
    private var ttsService: TTSService? = null
    private var flashlightService: FlashlightService? = null
    private var showSystemUI: Boolean = false

    override fun onCreate() {
        super.onCreate()

        instance = this
        audioService = AudioService(this)
        vibrationService = VibrationService(this)
        volumeService = VolumeService(this)
        flashlightService = FlashlightService(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }

        val id = intent.getIntExtra("id", 0)
        val action = intent.getStringExtra(AlarmReceiver.EXTRA_ALARM_ACTION)

        if (action == "STOP_ALARM" && id != 0) {
            unsaveAlarm(id)
            return START_NOT_STICKY
        }

        // Build the notification
        val notificationHandler = NotificationHandler(this)
        val appIntent =
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
        val pendingIntent = PendingIntent.getActivity(
            this,
            id,
            appIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val alarmSettingsJson = intent.getStringExtra("alarmSettings")
        if (alarmSettingsJson == null) {
            Log.e(TAG, "Intent is missing AlarmSettings.")
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmSettings: AlarmSettings
        try {
            alarmSettings = Json.decodeFromString<AlarmSettings>(alarmSettingsJson)
            Log.d(TAG, "Alarm alarmSettings for ${alarmSettings}.")
        } catch (e: Exception) {
            Log.e(TAG, "Cannot parse AlarmSettings from Intent.")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = notificationHandler.buildNotification(
            alarmSettings.notificationSettings,
            alarmSettings.androidFullScreenIntent,
            pendingIntent,
            id
        )

        // Start the service in the foreground
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                try {
                    startAlarmService(id, notification)
                } catch (e: ForegroundServiceStartNotAllowedException) {
                    Log.e(TAG, "Foreground service start not allowed", e)
                    return START_NOT_STICKY
                }
            } else {
                startAlarmService(id, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception while starting foreground service: ${e.message}", e)
            return START_NOT_STICKY
        }

        // Check if an alarm is already ringing
        if (!alarmSettings.allowAlarmOverlap && ringingAlarmIds.isNotEmpty() && action != "STOP_ALARM") {
            Log.d(TAG, "An alarm is already ringing. Ignoring new alarm with id: $id")
            unsaveAlarm(id)
            return START_NOT_STICKY
        }

        if (alarmSettings.androidFullScreenIntent) {
            AlarmRingingLiveData.instance.update(true)
        }

        // Notify the plugin about the alarm ringing
        AlarmPlugin.alarmTriggerApi?.alarmRang(id.toLong()) {
            if (it.isSuccess) {
                Log.d(TAG, "Alarm rang notification for $id was processed successfully by Flutter.")
            } else {
                Log.d(TAG, "Alarm rang notification for $id encountered error in Flutter.")
            }
        }

        // 如果开启了语音标签，则播放语音标签
        if (alarmSettings.voiceTagSettings.enable) {
            ttsService = TTSService(
                this,
                alarmSettings.voiceTagSettings.text,
                alarmSettings.voiceTagSettings.volume,
                alarmSettings.voiceTagSettings.speechRate,
                alarmSettings.voiceTagSettings.pitch
            )
        }

        // Set the volume if specified
        if (alarmSettings.volumeSettings.volume != null) {
            volumeService?.setVolume(
                alarmSettings.volumeSettings.volume,
                alarmSettings.volumeSettings.volumeEnforced,
                showSystemUI
            )
        }

        // Request audio focus
        volumeService?.requestAudioFocus()

        // Set up audio completion listener
        audioService?.setOnAudioCompleteListener {
            if (!alarmSettings.loopAudio) {
                vibrationService?.stopVibrating()
                volumeService?.restorePreviousVolume(showSystemUI)
                volumeService?.abandonAudioFocus()
            }
        }

        // 在播放音频之前打开手电筒
        if (alarmSettings.flashlight) {
            flashlightService?.turnOnFlashlight()
        }

        // Play the alarm audio
        audioService?.playAudio(
            id,
            alarmSettings.assetAudioPath,
            alarmSettings.loopAudio,
            alarmSettings.volumeSettings.fadeDuration,
            alarmSettings.volumeSettings.fadeSteps,
            // 这里传入 volume，用于控制铃声暂停，在闹钟挑战时会把 volume 设置为 0
            alarmSettings.volumeSettings.volume
        )

        // Update the list of ringing alarms
        ringingAlarmIds = audioService?.getPlayingMediaPlayersIds() ?: listOf()

        // Start vibration if enabled
        if (alarmSettings.vibrate) {
            Log.d(TAG, "Starting vibration for alarm ID: $id")
            vibrationService?.startVibrating(longArrayOf(0, 500, 500), 1)
        } else {
            Log.d(TAG, "Vibration disabled for alarm ID: $id")
        }

        // Acquire a wake lock to wake up the device
        val wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "app:AlarmWakelockTag")
        wakeLock.acquire(5 * 60 * 1000L) // Acquire for 5 minutes

        return START_STICKY
    }

    private fun startAlarmService(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                id,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(id, notification)
        }
    }

    fun handleStopAlarmCommand(alarmId: Int) {
        if (alarmId == 0) return
        unsaveAlarm(alarmId)
    }

    /**
     * 仅修改响铃中闹钟的表现： 开始或者暂停 ： 铃声/震动/手电筒
     */
    fun editRingingAlarm(settings: EditRingingAlarmSettings) {
        val id = settings.id
        Log.d(TAG, "[EditRingingAlarm] Starting EDIT ID: $id")
        // 1. 震动
        if (settings.vibrate != null) {
            try {
                if (settings.vibrate) {
                    Log.d(TAG, "[EditRingingAlarm] Starting vibration for alarm ID: $id")
                    vibrationService?.startVibrating(longArrayOf(0, 500, 500), 1)
                } else {
                    Log.d(TAG, "[EditRingingAlarm] Stop vibration for alarm ID: $id")
                    vibrationService?.stopVibrating()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[EditRingingAlarm] vibrate error. ${e.message}")
            }
        }

        // 2. 手电筒
        if (settings.flashlight != null) {
            try {
                if (settings.flashlight) {
                    Log.d(TAG, "[EditRingingAlarm] Starting flashlight for alarm ID: $id")
                    flashlightService?.turnOnFlashlight()
                } else {
                    Log.d(TAG, "[EditRingingAlarm] Stop flashlight for alarm ID: $id")
                    flashlightService?.turnOffFlashlight()
                }
            } catch (e: Exception) {
                Log.e(TAG, "[EditRingingAlarm] flashlight error. ${e.message}")
            }
        }

        // 3. 铃声
        if (settings.volumeSettings != null) {
            try {
                // Play the alarm audio
                audioService?.playAudio(
                    id,
                    settings.assetAudioPath!!,
                    settings.loopAudio!!,
                    settings.volumeSettings.fadeDuration,
                    settings.volumeSettings.fadeSteps,
                    // 这里传入 volume，用于控制铃声暂停，在闹钟挑战时会把 volume 设置为 0
                    settings.volumeSettings.volume
                )
            } catch (e: Exception) {
                Log.e(TAG, "[EditRingingAlarm] audio error. ${e.message}")
            }
        }


    }



    private fun unsaveAlarm(id: Int) {
        AlarmStorage(this).unsaveAlarm(id)
        // Notify the plugin about the alarm being stopped.
        AlarmPlugin.alarmTriggerApi?.alarmStopped(id.toLong()) {
            if (it.isSuccess) {
                Log.d(TAG, "Alarm stopped notification for $id was processed successfully by Flutter.")
            } else {
                Log.d(TAG, "Alarm stopped notification for $id encountered error in Flutter.")
            }
        }
        stopAlarm(id)
    }

    private fun stopAlarm(id: Int) {
        AlarmRingingLiveData.instance.update(false)
        try {
            val playingIds = audioService?.getPlayingMediaPlayersIds() ?: listOf()
            ringingAlarmIds = playingIds

            // Safely call methods on 'volumeService' and 'audioService'
            volumeService?.restorePreviousVolume(showSystemUI)
            volumeService?.abandonAudioFocus()

            audioService?.stopAudio(id)

            // Check if media player is empty safely
            if (audioService?.isMediaPlayerEmpty() == true) {
                vibrationService?.stopVibrating()
                stopSelf()
            }

            flashlightService?.turnOffFlashlight()  // 关闭手电筒

            stopForeground(true)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Illegal State: ${e.message}", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error in stopping alarm: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        ringingAlarmIds = listOf()

        audioService?.cleanUp()
        vibrationService?.stopVibrating()
        volumeService?.restorePreviousVolume(showSystemUI)
        volumeService?.abandonAudioFocus()
        ttsService?.cleanup()  // 清理 TTS 资源
        flashlightService?.cleanup()  // 清理 FlashlightService

        AlarmRingingLiveData.instance.update(false)

        stopForeground(true)
        instance = null

        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
