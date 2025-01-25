package com.rian.osu.difficulty.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.DroidPlayableBeatmap
import com.rian.osu.beatmap.PlayableBeatmap
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.difficulty.DroidDifficultyHitObject
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes
import com.rian.osu.difficulty.attributes.HighStrainSection
import com.rian.osu.difficulty.skills.*
import com.rian.osu.mods.*
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.pow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating osu!droid star rating.
 */
class DroidDifficultyCalculator : DifficultyCalculator<DroidPlayableBeatmap, DroidDifficultyHitObject, DroidDifficultyAttributes>() {
    override val difficultyMultiplier = 0.18
    override val difficultyAdjustmentMods = super.difficultyAdjustmentMods + setOf(ModPrecise(), ModAutopilot())

    private val maximumSectionDeltaTime = 2000
    private val minimumSectionObjectCount = 5
    private val threeFingerStrainThreshold = 175.0

    override fun createDifficultyAttributes(
        beatmap: PlayableBeatmap,
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>,
    ) = DroidDifficultyAttributes().apply {
        mods = beatmap.mods?.toList() ?: mods
        customSpeedMultiplier = beatmap.customSpeedMultiplier
        clockRate = beatmap.overallSpeedMultiplier.toDouble()

        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount

        populateAimAttributes(skills)
        populateTapAttributes(skills, objects)
        populateRhythmAttributes(skills)
        populateFlashlightAttributes(skills)
        populateVisualAttributes(skills)

        if (mods.any { it is ModRelax }) {
            aimDifficulty *= 0.9
            tapDifficulty = 0.0
            rhythmDifficulty = 0.0
            flashlightDifficulty *= 0.7
            visualDifficulty = 0.0
        }

        if (mods.any { it is ModAutopilot }) {
            aimDifficulty = 0.0
            flashlightDifficulty *= 0.3
            visualDifficulty *= 0.8
        }

        val baseAimPerformance = (5 * max(1.0, aimDifficulty.pow(0.8) / 0.0675) - 4).pow(3) / 100000
        val baseTapPerformance = (5 * max(1.0, tapDifficulty / 0.0675) - 4).pow(3) / 100000
        val baseFlashlightPerformance = flashlightDifficulty.pow(1.6) * 25
        val baseVisualPerformance = visualDifficulty.pow(1.6) * 22.5

        val basePerformance = (
            baseAimPerformance.pow(1.1) +
            baseTapPerformance.pow(1.1) +
            baseFlashlightPerformance.pow(1.1) +
            baseVisualPerformance.pow(1.1)
        ).pow(1 / 1.1)

        // Document for formula derivation:
        // https://docs.google.com/document/d/10DZGYYSsT_yjz2Mtp6yIJld0Rqx4E-vVHupCqiM4TNI/edit
        starRating =
            if (basePerformance > 1e-5) 0.027 * (cbrt(100000 / 2.0.pow(1 / 1.1) * basePerformance) + 4)
            else 0.0

        val od = beatmap.difficulty.od
        val isPrecise = mods.any { it is ModPrecise }
        val greatWindow = (if (isPrecise) PreciseDroidHitWindow(od) else DroidHitWindow(od)).greatWindow.toDouble() / clockRate

        overallDifficulty = (
            if (isPrecise) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat())
            else DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat())
        ).toDouble()
    }

    override fun createSkills(beatmap: DroidPlayableBeatmap): Array<Skill<DroidDifficultyHitObject>> {
        val mods = beatmap.mods?.toList() ?: emptyList()

        val aim = DroidAim(mods, true)
        val aimNoSlider = DroidAim(mods, false)
        // Tap and visual skills depend on rhythm skill, so we will put it first
        val rhythm = DroidRhythm(mods)
        val tapCheese = DroidTap(mods, true)
        val tapNoCheese = DroidTap(mods, false)
        val visual = DroidVisual(mods, true)
        val visualNoSlider = DroidVisual(mods, false)

        if (mods.any { it is ModFlashlight }) {
            val flashlight = DroidFlashlight(mods, true)
            val flashlightNoSlider = DroidFlashlight(mods, false)

            return arrayOf(
                aim, aimNoSlider, rhythm, tapCheese, tapNoCheese,
                flashlight, flashlightNoSlider, visual, visualNoSlider
            )
        }

        return arrayOf(aim, aimNoSlider, rhythm, tapCheese, tapNoCheese, visual, visualNoSlider)
    }

    @Suppress("UNCHECKED_CAST")
    override fun createDifficultyHitObjects(beatmap: DroidPlayableBeatmap, scope: CoroutineScope?): Array<DroidDifficultyHitObject> {
        if (beatmap.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        val clockRate = beatmap.overallSpeedMultiplier.toDouble()

        val greatWindow = (
            if (beatmap.mods?.any { it is ModPrecise } == true) PreciseDroidHitWindow(beatmap.difficulty.od)
            else DroidHitWindow(beatmap.difficulty.od)
        ).greatWindow.toDouble() / clockRate

        val objects = beatmap.hitObjects.objects
        val arr = arrayOfNulls<DroidDifficultyHitObject>(objects.size)

        for (i in objects.indices) {
            scope?.ensureActive()

            arr[i] = DroidDifficultyHitObject(
                objects[i],
                if (i > 0) objects[i - 1] else null,
                if (i > 1) objects[i - 2] else null,
                clockRate,
                arr as Array<DroidDifficultyHitObject>,
                i - 1,
                greatWindow
            ).also { it.computeProperties(clockRate, objects) }
        }

        return arr as Array<DroidDifficultyHitObject>
    }

    override fun createPlayableBeatmap(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?,
        scope: CoroutineScope?
    ) = beatmap.createDroidPlayableBeatmap(
            parameters?.mods,
            parameters?.customSpeedMultiplier ?: 1f,
            parameters?.oldStatistics == true,
            scope
        )

    private fun DroidDifficultyAttributes.populateAimAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val aim = skills.first { it is DroidAim && it.withSliders } as DroidAim
        aimDifficulty = calculateRating(aim)
        aimDifficultStrainCount = aim.countDifficultStrains()
        aimDifficultSliderCount = aim.countDifficultSliders()

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

        if (aimDifficulty > 0) {
            val aimNoSlider = skills.first { it is DroidAim && !it.withSliders } as DroidAim

            aimSliderFactor = calculateRating(aimNoSlider) / aimDifficulty
        } else {
            aimSliderFactor = 1.0
        }
    }

    private fun DroidDifficultyAttributes.populateTapAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>
    ) {
        val tap = skills.first { it is DroidTap && it.considerCheesability } as DroidTap

        tapDifficulty = calculateRating(tap)
        tapDifficultStrainCount = tap.countDifficultStrains()
        speedNoteCount = tap.relevantNoteCount()
        averageSpeedDeltaTime = tap.relevantDeltaTime()

        if (tapDifficulty > 0) {
            val tapSkillVibro = DroidTap(mods, true, averageSpeedDeltaTime)

            objects.forEach { tapSkillVibro.process(it) }

            vibroFactor = calculateRating(tapSkillVibro) / tapDifficulty
        }

        var firstObjectIndex = 0
        val sectionBoundaries = mutableListOf<Pair<Int, Int>>()

        for (i in 0 until objects.size - 1) {
            val current = objects[i].obj
            val next = objects[i + 1].obj
            val deltaTime = next.startTime - current.endTime

            if (deltaTime >= maximumSectionDeltaTime) {
                // Ignore sections that do not meet object count requirement.
                if (i - firstObjectIndex >= minimumSectionObjectCount) {
                    sectionBoundaries.add(Pair(firstObjectIndex, i))
                }

                firstObjectIndex = i + 1
            }
        }

        // Do not forget to manually add the last beatmap section, which would otherwise be ignored.
        if (objects.size - firstObjectIndex >= minimumSectionObjectCount) {
            sectionBoundaries.add(Pair(firstObjectIndex, objects.size - 1))
        }

        val tapNoCheese = skills.first { it is DroidTap && !it.considerCheesability } as DroidTap

        // Re-filter with tap strain in mind.
        for (section in sectionBoundaries) {
            var inSpeedSection = false
            var newFirstObjectIndex = section.first

            for (i in section.first until section.second) {
                val strain = tapNoCheese.objectStrains[i]

                if (!inSpeedSection && strain >= threeFingerStrainThreshold) {
                    inSpeedSection = true
                    newFirstObjectIndex = i
                    continue
                }

                if (inSpeedSection && strain < threeFingerStrainThreshold) {
                    inSpeedSection = false

                    // Ignore sections that do not meet object count requirement.
                    if (i - newFirstObjectIndex < minimumSectionObjectCount) {
                        continue
                    }

                    possibleThreeFingeredSections.add(HighStrainSection(
                        newFirstObjectIndex,
                        i,
                        calculateThreeFingerSummedStrain(tapNoCheese.objectStrains.subList(newFirstObjectIndex, i))
                    ))
                }
            }

            // Do not forget to manually add the last beatmap section, which would otherwise be ignored.
            // Ignore sections that don't meet object count requirement.
            if (inSpeedSection && section.second - newFirstObjectIndex >= minimumSectionObjectCount) {
                possibleThreeFingeredSections.add(HighStrainSection(
                    newFirstObjectIndex,
                    section.second,
                    calculateThreeFingerSummedStrain(tapNoCheese.objectStrains.subList(newFirstObjectIndex, section.second))
                ))
            }
        }
    }

    private fun DroidDifficultyAttributes.populateRhythmAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val rhythm = skills.first { it is DroidRhythm } as DroidRhythm

        rhythmDifficulty = calculateRating(rhythm)
    }

    private fun DroidDifficultyAttributes.populateFlashlightAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val flashlight = skills.firstOrNull { it is DroidFlashlight && it.withSliders } as? DroidFlashlight ?: return

        flashlightDifficulty = calculateRating(flashlight)
        flashlightDifficultStrainCount = flashlight.countDifficultStrains()

        if (flashlightDifficulty > 0) {
            val flashlightNoSlider = skills.first { it is DroidFlashlight && !it.withSliders } as DroidFlashlight

            flashlightSliderFactor = calculateRating(flashlightNoSlider) / flashlightDifficulty
        } else {
            flashlightSliderFactor = 1.0
        }
    }

    private fun DroidDifficultyAttributes.populateVisualAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val visual = skills.first { it is DroidVisual && it.withSliders } as DroidVisual

        visualDifficulty = calculateRating(visual)
        visualDifficultStrainCount = visual.countDifficultStrains()

        if (visualDifficulty > 0) {
            val visualNoSlider = skills.first { it is DroidVisual && !it.withSliders } as DroidVisual

            visualSliderFactor = calculateRating(visualNoSlider) / visualDifficulty
        } else {
            visualSliderFactor = 1.0
        }
    }

    private fun calculateThreeFingerSummedStrain(strains: List<Double>) =
        strains.fold(0.0) { acc, d -> acc + d / threeFingerStrainThreshold }.pow(0.75)
}