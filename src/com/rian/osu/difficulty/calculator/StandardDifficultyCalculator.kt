package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.PlayableBeatmap
import com.rian.osu.beatmap.StandardHitWindow
import com.rian.osu.beatmap.StandardPlayableBeatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes
import com.rian.osu.difficulty.skills.Skill
import com.rian.osu.difficulty.skills.StandardAim
import com.rian.osu.difficulty.skills.StandardFlashlight
import com.rian.osu.difficulty.skills.StandardSpeed
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModRelax
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating osu!standard star rating.
 */
class StandardDifficultyCalculator : DifficultyCalculator<StandardPlayableBeatmap, StandardDifficultyHitObject, StandardDifficultyAttributes>() {
    override val difficultyMultiplier = 0.0675

    override fun createDifficultyAttributes(
        beatmap: PlayableBeatmap,
        skills: Array<Skill<StandardDifficultyHitObject>>,
        objects: Array<StandardDifficultyHitObject>
    ) = StandardDifficultyAttributes().apply {
        mods = beatmap.mods?.toList() ?: mods

        aimDifficulty = calculateRating(skills[0])
        speedDifficulty = calculateRating(skills[2])
        speedNoteCount = (skills[2] as StandardSpeed).relevantNoteCount()
        flashlightDifficulty = calculateRating(skills[3])

        aimSliderFactor = if (aimDifficulty > 0) calculateRating(skills[1]) / aimDifficulty else 1.0

        aimDifficultStrainCount = (skills[0] as StandardAim).countDifficultStrains()
        speedDifficultStrainCount = (skills[2] as StandardSpeed).countDifficultStrains()

        if (mods.any { it is ModRelax }) {
            aimDifficulty *= 0.9
            speedDifficulty = 0.0
            flashlightDifficulty *= 0.7
        }

        val baseAimPerformance = (5 * max(1.0, aimDifficulty / 0.0675) - 4).pow(3) / 100000
        val baseSpeedPerformance = (5 * max(1.0, speedDifficulty / 0.0675) - 4).pow(3) / 100000
        val baseFlashlightPerformance = if (mods.any { it is ModFlashlight }) flashlightDifficulty.pow(2) * 25 else 0.0

        val basePerformance = (
                baseAimPerformance.pow(1.1) +
                        baseSpeedPerformance.pow(1.1) +
                        baseFlashlightPerformance.pow(1.1)
                ).pow(1 / 1.1)

        // Document for formula derivation:
        // https://docs.google.com/document/d/10DZGYYSsT_yjz2Mtp6yIJld0Rqx4E-vVHupCqiM4TNI/edit
        starRating =
            if (basePerformance > 1e-5)
                cbrt(StandardPerformanceCalculator.FINAL_MULTIPLIER) * 0.027 *
                        (cbrt(100000 / 2.0.pow(1 / 1.1) * basePerformance) + 4)
            else 0.0

        val speedMultiplier = beatmap.overallSpeedMultiplier.toDouble()
        val preempt = BeatmapDifficulty.difficultyRange(beatmap.difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN) / speedMultiplier

        approachRate = BeatmapDifficulty.inverseDifficultyRange(preempt, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)

        // Weird casts, but necessary for difficulty calculation parity
        val greatWindow = StandardHitWindow(beatmap.difficulty.od).greatWindow.toDouble() / speedMultiplier

        overallDifficulty = StandardHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat()).toDouble()
        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount
    }

    override fun createSkills(beatmap: StandardPlayableBeatmap): Array<Skill<StandardDifficultyHitObject>> {
        val mods = beatmap.mods?.toList() ?: emptyList()

        return arrayOf(
            StandardAim(mods, true),
            StandardAim(mods, false),
            StandardSpeed(mods),
            StandardFlashlight(mods)
        )
    }

    @Suppress("UNCHECKED_CAST")
    override fun createDifficultyHitObjects(beatmap: StandardPlayableBeatmap, scope: CoroutineScope?): Array<StandardDifficultyHitObject> {
        val clockRate = beatmap.overallSpeedMultiplier.toDouble()
        val greatWindow = StandardHitWindow(beatmap.difficulty.od).greatWindow.toDouble() / clockRate

        val objects = beatmap.hitObjects.objects
        val arr = arrayOfNulls<StandardDifficultyHitObject>(objects.size - 1)

        for (i in 1 until objects.size) {
            scope?.ensureActive()

            arr[i - 1] = StandardDifficultyHitObject(
                objects[i],
                objects[i - 1],
                if (i > 1) objects[i - 2] else null,
                clockRate,
                arr as Array<StandardDifficultyHitObject>,
                i - 1,
                greatWindow
            ).also { it.computeProperties(clockRate, objects) }
        }

        return arr as Array<StandardDifficultyHitObject>
    }

    override fun createPlayableBeatmap(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?,
        scope: CoroutineScope?
    ) = beatmap.createStandardPlayableBeatmap(parameters?.mods, parameters?.customSpeedMultiplier ?: 1f, scope)
}