package com.gdelataillade.alarm.services

import android.os.Build
import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
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
//    private var ttsInitialized = false
//    private val pendingMethodCalls = ArrayList<Runnable>()
//    private var ttsStatus: Int? = null

    init {
        initTTS()
    }

    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            Log.d(TAG, "TTS initializing status: $status")
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // 语言数据缺失或不支持
                    Log.e(TAG, "TTS Language is not supported")
                } else {
                    // 播放音频
                    tts?.setSpeechRate(speechRate.toFloat()) //倍速
                    tts?.setPitch(pitch.toFloat()) //音调
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        // 调用需要 API 21 的方法
                        val params = Bundle()
                        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.toFloat()) // 设置音量
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "TTS_DONE")
                    } else {
                        // 在低版本设备上使用替代方案
                        val params = HashMap<String, String>()
                        params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "TTS_DONE"
                        params[TextToSpeech.Engine.KEY_PARAM_VOLUME] = volume.toString() // 设置音量
                        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
                    }
                    Log.d(TAG, "TTS Hello, welcome to the world of TextToSpeech!")
                }
            } else {
                // TextToSpeech 初始化失败
                Log.e(TAG, "TTS Initialization failed")
            }
        }
    }

    fun cleanup() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
} 