package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.BeatmapConverter
import com.rian.osu.beatmap.BeatmapProcessor
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.attributes.DifficultyAttributes
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes
import com.rian.osu.difficulty.skills.Skill
import com.rian.osu.mods.*
import kotlin.math.sqrt

/**
 * A difficulty calculator for calculating star rating.
 */
abstract class DifficultyCalculator<TObject : DifficultyHitObject, TAttributes : DifficultyAttributes> {
    protected abstract val mode: GameMode
    protected abstract val difficultyMultiplier: Double

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
    ): TAttributes {
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
    ): List<TimedDifficultyAttributes<TAttributes>> {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate = convertBeatmap(beatmap, parameters)
        val skills = createSkills(beatmapToCalculate, parameters)
        val attributes = mutableListOf<TimedDifficultyAttributes<TAttributes>>()

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
     * Creates the [Skill]s to calculate the difficulty of a beatmap.
     *
     * @param beatmap The beatmap whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The [Skill]s.
     */
    protected abstract fun createSkills(beatmap: Beatmap, parameters: DifficultyCalculationParameters?): Array<Skill<TObject>>

    /**
     * Retrieves the [DifficultyHitObject]s to calculate against.
     *
     * @param beatmap The [Beatmap] providing the hit objects to generate from.
     * @param parameters The difficulty calculation parameter being used.
     * @return The generated [DifficultyHitObject]s.
     */
    protected abstract fun createDifficultyHitObjects(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters?
    ): List<TObject>

    protected fun calculateRating(skill: Skill<TObject>) = sqrt(skill.difficultyValue()) * difficultyMultiplier

    /**
     * Creates a [DifficultyAttributes] to describe a beatmap's difficulty.
     *
     * @param beatmap The beatmap whose difficulty was calculated.
     * @param skills The skills which processed the beatmap.
     * @param parameters The difficulty calculation parameters used.
     * @return [DifficultyAttributes] describing the beatmap's difficulty.
     */
    protected abstract fun createDifficultyAttributes(
        beatmap: Beatmap,
        skills: Array<Skill<TObject>>,
        parameters: DifficultyCalculationParameters?
    ): TAttributes

    private fun convertBeatmap(beatmap: Beatmap, parameters: DifficultyCalculationParameters?): Beatmap {
        if (!needToConvertBeatmap(beatmap, parameters)) {
            return beatmap
        }

        val converter = BeatmapConverter(beatmap)

        // Convert
        val converted = converter.convert()

        // Apply difficulty mods
        parameters?.mods?.filterIsInstance<IApplicableToDifficulty>()?.forEach {
            it.applyToDifficulty(mode, converted.difficulty)
        }

        parameters?.mods?.filterIsInstance<IApplicableToDifficultyWithSettings>()?.forEach {
            it.applyToDifficulty(mode, converted.difficulty, parameters.mods, parameters.customSpeedMultiplier)
        }

        val processor = BeatmapProcessor(converted)

        processor.preProcess()

        // Compute default values for hit objects, including creating nested hit objects in-case they're needed
        converted.hitObjects.objects.forEach {
            it.applyDefaults(converted.controlPoints, converted.difficulty, mode)
        }

        processor.postProcess(mode)

        parameters?.mods?.filterIsInstance<IApplicableToBeatmap>()?.forEach {
            it.applyToBeatmap(converted)
        }

        return converted
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
            (it.cs != null && it.cs != beatmap.difficulty.cs) ||
            (it.ar != null && it.ar != beatmap.difficulty.ar) ||
            (it.od != null && it.od != beatmap.difficulty.od)
        } ?: false)
    } ?: false

    companion object {
        /**
         * Retains [Mod]s based on [difficultyAdjustmentMods].
         *
         * This is used rather than [MutableCollection.retainAll] as some [Mod]s need a special treatment.
         */
        @JvmStatic
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
         * [Mod]s that can alter the star rating when they are used in calculation with one or more [Mod]s.
         */
        private val difficultyAdjustmentMods = setOf(
            ModDoubleTime(), ModHalfTime(), ModNightCore(),
            ModRelax(), ModEasy(), ModReallyEasy(),
            ModHardRock(), ModHidden(), ModFlashlight(),
            ModDifficultyAdjust()
        )
    }
}
