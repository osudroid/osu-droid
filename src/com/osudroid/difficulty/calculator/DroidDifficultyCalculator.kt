package com.osudroid.difficulty.calculator

import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.DroidPlayableBeatmap
import com.osudroid.beatmaps.PlayableBeatmap
import com.osudroid.difficulty.DroidDifficultyHitObject
import com.osudroid.difficulty.attributes.DroidDifficultyAttributes
import com.osudroid.difficulty.attributes.HighStrainSection
import com.osudroid.difficulty.skills.DroidAim
import com.osudroid.difficulty.skills.DroidFlashlight
import com.osudroid.difficulty.skills.DroidReading
import com.osudroid.difficulty.skills.DroidRhythm
import com.osudroid.difficulty.skills.DroidTap
import com.osudroid.difficulty.skills.HarmonicSkill
import com.osudroid.difficulty.skills.Skill
import com.osudroid.difficulty.skills.VariableLengthStrainSkill
import com.osudroid.difficulty.utils.DifficultyCalculationUtils
import com.osudroid.difficulty.utils.DroidScoreUtils
import com.osudroid.mods.Mod
import com.osudroid.mods.ModAutopilot
import com.osudroid.mods.ModFlashlight
import com.osudroid.mods.ModFreezeFrame
import com.osudroid.mods.ModPrecise
import com.osudroid.mods.ModRelax
import com.osudroid.mods.ModReplayV6
import com.osudroid.mods.ModScoreV2
import com.osudroid.mods.ModTraceable
import com.osudroid.utils.ModUtils
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating osu!droid star rating.
 */
class DroidDifficultyCalculator : DifficultyCalculator<DroidPlayableBeatmap, DroidDifficultyHitObject, DroidDifficultyAttributes>() {
    override val difficultyAdjustmentMods = super.difficultyAdjustmentMods +
        setOf(ModPrecise::class, ModScoreV2::class, ModFreezeFrame::class, ModReplayV6::class, ModTraceable::class)

    private val minimumSectionObjectCount = 5
    private val threeFingerStrainThreshold = 175.0

    override fun createDifficultyAttributes(
        beatmap: Beatmap,
        playableBeatmap: PlayableBeatmap,
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>,
        forReplay: Boolean
    ) = DroidDifficultyAttributes().apply {
        mods = playableBeatmap.mods.values.toSet()
        clockRate = playableBeatmap.speedMultiplier.toDouble()

        maxCombo = playableBeatmap.maxCombo
        hitCircleCount = playableBeatmap.hitObjects.circleCount
        sliderCount = playableBeatmap.hitObjects.sliderCount
        spinnerCount = playableBeatmap.hitObjects.spinnerCount
        overallDifficulty = playableBeatmap.difficulty.od.toDouble()
        maximumScore = beatmap.calculateMaximumScore(playableBeatmap.mods) + DroidScoreUtils.calculateMaximumSpinnerBonus(playableBeatmap)

        populateAimAttributes(skills, forReplay)
        populateTapAttributes(skills, objects, forReplay)
        populateRhythmAttributes(skills)
        populateFlashlightAttributes(skills)
        populateReadingAttributes(skills)

        val baseAimPerformance = VariableLengthStrainSkill.difficultyToPerformance(aimDifficulty)
        val baseTapPerformance = HarmonicSkill.difficultyToPerformance(tapDifficulty)
        val baseFlashlightPerformance = DroidFlashlight.difficultyToPerformance(flashlightDifficulty)
        val baseReadingPerformance = HarmonicSkill.difficultyToPerformance(readingDifficulty)

        val baseCognitionPerformance = sumCognitionDifficulty(baseReadingPerformance, baseFlashlightPerformance)

        val basePerformance = DifficultyCalculationUtils.norm(
            DroidPerformanceCalculator.NORM_EXPONENT,
            baseAimPerformance,
            baseTapPerformance,
            baseCognitionPerformance
        )

        starRating = cbrt(basePerformance * DroidPerformanceCalculator.FINAL_MULTIPLIER)
    }

    override fun createSkills(beatmap: DroidPlayableBeatmap, forReplay: Boolean): Array<Skill<DroidDifficultyHitObject>> {
        val mods = beatmap.mods.values
        val skills = mutableListOf<Skill<DroidDifficultyHitObject>>()

        if (ModAutopilot::class !in beatmap.mods) {
            skills.add(DroidAim(mods, true))
            skills.add(DroidAim(mods, false))
        }

        if (ModRelax::class !in beatmap.mods) {
            // Tap skills depend on rhythm skill, so we put it first
            skills.add(DroidRhythm(mods))
            skills.add(DroidTap(mods, true))

            if (forReplay) {
                skills.add(DroidTap(mods, false))
            }
        }

        if (ModFlashlight::class in beatmap.mods) {
            skills.add(DroidFlashlight(mods, beatmap.hitObjects.objects.size))
        }

        skills.add(DroidReading(mods, beatmap.speedMultiplier.toDouble(), beatmap.hitObjects.objects))

        return skills.toTypedArray()
    }

    @Suppress("UNCHECKED_CAST")
    override fun createDifficultyHitObjects(beatmap: DroidPlayableBeatmap, scope: CoroutineScope?): Array<DroidDifficultyHitObject> {
        if (beatmap.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        val clockRate = beatmap.speedMultiplier.toDouble()
        val objects = beatmap.hitObjects.objects
        val arr = arrayOfNulls<DroidDifficultyHitObject>(objects.size)

        for (i in objects.indices) {
            scope?.ensureActive()

            arr[i] = DroidDifficultyHitObject(
                objects[i],
                if (i > 0) objects[i - 1] else null,
                clockRate,
                arr as Array<DroidDifficultyHitObject>,
                i - 1
            ).also { it.computeProperties(clockRate) }
        }

        return arr as Array<DroidDifficultyHitObject>
    }

    override fun createPlayableBeatmap(beatmap: Beatmap, mods: Iterable<Mod>?, scope: CoroutineScope?) =
        beatmap.createDroidPlayableBeatmap(mods, scope)

    private fun DroidDifficultyAttributes.populateAimAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>,
        forReplay: Boolean
    ) {
        val aim = skills.find<DroidAim> { it.withSliders } ?: return
        val aimNoSlider = skills.find<DroidAim> { !it.withSliders } ?: return

        val aimDifficultyValue = aim.difficultyValue()
        val aimNoSliderDifficultyValue = aimNoSlider.difficultyValue()

        aimDifficulty = calculateAimDifficultyRating(aimDifficultyValue)
        aimDifficultStrainCount = aim.countTopWeightedStrains(aimDifficultyValue)
        aimDifficultSliderCount = aim.countDifficultSliders()

        aimSliderFactor = if (aimDifficulty > 0) {
            calculateAimDifficultyRating(aimNoSliderDifficultyValue) /
                    calculateAimDifficultyRating(aimDifficultyValue)
        } else 1.0

        val aimNoSliderTopWeightedSliderCount = aimNoSlider.countTopWeightedSliders(aimNoSliderDifficultyValue)
        val aimNoSliderDifficultStrainCount = aimNoSlider.countTopWeightedStrains(aimNoSliderDifficultyValue)

        aimTopWeightedSliderFactor =
            aimNoSliderTopWeightedSliderCount / max(1.0, aimNoSliderDifficultStrainCount - aimNoSliderTopWeightedSliderCount)

        if (!forReplay) {
            return
        }

        val velocitySum = aim.sliderVelocities.sumOf { s -> s.difficultyRating }

        for (slider in aim.sliderVelocities) {
            val difficultyRating = slider.difficultyRating / velocitySum

            // Only consider sliders that are fast enough.
            if (difficultyRating > 0.02) {
                difficultSliders.add(slider.copy(difficultyRating = difficultyRating))
            }
        }

        difficultSliders.sortByDescending { s -> s.difficultyRating }

        // Take the top 15% most difficult sliders.
        difficultSliders.dropLastWhile { difficultSliders.size > ceil(0.15 * sliderCount) }
    }

    private fun DroidDifficultyAttributes.populateTapAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>,
        forReplay: Boolean
    ) {
        val tap = skills.find<DroidTap> { it.considerCheesability } ?: return

        val tapDifficultyValue = tap.difficultyValue()

        tapDifficulty = calculateDifficultyRating(tapDifficultyValue)
        tapDifficultStrainCount = tap.countTopWeightedObjectDifficulties(tapDifficulty)
        speedNoteCount = tap.relevantNoteCount()

        val tapTopWeightedSliderCount = tap.countTopWeightedSliders(tapDifficultyValue)

        tapTopWeightedSliderFactor = tapTopWeightedSliderCount / max(1.0, tapDifficultStrainCount - tapTopWeightedSliderCount)

        if (!forReplay) {
            return
        }

        val tapNoCheese = skills.find<DroidTap> { !it.considerCheesability } ?: return

        var inSpeedSection = false
        var firstSpeedObjectIndex = 0
        val clockRate = ModUtils.calculateRateWithMods(mods)

        for (i in 2 until objects.size) {
            val current = objects[i]
            val prev = objects[i - 1]
            val prevPrev = objects[i - 2]

            val currentStrain = tapNoCheese.objectDifficulties[i]

            if (!inSpeedSection && currentStrain >= threeFingerStrainThreshold) {
                inSpeedSection = true
                firstSpeedObjectIndex = i
                continue
            }

            val currentDelta = (current.startTime - prev.startTime) / clockRate
            val prevDelta = (prev.startTime - prevPrev.startTime) / clockRate

            val deltaRatio = min(prevDelta, currentDelta) / max(prevDelta, currentDelta)

            if (
                inSpeedSection &&
                (currentStrain < threeFingerStrainThreshold ||
                    // Stop speed section on slowing down 1/2 rhythm change or anything slower.
                    (prevDelta < currentDelta && deltaRatio <= 0.5) ||
                    // Don't forget to manually add the last section, which would otherwise be ignored.
                    i == objects.size - 1)
            ) {
                val lastSpeedObjectIndex = i - if (i == objects.size - 1) 0 else 1
                inSpeedSection = false

                // Ignore sections that don't meet object count requirement.
                if (i - firstSpeedObjectIndex < minimumSectionObjectCount) {
                    continue
                }

                possibleThreeFingeredSections.add(HighStrainSection(
                    firstSpeedObjectIndex,
                    lastSpeedObjectIndex,
                    calculateThreeFingerSummedStrain(tapNoCheese.objectDifficulties.subList(firstSpeedObjectIndex, lastSpeedObjectIndex))
                ))
            }
        }
    }

    private fun DroidDifficultyAttributes.populateRhythmAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>
    ) {
        val rhythm = skills.find<DroidRhythm>() ?: return

        rhythmDifficulty = calculateDifficultyRating(rhythm.difficultyValue())
    }

    private fun DroidDifficultyAttributes.populateFlashlightAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>
    ) {
        val flashlight = skills.find<DroidFlashlight>() ?: return

        flashlightDifficulty = sqrt(flashlight.difficultyValue()) * 0.18
    }

    private fun DroidDifficultyAttributes.populateReadingAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>
    ) {
        val reading = skills.find<DroidReading>() ?: return
        val readingDifficultyValue = reading.difficultyValue()

        readingDifficulty = calculateDifficultyRating(readingDifficultyValue)
        readingDifficultNoteCount = reading.countTopWeightedObjectDifficulties(readingDifficultyValue)
    }

    private fun calculateAimDifficultyRating(difficultyValue: Double) = difficultyValue.pow(0.63) * 0.02275
    private fun calculateDifficultyRating(difficultyValue: Double) = sqrt(difficultyValue) * 0.0675

    private fun calculateThreeFingerSummedStrain(strains: List<Double>) =
        strains.fold(0.0) { acc, d -> acc + d / threeFingerStrainThreshold }.pow(0.75)

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