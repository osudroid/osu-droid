package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.utils.HitObjectGenerationUtils
import org.json.JSONObject

/**
 * Represents the Mirror mod.
 */
class ModMirror : Mod(), IModApplicableToHitObject {
    override val name = "Mirror"
    override val acronym = "MR"
    override val description = "Flip objects on the chosen axes."
    override val type = ModType.Conversion
    override val incompatibleMods = super.incompatibleMods + ModHardRock::class

    override val isRelevant
        get() = flipHorizontally || flipVertically

    /**
     * Whether to flip the [HitObject]s horizontally.
     */
    var flipHorizontally by BooleanModSetting(
        name = "Flip Horizontally",
        defaultValue = true
    )

    /**
     * Whether to flip the [HitObject]s vertically.
     */
    var flipVertically by BooleanModSetting(
        name = "Flip Vertically",
        defaultValue = false
    )

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        if (!settings.has("flippedAxes")) {
            return
        }

        val flippedAxes = settings.getInt("flippedAxes")
        flipHorizontally = flippedAxes and 1 == 1
        flipVertically = flippedAxes and 2 == 2
    }

    override fun serializeSettings(): JSONObject? {
        if (!isRelevant) {
            return null
        }

        return JSONObject().apply {
            put("flippedAxes", when {
                flipHorizontally && flipVertically -> 2
                flipHorizontally -> 0
                flipVertically -> 1
                else -> null
            })
        }
    }

    override fun applyToHitObject(mode: GameMode, hitObject: HitObject) {
        if (flipHorizontally) {
            HitObjectGenerationUtils.reflectHorizontallyAlongPlayfield(hitObject)
        }

        if (flipVertically) {
            HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject)
        }
    }

    override fun toString() = buildString {
        append(super.toString())

        if (isRelevant) {
            val settings = mutableListOf<Char>()

            if (flipHorizontally) {
                settings.add('↔')
            }

            if (flipVertically) {
                settings.add('↕')
            }

            append(" (${settings.joinToString(", ")})")
        }
    }

    override fun deepCopy() = ModMirror().also {
        it.flipHorizontally = flipHorizontally
        it.flipVertically = flipVertically
    }
}