package com.rian.osu.utils

import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.mods.*
import java.util.EnumSet
import kotlin.reflect.KClass
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * A [HashSet] of [Mod]s that has additional utilities specifically for [Mod]s.
 */
class ModHashSet : HashSet<Mod> {
    private val classSet = mutableSetOf<Class<out Mod>>()

    constructor() : super()
    constructor(mods: Collection<Mod>) : this(mods as Iterable<Mod>)
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    constructor(mods: Iterable<Mod>) : super() {
        for (m in mods) {
            add(m)
            classSet.add(m::class.java)
        }
    }

    override fun add(element: Mod): Boolean {
        // Ensure the mod itself is not a duplicate.
        if (element::class in this) {
            remove(element)
        }

        // If all difficulty statistics are set, all other difficulty adjusting mods are irrelevant, so we remove them.
        // This prevents potential abuse cases where score multipliers from non-affecting mods stack (i.e., forcing
        // all difficulty statistics while using the Hard Rock mod).
        val removeDifficultyAdjustmentMods =
            element is ModDifficultyAdjust &&
            element.cs != null &&
            element.ar != null &&
            element.od != null &&
            element.hp != null

        if (removeDifficultyAdjustmentMods) {
            remove(ModEasy::class)
            remove(ModHardRock::class)
            remove(ModReallyEasy::class)
        }

        // Check if there is any mod that is incompatible with the new mod.
        element.incompatibleMods.fastForEach {
            if (it in this) {
                remove(it)
            }
        }

        classSet.add(element::class.java)
        return super.add(element)
    }

    /**
     * Checks if this [ModHashSet] contains a [Mod] of the specified type.
     *
     * @param mod The [Mod] type to check for.
     * @return `true` if this [ModHashSet] contains a [Mod] of the specified type, `false` otherwise.
     */
    operator fun contains(mod: KClass<out Mod>) = mod.java in classSet

    /**
     * Checks if this [ModHashSet] contains a [Mod] of the specified type.
     *
     * @param mod The [Mod] type to check for.
     * @return `true` if this [ModHashSet] contains a [Mod] of the specified type, `false` otherwise.
     */
    operator fun contains(mod: Class<out Mod>) = mod in classSet

    /**
     * Gets a [Mod] of the specified type from this [ModHashSet].
     *
     * @param mod The [Mod] type to get.
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    operator fun <T : Mod> get(mod: KClass<out T>) = this[mod.java]

    /**
     * Gets a [Mod] of the specified type from this [ModHashSet].
     *
     * @param mod The [Mod] type to get.
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    operator fun <T : Mod> get(mod: Class<out T>): T? {
        if (mod !in classSet) {
            return null
        }

        for (m in this) {
            if (mod.isInstance(m)) {
                @Suppress("UNCHECKED_CAST")
                return m as T
            }
        }

        return null
    }

    /**
     * Removes a [Mod] of the specified type from this [ModHashSet].
     *
     * @param mod The [Mod] type to remove.
     * @return `true` if a [Mod] of the specified type was removed, `false` otherwise.
     */
    fun remove(mod: KClass<out Mod>) = remove(mod.java)

    /**
     * Removes a [Mod] of the specified type from this [ModHashSet].
     *
     * @param mod The [Mod] type to remove.
     * @return `true` if a [Mod] of the specified type was removed, `false` otherwise.
     */
    fun remove(mod: Class<out Mod>): Boolean {
        if (mod !in classSet) {
            return false
        }

        for (m in this) {
            if (mod.isInstance(m)) {
                return remove(m)
            }
        }

        return false
    }

    override fun remove(element: Mod): Boolean {
        val removed = super.remove(element)

        if (removed) {
            classSet.remove(element::class.java)
        }

        return removed
    }

    override fun clear() {
        super.clear()
        classSet.clear()
    }

    /**
     * Converts this [ModHashSet] to a [String] that can be displayed to the player.
     */
    fun toReadable(): String {
        if (isEmpty())
            return "None"

        return buildString {
            for (m in this@ModHashSet) when (m) {
                is ModFlashlight -> {
                    if (m.followDelay == ModFlashlight.DEFAULT_FOLLOW_DELAY)
                        append("${m.acronym}, ")
                    else
                        append("${m.acronym} ${(m.followDelay * 1000).toInt()}ms, ")
                }

                is IModUserSelectable -> append("${m.acronym}, ")

                is ModDifficultyAdjust -> {
                    if (m.ar != null) {
                        append("AR ${m.ar}, ")
                    }

                    if (m.od != null) {
                        append("OD ${m.od}, ")
                    }

                    if (m.cs != null) {
                        append("CS ${m.cs}, ")
                    }

                    if (m.hp != null) {
                        append("HP ${m.hp}, ")
                    }
                }

                is ModCustomSpeed -> append("${m.trackRateMultiplier}x, ")

                else -> Unit
            }
        }.substringBeforeLast(',')
    }

    /**
     * Converts this [ModHashSet] to a [EnumSet] of [GameMod]s.
     */
    fun toGameModSet(): EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java).also {
        for (m in this) {
            if (m is IModUserSelectable) {
                it.add(m.enum)
            }
        }
    }

    /**
     * Converts the container [Mod]s in this [ModHashSet] to their [String] representative.
     */
    fun getContainerModString() = buildString {
        var difficultyAdjust: ModDifficultyAdjust? = null
        var customSpeed: ModCustomSpeed? = null
        var flashlight: ModFlashlight? = null

        for (m in this@ModHashSet) when (m) {
            is ModDifficultyAdjust -> difficultyAdjust = m
            is ModCustomSpeed -> customSpeed = m
            is ModFlashlight -> flashlight = m
            else -> {
                if (difficultyAdjust != null && customSpeed != null && flashlight != null) {
                    break
                }

                continue
            }
        }

        if (difficultyAdjust == null && customSpeed == null && flashlight == null) {
            return@buildString
        }

        if (customSpeed != null) {
            append(String.format("x%.2f|", customSpeed.trackRateMultiplier))
        }

        difficultyAdjust?.let {
            if (it.ar != null) {
                append(String.format("AR%.1f|", it.ar))
            }

            if (it.od != null) {
                append(String.format("OD%.1f|", it.od))
            }

            if (it.cs != null) {
                append(String.format("CS%.1f|", it.cs))
            }

            if (it.hp != null) {
                append(String.format("HP%.1f|", it.hp))
            }
        }

        if (flashlight != null && flashlight.followDelay != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
            append(String.format("FLD%.2f|", flashlight.followDelay))
        }
    }.substringBeforeLast('|')

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is ModHashSet) {
            return false
        }

        return size == other.size && containsAll(other)
    }

    override fun hashCode() = fold(0) { acc, mod -> 31 * acc + mod.hashCode() }

    override fun toString() = buildString {
        modStringOrder.fastForEach {
            if (it::class.java in classSet) {
                append((it as IModUserSelectable).droidChar)
            }
        }

        append('|')
        append(getContainerModString())
    }

    companion object {
        /**
         * The order in which mods should be displayed.
         *
         * This is intentionally kept to keep the order consistent with what players are used to.
         */
        private val modStringOrder = arrayOf<Mod>(
            ModAuto(), ModRelax(), ModAutopilot(), ModEasy(), ModNoFail(), ModHardRock(),
            ModHidden(), ModFlashlight(), ModDoubleTime(), ModNightCore(), ModHalfTime(),
            ModPrecise(), ModReallyEasy(), ModPerfect(), ModSuddenDeath(), ModScoreV2()
        )
    }
}