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
        // 整段包 try-catch：极少数精简版 ROM 没有任何 TTS 引擎时，
        // TextToSpeech() 构造或 setStreamVolume() 都可能直接抛异常
        // (NPE / DeadObjectException 等)。
        // 一旦在这里抛出，外层 AlarmService 的 playAudio() 就拿不到执行机会，
        // 导致主铃声“哑巴”——这是真实的客诉来源，必须兜底。
        try {
            initTTS()
        } catch (t: Throwable) {
            Log.e(TAG, "TTS init crashed (likely no TTS engine on device): ${t.message}", t)
            tts = null
        }
    }

    private fun initTTS() {
        // 保存当前媒体音量（个别 ROM 这步也可能抛，所以也包 try-catch）
        try {
            originalMusicVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        } catch (t: Throwable) {
            Log.e(TAG, "Get original music volume failed: ${t.message}")
            originalMusicVolume = 0
        }

        tts = try {
            TextToSpeech(context) { status ->
                Log.d(TAG, "TTS initializing status: $status")
                try {
                    if (status == TextToSpeech.SUCCESS) {
                        val result = tts?.setLanguage(Locale.CHINA)
                        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                            // 语言数据缺失或不支持 —— 静默降级，不影响主铃声
                            Log.e(TAG, "TTS Language is not supported, voice tag disabled")
                        } else {
                            setupAndSpeak()
                        }
                    } else {
                        // 设备未安装任何 TTS 引擎（OPPO/vivo 精简包常见）
                        // 静默降级：主铃声 + 震动 + 闪光仍正常工作
                        Log.e(TAG, "TTS Initialization failed (no engine), voice tag disabled silently")
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "TTS callback crashed: ${t.message}", t)
                }
            }
        } catch (t: Throwable) {
            // 构造 TextToSpeech 本身抛了
            Log.e(TAG, "Construct TextToSpeech failed: ${t.message}", t)
            null
        }
    }

    private fun setupAndSpeak() {
        try {
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

            // 设置媒体音量为指定音量（个别 ROM 这步也可能抛 SecurityException）
            val maxMusicVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (maxMusicVolume * volume).toInt(), 0)
        } catch (t: Throwable) {
            Log.e(TAG, "setupAndSpeak failed: ${t.message}", t)
            return
        }

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