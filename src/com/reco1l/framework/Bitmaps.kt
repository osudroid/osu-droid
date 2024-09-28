@file:JvmName("Bitmaps")

package com.reco1l.framework

import android.graphics.*
import com.reco1l.osu.*
import kotlin.random.*




/**
 * Converts the bitmap to a safe for work version where the content is black and the bounds are red.
 */
@JvmOverloads
fun Bitmap?.paintBitmap(boundSize: Int = 3, boundColor: Int = Random.nextColor(), fillColor: Int = 0x33000000): Bitmap? {

    if (this == null) {
        return null
    }

    val width: Int = getWidth()
    val height: Int = getHeight()

    for (x in 0 until width) {
        for (y in 0 until height) {

            if (x < boundSize || x > width - boundSize || y < boundSize || y > height - boundSize) {
                setPixel(x, y, boundColor)
            } else {
                setPixel(x, y, fillColor)
            }
        }
    }

    return this
}