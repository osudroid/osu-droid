package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.PlayableBeatmap
import com.rian.osu.beatmap.StandardHitWindow
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.difficulty.attributes.HighStrainSection
import com.rian.osu.difficulty.skills.*
import com.rian.osu.mods.*
import com.rian.osu.utils.ModUtils
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
        setOf(ModPrecise::class, ModScoreV2::class, ModFreezeFrame::class, ModReplayV6::class)

    private val minimumSectionObjectCount = 5
    private val threeFingerStrainThreshold = 175.0

    override fun createDifficultyAttributes(
        beatmap: PlayableBeatmap,
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>,
        forReplay: Boolean
    ) = DroidDifficultyAttributes().apply {
        mods = beatmap.mods.values.toSet()
        clockRate = beatmap.speedMultiplier.toDouble()

        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount

        // Weird cast of greatWindow, but necessary for difficulty calculation parity
        val greatWindow = beatmap.hitWindow.greatWindow.toDouble() / clockRate

        overallDifficulty = StandardHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat()).toDouble()

        populateAimAttributes(skills, forReplay)
        populateTapAttributes(skills, objects, forReplay)
        populateRhythmAttributes(skills)
        populateFlashlightAttributes(skills)
        populateReadingAttributes(skills)

        if (ModRelax::class in beatmap.mods) {
            aimDifficulty *= 0.9
            tapDifficulty = 0.0
            rhythmDifficulty = 0.0
            flashlightDifficulty *= 0.7
            readingDifficulty *= 0.7
        }

        if (ModAutopilot::class in beatmap.mods) {
            aimDifficulty = 0.0
            flashlightDifficulty *= 0.3
            readingDifficulty *= 0.4
        }

        val baseAimPerformance = DroidAim.difficultyToPerformance(aimDifficulty)
        val baseTapPerformance = StrainSkill.difficultyToPerformance(tapDifficulty)
        val baseFlashlightPerformance = DroidFlashlight.difficultyToPerformance(flashlightDifficulty)
        val baseReadingPerformance = DroidReading.difficultyToPerformance(readingDifficulty)

        val basePerformance = (
            baseAimPerformance.pow(1.1) +
            baseTapPerformance.pow(1.1) +
            baseFlashlightPerformance.pow(1.1) +
            baseReadingPerformance.pow(1.1)
        ).pow(1 / 1.1)

        // Document for formula derivation:
        // https://docs.google.com/document/d/10DZGYYSsT_yjz2Mtp6yIJld0Rqx4E-vVHupCqiM4TNI/edit
        starRating =
            if (basePerformance > 1e-5) 0.027 * (cbrt(100000 / 2.0.pow(1 / 1.1) * basePerformance) + 4)
            else 0.0
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
            skills.add(DroidTap(mods, true, 50.0))

            if (forReplay) {
                skills.add(DroidTap(mods, false))
            }
        }

        if (ModFlashlight::class in beatmap.mods) {
            skills.add(DroidFlashlight(mods, true))

            if (forReplay) {
                skills.add(DroidFlashlight(mods, false))
            }
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

    private fun DroidDifficultyAttributes.populateAimAttributes(skills: Array<Skill<DroidDifficultyHitObject>>, forReplay: Boolean) {
        val aim = skills.find<DroidAim> { it.withSliders } ?: return

        aimDifficulty = calculateRating(aim)
        aimDifficultStrainCount = aim.countTopWeightedStrains()
        aimDifficultSliderCount = aim.countDifficultSliders()

        if (aimDifficulty > 0) {
            val aimNoSlider = skills.find<DroidAim> { !it.withSliders }!!

            aimSliderFactor = calculateRating(aimNoSlider) / aimDifficulty
        } else {
            aimSliderFactor = 1.0
        }

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
        val tapVibro = skills.find<DroidTap> { it.considerCheesability && it.strainTimeCap != null } ?: return

        tapDifficulty = calculateRating(tap)
        tapDifficultStrainCount = tap.countTopWeightedStrains()
        speedNoteCount = tap.relevantNoteCount()
        averageSpeedDeltaTime = tap.relevantDeltaTime()

        if (tapDifficulty > 0) {
            vibroFactor = calculateRating(tapVibro) / tapDifficulty
        }

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

            val currentStrain = tapNoCheese.objectStrains[i]

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
                    calculateThreeFingerSummedStrain(tapNoCheese.objectStrains.subList(firstSpeedObjectIndex, lastSpeedObjectIndex))
                ))
            }
        }
    }

    private fun DroidDifficultyAttributes.populateRhythmAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val rhythm = skills.find<DroidRhythm>() ?: return

        rhythmDifficulty = calculateRating(rhythm)
    }

    private fun DroidDifficultyAttributes.populateFlashlightAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val flashlight = skills.find<DroidFlashlight> { it.withSliders } ?: return
        val flashlightNoSlider = skills.find<DroidFlashlight> { !it.withSliders }

        flashlightDifficulty = calculateRating(flashlight)
        flashlightDifficultStrainCount = flashlight.countTopWeightedStrains()
        flashlightDifficulty =
            if (flashlightNoSlider != null && flashlightDifficulty > 0) calculateRating(flashlightNoSlider) / flashlightDifficulty
            else 1.0
    }

    private fun DroidDifficultyAttributes.populateReadingAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val reading = skills.find<DroidReading>() ?: return

        readingDifficulty = calculateRating(reading)
        readingDifficultNoteCount = reading.countTopWeightedNotes()

        // Consider accuracy difficulty.
        val ratingMultiplier = 0.75 + max(0.0, overallDifficulty).pow(2.2) / 800

        readingDifficulty *= sqrt(ratingMultiplier)
    }

    private fun calculateThreeFingerSummedStrain(strains: List<Double>) =
        strains.fold(0.0) { acc, d -> acc + d / threeFingerStrainThreshold }.pow(0.75)

    /**
     * Calculates the rating of a [Skill] based on its difficulty.
     *
     * @param skill The [Skill] to calculate the rating for.
     * @return The rating of the [Skill].
     */
    private fun calculateRating(skill: Skill<*>) = sqrt(skill.difficultyValue()) * DIFFICULTY_MULTIPLIER

    companion object {
        private const val DIFFICULTY_MULTIPLIER = 0.18

        /**
         * The epoch time of the last change to difficulty calculation, in milliseconds.
         */
        const val VERSION = 1759210780000
    }
}