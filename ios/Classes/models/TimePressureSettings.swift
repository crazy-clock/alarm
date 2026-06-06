import Foundation

struct TimePressureSettings: Codable {
    let enable: Bool
    let volume: Double
    let speechRate: Double
    let pitch: Double
    let loop: Bool
    let loopInterval: Int64
    let languageTag: String?

    static func from(wire: TimePressureSettingsWire) -> TimePressureSettings {
        return TimePressureSettings(
            enable: wire.enable,
            volume: wire.volume,
            speechRate: wire.speechRate,
            pitch: wire.pitch,
            loop: wire.loop,
            loopInterval: wire.loopInterval,
            languageTag: wire.languageTag
        )
    }
}
