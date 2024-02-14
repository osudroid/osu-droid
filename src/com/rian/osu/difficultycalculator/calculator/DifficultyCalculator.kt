package com.rian.osu.difficultycalculator.calculator

import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.BeatmapConverter
import com.rian.osu.beatmap.BeatmapProcessor
import com.rian.osu.difficultycalculator.DifficultyHitObject
import com.rian.osu.difficultycalculator.attributes.DifficultyAttributes
import com.rian.osu.difficultycalculator.attributes.TimedDifficultyAttributes
import com.rian.osu.difficultycalculator.skills.Aim
import com.rian.osu.difficultycalculator.skills.Flashlight
import com.rian.osu.difficultycalculator.skills.Skill
import com.rian.osu.difficultycalculator.skills.Speed
import com.rian.osu.mods.*
import com.rian.osu.utils.HitWindowConverter.hitWindow300ToOD
import com.rian.osu.utils.HitWindowConverter.odToHitWindow300
import kotlin.math.cbrt
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * A difficulty calculator for calculating star rating.
 */
object DifficultyCalculator {
    /**
     * [Mod]s that can alter the star rating when they are used in calculation with one or more [Mod]s.
     */
    private val difficultyAdjustmentMods = setOf(
        ModDoubleTime(), ModHalfTime(), ModNightCore(),
        ModRelax(), ModEasy(), ModReallyEasy(),
        ModHardRock(), ModHidden(), ModFlashlight(),
        ModDifficultyAdjust()
    )

    private const val DIFFICULTY_MULTIPLIER = 0.0675

    /**
     * Calculates the difficulty of a [Beatmap] with specific parameters.
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the [Beatmap].
     * @return A structure describing the difficulty of the [Beatmap].
     */
    @JvmOverloads
    fun calculate(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters? = null
    ): DifficultyAttributes {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate = convertBeatmap(beatmap, parameters)
        val skills = createSkills(beatmapToCalculate, parameters)

        for (obj in createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            for (skill in skills) {
                skill.process(obj)
            }
        }

        return createDifficultyAttributes(beatmapToCalculate, skills, parameters)
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific parameters and returns a set of
     * [TimedDifficultyAttributes] representing the difficulty at every relevant time
     * value in the [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the [Beatmap].
     * @return The set of [TimedDifficultyAttributes].
     */
    @JvmOverloads
    fun calculateTimed(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters? = null
    ): List<TimedDifficultyAttributes> {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate = convertBeatmap(beatmap, parameters)
        val skills = createSkills(beatmapToCalculate, parameters)
        val attributes = ArrayList<TimedDifficultyAttributes>()

        if (beatmapToCalculate.hitObjects.objects.isEmpty()) {
            return attributes
        }

        val progressiveBeatmap = Beatmap().apply {
            difficulty.apply(beatmapToCalculate.difficulty)
        }

        // Add the first object in the beatmap, otherwise it will be ignored.
        progressiveBeatmap.hitObjects.add(beatmapToCalculate.hitObjects.objects.first())

        for (obj in createDifficultyHitObjects(beatmapToCalculate, parameters)) {
            progressiveBeatmap.hitObjects.add(obj.obj)

            for (skill in skills) {
                skill.process(obj)
            }

            attributes.add(
                TimedDifficultyAttributes(
                    obj.endTime * (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0),
                    createDifficultyAttributes(progressiveBeatmap, skills, parameters)
                )
            )
        }

        return attributes
    }

    /**
     * Retains [Mod]s based on [difficultyAdjustmentMods].
     *
     * This is used rather than [MutableCollection.retainAll] as some [Mod]s need a special treatment.
     */
    fun retainDifficultyAdjustmentMods(parameters: DifficultyCalculationParameters) =
        parameters.mods.iterator().run {
            for (mod in this) {
                // ModDifficultyAdjust always changes difficulty.
                if (mod is ModDifficultyAdjust) {
                    continue
                }

                if (!difficultyAdjustmentMods.contains(mod)) {
                    remove()
                }
            }
        }

    /**
     * Creates difficulty attributes to describe a beatmap's difficulty.
     *
     * @param beatmap The beatmap whose difficulty was calculated.
     * @param skills The skills which processed the beatmap.
     * @param parameters The difficulty calculation parameters used.
     * @return Difficulty attributes describing the beatmap's difficulty.
     */
    private fun createDifficultyAttributes(
        beatmap: Beatmap, skills: Array<Skill>,
        parameters: DifficultyCalculationParameters?
    ) = DifficultyAttributes().apply {
            mods = parameters?.mods?.slice(parameters.mods.indices) ?: mods

            aimDifficulty = calculateRating(skills[0])
            speedDifficulty = calculateRating(skills[2])
            speedNoteCount = (skills[2] as Speed).relevantNoteCount()
            flashlightDifficulty = calculateRating(skills[3])

            aimSliderFactor = if (aimDifficulty > 0) calculateRating(skills[1]) / aimDifficulty else 1.0

            if (parameters?.mods?.any { it is ModRelax } == true) {
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
                    cbrt(PerformanceCalculator.FINAL_MULTIPLIER) * 0.027 *
                    (cbrt(100000 / 2.0.pow(1 / 1.1) * basePerformance) + 4)
                else 0.0

            val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as ModDifficultyAdjust?
            val ar = difficultyAdjustMod?.ar?.takeUnless { it.isNaN() } ?: beatmap.difficulty.ar
            var preempt = if (ar <= 5) 1800.0 - 120 * ar else 1950.0 - 150 * ar

            if (difficultyAdjustMod == null || difficultyAdjustMod.ar.isNaN()) {
                preempt /= parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0
            }

            approachRate = if (preempt > 1200) (1800 - preempt) / 120 else (1200 - preempt) / 150 + 5

            val greatWindow =
                odToHitWindow300(beatmap.difficulty.od) /
                (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0)

            overallDifficulty = hitWindow300ToOD(greatWindow).toDouble()
            maxCombo = beatmap.maxCombo
            hitCircleCount = beatmap.hitObjects.circleCount
            sliderCount = beatmap.hitObjects.sliderCount
            spinnerCount = beatmap.hitObjects.spinnerCount
        }

    private fun convertBeatmap(beatmap: Beatmap, parameters: DifficultyCalculationParameters?): Beatmap {
        if (!needToConvertBeatmap(beatmap, parameters)) {
            return beatmap
        }

        val converter = BeatmapConverter(beatmap)

        // Convert
        val converted = converter.convert()

        // Apply difficulty mods
        parameters?.mods?.filterIsInstance<IApplicableToDifficulty>()?.forEach {
            it.applyToDifficulty(converted.difficulty)
        }

        parameters?.mods?.filterIsInstance<IApplicableToDifficultyWithSettings>()?.forEach {
            it.applyToDifficulty(converted.difficulty, parameters.mods, parameters.customSpeedMultiplier)
        }

        val processor = BeatmapProcessor(converted)

        processor.preProcess()

        // Compute default values for hit objects, including creating nested hit objects in-case they're needed
        converted.hitObjects.objects.forEach {
            it.applyDefaults(converted.controlPoints, converted.difficulty)
        }

        processor.postProcess()

        parameters?.mods?.filterIsInstance<IApplicableToBeatmap>()?.forEach {
            it.applyToBeatmap(converted)
        }

        return converted
    }

    /**
     * Creates the skills to calculate the difficulty of a beatmap.
     *
     * @param beatmap The beatmap whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The skills.
     */
    private fun createSkills(beatmap: Beatmap, parameters: DifficultyCalculationParameters?): Array<Skill> {
        val mods = parameters?.mods ?: mutableListOf()
        val greatWindow =
            odToHitWindow300(beatmap.difficulty.od) /
            (parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0)

        return arrayOf(
            Aim(mods, true),
            Aim(mods, false),
            Speed(mods, greatWindow),
            Flashlight(mods)
        )
    }

    private fun calculateRating(skill: Skill) = sqrt(skill.difficultyValue()) * DIFFICULTY_MULTIPLIER

    /**
     * Retrieves the difficulty hit objects to calculate against.
     *
     * @param beatmap The beatmap providing the hit objects to generate from.
     * @param parameters The difficulty calculation parameter being used.
     * @return The generated difficulty hit objects.
     */
    private fun createDifficultyHitObjects(
        beatmap: Beatmap, parameters: DifficultyCalculationParameters?
    ) = mutableListOf<DifficultyHitObject>().apply {
        beatmap.hitObjects.objects.let {
            for (i in 1 until it.size) {
                add(
                    DifficultyHitObject(
                        it[i],
                        it[i - 1],
                        it.getOrNull(i - 2),
                        parameters?.totalSpeedMultiplier?.toDouble() ?: 1.0,
                        this,
                        size
                    )
                )
            }
        }
    }

    /**
     * Checks whether a [Beatmap] must be copied with respect to a [DifficultyCalculationParameters].
     *
     * @param beatmap The [Beatmap].
     * @param parameters The [DifficultyCalculationParameters].
     * @return Whether the [Beatmap] should be copied.
     */
    private fun needToConvertBeatmap(beatmap: Beatmap, parameters: DifficultyCalculationParameters?) = parameters?.run {
        val difficultyAdjustMod = mods.find { it is ModDifficultyAdjust } as ModDifficultyAdjust?

        customSpeedMultiplier != 1.0f ||
        mods.any { difficultyAdjustmentMods.contains(it) } ||
        (difficultyAdjustMod?.let {
            (!it.cs.isNaN() && it.cs != beatmap.difficulty.cs) ||
            (!it.ar.isNaN() && it.ar != beatmap.difficulty.ar) ||
            (!it.od.isNaN() && it.od != beatmap.difficulty.od)
        } ?: false)
    } ?: false
}
