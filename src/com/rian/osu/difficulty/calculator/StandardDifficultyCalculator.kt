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
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModRelax
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow

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

        val baseAimPerformance = (5 * max(1.0, aimDifficulty / 0.0675) - 4).pow(3.0) / 100000
        val baseSpeedPerformance = (5 * max(1.0, speedDifficulty / 0.0675) - 4).pow(3.0) / 100000
        var baseFlashlightPerformance = 0.0
        if (parameters?.mods?.any { it is ModFlashlight } == true) {
            baseFlashlightPerformance = flashlightDifficulty.pow(2.0) * 25.0
        }

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

        val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as ModDifficultyAdjust?
        val ar = difficultyAdjustMod?.ar?.takeUnless { it.isNaN() } ?: beatmap.difficulty.ar
        var preempt = BeatmapDifficulty.difficultyRange(ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)

        if (difficultyAdjustMod?.ar != null) {
            preempt /= parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0
        }

        approachRate = if (preempt > HitObject.PREEMPT_MID) (HitObject.PREEMPT_MAX - preempt) / 120 else (HitObject.PREEMPT_MID - preempt) / 150 + 5

        val greatWindow = StandardHitWindow(beatmap.difficulty.od).greatWindow /
            (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0)

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
        val greatWindow = StandardHitWindow(beatmap.difficulty.od).greatWindow /
            (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0)

        return arrayOf(
            StandardAim(mods, true),
            StandardAim(mods, false),
            StandardSpeed(mods, greatWindow),
            StandardFlashlight(mods)
        )
    }

    override fun createDifficultyHitObjects(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?
    ) = mutableListOf<StandardDifficultyHitObject>().apply {
        val clockRate = parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0

        beatmap.hitObjects.objects.let {
            for (i in 1 until it.size) {
                add(
                    StandardDifficultyHitObject(it[i],
                        it[i - 1],
                        it.getOrNull(i - 2),
                        clockRate,
                        this,
                        size
                    ).also { d -> d.computeProperties(clockRate, it) }
                )
            }
        }
    }
}