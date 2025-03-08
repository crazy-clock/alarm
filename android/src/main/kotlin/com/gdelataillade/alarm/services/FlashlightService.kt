package com.gdelataillade.alarm.services

import android.content.Context
import android.hardware.camera2.CameraManager
import io.flutter.Log

class FlashlightService(private val context: Context) {
    companion object {
        private const val TAG = "FlashlightService"
    }

    private var cameraManager: CameraManager? = null
    private var cameraId: String? = null
    private var flashEnabled: Boolean = false

    init {
        initCamera()
    }

    private fun initCamera() {
        try {
            cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            // 通常后置摄像头才有闪光灯，所以获取第一个摄像头ID
            cameraId = cameraManager?.cameraIdList?.firstOrNull()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize camera manager: ${e.message}")
        }
    }

    fun turnOnFlashlight() {
        try {
            cameraId?.let { id ->
                cameraManager?.setTorchMode(id, true)
                flashEnabled = true
                Log.d(TAG, "Flashlight turned on")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn on flashlight: ${e.message}")
        }
    }

    fun turnOffFlashlight() {
        try {
            if (flashEnabled) {
                cameraId?.let { id ->
                    cameraManager?.setTorchMode(id, false)
                    flashEnabled = false
                    Log.d(TAG, "Flashlight turned off")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to turn off flashlight: ${e.message}")
        }
    }

    fun cleanup() {
        turnOffFlashlight()
        cameraManager = null
        cameraId = null
    }
} 