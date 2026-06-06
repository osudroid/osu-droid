package com.osudroid.difficulty.calculator

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.PlayableBeatmap
import com.osudroid.beatmaps.StandardPlayableBeatmap
import com.osudroid.difficulty.StandardDifficultyHitObject
import com.osudroid.difficulty.attributes.StandardDifficultyAttributes
import com.osudroid.difficulty.skills.HarmonicSkill
import com.osudroid.difficulty.skills.Skill
import com.osudroid.difficulty.skills.StandardAim
import com.osudroid.difficulty.skills.StandardFlashlight
import com.osudroid.difficulty.skills.StandardReading
import com.osudroid.difficulty.skills.StandardSpeed
import com.osudroid.difficulty.skills.VariableLengthStrainSkill
import com.osudroid.difficulty.utils.DifficultyCalculationUtils
import com.osudroid.mods.Mod
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModRelax
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating osu!standard star rating.
 */
class StandardDifficultyCalculator : DifficultyCalculator<StandardPlayableBeatmap, StandardDifficultyHitObject, StandardDifficultyAttributes>() {
    override fun createDifficultyAttributes(
        beatmap: Beatmap,
        playableBeatmap: PlayableBeatmap,
        skills: Array<Skill<StandardDifficultyHitObject>>,
        objects: Array<StandardDifficultyHitObject>,
        forReplay: Boolean
    ) = StandardDifficultyAttributes().apply {
        mods = playableBeatmap.mods.values.toSet()
        clockRate = playableBeatmap.speedMultiplier.toDouble()
        maxCombo = playableBeatmap.maxCombo
        hitCircleCount = playableBeatmap.hitObjects.circleCount
        sliderCount = playableBeatmap.hitObjects.sliderCount
        spinnerCount = playableBeatmap.hitObjects.spinnerCount
        approachRate = playableBeatmap.difficulty.ar.toDouble()
        overallDifficulty = playableBeatmap.difficulty.od.toDouble()

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
            calculateAimDifficultyRating(aimNoSliderDifficultyValue) / calculateAimDifficultyRating(aimDifficultyValue)
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
        aimDifficulty = calculateAimDifficultyRating(aimDifficultyValue)
        speedDifficulty = calculateDifficultyRating(speedDifficultyValue)
        flashlightDifficulty = calculateDifficultyRating(flashlight?.difficultyValue() ?: 0.0)
        readingDifficulty = calculateDifficultyRating(readingDifficultyValue)

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
            skills.add(StandardFlashlight(mods, beatmap.hitObjects.objects.size))
        }

        skills.add(StandardReading(mods, beatmap.speedMultiplier.toDouble(), beatmap.hitObjects.objects))

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

    private fun calculateAimDifficultyRating(difficultyValue: Double) = difficultyValue.pow(0.63) * 0.02275
    private fun calculateDifficultyRating(difficultyValue: Double) = sqrt(difficultyValue) * 0.0675

    companion object {
        /**
         * The epoch time of the last change to difficulty calculation, in milliseconds.
         */
        const val VERSION = 1780707920000

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
    }
}