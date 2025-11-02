package com.gdelataillade.alarm.models

import com.gdelataillade.alarm.generated.TimePressureSettingsWire
import kotlinx.serialization.Serializable

@Serializable
data class TimePressureSettings(
    val enable: Boolean,
    val volume: Double,
    val speechRate: Double,
    val pitch: Double,
    val loop: Boolean,
    val loopInterval: Long,
) {
    companion object {
        fun fromWire(e: TimePressureSettingsWire): TimePressureSettings {
            return TimePressureSettings(
                e.enable,
                e.volume,
                e.speechRate,
                e.pitch,
                e.loop,
                e.loopInterval,
            )
        }
    }
}