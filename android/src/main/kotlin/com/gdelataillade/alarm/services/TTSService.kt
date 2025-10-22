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

class TTSService(
    private val context: Context,
    private val text: String,
    private val volume: Double,
    private val speechRate: Double,
    private val pitch: Double,
    private val loop: Boolean = false,
    private val loopInterval: Long = 1000L // 新增循环间隔，默认1秒
//    private val onComplete: () -> Unit,
//    private val onError: () -> Unit
) {

    companion object {
        private const val TAG = "TTSService"
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
        val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusicVolume * volume).toInt(), 0)

        // 添加完成监听器，在 TTS 播放完成后恢复原来的媒体音量
        tts?.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking = false
                if (utteranceId == "TTS_DONE") {
                    if (loop) {
                        // 如果需要循环播放，延迟指定时间后再次调用speak方法
                        handler.postDelayed({
                            speakText()
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

        speakText()
    }

    private fun speakText() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TTS_DONE")
        } else {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "TTS_DONE"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        }
        Log.d(TAG, "TTS started speaking with volume: $volume, loop: $loop, interval: ${loopInterval}ms")
    }

    fun cleanup() {
        Log.d(TAG, "TTS cleanup now")
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