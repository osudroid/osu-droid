package com.rian.osu.mods

import com.rian.osu.mods.settings.*
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
     * The type of this [Mod].
     */
    abstract val type: ModType

    /**
     * The suffix to append to [iconTextureName].
     *
     * This is a separate property to allow for custom icon texture names for certain [Mod]s
     * that do not follow the default naming convention.
     */
    protected open val iconTextureNameSuffix
        get() = name.replace(" ", "").lowercase()

    /**
     * The texture name of the icon of this [Mod].
     */
    val iconTextureName
        get() = "selection-mod-${iconTextureNameSuffix}"

    /**
     * Whether scores with this [Mod] active can be submitted online.
     */
    open val isRanked = false

    /**
     * Whether this [Mod] requires configuration by the user to make any meaningful changes to gameplay.
     */
    open val requiresConfiguration = false

    /**
     * Whether adding this [Mod] will affect gameplay.
     */
    open val isRelevant
        get() = !requiresConfiguration || !usesDefaultSettings

    /**
     * Whether this [Mod] is playable by a real human user.
     *
     * Should be `false` for cases where the user is not meant to apply the [Mod] by themselves.
     */
    open val isUserPlayable = true

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
     * The score multiplier for this [Mod].
     *
     * Note that some [Mod]s may require additional configuration to have a score multiplier (i.e., [ModDifficultyAdjust]
     * needs [IModRequiresOriginalBeatmap.applyFromBeatmap] to be called first).
     */
    open val scoreMultiplier = 1f

    /**
     * The [Mod]s this [Mod] cannot be enabled with. This is merely a static list of [KClass]es that this [Mod] is
     * incompatible with, regardless of the actual instance of the [Mod].
     *
     * Some [Mod]s may have additional compatibility requirements that are captured in [isCompatibleWith].
     * When checking for [Mod] compatibility, always use [isCompatibleWith].
     */
    open val incompatibleMods = emptyArray<KClass<out Mod>>()

    private var settingsBacking: List<ModSetting<Any?>>? = null

    /**
     * The mod specific settings.
     */
    @Suppress("UNCHECKED_CAST")
    val settings: List<ModSetting<Any?>>
        get() = settingsBacking ?: this::class.memberProperties.mapNotNull { property ->
            property as KProperty1<Mod, Any?>
            property.isAccessible = true
            property.getDelegate(this) as? ModSetting<Any?>
        }.sorted().also { settingsBacking = it }

    /**
     * Whether all [ModSetting]s in this [Mod] are set to their default values.
     */
    val usesDefaultSettings
        get() = settings.all { it.isDefault }

    /**
     * Determines whether this [Mod] is compatible with another [Mod].
     *
     * This extends [incompatibleMods] by allowing for dynamic checks against the actual instance of the [Mod] (i.e.,
     * its specific settings).
     *
     * @param other The [Mod] to check compatibility with.
     * @return `true` if this [Mod] is compatible with [other], `false` otherwise.
     */
    open fun isCompatibleWith(other: Mod) =
        incompatibleMods.none { it.isInstance(other) } &&
        other.incompatibleMods.none { it.isInstance(this) }

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
        result = 31 * result + iconTextureNameSuffix.hashCode()
        result = 31 * result + isRelevant.hashCode()
        result = 31 * result + isValidForMultiplayer.hashCode()
        result = 31 * result + isValidForMultiplayerAsFreeMod.hashCode()
        result = 31 * result + incompatibleMods.contentHashCode()

        for (setting in settings) {
            result = 31 * result + setting.value.hashCode()
        }

        return result
    }

    final override fun toString() = buildString {
        append(acronym)

        val extendedInformation = extraInformation

        if (extendedInformation.isNotEmpty()) {
            append(" ($extendedInformation)")
        }
    }

    /**
     * Extra information to be appended to the [toString] representation of this [Mod].
     */
    protected open val extraInformation = ""

    /**
     * Creates a deep copy of this [Mod].
     *
     * @return A deep copy of this [Mod].
     */
    abstract fun deepCopy(): Mod
}