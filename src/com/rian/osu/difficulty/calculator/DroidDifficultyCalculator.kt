package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.DroidHitWindow
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

/**
 * A difficulty calculator for calculating osu!droid star rating.
 */
class DroidDifficultyCalculator : DifficultyCalculator<DroidDifficultyHitObject, DroidDifficultyAttributes>() {
    override val mode = GameMode.Droid
    override val difficultyMultiplier = 0.18

    override val difficultyAdjustmentMods = mutableSetOf<Mod>().also {
        it.addAll(super.difficultyAdjustmentMods)
        it.add(ModPrecise())
        it.add(ModAutopilot())
    }.toSet()

    private val maximumSectionDeltaTime = 2000
    private val minimumSectionObjectCount = 5
    private val threeFingerStrainThreshold = 175.0

    override fun createDifficultyAttributes(
        beatmap: Beatmap,
        skills: Array<Skill<DroidDifficultyHitObject>>,
        objects: List<DroidDifficultyHitObject>,
        parameters: DifficultyCalculationParameters?
    ) = DroidDifficultyAttributes().apply {
        mods = parameters?.mods?.slice(parameters.mods.indices) ?: mods
        customSpeedMultiplier = parameters?.customSpeedMultiplier ?: customSpeedMultiplier
        clockRate = parameters?.totalSpeedMultiplier?.toDouble() ?: clockRate

        maxCombo = beatmap.maxCombo
        hitCircleCount = beatmap.hitObjects.circleCount
        sliderCount = beatmap.hitObjects.sliderCount
        spinnerCount = beatmap.hitObjects.spinnerCount

        (skills[0] as DroidAim).let {
            aimDifficulty = calculateRating(it)
            aimDifficultStrainCount = it.countDifficultStrains()

            val velocitySum = it.sliderVelocities.sumOf { s -> s.difficultyRating }

            for (slider in it.sliderVelocities) {
                val difficultyRating = slider.difficultyRating / velocitySum

                // Only consider sliders that are fast enough.
                if (difficultyRating > 0.02) {
                    difficultSliders.add(slider.copy(difficultyRating = difficultyRating))
                }
            }

            difficultSliders.sortByDescending { s -> s.difficultyRating }

            // Take the top 15% most difficult sliders.
            while (difficultSliders.size > ceil(0.15 * sliderCount)) {
                difficultSliders.remove(difficultSliders.last())
            }
        }

        aimSliderFactor = if (aimDifficulty > 0) calculateRating(skills[1]) / aimDifficulty else 1.0

        (skills[2] as DroidTap).let {
            tapDifficulty = calculateRating(it)
            tapDifficultStrainCount = it.countDifficultStrains()
            speedNoteCount = it.relevantNoteCount()
            averageSpeedDeltaTime = it.relevantDeltaTime()

            if (tapDifficulty > 0) {
                val tapSkillVibro = DroidTap(mods, true, averageSpeedDeltaTime)

                objects.forEach { o -> tapSkillVibro.process(o) }

                vibroFactor = calculateRating(tapSkillVibro) / tapDifficulty
            }
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

        // Re-filter with tap strain in mind.
        (skills[3] as DroidTap).objectStrains.let {
            for (section in sectionBoundaries) {
                var inSpeedSection = false
                var newFirstObjectIndex = section.first

                for (i in section.first until section.second) {
                    val strain = it[i]

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
                            calculateThreeFingerSummedStrain(it.subList(newFirstObjectIndex, i))
                        ))
                    }
                }

                // Do not forget to manually add the last beatmap section, which would otherwise be ignored.
                // Ignore sections that don't meet object count requirement.
                if (inSpeedSection && section.second - newFirstObjectIndex >= minimumSectionObjectCount) {
                    possibleThreeFingeredSections.add(HighStrainSection(
                        newFirstObjectIndex,
                        section.second,
                        calculateThreeFingerSummedStrain(it.subList(newFirstObjectIndex, section.second))
                    ))
                }
            }
        }

        rhythmDifficulty = calculateRating(skills[4])

        (skills[5] as DroidFlashlight).let {
            flashlightDifficulty = calculateRating(it)
            flashlightDifficultStrainCount = it.countDifficultStrains()
        }

        flashlightSliderFactor = if (flashlightDifficulty > 0) calculateRating(skills[6]) / flashlightDifficulty else 1.0

        (skills[7] as DroidVisual).let {
            visualDifficulty = calculateRating(it)
            visualDifficultStrainCount = it.countDifficultStrains()
        }

        visualSliderFactor = if (visualDifficulty > 0) calculateRating(skills[8]) / visualDifficulty else 1.0

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

        val baseAimPerformance = (5 * max(1.0, aimDifficulty.pow(0.8) / 0.0675) - 4).pow(3.0) / 100000
        val baseTapPerformance = (5 * max(1.0, tapDifficulty / 0.0675) - 4).pow(3.0) / 100000
        val baseFlashlightPerformance =
            if (mods.any { it is ModFlashlight }) flashlightDifficulty.pow(1.6) * 25 else 0.0
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

    override fun createSkills(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?
    ): Array<Skill<DroidDifficultyHitObject>> {
        val mods = parameters?.mods ?: mutableListOf()

        return arrayOf(
            DroidAim(mods, true),
            DroidAim(mods, false),
            DroidTap(mods, true),
            DroidTap(mods, false),
            DroidRhythm(mods),
            DroidFlashlight(mods, true),
            DroidFlashlight(mods, false),
            DroidVisual(mods, true),
            DroidVisual(mods, false)
        )
    }

    override fun createDifficultyHitObjects(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?
    ) = mutableListOf<DroidDifficultyHitObject>().apply {
        val clockRate = parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0
        val greatWindow = (
            if (parameters?.mods?.any { it is ModPrecise } == true) PreciseDroidHitWindow(beatmap.difficulty.od)
            else DroidHitWindow(beatmap.difficulty.od)
        ).greatWindow.toDouble() / clockRate

        beatmap.hitObjects.objects.let {
            for (i in 0 until it.size) {
                add(
                    DroidDifficultyHitObject(
                        it[i],
                        it.getOrNull(i - 1),
                        it.getOrNull(i - 2),
                        clockRate,
                        this,
                        size - 1,
                        greatWindow
                    ).also { d -> d.computeProperties(clockRate, it) }
                )
            }
        }
    }

    private fun calculateThreeFingerSummedStrain(strains: List<Double>) =
        strains.fold(0.0) { acc, d -> acc + d / threeFingerStrainThreshold }.pow(0.75)
}