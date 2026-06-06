import Foundation

struct EditRingingAlarmSettings {
    let id: Int
    let volumeSettings: VolumeSettings?
    let assetAudioPath: String?
    let loopAudio: Bool?
    let vibrate: Bool?
    let flashlight: Bool?
    let voiceTagSettings: VoiceTagSettings?
    let timePressureSettings: TimePressureSettings?

    static func from(wire: EditRingingAlarmSettingsWire) -> EditRingingAlarmSettings {
        return EditRingingAlarmSettings(
            id: Int(truncatingIfNeeded: wire.id),
            volumeSettings: wire.volumeSettings.map { VolumeSettings.from(wire: $0) },
            assetAudioPath: wire.assetAudioPath,
            loopAudio: wire.loopAudio,
            vibrate: wire.vibrate,
            flashlight: wire.flashlight,
            voiceTagSettings: wire.voiceTagSettings.map { VoiceTagSettings.from(wire: $0) },
            timePressureSettings: wire.timePressureSettings.map { TimePressureSettings.from(wire: $0) }
        )
    }
}
