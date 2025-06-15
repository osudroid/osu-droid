package com.reco1l.andengine

import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.util.*


private val TEMP_COORDINATES = FloatArray(2)


fun Camera.convertSceneToSurfaceCoordinates(x: Float, y: Float, invertYAxis: Boolean = true): FloatArray {
    TEMP_COORDINATES[0] = x
    TEMP_COORDINATES[1] = y
    return convertSceneToSurfaceCoordinates(TEMP_COORDINATES, invertYAxis)
}

fun Camera.convertSceneToSurfaceCoordinates(coordinates: FloatArray, invertYAxis: Boolean = true): FloatArray {

    val (x, y) = coordinates

    var relativeX = (x - minX) / (maxX - minX) * surfaceWidth
    var relativeY = (y - minY) / (maxY - minY) * surfaceHeight

    // Adjust for the Y-axis inversion if needed. When working with OpenGL functions the Y-axis is usually already inverted.
    if (invertYAxis) {
        relativeY = (1f - (y - minY) / (maxY - minY)) * surfaceHeight
    }

    when (rotation) {

        // Nothing to do.
        0f -> Unit

        180f -> {
            relativeX = surfaceWidth - relativeX
            relativeY = surfaceHeight - relativeY
        }

        else -> {
            TEMP_COORDINATES[0] = relativeX
            TEMP_COORDINATES[1] = relativeY

            MathUtils.revertRotateAroundCenter(TEMP_COORDINATES, rotation, (surfaceWidth shr 1).toFloat(), (surfaceHeight shr 1).toFloat())

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

    when (rotation) {

        0f -> {
            relativeX = x / surfaceWidth
            relativeY = y / surfaceHeight
        }

        180f -> {
            relativeX = 1f - (x / surfaceWidth)
            relativeY = 1f - (y / surfaceHeight)
        }

        else -> {
            TEMP_COORDINATES[0] = x
            TEMP_COORDINATES[1] = y

            MathUtils.rotateAroundCenter(TEMP_COORDINATES, rotation, surfaceWidth / 2f, surfaceHeight / 2f)

            relativeX = TEMP_COORDINATES[0] / surfaceWidth
            relativeY = TEMP_COORDINATES[1] / surfaceHeight
        }
    }

    coordinates[0] = minX + relativeX * (maxX - minX)
    coordinates[1] = minY + relativeY * (maxY - minY)
    return coordinates
}
