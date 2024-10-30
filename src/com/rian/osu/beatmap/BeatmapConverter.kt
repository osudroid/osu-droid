package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.beatmap.sections.BeatmapHitObjects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Converts a [Beatmap] for another [GameMode].
 */
class BeatmapConverter @JvmOverloads constructor(
    /**
     * The [Beatmap] to convert.
     */
    @JvmField
    val beatmap: Beatmap,

    /**
     * The [CoroutineScope] to use for coroutines.
     */
    private val scope: CoroutineScope? = null
) {
    /**
     * Converts [Beatmap].
     *
     * @return The converted [Beatmap].
     */
    fun convert() = beatmap.clone().also {
        // Shallow clone isn't enough to ensure we don't mutate some beatmap properties unexpectedly.
        it.difficulty = beatmap.difficulty.clone()

        scope?.ensureActive()

        it.hitObjects = convertHitObjects().also { b -> b.objects.sortBy { o -> o.startTime } }
    }

    private fun convertHitObjects() = BeatmapHitObjects().also {
        beatmap.hitObjects.objects.forEach { obj ->
            scope?.ensureActive()

            it.add(convertHitObject(obj))
        }
    }

    private fun convertHitObject(hitObject: HitObject) =
        when (hitObject) {
            is HitCircle -> HitCircle(
                hitObject.startTime,
                hitObject.position,
                hitObject.isNewCombo,
                hitObject.comboOffset
            )

            is Slider -> Slider(
                hitObject.startTime,
                hitObject.position,
                hitObject.repeatCount,
                hitObject.path,
                hitObject.isNewCombo,
                hitObject.comboOffset,
                hitObject.nodeSamples
            ).also {
                // Prior to v8, speed multipliers don't adjust for how many ticks are generated over the same distance.
                // This results in more (or less) ticks being generated in <v8 maps for the same time duration.
                it.tickDistanceMultiplier =
                    if (beatmap.formatVersion < 8) 1.0 / beatmap.controlPoints.difficulty.controlPointAt(it.startTime).speedMultiplier
                    else 1.0
                it.generateTicks = hitObject.generateTicks
            }

            is Spinner -> Spinner(hitObject.startTime, hitObject.endTime, hitObject.isNewCombo)

            else -> throw IllegalArgumentException("Invalid type of hit object")
        }.also {
            it.samples = hitObject.samples
            it.auxiliarySamples = hitObject.auxiliarySamples
        }
}