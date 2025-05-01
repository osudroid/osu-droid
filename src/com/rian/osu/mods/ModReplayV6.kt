package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.utils.CircleSizeCalculator
import kotlin.collections.forEach
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * Represents the Replay V6 mod.
 *
 * Some behavior of beatmap parsing was changed in replay version 7. More specifically, object stacking behavior now
 * matches osu!stable and osu!lazer.
 *
 * This [Mod] is meant to reapply the stacking behavior prior to replay version 7 to a [Beatmap] that was played in
 * replays recorded in version 6 and older for replayability and difficulty calculation.
 */
class ModReplayV6 : Mod(), IModApplicableToBeatmap {
    override val name = "Replay V6"
    override val acronym = "RV6"
    override val description = "Applies the old object stacking behavior to a beatmap."
    override val type = ModType.System

    override val isUserPlayable = false

    override fun applyToBeatmap(beatmap: Beatmap, scope: CoroutineScope?) {
        if (beatmap.mode != GameMode.Droid) {
            return
        }

        val objects = beatmap.hitObjects.objects

        if (objects.isEmpty()) {
            return
        }

        // Reset stacking
        objects.forEach {
            scope?.ensureActive()

            it.difficultyStackHeight = 0
            it.gameplayStackHeight = 0
        }

        val droidDifficultyScale =
            CircleSizeCalculator.standardScaleToOldDroidDifficultyScale(objects[0].difficultyScale, true)

        val maxDeltaTime = 2000 * beatmap.general.stackLeniency

        for (i in 0 until objects.size - 1) {
            scope?.ensureActive()

            val current = objects[i]
            val next = objects[i + 1]

            current.stackOffsetMultiplier = 4f
            next.stackOffsetMultiplier = 4f

            if (current is HitCircle && next.startTime - current.startTime < maxDeltaTime) {
                val distanceSquared = next.position.getDistance(current.position).pow(2)

                if (distanceSquared < droidDifficultyScale) {
                    next.difficultyStackHeight = current.difficultyStackHeight + 1
                }

                if (distanceSquared < current.gameplayScale) {
                    next.gameplayStackHeight = current.gameplayStackHeight + 1
                }
            }
        }
    }

    override fun deepCopy() = ModReplayV6()
}