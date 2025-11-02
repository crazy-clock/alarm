package com.gdelataillade.alarm.services

import android.os.Build
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.media.AudioManager
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import io.flutter.Log
import java.util.Locale
import java.util.Calendar

/**
 * 时间播报服务 - 每10秒播报当前时间（小时和分钟）
 * 例如："8点30分"
 */
class TimeAnnouncementService(
    private val context: Context,
    private val volume: Double = 0.8,
    private val speechRate: Double = 1.0,
    private val pitch: Double = 1.0
) {

    companion object {
        private const val TAG = "TimeAnnouncementService"
        private const val ANNOUNCEMENT_INTERVAL = 10000L // 10秒
    }

    private var tts: TextToSpeech? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMusicVolume: Int = 0
    private var isSpeaking = false
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    init {
        initTTS()
    }

    private fun initTTS() {
        // 保存当前媒体音量
        originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        tts = TextToSpeech(context) { status ->
            Log.d(TAG, "TTS initializing status: $status")
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language is not supported")
                } else {
                    setupTTS()
                    startAnnouncement()
                }
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun setupTTS() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ALARM) // 设置为闹钟音频流
                .build()
            tts?.setAudioAttributes(audioAttributes)
        }

        // 设置 TTS 参数
        tts?.setSpeechRate(speechRate.toFloat())
        tts?.setPitch(pitch.toFloat())

        // 添加完成监听器
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
                Log.d(TAG, "TTS speaking started")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                Log.d(TAG, "TTS speaking done")
                // 恢复原来的媒体音量
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                Log.e(TAG, "TTS speaking error")
                // 发生错误时也恢复原来的媒体音量
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
            }
        })
    }

    /**
     * 开始时间播报
     */
    private fun startAnnouncement() {
        if (isRunning) {
            return
        }
        isRunning = true
        scheduleNextAnnouncement()
    }

    /**
     * 调度下一次播报
     */
    private fun scheduleNextAnnouncement() {
        if (!isRunning) {
            return
        }

        handler.postDelayed({
            announceCurrentTime()
            scheduleNextAnnouncement() // 继续调度下一次
        }, ANNOUNCEMENT_INTERVAL)
    }

    /**
     * 播报当前时间
     */
    private fun announceCurrentTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeText = formatTimeText(hour, minute)
        speakText(timeText)
    }

    /**
     * 格式化时间文本为中文
     * 例如：8:30 -> "8点30分"
     */
    private fun formatTimeText(hour: Int, minute: Int): String {
        return if (minute == 0) {
            "${hour}点整"
        } else {
            "${hour}点${minute}分"
        }
    }

    /**
     * 使用TTS播报文本
     */
    private fun speakText(text: String) {
        // 保存当前媒体音量
        originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)

        // 设置媒体音量为指定音量
        val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusicVolume * volume).toInt(), 0)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TIME_ANNOUNCEMENT")
        } else {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "TIME_ANNOUNCEMENT"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
        
        Log.d(TAG, "TTS announcing: $text with volume: $volume")
    }

    /**
     * 停止时间播报
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacksAndMessages(null)
        Log.d(TAG, "Time announcement stopped")
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        Log.d(TAG, "Time announcement cleanup")
        stop()
        
        // 确保恢复原来的媒体音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
        
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking = false
    }

    /**
     * 是否正在播报
     */
    fun isSpeaking(): Boolean {
        return isSpeaking
    }

    /**
     * 是否正在运行
     */
    fun isRunning(): Boolean {
        return isRunning
    }
}
