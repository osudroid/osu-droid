package com.osudroid.scoring

import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.Mod
import kotlin.reflect.KClass

/**
 * Calculates the multiplier to be applied to score with a given combination of [Mod]s.
 */
abstract class BaseScoreMultiplierCalculator<TMultiplier : Number> @JvmOverloads constructor(
    /**
     * The [BeatmapDifficulty] for the beatmap that the multipliers are calculated for. This must be the
     * [BeatmapDifficulty] for the beatmap **before** any [Mod] application.
     */
    protected val difficulty: BeatmapDifficulty? = null
) {
    /**
     * The multiplier of [Mod]s when they are applied alone.
     */
    protected val singleMultipliers = mutableMapOf<KClass<out Mod>, (Mod) -> TMultiplier>()

    /**
     * The multiplier of [Mod]s when they are applied with other specific [Mod]s. During calculation, combination
     * multipliers override single multipliers.
     */
    protected val combinationMultipliers = mutableListOf<Pair<List<KClass<out Mod>>, (List<Mod>) -> TMultiplier>>()

    /**
     * Multipliers applied to all [Mod]s that are instances of a common base class, collected together so the
     * multiplier function receives them as a group. During calculation, group multipliers take precedence over
     * single multipliers for the matched mods.
     */
    protected val groupMultipliers = mutableListOf<Pair<(Mod) -> Boolean, (List<Mod>) -> TMultiplier>>()

    /**
     * Defines a flat, setting-independent score multiplier for the given [TMod].
     */
    protected inline fun <reified TMod : Mod> single(multiplier: TMultiplier) {
        singleMultipliers[TMod::class] = { multiplier }
    }

    /**
     * Defines a setting-dependent score multiplier for the given [TMod].
     */
    protected inline fun <reified TMod : Mod> single(noinline multiplier: TMod.() -> TMultiplier) {
        singleMultipliers[TMod::class] = { (it as TMod).multiplier() }
    }

    /**
     * Defines a score multiplier specific to when both [T1] and [T2] [Mod]s are present. In that case, the combination
     * multiplier will be used instead of the individual single multipliers, if any.
     */
    protected inline fun <reified T1 : Mod, reified T2 : Mod> combination(
        noinline multiplier: (T1, T2) -> TMultiplier
    ) {
        combinationMultipliers += listOf(T1::class, T2::class) to { multiplier(it[0] as T1, it[1] as T2) }
    }

    /**
     * Defines a score multiplier for all [Mod]s that are instances of [TMod], applied as a group. The multiplier
     * function receives the full list of matched [Mod]s, allowing their combined state to influence the result.
     * Group multipliers take precedence over single multipliers for the same [Mod]s.
     */
    @Suppress("UNCHECKED_CAST")
    protected inline fun <reified TMod : Mod> group(noinline multiplier: (List<TMod>) -> TMultiplier) {
        groupMultipliers += { mod: Mod -> mod is TMod } to { mods: List<Mod> -> multiplier(mods as List<TMod>) }
    }

    fun calculateFor(mods: Iterable<Mod>): TMultiplier {
        val modsByType = mods.associateBy { it::class }

        if (modsByType.isEmpty()) {
            return defaultMultiplier
        }

        val remaining = modsByType.keys.toMutableSet()
        var result = defaultMultiplier

        if (modsByType.size > 1) {
            for ((types, multiplier) in combinationMultipliers) {
                if (remaining.containsAll(types)) {
                    val instances = types.map { modsByType.getValue(it) }

                    result = multiply(result, multiplier(instances))
                    remaining.removeAll(types.toSet())
                }
            }
        }

        for ((predicate, multiplier) in groupMultipliers) {
            val matched = remaining.filter { predicate(modsByType.getValue(it)) }

            if (matched.isEmpty()) {
                continue
            }

            result = multiply(result, multiplier(matched.map { modsByType.getValue(it) }))
            remaining.removeAll(matched.toSet())
        }

        for (type in remaining) {
            val multiplier = singleMultipliers[type] ?: continue

            result = multiply(result, multiplier(modsByType.getValue(type)))
        }

        return result
    }

    /**
     * The default multiplier used as an initial point of the application.
     */
    protected abstract val defaultMultiplier: TMultiplier

    /**
     * Multiplies two [TMultiplier] values together. This is used to combine the results of multiple multipliers.
     */
    protected abstract fun multiply(a: TMultiplier, b: TMultiplier): TMultiplier
}
