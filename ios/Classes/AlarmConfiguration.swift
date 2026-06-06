import AVFoundation

class AlarmConfiguration {
    let settings: AlarmSettings

    var triggerTime: Date?
    var audioPlayer: AVAudioPlayer?
    var timer: Timer?
    var volumeEnforcementTimer: Timer?
    var task: DispatchWorkItem?

    // TTS service for voice tag
    var ttsService: TTSService?
    // Time announcement service
    var timeAnnouncementService: TimeAnnouncementService?
    // Flashlight service
    var flashlightService: FlashlightService?

    init(settings: AlarmSettings) {
        self.settings = settings
    }
}
