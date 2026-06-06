import Foundation

struct VoiceTagSettings: Codable {
    let enable: Bool
    let text: String
    let volume: Double
    let speechRate: Double
    let pitch: Double
    let loop: Bool
    let loopInterval: Int64

    static func from(wire: VoiceTagSettingsWire) -> VoiceTagSettings {
        return VoiceTagSettings(
            enable: wire.enable,
            text: wire.text,
            volume: wire.volume,
            speechRate: wire.speechRate,
            pitch: wire.pitch,
            loop: wire.loop,
            loopInterval: wire.loopInterval
        )
    }
}
