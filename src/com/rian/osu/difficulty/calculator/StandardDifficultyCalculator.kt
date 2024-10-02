package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.StandardHitWindow
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
class StandardDifficultyCalculator : DifficultyCalculator<StandardDifficultyHitObject, StandardDifficultyAttributes>() {
    override val mode = GameMode.Standard
    override val difficultyMultiplier = 0.0675

    override fun createDifficultyAttributes(
        beatmap: Beatmap,
        skills: Array<Skill<StandardDifficultyHitObject>>,
        objects: List<StandardDifficultyHitObject>,
        parameters: DifficultyCalculationParameters?
    ) = StandardDifficultyAttributes().apply {
        mods = parameters?.mods?.slice(parameters.mods.indices) ?: mods

        aimDifficulty = calculateRating(skills[0])
        speedDifficulty = calculateRating(skills[2])
        speedNoteCount = (skills[2] as StandardSpeed).relevantNoteCount()
        flashlightDifficulty = calculateRating(skills[3])

        aimSliderFactor = if (aimDifficulty > 0) calculateRating(skills[1]) / aimDifficulty else 1.0

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

        val speedMultiplier = parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0
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

    override fun createSkills(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?
    ): Array<Skill<StandardDifficultyHitObject>> {
        val mods = parameters?.mods ?: mutableListOf()

        return arrayOf(
            StandardAim(mods, true),
            StandardAim(mods, false),
            StandardSpeed(mods),
            StandardFlashlight(mods)
        )
    }

    override fun createDifficultyHitObjects(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?,
        scope: CoroutineScope?
    ) = mutableListOf<StandardDifficultyHitObject>().apply {
        val clockRate = parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0
        val greatWindow = StandardHitWindow(beatmap.difficulty.od).greatWindow.toDouble() / clockRate

        beatmap.hitObjects.objects.let {
            for (i in 1 until it.size) {
                scope?.ensureActive()

                add(
                    StandardDifficultyHitObject(
                        it[i],
                        it[i - 1],
                        if (i > 1) it[i - 2] else null,
                        clockRate,
                        this,
                        size,
                        greatWindow
                    ).also { d -> d.computeProperties(clockRate, it) }
                )
            }
        }
    }
}