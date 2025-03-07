package com.gdelataillade.alarm.services

import android.os.Build
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.media.AudioManager
import android.media.AudioAttributes
import io.flutter.Log
import java.util.Locale

class TTSService(
    private val context: Context,
    private val text: String,
    private val volume: Double,
    private val speechRate: Double,
    private val pitch: Double
//    private val onComplete: () -> Unit,
//    private val onError: () -> Unit
) {

    companion object {
        private const val TAG = "TTSService"
    }

    private var tts: TextToSpeech? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var originalMusicVolume: Int = 0

    init {
        initTTS()
    }

    private fun initTTS() {
        // 保存当前媒体音量
        originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)

        tts = TextToSpeech(context) { status ->
            Log.d(TAG, "TTS initializing status: $status")
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 语言数据缺失或不支持
                    Log.e(TAG, "TTS Language is not supported")
                } else {
                    setupAndSpeak()
                }
            } else {
                // TextToSpeech 初始化失败
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    private fun setupAndSpeak() {
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

        // 设置媒体音量为指定音量
        val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, (maxMusicVolume * volume).toInt(), 0)

        // 添加完成监听器，在 TTS 播放完成后恢复原来的媒体音量
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}

            override fun onDone(utteranceId: String?) {
                if (utteranceId == "TTS_DONE") {
                    // 恢复原来的媒体音量
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalMusicVolume, 0)
                }
            }

            override fun onError(utteranceId: String?) {
                // 发生错误时也恢复原来的媒体音量
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalMusicVolume, 0)
            }
        })

        // 播放 TTS
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TTS_DONE")
        } else {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "TTS_DONE"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
        Log.d(TAG, "TTS started speaking with volume: $volume")
    }

    fun cleanup() {
        // 确保恢复原来的媒体音量
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, originalMusicVolume, 0)
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
} 