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
 * 时间播报服务 - 循环播报当前时间（小时和分钟）
 * 例如："8点30分"
 * 参考 TTSService 实现，支持 loop 和 loopInterval 配置
 */
class TimeAnnouncementService(
    private val context: Context,
    private val audioService: AudioService?,
    private val volume: Double = 0.8,
    private val speechRate: Double = 1.0,
    private val pitch: Double = 1.0,
    private val loop: Boolean = true,
    private val loopInterval: Long = 10000L // 默认10秒
) {

    companion object {
        private const val TAG = "TimeAnnouncementService"
        private const val AUDIO_DUCK_VOLUME = 0.1f // 播报时将音频降低到10%
    }

    private var tts: TextToSpeech? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMusicVolume: Int = 0
    private var isSpeaking = false
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
                    setupAndSpeak()
                }
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun setupAndSpeak() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_MEDIA)
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
                Log.d(TAG, "Time announcement TTS started")
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                if (utteranceId == "TIME_ANNOUNCEMENT") {
                    Log.d(TAG, "Time announcement TTS done")
                    
                    if (loop) {
                        // 如果需要循环播放，延迟指定时间后再次播报
                        handler.postDelayed({
                            announceCurrentTime()
                        }, loopInterval)
                    } else {
                        // 恢复系统音量
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                // 发生错误时恢复音频音量
                restoreAudioVolume()
                // 恢复系统音量
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
                Log.e(TAG, "Time announcement error")
            }
        })

        // 开始第一次播报
        announceCurrentTime()
    }

    /**
     * 播报当前时间
     */
    private fun announceCurrentTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timeText = formatTimeText(hour, minute)
        
        // 播报前降低音频音量
        Log.d(TAG, "Ducking audio volume before announcement")
        duckAudioVolume()
        
        // 开始播报
        speakText(timeText)
        
        // 延迟2秒后恢复音频音量（给TTS播报留出时间）
        handler.postDelayed({
            Log.d(TAG, "Restoring audio volume after announcement")
            restoreAudioVolume()
        }, 2000L)
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
        
        Log.d(TAG, "TTS announcing: $text with volume: $volume, loop: $loop, interval: ${loopInterval}ms")
    }

    /**
     * 降低音频播放音量（播报时间时）
     */
    private fun duckAudioVolume() {
        audioService?.duckVolume(AUDIO_DUCK_VOLUME)
    }

    /**
     * 恢复音频播放音量（播报完成后）
     */
    private fun restoreAudioVolume() {
        audioService?.restoreVolume()
    }

    /**
     * 清理资源
     */
    fun cleanup() {
        Log.d(TAG, "Time announcement cleanup")
        
        // 确保恢复原来的媒体音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
        
        // 移除所有待处理的延迟消息
        handler.removeCallbacksAndMessages(null)
        
        // 恢复音频音量
        restoreAudioVolume()
        
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
}
