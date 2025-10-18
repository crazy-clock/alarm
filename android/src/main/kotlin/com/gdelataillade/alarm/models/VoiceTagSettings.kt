package com.gdelataillade.alarm.models

import com.gdelataillade.alarm.generated.VoiceTagSettingsWire
import kotlinx.serialization.Serializable

@Serializable
data class VoiceTagSettings(
    val enable: Boolean,
    val text: String,
    val volume: Double,
    val speechRate: Double,
    val pitch: Double,
    val loop: Boolean,
    val loopInterval: Long,
) {
    companion object {
        fun fromWire(e: VoiceTagSettingsWire): VoiceTagSettings {
            return VoiceTagSettings(
                e.enable,
                e.text,
                e.volume,
                e.speechRate,
                e.pitch,
                e.loop,
                e.loopInterval,
            )
        }
    }
}