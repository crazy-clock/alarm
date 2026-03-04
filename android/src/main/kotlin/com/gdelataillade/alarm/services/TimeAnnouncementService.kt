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

class TimeAnnouncementService(
    private val context: Context,
    private val volume: Double,
    private val speechRate: Double,
    private val pitch: Double,
    private val loop: Boolean = true,
    private val loopInterval: Long = 10000L,
    private val languageTag: String? = null
) {

    companion object {
        private const val TAG = "TimeAnnouncementService"
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
                val locale = parseLanguageTag(languageTag)
                val result = tts?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS Language $languageTag not supported, fallback to default")
                    tts?.language?.let { tts?.setLanguage(it) }
                }
                setupAndSpeak()
            } else {
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun parseLanguageTag(tag: String?): Locale {
        if (tag.isNullOrBlank()) return Locale.SIMPLIFIED_CHINESE
        val parts = tag.split("-")
        return when (parts[0].lowercase()) {
            "zh" -> if (parts.size > 1 && parts[1].equals("TW", ignoreCase = true)) {
                Locale.TRADITIONAL_CHINESE
            } else {
                Locale.SIMPLIFIED_CHINESE
            }
            "en" -> if (parts.size > 1) Locale(parts[0], parts[1]) else Locale.ENGLISH
            "ja" -> Locale.JAPANESE
            "ko" -> Locale.KOREAN
            "hi" -> Locale("hi", "IN")
            else -> try {
                Locale.forLanguageTag(tag)
            } catch (_: Exception) {
                Locale.getDefault()
            }
        }
    }

    private fun setupAndSpeak() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        }

        // 设置 TTS 参数
        tts?.setSpeechRate(speechRate.toFloat())
        tts?.setPitch(pitch.toFloat())

        // 设置媒体音量为指定音量
        val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusicVolume * volume).toInt(), 0)

        // 添加完成监听器
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                if (utteranceId == "TIME_ANNOUNCEMENT_DONE") {
                    if (loop) {
                        // 如果需要循环播放，延迟指定时间后再次播报
                        handler.postDelayed({
                            speakCurrentTime()
                        }, loopInterval)
                    } else {
                        // 恢复原来的媒体音量
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                isSpeaking = false
                // 发生错误时也恢复原来的媒体音量
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
            }
        })

        speakCurrentTime()
    }

    private fun speakCurrentTime() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)
        val timeText = formatTimeText(hour, minute)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle()
            tts?.speak(timeText, TextToSpeech.QUEUE_FLUSH, params, "TIME_ANNOUNCEMENT_DONE")
        } else {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "TIME_ANNOUNCEMENT_DONE"
            tts?.speak(timeText, TextToSpeech.QUEUE_FLUSH, params)
        }
        Log.d(TAG, "TTS started speaking time: $timeText with volume: $volume, loop: $loop, interval: ${loopInterval}ms")
    }

    private fun formatTimeText(hour: Int, minute: Int): String {
        val lang = (languageTag ?: "zh-CN").lowercase()
        return when {
            lang.startsWith("zh") -> if (minute == 0) "${hour}点整" else "${hour}点${minute}分"
            lang.startsWith("ja") -> if (minute == 0) "${hour}時" else "${hour}時${minute}分"
            lang.startsWith("ko") -> if (minute == 0) "${hour}시" else "${hour}시 ${minute}분"
            lang.startsWith("hi") -> if (minute == 0) "$hour बजे" else "$hour बजकर $minute मिनट"
            else -> if (minute == 0) "$hour o'clock" else String.format("%d:%02d", hour, minute)
        }
    }

    fun cleanup() {
        // 确保恢复原来的媒体音量
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalMusicVolume, 0)
        // 移除所有待处理的延迟消息
        handler.removeCallbacksAndMessages(null)
        tts?.stop()
        tts?.shutdown()
        tts = null
        isSpeaking = false
    }

    fun isSpeaking(): Boolean {
        return isSpeaking
    }
}
