package com.osudroid.difficulty.calculator

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.PlayableBeatmap
import com.osudroid.beatmaps.StandardHitWindow
import com.osudroid.beatmaps.StandardPlayableBeatmap
import com.osudroid.beatmaps.hitobjects.HitObject
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.difficulty.StandardDifficultyHitObject
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes
import com.osudroid.difficulty.skills.Skill
import com.osudroid.difficulty.skills.StandardAim
import com.osudroid.difficulty.skills.StandardFlashlight
import com.osudroid.difficulty.skills.StandardSpeed
import com.osudroid.difficulty.skills.StrainSkill
import com.osudroid.mods.Mod
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModRelax
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow
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

        // Aim attributes
        val aimDifficultyValue = aim?.difficultyValue() ?: 0.0

        aimDifficultSliderCount = aim?.countDifficultSliders() ?: 0.0
        aimDifficultStrainCount = aim?.countTopWeightedStrains() ?: 0.0

        aimSliderFactor = if (aimDifficultyValue > 0) {
            StandardRatingCalculator.calculateDifficultyRating(aimNoSlider?.difficultyValue() ?: 0.0) /
                StandardRatingCalculator.calculateDifficultyRating(aimDifficultyValue)
        } else 1.0

        val aimNoSliderTopWeightedSliderCount = aimNoSlider?.countTopWeightedSliders() ?: 0.0
        val aimNoSliderDifficultStrainCount = aimNoSlider?.countTopWeightedStrains() ?: 0.0

        aimTopWeightedSliderFactor =
            aimNoSliderTopWeightedSliderCount / max(1.0, aimNoSliderDifficultStrainCount - aimNoSliderTopWeightedSliderCount)

        // Speed attributes
        val speedDifficultyValue = speed?.difficultyValue() ?: 0.0

        speedNoteCount = speed?.relevantNoteCount() ?: 0.0
        speedDifficultStrainCount = speed?.countTopWeightedStrains() ?: 0.0

        val speedTopWeightedSliderCount = speed?.countTopWeightedSliders() ?: 0.0

        speedTopWeightedSliderFactor =
            speedTopWeightedSliderCount / max(1.0, speedDifficultStrainCount - speedTopWeightedSliderCount)

        // Final rating
        val mechanicalDifficultyRating = calculateMechanicalDifficultyRating(aimDifficultyValue, speedDifficultyValue)
        val ratingCalculator = StandardRatingCalculator(beatmap.mods, beatmap.hitObjects.objects.size, approachRate, overallDifficulty, mechanicalDifficultyRating, aimSliderFactor)

        aimDifficulty = ratingCalculator.computeAimRating(aimDifficultyValue)
        speedDifficulty = ratingCalculator.computeSpeedRating(speedDifficultyValue)
        flashlightDifficulty = ratingCalculator.computeFlashlightRating(flashlight?.difficultyValue() ?: 0.0)

        val baseAimPerformance = StrainSkill.difficultyToPerformance(aimDifficulty)
        val baseSpeedPerformance = StrainSkill.difficultyToPerformance(speedDifficulty)
        val baseFlashlightPerformance = StandardFlashlight.difficultyToPerformance(flashlightDifficulty)

        val basePerformance = (
            baseAimPerformance.pow(1.1) +
            baseSpeedPerformance.pow(1.1) +
            baseFlashlightPerformance.pow(1.1)
        ).pow(1 / 1.1)

        starRating = calculateStarRating(basePerformance)
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

    private fun calculateMechanicalDifficultyRating(aimDifficultyValue: Double, speedDifficultyValue: Double): Double {
        val aimValue = StrainSkill.difficultyToPerformance(StandardRatingCalculator.calculateDifficultyRating(aimDifficultyValue))
        val speedValue = StrainSkill.difficultyToPerformance(StandardRatingCalculator.calculateDifficultyRating(speedDifficultyValue))
        val totalValue = (aimValue.pow(1.1) + speedValue.pow(1.1)).pow(1 / 1.1)

        return calculateStarRating(totalValue)
    }

    private fun calculateStarRating(basePerformance: Double) =
        if (basePerformance > 1e-5)
            cbrt(StandardPerformanceCalculator.FINAL_MULTIPLIER) * STAR_RATING_MULTIPLIER *
                    (cbrt(100000 / 2.0.pow(1 / 1.1) * basePerformance) + 4)
        else 0.0

    companion object {
        private const val STAR_RATING_MULTIPLIER = 0.0265

        /**
         * The epoch time of the last change to difficulty calculation, in milliseconds.
         */
        const val VERSION = 1762003732000

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