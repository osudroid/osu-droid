package com.rian.osu.difficulty.calculator

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.sections.BeatmapHitObjects
import com.rian.osu.difficulty.DifficultyHitObject
import com.rian.osu.difficulty.attributes.DifficultyAttributes
import com.rian.osu.difficulty.attributes.TimedDifficultyAttributes
import com.rian.osu.difficulty.skills.Skill
import com.rian.osu.mods.*
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive

/**
 * A difficulty calculator for calculating star rating.
 */
abstract class DifficultyCalculator<TObject : DifficultyHitObject, TAttributes : DifficultyAttributes> {
    protected abstract val mode: GameMode
    protected abstract val difficultyMultiplier: Double

    /**
     * [Mod]s that can alter the star rating when they are used in calculation with one or more [Mod]s.
     */
    protected open val difficultyAdjustmentMods = setOf(
        ModRelax::class, ModEasy::class, ModReallyEasy::class,
        ModHardRock::class, ModHidden::class, ModFlashlight::class,
        ModDifficultyAdjust::class, ModRateAdjust::class
    )

    /**
     * Retains [Mod]s that change star rating.
     */
    fun retainDifficultyAdjustmentMods(parameters: DifficultyCalculationParameters) {
        if (parameters.mods.isEmpty()) {
            return
        }

        for (adjustmentMod in difficultyAdjustmentMods) {
            for (parameterMod in parameters.mods) {
                if (adjustmentMod.isInstance(parameterMod)) {
                    parameters.mods.remove(parameterMod)
                    break
                }
            }
        }
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific parameters.
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the difficulty of the [Beatmap].
     */
    @JvmOverloads
    fun calculate(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters? = null,
        scope: CoroutineScope? = null
    ): TAttributes {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate = beatmap.createPlayableBeatmap(mode, parameters?.mods, parameters?.customSpeedMultiplier ?: 1f, scope)
        val skills = createSkills(beatmapToCalculate, parameters)

        val objects = createDifficultyHitObjects(beatmapToCalculate, parameters, scope)

        for (obj in objects) {
            for (skill in skills) {
                scope?.ensureActive()
                skill.process(obj)
            }
        }

        return createDifficultyAttributes(beatmapToCalculate, skills, objects, parameters)
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific parameters and returns a set of
     * [TimedDifficultyAttributes] representing the difficulty at every relevant time value in the [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param parameters The calculation parameters that should be applied to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The set of [TimedDifficultyAttributes].
     */
    @JvmOverloads
    fun calculateTimed(
        beatmap: Beatmap,
        parameters: DifficultyCalculationParameters? = null,
        scope: CoroutineScope? = null
    ): Array<TimedDifficultyAttributes<TAttributes>> {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val beatmapToCalculate = beatmap.createPlayableBeatmap(mode, parameters?.mods, parameters?.customSpeedMultiplier ?: 1f, scope)
        val skills = createSkills(beatmapToCalculate, parameters)

        if (beatmapToCalculate.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        val attributes = arrayOfNulls<TimedDifficultyAttributes<TAttributes>>(beatmapToCalculate.hitObjects.objects.size)
        val progressiveBeatmap = ProgressiveCalculationBeatmap().apply {
            difficulty.apply(beatmapToCalculate.difficulty)
        }

        val difficultyObjects = createDifficultyHitObjects(beatmapToCalculate, parameters, scope)
        var currentIndex = 0

        for (i in beatmapToCalculate.hitObjects.objects.indices) {
            val obj = beatmapToCalculate.hitObjects.objects[i]

            progressiveBeatmap.hitObjects.add(obj)

            while (currentIndex < difficultyObjects.size && difficultyObjects[currentIndex].obj.endTime <= obj.endTime) {
                for (skill in skills) {
                    scope?.ensureActive()
                    skill.process(difficultyObjects[currentIndex])
                }

                currentIndex++
            }

            attributes[i] = TimedDifficultyAttributes(
                obj.endTime,
                createDifficultyAttributes(progressiveBeatmap, skills, difficultyObjects.sliceArray(0..<currentIndex), parameters)
            )
        }

        @Suppress("UNCHECKED_CAST")
        return attributes as Array<TimedDifficultyAttributes<TAttributes>>
    }

    /**
     * Creates the [Skill]s to calculate the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty will be calculated.
     * @param parameters The difficulty calculation parameter being used.
     * @return The [Skill]s.
     */
    protected abstract fun createSkills(beatmap: Beatmap, parameters: DifficultyCalculationParameters?): Array<Skill<TObject>>

    /**
     * Retrieves the [DifficultyHitObject]s to calculate against.
     *
     * @param beatmap The [Beatmap] providing the hit objects to generate from.
     * @param parameters The difficulty calculation parameter being used.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The generated [DifficultyHitObject]s.
     */
    protected abstract fun createDifficultyHitObjects(beatmap: Beatmap, parameters: DifficultyCalculationParameters?, scope: CoroutineScope? = null): Array<TObject>

    /**
     * Calculates the rating of a [Skill] based on its difficulty.
     *
     * @param skill The [Skill] to calculate the rating for.
     * @return The rating of the [Skill].
     */
    protected fun calculateRating(skill: Skill<TObject>) = sqrt(skill.difficultyValue()) * difficultyMultiplier

    /**
     * Creates a [TAttributes] to describe a beatmap's difficulty.
     *
     * @param beatmap The [Beatmap] whose difficulty was calculated.
     * @param skills The [Skill]s which processed the beatmap.
     * @param objects The [TObject]s that were generated.
     * @param parameters The difficulty calculation parameters used.
     * @return [TAttributes] describing the beatmap's difficulty.
     */
    protected abstract fun createDifficultyAttributes(
        beatmap: Beatmap,
        skills: Array<Skill<TObject>>,
        objects: Array<TObject>,
        parameters: DifficultyCalculationParameters?
    ): TAttributes

    /**
     * A [Beatmap] that is used for timed difficulty calculation.
     */
    private class ProgressiveCalculationBeatmap : Beatmap() {
        // The implementation of maximum combo in Beatmap is lazily evaluated, so we need to override it here
        // as the maximum combo of a progressive beatmap changes overtime.
        override var maxCombo = 0
            private set

        override var hitObjects = object : BeatmapHitObjects() {
            override fun add(obj: HitObject) {
                super.add(obj)

                maxCombo += if (obj is Slider) obj.nestedHitObjects.size else 1
            }

            override fun remove(obj: HitObject): Boolean {
                val removed = super.remove(obj)

                if (removed) {
                    maxCombo -= if (obj is Slider) obj.nestedHitObjects.size else 1
                }

                return removed
            }

            override fun remove(index: Int): HitObject? {
                val removed = super.remove(index)

                if (removed != null) {
                    maxCombo -= if (removed is Slider) removed.nestedHitObjects.size else 1
                }

                return removed
            }
        }
    }
}
