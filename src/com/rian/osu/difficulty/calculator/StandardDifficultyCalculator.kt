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
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModRelax
import com.rian.osu.utils.ModUtils
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
        mods: Iterable<Mod>,
        skills: Array<Skill<StandardDifficultyHitObject>>,
        objects: Array<StandardDifficultyHitObject>
    ) = StandardDifficultyAttributes().apply {
        this.mods = mods.toSet()

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

        val clockRate = ModUtils.calculateRateWithMods(mods)
        val preempt = BeatmapDifficulty.difficultyRange(beatmap.difficulty.ar.toDouble(), HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN) / clockRate

        approachRate = BeatmapDifficulty.inverseDifficultyRange(preempt, HitObject.PREEMPT_MAX, HitObject.PREEMPT_MID, HitObject.PREEMPT_MIN)

        // Weird casts, but necessary for difficulty calculation parity
        val greatWindow = StandardHitWindow(beatmap.difficulty.od).greatWindow.toDouble() / clockRate

        overallDifficulty = StandardHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat()).toDouble()
        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount
    }

    override fun createSkills(beatmap: Beatmap, mods: Iterable<Mod>) = arrayOf<Skill<StandardDifficultyHitObject>>(
        StandardAim(mods, true),
        StandardAim(mods, false),
        StandardSpeed(mods),
        StandardFlashlight(mods)
    )

    @Suppress("UNCHECKED_CAST")
    override fun createDifficultyHitObjects(beatmap: Beatmap, mods: Iterable<Mod>, scope: CoroutineScope?): Array<StandardDifficultyHitObject> {
        val clockRate = ModUtils.calculateRateWithMods(mods).toDouble()
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
            )
        }

        return arr as Array<StandardDifficultyHitObject>
    }
}