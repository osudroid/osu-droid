package com.acivev

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.acivev.utils.VibrationSupport
import com.acivev.utils.VibratorCheckUtils
import ru.nsu.ccfit.zuev.osu.Config

/**
 * Manage vibration effects for Android 7+.
 * It supports different vibration patterns and intensities.
 * - Vibration intensity can be set dynamically.
 */
object VibratorManager {

    private var vibrator: Vibrator? = null
    private var hasVibrationSupport = false

    /**
     * Whether vibration for circles is enabled.
     */
    var isCircleVibrationEnabled = false
        set(value) {
            field = value
            Config.setBoolean("vibrationCircle", value)
        }

    /**
     * Whether vibration for sliders is enabled.
     */
    var isSliderVibrationEnabled = false
        set(value) {
            field = value
            Config.setBoolean("vibrationSlider", value)
        }

    /**
     * Whether vibration for spinners is enabled.
     */
    var isSpinnerVibrationEnabled = false
        set(value) {
            field = value
            Config.setBoolean("vibrationSpinner", value)
        }

    /**
     * The intensity of the vibration.
     */
    var intensity = 127
        set(value) {
            field = value
            Config.setInt("seekBarVibrateIntensity", value)
        }

    /**
     * Initializes the vibrator and checks supported vibration features.
     * MUST be called before using any vibration function.
     */
    fun init(context: Context) {
        intensity = Config.getInt("seekBarVibrateIntensity", intensity)
        isCircleVibrationEnabled = Config.getBoolean("vibrationCircle", isCircleVibrationEnabled)
        isSliderVibrationEnabled = Config.getBoolean("vibrationSlider", isSliderVibrationEnabled)
        isSpinnerVibrationEnabled = Config.getBoolean("vibrationSpinner", isSpinnerVibrationEnabled)

        vibrator = VibratorCheckUtils.getVibrator(context)
        hasVibrationSupport = VibratorCheckUtils.checkVibratorSupport(context) != VibrationSupport.NO_VIBRATION
    }

    /**
     * Cancels any ongoing vibration.
     */
    fun cancel() { vibrator?.cancel() }

    /**
     * Vibrates for a given duration with intensity.
     * @param milliseconds The number of milliseconds to vibrate.
     */
    fun vibrateFor(milliseconds: Long) {
        if (isCircleVibrationEnabled || isSliderVibrationEnabled || isSpinnerVibrationEnabled) {
            if (!hasVibrationSupport || vibrator == null) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(milliseconds, intensity)) // Amplitudes based on intensity
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(milliseconds)
            }
        } else {
            cancel()
        }
    }

    /**
     * Vibrates with a pattern and intensities.
     * @param pattern an array of longs of times for which to turn the vibrator on or off.
     * @param repeat the index into pattern at which to repeat, or -1 if you don't want to repeat.
     */
    fun vibrateFor(pattern: LongArray, repeat: Int = -1) {
        if (isCircleVibrationEnabled || isSliderVibrationEnabled || isSpinnerVibrationEnabled) {
            if (!hasVibrationSupport || vibrator == null) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val amplitudes = IntArray(pattern.size) { intensity } // Apply intensity to all pattern segments
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, repeat))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, repeat)
            }
        } else {
            cancel()
        }
    }


    /**
     * Provides haptic vibration for a click action using intensity.
     */
    fun circleVibration() {
        if (isCircleVibrationEnabled) {
            if (!hasVibrationSupport || vibrator == null) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, intensity)) // Amplitudes based on intensity
            } else {
                // Fallback for older APIs
                vibrateFor(50)
            }
        } else {
            cancel()
        }
    }

    /**
     * Provides haptic vibration for a slider or scroll action using intensity.
     */
    fun sliderVibration() {
        if (isSliderVibrationEnabled) {
            if (!hasVibrationSupport || vibrator == null) return

            val pattern = longArrayOf(0, 30, 20, 50) // Time intervals
            val amplitudes = intArrayOf(0, intensity, 0, intensity) // Amplitudes based on intensity

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                // Fallback for older APIs
                vibrateFor(pattern, -1)
            }
        } else {
            cancel()
        }
    }

    /**
     * Provides haptic vibration for a spinner using intensity.
     */
    fun spinnerVibration() {
        if (isSpinnerVibrationEnabled) {
            if (!hasVibrationSupport || vibrator == null) return

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val pattern = longArrayOf(0, 20, 10, 30) // Time intervals
                val amplitudes = intArrayOf(0, intensity, 0, intensity) // Amplitudes based on intensity

                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
            } else {
                // Fallback for older APIs
                vibrateFor(longArrayOf(0, 20, 10, 30), -1)
            }
        } else {
            cancel()
        }
    }
}