package com.reco1l.legacy.graphics

import kotlin.math.atan2

data class Line(
    /**
     * Begin point of the line.
     */
    val startPoint: Vector3,

    /**
     * End point of the line.
     */
    val endPoint: Vector3
) {

    /**
     * The direction of the second point from the first.
     */
    val theta: Float
        get() = atan2(endPoint.y - startPoint.y, endPoint.x - startPoint.x)

    /**
     * The direction of this line.
     */
    val direction: Vector3
        get() = endPoint - startPoint

    /**
     * The normalized direction of this line.
     */
    val directionNormalized: Vector3
        get()
        {
            val direction = direction
            return Vector3(
                direction.x / direction.length,
                direction.y / direction.length,
                direction.z / direction.length
            )
        }

    /**
     * Orthogonal direction of this line.
     */
    val orthogonalDirection: Vector3
        get()
        {
            val dir = directionNormalized
            return Vector3(-dir.y, dir.x, dir.z)
        }

}