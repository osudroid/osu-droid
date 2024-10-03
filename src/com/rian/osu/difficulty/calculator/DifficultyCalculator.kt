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
     * Retains [Mod]s that change star rating within a collection of [Mod]s.
     *
     * @param mods The collection of [Mod]s to check.
     */
    fun retainDifficultyAdjustmentMods(mods: MutableCollection<Mod>) {
        for (mod in mods) {
            if (difficultyAdjustmentMods.none { it.isInstance(mod) }) {
                mods.remove(mod)
            }
        }
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s.
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return A structure describing the difficulty of the [Beatmap].
     */
    @JvmOverloads
    fun calculate(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null): TAttributes {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val modsCalculated = mods ?: emptySet()
        val playableBeatmap = beatmap.createPlayableBeatmap(mode, modsCalculated, scope)
        val skills = createSkills(playableBeatmap, modsCalculated)

        val objects = createDifficultyHitObjects(playableBeatmap, modsCalculated, scope)

        for (obj in objects) {
            for (skill in skills) {
                scope?.ensureActive()
                skill.process(obj)
            }
        }

        return createDifficultyAttributes(playableBeatmap, modsCalculated, skills, objects)
    }

    /**
     * Calculates the difficulty of a [Beatmap] with specific [Mod]s and returns a set of
     * [TimedDifficultyAttributes] representing the difficulty at every relevant time value in the [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty is to be calculated.
     * @param mods The [Mod]s to apply to the [Beatmap].
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The set of [TimedDifficultyAttributes].
     */
    @JvmOverloads
    fun calculateTimed(beatmap: Beatmap, mods: Iterable<Mod>? = null, scope: CoroutineScope? = null): Array<TimedDifficultyAttributes<TAttributes>> {
        // Always operate on a clone of the original beatmap when needed, to not modify it game-wide
        val modsCalculated = mods ?: emptySet()
        val playableBeatmap = beatmap.createPlayableBeatmap(mode, modsCalculated, scope)
        val skills = createSkills(playableBeatmap, modsCalculated)

        if (playableBeatmap.hitObjects.objects.isEmpty()) {
            return emptyArray()
        }

        val attributes = arrayOfNulls<TimedDifficultyAttributes<TAttributes>>(playableBeatmap.hitObjects.objects.size)
        val progressiveBeatmap = ProgressiveCalculationBeatmap().apply {
            difficulty.apply(playableBeatmap.difficulty)
        }

        val difficultyObjects = createDifficultyHitObjects(playableBeatmap, modsCalculated, scope)
        var currentIndex = 0

        for (i in playableBeatmap.hitObjects.objects.indices) {
            val obj = playableBeatmap.hitObjects.objects[i]

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
                createDifficultyAttributes(progressiveBeatmap, modsCalculated, skills, difficultyObjects.sliceArray(0..<currentIndex))
            )
        }

        @Suppress("UNCHECKED_CAST")
        return attributes as Array<TimedDifficultyAttributes<TAttributes>>
    }

    /**
     * Creates the [Skill]s to calculate the difficulty of a [Beatmap].
     *
     * @param beatmap The [Beatmap] whose difficulty will be calculated.
     * @param mods The [Mod]s that are used.
     * @return The [Skill]s.
     */
    protected abstract fun createSkills(beatmap: Beatmap, mods: Iterable<Mod> = emptySet()): Array<Skill<TObject>>

    /**
     * Retrieves the [DifficultyHitObject]s to calculate against.
     *
     * @param beatmap The [Beatmap] providing the hit objects to generate from.
     * @param mods The [Mod]s that are used.
     * @param scope The [CoroutineScope] to use for coroutines.
     * @return The generated [DifficultyHitObject]s.
     */
    protected abstract fun createDifficultyHitObjects(beatmap: Beatmap, mods: Iterable<Mod> = emptySet(), scope: CoroutineScope? = null): Array<TObject>

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
     * @param mods The [Mod]s that were used.
     * @param skills The [Skill]s which processed the beatmap.
     * @param objects The [TObject]s that were generated.
     * @return [TAttributes] describing the beatmap's difficulty.
     */
    protected abstract fun createDifficultyAttributes(beatmap: Beatmap, mods: Iterable<Mod>, skills: Array<Skill<TObject>>, objects: Array<TObject>): TAttributes

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
