package com.gdelataillade.alarm.services

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.AudioManager
import com.gdelataillade.alarm.models.VolumeFadeStep
import java.util.concurrent.ConcurrentHashMap
import java.util.Timer
import java.util.TimerTask
import io.flutter.Log
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class AudioService(private val context: Context) {
    companion object {
        private const val TAG = "AudioService"
    }

    private val mediaPlayers = ConcurrentHashMap<Int, MediaPlayer>()
    private val timers = ConcurrentHashMap<Int, Timer>()

    // 追踪 volume=0 的静音闹铃（挑战模式下铃声被暂停，但闹铃仍在"响铃"状态）
    private val silentAlarmIds = ConcurrentHashMap.newKeySet<Int>()

    private var onAudioComplete: (() -> Unit)? = null
    private var originalVolume: Float = 1.0f
    private var isDucked: Boolean = false

    fun setOnAudioCompleteListener(listener: () -> Unit) {
        onAudioComplete = listener
    }

    fun isMediaPlayerEmpty(): Boolean {
        // 同时检查真实播放的 MediaPlayer 和静音闹铃
        return mediaPlayers.isEmpty() && silentAlarmIds.isEmpty()
    }

    fun getPlayingMediaPlayersIds(): List<Int> {
        // 只返回真正在播放（isPlaying=true）的 MediaPlayer id，过滤掉空/已停止的实例
        val playingIds = mediaPlayers.filter { (_, mp) ->
            try { mp.isPlaying } catch (e: IllegalStateException) { false }
        }.keys.toList()
        // 合并静音闹铃 id（volume=0 时闹铃仍处于"响铃"状态，需要被 isRinging 感知到）
        return (playingIds + silentAlarmIds.toList()).distinct()
    }

    fun playAudio(
        id: Int,
        filePath: String,
        loopAudio: Boolean,
        fadeDuration: Duration?,
        fadeSteps: List<VolumeFadeStep>,
        volume: Double?
    ) {
        stopAudio(id) // Stop and release any existing MediaPlayer and Timer for this ID
        if (volume != null && volume <= 0) {
            // volume=0 表示挑战模式下铃声被静音，但闹铃仍在响铃状态
            // 不放入 mediaPlayers（避免空 MediaPlayer 污染状态），改用 silentAlarmIds 追踪
            Log.d(TAG, "AudioService playAudio, volume = 0, tracking as silent alarm id=$id")
            silentAlarmIds.add(id)
            return
        }

        val baseAppFlutterPath = context.filesDir.parent?.plus("/app_flutter/")
        val adjustedFilePath = when {
            filePath.startsWith("assets/") -> "flutter_assets/$filePath"
            !filePath.startsWith("/") -> baseAppFlutterPath + filePath
            else -> filePath
        }

        try {
            MediaPlayer().apply {
                // 设置音频流类型为警报流
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                setAudioAttributes(audioAttributes)
                when {
                    adjustedFilePath.startsWith("flutter_assets/") -> {
                        // It's an asset file
                        val assetManager = context.assets
                        val descriptor = assetManager.openFd(adjustedFilePath)
                        setDataSource(
                            descriptor.fileDescriptor,
                            descriptor.startOffset,
                            descriptor.length
                        )
                    }

                    else -> {
                        // Handle local files and adjusted paths
                        setDataSource(adjustedFilePath)
                    }
                }

                prepare()
                isLooping = loopAudio
                start()

                setOnCompletionListener {
                    if (!loopAudio) {
                        onAudioComplete?.invoke()
                    }
                }

                mediaPlayers[id] = this

                if (fadeSteps.isNotEmpty()) {
                    val timer = Timer(true)
                    timers[id] = timer
                    startStaircaseFadeIn(this, fadeSteps, timer)
                } else if (fadeDuration != null) {
                    val timer = Timer(true)
                    timers[id] = timer
                    startFadeIn(this, fadeDuration, timer)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e(TAG, "Error playing audio: $e")
        }
    }

    fun stopAudio(id: Int) {
        timers[id]?.cancel()
        timers.remove(id)

        mediaPlayers[id]?.apply {
            try {
                if (isPlaying) stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "stopAudio: MediaPlayer illegal state for id=$id: ${e.message}")
            }
            reset()
            release()
        }
        mediaPlayers.remove(id)

        // 同步清理静音闹铃追踪
        silentAlarmIds.remove(id)
    }

    private fun startFadeIn(mediaPlayer: MediaPlayer, duration: Duration, timer: Timer) {
        val maxVolume = 1.0f
        val fadeDuration = duration.inWholeMilliseconds
        val fadeInterval = 100L
        val numberOfSteps = fadeDuration / fadeInterval
        val deltaVolume = maxVolume / numberOfSteps
        var volume = 0.0f

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (!mediaPlayer.isPlaying) {
                    cancel()
                    return
                }

                mediaPlayer.setVolume(volume, volume)
                volume += deltaVolume

                if (volume >= maxVolume) {
                    mediaPlayer.setVolume(maxVolume, maxVolume)
                    cancel()
                }
            }
        }, 0, fadeInterval)
    }

    private fun startStaircaseFadeIn(
        mediaPlayer: MediaPlayer,
        steps: List<VolumeFadeStep>,
        timer: Timer
    ) {
        val fadeIntervalMillis = 100L
        var currentStep = 0

        timer.schedule(object : TimerTask() {
            override fun run() {
                if (!mediaPlayer.isPlaying) {
                    cancel()
                    return
                }

                val currentTime = (currentStep * fadeIntervalMillis).milliseconds
                val nextIndex = steps.indexOfFirst { it.time >= currentTime }

                if (nextIndex < 0) {
                    cancel()
                    return
                }

                val nextVolume = steps[nextIndex].volume
                var currentVolume = nextVolume

                if (nextIndex > 0) {
                    val prevTime = steps[nextIndex - 1].time
                    val nextTime = steps[nextIndex].time
                    val nextRatio = (currentTime - prevTime) / (nextTime - prevTime)

                    val prevVolume = steps[nextIndex - 1].volume
                    currentVolume = nextVolume * nextRatio + prevVolume * (1 - nextRatio)
                }

                mediaPlayer.setVolume(currentVolume.toFloat(), currentVolume.toFloat())
                currentStep++
            }
        }, 0, fadeIntervalMillis)
    }

    fun cleanUp() {
        timers.values.forEach(Timer::cancel)
        timers.clear()

        mediaPlayers.values.forEach { mediaPlayer ->
            try {
                if (mediaPlayer.isPlaying) mediaPlayer.stop()
            } catch (e: IllegalStateException) {
                Log.w(TAG, "cleanUp: MediaPlayer illegal state: ${e.message}")
            }
            mediaPlayer.reset()
            mediaPlayer.release()
        }
        mediaPlayers.clear()

        // 同步清理静音闹铃追踪
        silentAlarmIds.clear()
    }

    /**
     * 降低音频音量（用于时间播报时降低背景音乐/闹钟铃声）
     */
    fun duckVolume(duckVolume: Float) {
        if (isDucked) {
            Log.d(TAG, "Audio already ducked, skipping")
            return
        }
        
        originalVolume = 1.0f
        isDucked = true
        
        mediaPlayers.values.forEach { mediaPlayer ->
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.setVolume(duckVolume, duckVolume)
                    Log.d(TAG, "Audio volume ducked to $duckVolume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ducking volume: $e")
            }
        }
    }

    /**
     * 恢复音频音量
     */
    fun restoreVolume() {
        if (!isDucked) {
            return
        }
        
        isDucked = false
        
        mediaPlayers.values.forEach { mediaPlayer ->
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.setVolume(originalVolume, originalVolume)
                    Log.d(TAG, "Audio volume restored to $originalVolume")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error restoring volume: $e")
            }
        }
    }
}