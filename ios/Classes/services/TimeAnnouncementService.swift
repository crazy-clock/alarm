import Foundation
import AVFoundation

class TimeAnnouncementService: NSObject {
    private var synthesizer: AVSpeechSynthesizer
    private let volume: Float
    private let speechRate: Float
    private let pitch: Float
    private let loop: Bool
    private let loopInterval: Int64
    private let languageTag: String?
    private var isCleanedUp = false

    init(volume: Double, speechRate: Double, pitch: Double, loop: Bool, loopInterval: Int64, languageTag: String?) {
        self.synthesizer = AVSpeechSynthesizer()
        self.volume = Float(volume)
        self.speechRate = Float(speechRate)
        self.pitch = Float(pitch)
        self.loop = loop
        self.loopInterval = loopInterval
        self.languageTag = languageTag
        super.init()
        self.synthesizer.delegate = self
        speakCurrentTime()
    }

    private func speakCurrentTime() {
        guard !isCleanedUp else { return }

        let calendar = Calendar.current
        let now = Date()
        let hour = calendar.component(.hour, from: now)
        let minute = calendar.component(.minute, from: now)

        let timeText = formatTimeText(hour: hour, minute: minute)

        let utterance = AVSpeechUtterance(string: timeText)
        utterance.volume = volume
        utterance.rate = speechRate
        utterance.pitchMultiplier = pitch

        // Set language based on languageTag
        if let tag = languageTag {
            if #available(iOS 16, *) {
                let locale = Locale(identifier: tag)
                if locale.language.languageCode != nil {
                    utterance.voice = AVSpeechSynthesisVoice(language: tag)
                }
            } else {
                utterance.voice = AVSpeechSynthesisVoice(language: tag)
            }
        }

        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .default, options: [.duckOthers])
            try audioSession.setActive(true)
        } catch {
            NSLog("[TimeAnnouncementService] Error setting audio session: \(error.localizedDescription)")
        }

        synthesizer.speak(utterance)
        NSLog("[TimeAnnouncementService] Speaking time: \(timeText), volume: \(volume), loop: \(loop), interval: \(loopInterval)ms")
    }

    private func formatTimeText(hour: Int, minute: Int) -> String {
        let lang = (languageTag ?? "zh-CN").lowercased()
        if lang.hasPrefix("zh") {
            return minute == 0 ? "\(hour)点整" : "\(hour)点\(minute)分"
        } else if lang.hasPrefix("ja") {
            return minute == 0 ? "\(hour)時" : "\(hour)時\(minute)分"
        } else if lang.hasPrefix("ko") {
            return minute == 0 ? "\(hour)시" : "\(hour)시 \(minute)분"
        } else if lang.hasPrefix("hi") {
            return minute == 0 ? "\(hour) बजे" : "\(hour) बजकर \(minute) मिनट"
        } else {
            return minute == 0 ? "\(hour) o'clock" : String(format: "%d:%02d", hour, minute)
        }
    }

    func cleanup() {
        isCleanedUp = true
        synthesizer.stopSpeaking(at: .immediate)
        NSLog("[TimeAnnouncementService] Cleaned up")
    }
}

extension TimeAnnouncementService: AVSpeechSynthesizerDelegate {
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        guard !isCleanedUp, loop else {
            return
        }

        let interval = TimeInterval(loopInterval) / 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + interval) { [weak self] in
            self?.speakCurrentTime()
        }
    }
}
