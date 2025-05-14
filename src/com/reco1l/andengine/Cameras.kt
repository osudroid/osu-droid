package com.reco1l.andengine

import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.util.*


private val TEMP_COORDINATES = FloatArray(2)


fun Camera.getScreenSpaceCoordinates(x: Float, y: Float): FloatArray {
    TEMP_COORDINATES[0] = x
    TEMP_COORDINATES[1] = y
    return getScreenSpaceCoordinates(TEMP_COORDINATES)
}

fun Camera.getScreenSpaceCoordinates(coordinates: FloatArray): FloatArray {

    var (x, y) = coordinates

    val relativeX = (x - minX) / (maxX - minX)
    val relativeY = 1f - (y - minY) / (maxY - minY)

    x = relativeX * surfaceWidth
    y = relativeY * surfaceHeight

    if (rotation != 0f) {

        if (rotation == 180f) {
            coordinates[0] = surfaceHeight - y
            coordinates[1] = surfaceWidth - x
        } else {
            MathUtils.revertRotateAroundCenter(coordinates, -rotation, surfaceWidth.shr(1).toFloat(), surfaceHeight.shr(1).toFloat())
        }
    } else {
        coordinates[0] = x
        coordinates[1] = y
    }

    return coordinates
}
