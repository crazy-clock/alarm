import Foundation
import AVFoundation

class TTSService: NSObject {
    private var synthesizer: AVSpeechSynthesizer
    private let text: String
    private let volume: Float
    private let speechRate: Float
    private let pitch: Float
    private let loop: Bool
    private let loopInterval: Int64
    private var isCleanedUp = false

    init(text: String, volume: Double, speechRate: Double, pitch: Double, loop: Bool, loopInterval: Int64) {
        self.synthesizer = AVSpeechSynthesizer()
        self.text = text
        self.volume = Float(volume)
        self.speechRate = Float(speechRate)
        self.pitch = Float(pitch)
        self.loop = loop
        self.loopInterval = loopInterval
        super.init()
        self.synthesizer.delegate = self
        speakText()
    }

    private func speakText() {
        guard !isCleanedUp else { return }

        let utterance = AVSpeechUtterance(string: text)
        utterance.volume = volume
        utterance.rate = speechRate
        utterance.pitchMultiplier = pitch

        do {
            let audioSession = AVAudioSession.sharedInstance()
            try audioSession.setCategory(.playback, mode: .default, options: [.duckOthers])
            try audioSession.setActive(true)
        } catch {
            NSLog("[TTSService] Error setting audio session: \(error.localizedDescription)")
        }

        synthesizer.speak(utterance)
        NSLog("[TTSService] Speaking text: \(text), volume: \(volume), loop: \(loop), interval: \(loopInterval)ms")
    }

    func cleanup() {
        isCleanedUp = true
        synthesizer.stopSpeaking(at: .immediate)
        NSLog("[TTSService] Cleaned up")
    }
}

extension TTSService: AVSpeechSynthesizerDelegate {
    func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        guard !isCleanedUp, loop else {
            return
        }

        // Schedule the next speak after loopInterval
        let interval = TimeInterval(loopInterval) / 1000.0
        DispatchQueue.main.asyncAfter(deadline: .now() + interval) { [weak self] in
            self?.speakText()
        }
    }
}
