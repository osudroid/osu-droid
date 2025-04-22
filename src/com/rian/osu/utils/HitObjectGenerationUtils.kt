package com.rian.osu.utils

import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.math.Vector2

/**
 * Utilities for [HitObject] generation.
 */
object HitObjectGenerationUtils {
    /**
     * Reflects the position of a [HitObject] in the playfield horizontally.
     *
     * @param hitObject The [HitObject] to reflect.
     */
    fun reflectHorizontallyAlongPlayfield(hitObject: HitObject) {
        // Reflect the position of the hit object.
        hitObject.position = reflectVectorHorizontallyAlongPlayfield(hitObject.position)

        if (hitObject !is Slider) {
            return
        }

        // Reflect the control points of the slider. This will reflect the positions of head and tail circles.
        hitObject.path = SliderPath(
            hitObject.path.pathType,
            hitObject.path.controlPoints.map { Vector2(-it.x, it.y) },
            hitObject.path.expectedDistance
        )

        // Reflect the position of slider ticks and repeats.
        for (i in 1 until hitObject.nestedHitObjects.size - 1) {
            val obj = hitObject.nestedHitObjects[i]

            obj.position = reflectVectorHorizontallyAlongPlayfield(obj.position)
        }
    }

    /**
     * Reflects the position of a [HitObject] in the playfield vertically.
     *
     * @param hitObject The [HitObject] to reflect.
     */
    fun reflectVerticallyAlongPlayfield(hitObject: HitObject) {
        // Reflect the position of the hit object.
        hitObject.position = reflectVectorVerticallyAlongPlayfield(hitObject.position)

        if (hitObject !is Slider) {
            return
        }

        // Reflect the control points of the slider. This will reflect the positions of head and tail circles.
        hitObject.path = SliderPath(
            hitObject.path.pathType,
            hitObject.path.controlPoints.map { Vector2(it.x, -it.y) },
            hitObject.path.expectedDistance
        )

        // Reflect the position of slider ticks and repeats.
        for (i in 1 until hitObject.nestedHitObjects.size - 1) {
            val obj = hitObject.nestedHitObjects[i]

            obj.position = reflectVectorVerticallyAlongPlayfield(obj.position)
        }
    }

    private fun reflectVectorHorizontallyAlongPlayfield(vector: Vector2) = Vector2(512 - vector.x, vector.y)
    private fun reflectVectorVerticallyAlongPlayfield(vector: Vector2) = Vector2(vector.x, 384 - vector.y)
}