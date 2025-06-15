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
    override val difficultyAdjustmentMods = super.difficultyAdjustmentMods +
        setOf(ModPrecise::class, ModScoreV2::class, ModTraceable::class, ModReplayV6::class)

    private val maximumSectionDeltaTime = 2000
    private val minimumSectionObjectCount = 5
    private val threeFingerStrainThreshold = 175.0

    override fun createDifficultyAttributes(
        beatmap: PlayableBeatmap,
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>,
    ) = DroidDifficultyAttributes().apply {
        mods = beatmap.mods.values.toSet()
        clockRate = beatmap.speedMultiplier.toDouble()

        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount

        populateAimAttributes(skills)
        populateTapAttributes(skills, objects)
        populateRhythmAttributes(skills)
        populateFlashlightAttributes(skills)
        populateVisualAttributes(skills)

        if (ModRelax::class in beatmap.mods) {
            aimDifficulty *= 0.9
            tapDifficulty = 0.0
            rhythmDifficulty = 0.0
            flashlightDifficulty *= 0.7
            visualDifficulty = 0.0
        }

        if (ModAutopilot::class in beatmap.mods) {
            aimDifficulty = 0.0
            flashlightDifficulty *= 0.3
            visualDifficulty *= 0.8
        }

        val baseAimPerformance = (5 * max(1.0, aimDifficulty.pow(0.8) / 0.0675) - 4).pow(3) / 100000
        val baseTapPerformance = (5 * max(1.0, tapDifficulty / 0.0675) - 4).pow(3) / 100000
        val baseFlashlightPerformance = if (ModFlashlight::class in beatmap.mods) flashlightDifficulty.pow(1.6) * 25 else 0.0
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

        val isPrecise = ModPrecise::class in beatmap.mods
        // Weird cast of greatWindow, but necessary for difficulty calculation parity
        val greatWindow = beatmap.hitWindow.greatWindow.toDouble() / clockRate

        overallDifficulty = (
            if (isPrecise) PreciseDroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat())
            else DroidHitWindow.hitWindow300ToOverallDifficulty(greatWindow.toFloat())
        ).toDouble()
    }

    override fun createSkills(beatmap: DroidPlayableBeatmap): Array<Skill<DroidDifficultyHitObject>> {
        val mods = beatmap.mods.values
        val skills = mutableListOf<Skill<DroidDifficultyHitObject>>()

        if (ModAutopilot::class !in beatmap.mods) {
            skills.add(DroidAim(mods, true))
            skills.add(DroidAim(mods, false))
        }

        if (ModRelax::class !in beatmap.mods) {
            // Tap and visual skills depend on rhythm skill, so we put it first
            skills.add(DroidRhythm(mods))

            skills.add(DroidTap(mods, true))
            skills.add(DroidTap(mods, false))

            skills.add(DroidVisual(mods, true))
            skills.add(DroidVisual(mods, false))
        }

        if (ModFlashlight::class in beatmap.mods) {
            skills.add(DroidFlashlight(mods, true))
            skills.add(DroidFlashlight(mods, false))
        }

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
            ).also { it.computeProperties(clockRate, objects) }
        }

        return arr as Array<DroidDifficultyHitObject>
    }

    override fun createPlayableBeatmap(beatmap: Beatmap, mods: Iterable<Mod>?, scope: CoroutineScope?) =
        beatmap.createDroidPlayableBeatmap(mods, scope)

    private fun DroidDifficultyAttributes.populateAimAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val aim = skills.find<DroidAim> { it.withSliders } ?: return

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
            val aimNoSlider = skills.find<DroidAim> { !it.withSliders }!!

            aimSliderFactor = calculateRating(aimNoSlider) / aimDifficulty
        } else {
            aimSliderFactor = 1.0
        }
    }

    private fun DroidDifficultyAttributes.populateTapAttributes(
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: Array<DroidDifficultyHitObject>
    ) {
        val tap = skills.find<DroidTap> { it.considerCheesability } ?: return

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

        val tapNoCheese = skills.find<DroidTap> { !it.considerCheesability }!!

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
        val rhythm = skills.find<DroidRhythm>() ?: return

        rhythmDifficulty = calculateRating(rhythm)
    }

    private fun DroidDifficultyAttributes.populateFlashlightAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val flashlight = skills.find<DroidFlashlight> { it.withSliders } ?: return

        flashlightDifficulty = calculateRating(flashlight)
        flashlightDifficultStrainCount = flashlight.countDifficultStrains()

        if (flashlightDifficulty > 0) {
            val flashlightNoSlider = skills.find<DroidFlashlight> { !it.withSliders }!!

            flashlightSliderFactor = calculateRating(flashlightNoSlider) / flashlightDifficulty
        } else {
            flashlightSliderFactor = 1.0
        }
    }

    private fun DroidDifficultyAttributes.populateVisualAttributes(skills: Array<Skill<DroidDifficultyHitObject>>) {
        val visual = skills.find<DroidVisual> { it.withSliders } ?: return

        visualDifficulty = calculateRating(visual)
        visualDifficultStrainCount = visual.countDifficultStrains()

        if (visualDifficulty > 0) {
            val visualNoSlider = skills.find<DroidVisual> { !it.withSliders }!!

            visualSliderFactor = calculateRating(visualNoSlider) / visualDifficulty
        } else {
            visualSliderFactor = 1.0
        }
    }

    private fun calculateThreeFingerSummedStrain(strains: List<Double>) =
        strains.fold(0.0) { acc, d -> acc + d / threeFingerStrainThreshold }.pow(0.75)
}