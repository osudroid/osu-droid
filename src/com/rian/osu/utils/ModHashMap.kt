package com.rian.osu.utils

import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.mods.*
import java.util.EnumSet
import kotlin.reflect.KClass
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * A [HashMap] of [Mod]s with additional functionalities.
 */
class ModHashMap : HashMap<Class<out Mod>, Mod> {
    constructor() : super()
    constructor(map: Map<out Class<out Mod>, Mod>) : super(map)
    constructor(mods: Collection<Mod>) : this(mods as Iterable<Mod>)
    constructor(initialCapacity: Int) : super(initialCapacity)
    constructor(initialCapacity: Int, loadFactor: Float) : super(initialCapacity, loadFactor)

    constructor(mods: Iterable<Mod>?) : super() {
        mods?.forEach { put(it::class.java, it) }
    }

    /**
     * Inserts the specified [Mod] into this [ModHashMap].
     *
     * @param mod The [Mod] to insert.
     * @return The [Mod] instance that was previously in this [ModHashMap], or `null` if there was no such [Mod].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> put(mod: T) = put(mod::class.java, mod) as? T

    override fun put(key: Class<out Mod>, value: Mod): Mod? {
        // Ensure the mod class corresponds to the mod itself.
        if (key != value::class.java) {
            throw IllegalArgumentException("The key class must correspond to the value class.")
        }

        // If all difficulty statistics are set, all other difficulty adjusting mods are irrelevant, so we remove them.
        // This prevents potential abuse cases where score multipliers from non-affecting mods stack (i.e., forcing
        // all difficulty statistics while using the Hard Rock mod).
        val removeDifficultyAdjustmentMods =
            value is ModDifficultyAdjust &&
            value.cs != null &&
            value.ar != null &&
            value.od != null &&
            value.hp != null

        if (removeDifficultyAdjustmentMods) {
            remove(ModEasy::class)
            remove(ModHardRock::class)
            remove(ModReallyEasy::class)
        }

        // Check if there are any mods that are incompatible with the new mod.
        value.incompatibleMods.fastForEach {
            if (it in this) {
                remove(it)
            }
        }

        return super.put(key, value)
    }

    /**
     * Checks if this [ModHashMap] contains a [Mod] of the specified type.
     *
     * @param key The [Mod] type to check for.
     * @return `true` if this [ModHashMap] contains a [Mod] of the specified type, `false` otherwise.
     */
    operator fun contains(key: KClass<out Mod>) = containsKey(key.java)

    /**
     * Checks if this [ModHashMap] contains a [Mod] of the specified type.
     *
     * @param key The [Mod] type to check for.
     * @return `true` if this [ModHashMap] contains a [Mod] of the specified type, `false` otherwise.
     */
    operator fun contains(key: Class<out Mod>) = containsKey(key)

    /**
     * Checks if this [ModHashMap] contains a [Mod].
     *
     * @param value The [Mod] to check for.
     * @return `true` if this [ModHashMap] contains the [Mod], `false` otherwise.
     */
    operator fun contains(value: Mod) = value::class in this

    /**
     * Gets a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to get.
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    @JvmName("ofType")
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Mod> get(key: KClass<out T>) = get(key.java) as? T

    /**
     * Gets a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to get.
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    @JvmName("ofType")
    @Suppress("UNCHECKED_CAST")
    operator fun <T : Mod> get(key: Class<out T>) = get(key) as? T

    /**
     * Gets a [Mod] of the specified type from this [ModHashMap].
     *
     * @return The [Mod] of the specified type, or `null` if no such [Mod] exists.
     */
    inline fun <reified T : Mod> ofType() = get(T::class)

    /**
     * Removes a [Mod] from this [ModHashMap].
     *
     * @param key The [Mod] to remove.
     * @return The removed [Mod] instance, or `null` if the [Mod] does not exist in this [ModHashMap].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> remove(key: Mod) = remove(key::class.java) as? T

    /**
     * Removes a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to remove.
     * @return The removed [Mod] instance, or `null` if the [Mod] does not exist in this [ModHashMap].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> remove(key: KClass<out T>) = remove(key.java) as? T

    /**
     * Converts this [ModHashMap] to a [String] that can be displayed to the player.
     */
    fun toReadable(): String {
        if (isEmpty())
            return "None"

        return buildString {
            val difficultyAdjust = ofType<ModDifficultyAdjust>()
            val customSpeed = ofType<ModCustomSpeed>()

            for ((_, m) in this@ModHashMap) when (m) {
                is ModFlashlight -> {
                    if (m.followDelay == ModFlashlight.DEFAULT_FOLLOW_DELAY)
                        append("${m.acronym}, ")
                    else
                        append("${m.acronym} ${(m.followDelay * 1000).toInt()}ms, ")
                }

                is IModUserSelectable -> append("${m.acronym}, ")

                else -> Unit
            }

            if (customSpeed != null) {
                append("%.2fx, ".format(customSpeed.trackRateMultiplier))
            }

            if (difficultyAdjust != null) {
                if (difficultyAdjust.ar != null) {
                    append("AR%.1f, ".format(difficultyAdjust.ar))
                }

                if (difficultyAdjust.od != null) {
                    append("OD%.1f, ".format(difficultyAdjust.od))
                }

                if (difficultyAdjust.cs != null) {
                    append("CS%.1f, ".format(difficultyAdjust.cs))
                }

                if (difficultyAdjust.hp != null) {
                    append("HP%.1f, ".format(difficultyAdjust.hp))
                }
            }
        }.substringBeforeLast(',')
    }

    /**
     * Converts this [ModHashMap] to a [EnumSet] of [GameMod]s.
     */
    fun toGameModSet(): EnumSet<GameMod> = EnumSet.noneOf(GameMod::class.java).also {
        for ((_, m) in this) {
            if (m is IModUserSelectable) {
                it.add(m.enum)
            }
        }
    }

    /**
     * Converts the container [Mod]s in this [ModHashMap] to their [String] representative.
     */
    fun getContainerModString() = buildString {
        val difficultyAdjust = ofType<ModDifficultyAdjust>()
        val customSpeed = ofType<ModCustomSpeed>()
        val flashlight = ofType<ModFlashlight>()

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

        if (other !is ModHashMap) {
            return false
        }

        if (size != other.size) {
            return false
        }

        for ((k, v) in this) {
            if (v != other[k]) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 0

        for ((_, v) in this) {
            result = 31 * result + v.hashCode()
        }

        return result
    }

    override fun toString() = buildString {
        modStringOrder.fastForEach {
            if (it::class in this@ModHashMap) {
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