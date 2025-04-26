package com.gdelataillade.alarm.models

import com.gdelataillade.alarm.generated.EditRingingAlarmSettingsWire
import com.gdelataillade.alarm.generated.VoiceTagSettingsWire
import kotlinx.serialization.Serializable

@Serializable
data class EditRingingAlarmSettings(
    val id: Int,
    val volumeSettings: VolumeSettings?,
    val assetAudioPath: String?,
    val loopAudio: Boolean?,
    val vibrate: Boolean?,
    val flashlight: Boolean?,
) {
    companion object {
        fun fromWire(e: EditRingingAlarmSettingsWire): EditRingingAlarmSettings {
            return EditRingingAlarmSettings(
                e.id.toInt(),
                e.volumeSettings?.let { VolumeSettings.fromWire(it) },
                e.assetAudioPath,
                e.loopAudio,
                e.vibrate,
                e.flashlight
            )
        }
    }
}