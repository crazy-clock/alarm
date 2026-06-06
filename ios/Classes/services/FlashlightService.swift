import Foundation
import AVFoundation

class FlashlightService {
    private var isOn = false

    func turnOnFlashlight() {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else {
            NSLog("[FlashlightService] Device does not have a torch")
            return
        }

        do {
            try device.lockForConfiguration()
            try device.setTorchModeOn(level: AVCaptureDevice.maxAvailableTorchLevel)
            device.unlockForConfiguration()
            isOn = true
            NSLog("[FlashlightService] Flashlight turned on")
        } catch {
            NSLog("[FlashlightService] Failed to turn on flashlight: \(error.localizedDescription)")
        }
    }

    func turnOffFlashlight() {
        guard let device = AVCaptureDevice.default(for: .video),
              device.hasTorch else {
            NSLog("[FlashlightService] Device does not have a torch")
            return
        }

        do {
            try device.lockForConfiguration()
            device.torchMode = .off
            device.unlockForConfiguration()
            isOn = false
            NSLog("[FlashlightService] Flashlight turned off")
        } catch {
            NSLog("[FlashlightService] Failed to turn off flashlight: \(error.localizedDescription)")
        }
    }

    func cleanup() {
        turnOffFlashlight()
        NSLog("[FlashlightService] Cleaned up")
    }
}
