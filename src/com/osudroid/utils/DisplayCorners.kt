package com.osudroid.utils

import android.os.Build
import android.util.DisplayMetrics
import android.view.RoundedCorner
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * The rounded corners of the display, in scene units.
 */
object DisplayCorners {

    /**
     * The radius of the display corner at [position], one of the [RoundedCorner] positions, or 0 if the display
     * has no rounded corner there or does not report it (before Android 12).
     */
    fun getRadius(position: Int): Float {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return 0f
        }

        val display = GlobalManager.getInstance().mainActivity.display ?: return 0f
        val corner = display.getRoundedCorner(position) ?: return 0f

        // The radius is in display pixels, see Config.measureDisplaySize() for the scene scale.
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        return corner.radius * Config.getRES_WIDTH().toFloat() / maxOf(metrics.widthPixels, metrics.heightPixels)
    }

}
