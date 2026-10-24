package com.osudroid.mods

import com.osudroid.utils.allModsClassesByAcronym
import kotlin.reflect.full.createInstance
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Data class to allow serialization of [Mod]s.
 */
@Serializable
data class APIMod @JvmOverloads constructor(
    /**
     * The acronym of the [Mod].
     */
    val acronym: String,

    /**
     * The settings of the [Mod], if any.
     */
    val settings: JsonObject? = null
) {
    /**
     * Converts this [APIMod] to a [Mod].
     *
     * Returns `null` if [acronym] is not recognized.
     */
    fun toMod(): Mod? {
        val mod = allModsClassesByAcronym[acronym]?.createInstance() ?: return null

        if (settings != null) {
            mod.copySettings(settings)
        }

        return mod
    }
}
