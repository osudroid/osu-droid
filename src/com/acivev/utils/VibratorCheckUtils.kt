package com.acivev.utils

import android.content.Context
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

object VibratorCheckUtils {

    fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(VibratorManager::class.java)
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator?
        }
    }

    fun hasVibrationSupport(context: Context): Boolean {
        val vibrator = getVibrator(context)
        return vibrator?.hasVibrator() ?: false
    }

    fun hasHapticSupport(context: Context): Boolean {
        val vibrator = getVibrator(context)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.hasAmplitudeControl() ?: false
        } else {
            // Devices below API 26 cannot control vibration amplitude, assume no haptic feedback
            false
        }
    }

    fun checkVibratorSupport(context: Context): VibrationSupport {
        return when {
            hasVibrationSupport(context) -> {
                Log.i("VibratorCheckUtils", "Device supports basic vibration but no haptic feedback.")
                VibrationSupport.BASIC_VIBRATION
            }
            hasHapticSupport(context) -> {
                Log.i("VibratorCheckUtils", "Device supports basic haptic feedback.")
                VibrationSupport.ADVANCED_HAPTIC
            }
            else -> {
                Log.w("VibratorCheckUtils", "Device does not support vibration or haptic feedback.")
                VibrationSupport.NO_VIBRATION
            }
        }
    }
}

enum class VibrationSupport {
    ADVANCED_HAPTIC,  // Device supports amplitude control (haptic feedback)
    BASIC_VIBRATION,  // Device only has a simple on/off vibration
    NO_VIBRATION      // Device does not support vibration
}
