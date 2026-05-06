@file:JvmName("Cameras")

package com.reco1l.andengine

import org.andengine.engine.camera.*
import org.andengine.util.math.MathUtils


private val TEMP_COORDINATES = FloatArray(2)


fun Camera.convertSceneToSurfaceCoordinates(x: Float, y: Float, invertYAxis: Boolean = true): FloatArray {
    TEMP_COORDINATES[0] = x
    TEMP_COORDINATES[1] = y
    return convertSceneToSurfaceCoordinates(TEMP_COORDINATES, invertYAxis)
}

fun Camera.convertSceneToSurfaceCoordinates(coordinates: FloatArray, invertYAxis: Boolean = true): FloatArray {

    val (x, y) = coordinates

    val camMinX = xMin
    val camMaxX = xMax
    val camMinY = yMin
    val camMaxY = yMax
    val sw = surfaceWidth.toFloat()
    val sh = surfaceHeight.toFloat()

    var relativeX = (x - camMinX) / (camMaxX - camMinX) * sw
    var relativeY = (y - camMinY) / (camMaxY - camMinY) * sh

    // Adjust for the Y-axis inversion if needed. When working with OpenGL functions the Y-axis is usually already inverted.
    if (invertYAxis) {
        relativeY = (1f - (y - camMinY) / (camMaxY - camMinY)) * sh
    }

    when (rotation) {

        // Nothing to do.
        0f -> Unit

        180f -> {
            relativeX = sw - relativeX
            relativeY = sh - relativeY
        }

        else -> {
            TEMP_COORDINATES[0] = relativeX
            TEMP_COORDINATES[1] = relativeY

            MathUtils.revertRotateAroundCenter(TEMP_COORDINATES, rotation, sw / 2f, sh / 2f)

            relativeX = TEMP_COORDINATES[0]
            relativeY = TEMP_COORDINATES[1]
        }
    }

    coordinates[0] = relativeX
    coordinates[1] = relativeY
    return coordinates
}


fun Camera.convertSurfaceToSceneCoordinates(x: Float, y: Float): FloatArray {
    TEMP_COORDINATES[0] = x
    TEMP_COORDINATES[1] = y
    return convertSurfaceToSceneCoordinates(TEMP_COORDINATES)
}

fun Camera.convertSurfaceToSceneCoordinates(coordinates: FloatArray): FloatArray {

    val relativeX: Float
    val relativeY: Float

    val (x, y) = coordinates

    val sw = surfaceWidth.toFloat()
    val sh = surfaceHeight.toFloat()

    when (rotation) {

        0f -> {
            relativeX = x / sw
            relativeY = y / sh
        }

        180f -> {
            relativeX = 1f - (x / sw)
            relativeY = 1f - (y / sh)
        }

        else -> {
            TEMP_COORDINATES[0] = x
            TEMP_COORDINATES[1] = y

            MathUtils.rotateAroundCenter(TEMP_COORDINATES, rotation, sw / 2f, sh / 2f)

            relativeX = TEMP_COORDINATES[0] / sw
            relativeY = TEMP_COORDINATES[1] / sh
        }
    }

    val camMinX = xMin
    val camMaxX = xMax
    val camMinY = yMin
    val camMaxY = yMax

    coordinates[0] = camMinX + relativeX * (camMaxX - camMinX)
    coordinates[1] = camMinY + relativeY * (camMaxY - camMinY)
    return coordinates
}
