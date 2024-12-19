package com.acivev.utils

import android.app.ActivityManager
import android.content.Context
import android.content.pm.ConfigurationInfo
import android.util.Log
import androidx.core.content.getSystemService
import com.acivev.utils.OpenGLVersionUtil.supportsGLES3

/**
 * Utility class to check if the device is low-end or high-end.
 * The checks are based on OpenGL version and RAM.
 * Additional checks can be added as needed.
 */
object DevicePerformanceUtil {
    fun isLowEndDevice(context: Context): Boolean {
        // Check for GLES3 support
        if (!supportsGLES3(context)) {
            Log.w("DevicePerformance", "Low-end device detected")
            return true
        } else {
            Log.i("DevicePerformance", "High-end device detected")
        }

        // Additional checks like RAM or CPU can go here
        val totalRam = getTotalRAM(context)
        return totalRam < 2000 // High-end device < 2GB RAM
    }

    private fun getTotalRAM(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.totalMem / (1024 * 1024) // Convert bytes to MB
    }
}

object OpenGLVersionUtil {
    /**
     * Checks if the device supports OpenGL ES 3.0 or higher.
     *
     * @param context The application context.
     * @return true if the device supports GLES 3.0 or higher, false otherwise.
     */
    fun supportsGLES3(context: Context): Boolean {
        val activityManager = context.getSystemService<ActivityManager>()
        if (activityManager != null) {
            val configInfo: ConfigurationInfo = activityManager.deviceConfigurationInfo
            return configInfo.reqGlEsVersion >= 0x30000 // Check if GLES 3.0 or higher
        }
        return false // Default to false if unable to retrieve info
    }
}