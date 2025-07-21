package com.rian.osu.utils

import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.osu.mods.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance
import org.json.JSONArray

/**
 * A [HashMap] of [Mod]s with additional functionalities.
 */
open class ModHashMap : HashMap<Class<out Mod>, Mod> {
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
     * Do note that this method will remove any [Mod]s that are incompatible with the specified [Mod].
     *
     * @param mod The [Mod] to insert.
     * @return The [Mod] instance that was previously in this [ModHashMap], or `null` if there was no such [Mod].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> put(mod: T) = put(mod::class.java, mod) as? T

    /**
     * Inserts a new instance of the specified [Mod] type into this [ModHashMap].
     *
     * Do note that this method will remove any [Mod]s that are incompatible with the specified [Mod].
     *
     * @param mod The [Mod] type to insert.
     * @return The [Mod] instance that was previously in this [ModHashMap], or `null` if there was no such [Mod].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> put(mod: KClass<out T>) = put(mod.java, mod.createInstance()) as? T

    /**
     * Inserts a new instance of the specified [Mod] type into this [ModHashMap].
     *
     * Do note that this method will remove any [Mod]s that are incompatible with the specified [Mod].
     *
     * @param mod The [Mod] type to insert.
     * @return The [Mod] instance that was previously in this [ModHashMap], or `null` if there was no such [Mod].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> put(mod: Class<out T>) = put(mod, mod.getDeclaredConstructor().newInstance()) as? T

    override fun put(key: Class<out Mod>, value: Mod): Mod? {
        // Ensure the mod class corresponds to the mod itself.
        if (key != value::class.java) {
            throw IllegalArgumentException("The key class must correspond to the value class.")
        }

        // Check if there are any mods that are incompatible with the new mod.
        val iterator = iterator()
        while (iterator.hasNext()) {
            val (_, mod) = iterator.next()

            if (!value.isCompatibleWith(mod)) {
                iterator.remove()
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
    fun <T : Mod> remove(key: T) = remove(key::class.java) as? T

    /**
     * Removes a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to remove.
     * @return The removed [Mod] instance, or `null` if the [Mod] does not exist in this [ModHashMap].
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> remove(key: KClass<out T>) = remove(key.java) as? T

    /**
     * Removes a [Mod] of the specified type from this [ModHashMap].
     *
     * @param key The [Mod] type to remove.
     * @return The removed [Mod] instance, or `null` if the [Mod] does not exist in this [ModHashMap].
     */
    @JvmName("removeOfType")
    @Suppress("UNCHECKED_CAST")
    fun <T : Mod> remove(key: Class<out T>) = remove(key) as? T

    /**
     * Serializes the [Mod]s in this [ModHashMap] to a [JSONArray].
     *
     * @param includeNonUserPlayable Whether to include non-user-playable [Mod]s in the serialization.
     * Defaults to `true`.
     * @param includeIrrelevantMods Whether to include irrelevant [Mod]s in the serialization.
     * Defaults to `false`.
     * @return The serialized [Mod]s in a [JSONArray].
     */
    @JvmOverloads
    fun serializeMods(includeNonUserPlayable: Boolean = true, includeIrrelevantMods: Boolean = false) =
        ModUtils.serializeMods(values, includeNonUserPlayable, includeIrrelevantMods)

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
            append("x%.2f|".format(customSpeed.trackRateMultiplier))
        }

        difficultyAdjust?.let {
            if (it.ar != null) {
                append("AR%.1f|".format(it.ar))
            }

            if (it.od != null) {
                append("OD%.1f|".format(it.od))
            }

            if (it.cs != null) {
                append("CS%.1f|".format(it.cs))
            }

            if (it.hp != null) {
                append("HP%.1f|".format(it.hp))
            }
        }

        if (flashlight != null && flashlight.followDelay != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
            append("FLD%.2f|".format(flashlight.followDelay))
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

    /**
     * Converts this [ModHashMap] to its legacy mod string representation.
     */
    fun toLegacyModString() = buildString {
        modStringOrder.fastForEach {
            if (it::class in this@ModHashMap) {
                for ((k, v) in LegacyModConverter.legacyStorableMods) {
                    if (v.isInstance(this@ModHashMap[it::class])) {
                        append(k)
                        break
                    }
                }
            }
        }

        append('|')
        append(getContainerModString())
    }

    /**
     * Converts this [ModHashMap] to a string representation that can be used to display [Mod]s to the user.
     *
     * @param includeNonUserPlayable Whether to include non-user-playable [Mod]s in the string representation.
     * @return The string representation of the [Mod]s in this [ModHashMap].
     */
    @JvmOverloads
    fun toDisplayModString(includeNonUserPlayable: Boolean = true) = buildString {
        modStringOrder.fastForEach {
            if (!includeNonUserPlayable && !it.isUserPlayable) {
                return@fastForEach
            }

            if (it in this@ModHashMap) {
                append(this@ModHashMap[it::class]!!.toString() + ",")
            }
        }

        if (isEmpty()) {
            append('-')
        } else {
            deleteCharAt(length - 1)
        }
    }

    /**
     * Creates a deep copy of this [ModHashMap] and all [Mod]s inside of this [ModHashMap].
     */
    fun deepCopy() = ModHashMap().also {
        for ((k, v) in this) {
            it[k] = v.deepCopy()
        }
    }

    companion object {
        /**
         * The order in which mods should be displayed.
         *
         * This is intentionally kept to keep the order consistent with what players are used to.
         */
        private val modStringOrder = arrayOf<Mod>(
            ModAutoplay(),
            ModRelax(),
            ModAutopilot(),
            ModEasy(),
            ModNoFail(),
            ModHardRock(),
            ModDifficultyAdjust(),
            ModMirror(),
            ModRandom(),
            ModHidden(),
            ModApproachDifferent(),
            ModFreezeFrame(),
            ModTraceable(),
            ModFlashlight(),
            ModDoubleTime(),
            ModNightCore(),
            ModHalfTime(),
            ModCustomSpeed(),
            ModWindDown(),
            ModWindUp(),
            ModPrecise(),
            ModReallyEasy(),
            ModPerfect(),
            ModSuddenDeath(),
            ModMuted(),
            ModSynesthesia(),
            ModScoreV2(),
            ModReplayV6()
        )
    }
}