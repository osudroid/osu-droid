package com.rian.osu.mods

import com.rian.osu.beatmap.sections.BeatmapDifficulty
import org.json.JSONObject
import kotlin.reflect.*
import kotlin.reflect.full.*
import kotlin.reflect.jvm.*

/**
 * Represents a mod.
 */
sealed class Mod {
    /**
     * The name of this [Mod].
     */
    abstract val name: String

    /**
     * The acronym of this [Mod].
     */
    abstract val acronym: String

    /**
     * The user readable description of this [Mod].
     */
    abstract val description: String

    /**
     * The type fo this [Mod].
     */
    abstract val type: ModType

    /**
     * The suffix to append to the texture name of this [Mod].
     */
    protected open val textureNameSuffix
        get() = name.replace(" ", "").lowercase()

    /**
     * The texture name of this [Mod].
     */
    val textureName
        get() = "selection-mod-${textureNameSuffix}"

    /**
     * Whether scores with this [Mod] active can be submitted online.
     */
    open val isRanked = false

    /**
     * Whether adding this [Mod] will affect gameplay.
     */
    open val isRelevant = true

    /**
     * Whether this [Mod] is valid for multiplayer matches.
     *
     * Should be `false` for [Mod]s that make gameplay duration different across players.
     */
    open val isValidForMultiplayer = true

    /**
     * Whether this [Mod] is valid as a free mod in multiplayer matches.
     *
     * Should be `false` for [Mod]s that affect gameplay duration (e.g., [ModRateAdjust]).
     */
    open val isValidForMultiplayerAsFreeMod = true

    /**
     * The [Mod]s this [Mod] cannot be enabled with.
     */
    open val incompatibleMods = emptyArray<KClass<out Mod>>()

    /**
     * The mod specific settings.
     */
    @Suppress("UNCHECKED_CAST")
    open val settings
        get() = this::class.memberProperties.mapNotNull { property ->
            property as KProperty1<Mod, Any?>
            property.isAccessible = true
            property.getDelegate(this) as? ModSetting<Any?>
        }

    /**
     * Whether all [ModSetting]s in this [Mod] are set to their default values.
     */
    open val usesDefaultSettings
        get() = settings.all { it.isDefault }

    /**
     * Calculates the score multiplier for this [Mod] with the given [BeatmapDifficulty].
     *
     * @param difficulty The [BeatmapDifficulty] to calculate the score multiplier for.
     * @return The score multiplier for this [Mod] with the given [BeatmapDifficulty].
     */
    open fun calculateScoreMultiplier(difficulty: BeatmapDifficulty) = 1f

    /**
     * Serializes this [Mod] into a [JSONObject].
     *
     * The [JSONObject] will contain the following fields:
     *
     * - `acronym`: The acronym of this [Mod].
     * - `settings`: Settings specific to this [Mod] in a [JSONObject], if any.
     *
     * @return The serialized form of this [Mod] in a [JSONObject].
     */
    fun serialize() = JSONObject().apply {
        put("acronym", acronym)

        val settings = serializeSettings()

        if (settings != null) {
            put("settings", settings)
        }
    }

    /**
     * Copies the settings of this [Mod] from a [JSONObject].
     *
     * @param settings The [JSONObject] containing the settings to copy.
     */
    open fun copySettings(settings: JSONObject) {}

    /**
     * Serializes the settings of this [Mod] to a [JSONObject].
     *
     * @return The serialized settings of this [Mod], or `null` if this [Mod] has no settings.
     */
    protected open fun serializeSettings(): JSONObject? = null

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }

        if (other !is Mod) {
            return false
        }

        return hashCode() == other.hashCode()
    }

    override fun hashCode(): Int {
        var result = isRanked.hashCode()

        result = 31 * result + name.hashCode()
        result = 31 * result + acronym.hashCode()
        result = 31 * result + textureNameSuffix.hashCode()
        result = 31 * result + isRelevant.hashCode()
        result = 31 * result + isValidForMultiplayer.hashCode()
        result = 31 * result + isValidForMultiplayerAsFreeMod.hashCode()
        result = 31 * result + incompatibleMods.contentHashCode()

        for (setting in settings) {
            result = 31 * result + setting.value.hashCode()
        }

        return result
    }

    override fun toString(): String {
        return acronym
    }

    /**
     * Creates a deep copy of this [Mod].
     *
     * @return A deep copy of this [Mod].
     */
    abstract fun deepCopy(): Mod
}