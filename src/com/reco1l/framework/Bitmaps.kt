@file:JvmName("Bitmaps")

package com.reco1l.framework

import android.graphics.*
import com.reco1l.osu.*
import kotlin.random.*




/**
 * Converts the bitmap to a safe for work version where the content is black and the bounds are red.
 */
@JvmOverloads
fun Bitmap?.paintBitmap(boundSize: Int = if (keepTexturesShapeInNoTexturesMode) 1 else 3, boundColor: Int = Random.nextColor(), fillColor: Int = 0x33000000, preserveShape: Boolean = keepTexturesShapeInNoTexturesMode): Bitmap? {

    if (this == null) {
        return null
    }

    val width = getWidth()
    val height = getHeight()

    if (!preserveShape) {
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


    val isOuterBorder = { x: Int, y: Int ->
        x < boundSize || x >= width - boundSize || y < boundSize || y >= height - boundSize
    }

    for (x in 0 until width) {
        for (y in 0 until height) {
            val pixel = getPixel(x, y)

            if (pixel == Color.TRANSPARENT) {
                continue
            }

            var isBorder = false

            if (isOuterBorder(x, y)) {
                isBorder = true
            } else {
                for (dx in -boundSize..boundSize) {

                    for (dy in -boundSize..boundSize) {

                        if (dx == 0 && dy == 0) continue

                        val nx = x + dx
                        val ny = y + dy

                        if (nx in 0 until width && ny in 0 until height) {

                            if (getPixel(nx, ny) == Color.TRANSPARENT) {
                                isBorder = true
                                break
                            }
                        }
                    }

                    if (isBorder) {
                        break
                    }
                }
            }

            if (isBorder) {
                setPixel(x, y, boundColor)
            } else {
                setPixel(x, y, fillColor)
            }
        }
    }

    return this
}