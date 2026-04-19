package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.PlayableBeatmap
import com.rian.osu.beatmap.StandardHitWindow
import com.rian.osu.beatmap.StandardPlayableBeatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.difficulty.StandardDifficultyHitObject
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes
import com.rian.osu.difficulty.skills.HarmonicSkill
import com.rian.osu.difficulty.skills.Skill
import com.rian.osu.difficulty.skills.StandardAim
import com.rian.osu.difficulty.skills.StandardFlashlight
import com.rian.osu.difficulty.skills.StandardReading
import com.rian.osu.difficulty.skills.StandardSpeed
import com.rian.osu.difficulty.skills.VariableLengthStrainSkill
import com.rian.osu.difficulty.utils.DifficultyCalculationUtils
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModAutopilot
import com.rian.osu.mods.ModFlashlight
import com.rian.osu.mods.ModRelax
import kotlin.math.cbrt
import kotlin.math.max
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating osu!standard star rating.
 */
class StandardDifficultyCalculator : DifficultyCalculator<StandardPlayableBeatmap, StandardDifficultyHitObject, StandardDifficultyAttributes>() {
    override fun createDifficultyAttributes(
        beatmap: PlayableBeatmap,
        skills: Array<Skill<StandardDifficultyHitObject>>,
        objects: Array<StandardDifficultyHitObject>,
        forReplay: Boolean
    ) = StandardDifficultyAttributes().apply {
        mods = beatmap.mods.values.toSet()
        clockRate = beatmap.speedMultiplier.toDouble()
        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount
        approachRate = beatmap.difficulty.ar.toDouble()
        overallDifficulty = beatmap.difficulty.od.toDouble()

        val aim = skills.find<StandardAim> { it.withSliders }
        val aimNoSlider = skills.find<StandardAim> { !it.withSliders }
        val speed = skills.find<StandardSpeed>()
        val flashlight = skills.find<StandardFlashlight>()
        val reading = skills.find<StandardReading>()

        // Aim attributes
        val aimDifficultyValue = aim?.difficultyValue() ?: 0.0
        val aimNoSliderDifficultyValue = aimNoSlider?.difficultyValue() ?: 0.0

        aimDifficultSliderCount = aim?.countDifficultSliders() ?: 0.0
        aimDifficultStrainCount = aim?.countTopWeightedStrains(aimDifficultyValue) ?: 0.0

        aimSliderFactor = if (aimDifficultyValue > 0) {
            StandardRatingCalculator.calculateDifficultyRating(aimNoSliderDifficultyValue) /
                StandardRatingCalculator.calculateDifficultyRating(aimDifficultyValue)
        } else 1.0

        val aimNoSliderTopWeightedSliderCount = aimNoSlider?.countTopWeightedSliders(aimNoSliderDifficultyValue) ?: 0.0
        val aimNoSliderDifficultStrainCount = aimNoSlider?.countTopWeightedStrains(aimNoSliderDifficultyValue) ?: 0.0

        aimTopWeightedSliderFactor =
            aimNoSliderTopWeightedSliderCount / max(1.0, aimNoSliderDifficultStrainCount - aimNoSliderTopWeightedSliderCount)

        // Speed attributes
        val speedDifficultyValue = speed?.difficultyValue() ?: 0.0

        speedNoteCount = speed?.relevantNoteCount() ?: 0.0
        speedDifficultStrainCount = speed?.countTopWeightedObjectDifficulties(speedDifficultyValue) ?: 0.0

        val speedTopWeightedSliderCount = speed?.countTopWeightedSliders(speedDifficultyValue) ?: 0.0

        speedTopWeightedSliderFactor =
            speedTopWeightedSliderCount / max(1.0, speedDifficultStrainCount - speedTopWeightedSliderCount)

        // Reading attributes
        val readingDifficultyValue = reading?.difficultyValue() ?: 0.0

        readingDifficultNoteCount = reading?.countTopWeightedObjectDifficulties(readingDifficultyValue) ?: 0.0

        // Final rating
        val ratingCalculator = StandardRatingCalculator(beatmap.mods, beatmap.hitObjects.objects.size, overallDifficulty)

        aimDifficulty = ratingCalculator.computeAimRating(aimDifficultyValue)
        speedDifficulty = ratingCalculator.computeSpeedRating(speedDifficultyValue)
        flashlightDifficulty = ratingCalculator.computeFlashlightRating(flashlight?.difficultyValue() ?: 0.0)
        readingDifficulty = ratingCalculator.computeReadingRating(readingDifficultyValue)

        val baseAimPerformance = VariableLengthStrainSkill.difficultyToPerformance(aimDifficulty)
        val baseSpeedPerformance = HarmonicSkill.difficultyToPerformance(speedDifficulty)
        val baseFlashlightPerformance = StandardFlashlight.difficultyToPerformance(flashlightDifficulty)
        val baseReadingPerformance = HarmonicSkill.difficultyToPerformance(readingDifficulty)
        val baseCognitionPerformance = sumCognitionDifficulty(baseReadingPerformance, baseFlashlightPerformance)

        val basePerformance = DifficultyCalculationUtils.norm(
            StandardPerformanceCalculator.NORM_EXPONENT,
            baseAimPerformance,
            baseSpeedPerformance,
            baseCognitionPerformance
        )

        starRating = cbrt(basePerformance * StandardPerformanceCalculator.FINAL_MULTIPLIER)
    }

    override fun createSkills(beatmap: StandardPlayableBeatmap, forReplay: Boolean): Array<Skill<StandardDifficultyHitObject>> {
        val mods = beatmap.mods.values
        val skills = mutableListOf<Skill<StandardDifficultyHitObject>>()

        if (ModAutopilot::class !in beatmap.mods) {
            skills.add(StandardAim(mods, true))
            skills.add(StandardAim(mods, false))
        }

        if (ModRelax::class !in beatmap.mods) {
            skills.add(StandardSpeed(mods))
        }

        if (ModFlashlight::class in beatmap.mods) {
            skills.add(StandardFlashlight(mods))
        }

        return skills.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun createDifficultyHitObjects(beatmap: StandardPlayableBeatmap, scope: CoroutineScope?): Array<StandardDifficultyHitObject> {
        if (beatmap.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        val clockRate = beatmap.speedMultiplier.toDouble()
        val objects = beatmap.hitObjects.objects
        val arr = arrayOfNulls<StandardDifficultyHitObject>(objects.size - 1)

        for (i in 1 until objects.size) {
            scope?.ensureActive()

            arr[i - 1] = StandardDifficultyHitObject(
                objects[i],
                objects[i - 1],
                clockRate,
                arr as Array<StandardDifficultyHitObject>,
                i - 1
            ).also { it.computeProperties(clockRate) }
        }

        return arr as Array<StandardDifficultyHitObject>
    }

    override fun createPlayableBeatmap(beatmap: Beatmap, mods: Iterable<Mod>?, scope: CoroutineScope?) =
        beatmap.createStandardPlayableBeatmap(mods, scope)

    companion object {
        /**
         * The epoch time of the last change to difficulty calculation, in milliseconds.
         */
        const val VERSION = 1762003732000

        @JvmStatic
        fun sumCognitionDifficulty(reading: Double, flashlight: Double) = when {
            reading <= 0 -> flashlight
            flashlight <= 0 -> reading
            else -> DifficultyCalculationUtils.norm(
                StandardPerformanceCalculator.NORM_EXPONENT,
                reading,
                flashlight * (flashlight / reading).coerceIn(0.25, 1.0)
            )
        }

        @JvmStatic
        fun calculateRateAdjustedApproachRate(approachRate: Double, clockRate: Double): Double {
            val preempt = BeatmapDifficulty.difficultyRange(
                approachRate,
                HitObject.PREEMPT_MAX,
                HitObject.PREEMPT_MID,
                HitObject.PREEMPT_MIN
            ) / clockRate

            return BeatmapDifficulty.inverseDifficultyRange(
                preempt,
                HitObject.PREEMPT_MAX,
                HitObject.PREEMPT_MID,
                HitObject.PREEMPT_MIN
            )
        }

        @JvmStatic
        fun calculateRateAdjustedOverallDifficulty(overallDifficulty: Double, clockRate: Double): Double {
            val hitWindow = StandardHitWindow(overallDifficulty)
            val greatWindow = hitWindow.greatWindow / clockRate

            return (79.5 - greatWindow) / 6
        }
    }
}