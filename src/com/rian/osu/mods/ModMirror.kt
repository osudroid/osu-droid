package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.mods.settings.*
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

    /**
     * The axes along which to flip the [HitObject]s.
     */
    var reflection by EnumModSetting(
        name = "Flipped axes",
        valueFormatter = { it.name },
        defaultValue = MirrorType.Horizontal
    )

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        reflection = when (settings.optInt("flippedAxes")) {
            0 -> MirrorType.Horizontal
            1 -> MirrorType.Vertical
            2 -> MirrorType.Both
            else -> reflection
        }
    }

    override fun serializeSettings(): JSONObject? {
        if (!isRelevant) {
            return null
        }

        return JSONObject().apply {
            put("flippedAxes", reflection.ordinal)
        }
    }

    override fun applyToHitObject(
        mode: GameMode,
        hitObject: HitObject,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>
    ) {
        when (reflection) {
            MirrorType.Horizontal ->
                HitObjectGenerationUtils.reflectHorizontallyAlongPlayfield(hitObject)

            MirrorType.Vertical ->
                HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject)

            MirrorType.Both -> {
                HitObjectGenerationUtils.reflectHorizontallyAlongPlayfield(hitObject)
                HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject)
            }
        }
    }

    override val extraInformation: String
        get() {
            val settings = mutableListOf<Char>()

            if (reflection == MirrorType.Horizontal || reflection == MirrorType.Both) {
                settings.add('↔')
            }

            if (reflection == MirrorType.Vertical || reflection == MirrorType.Both) {
                settings.add('↕')
            }

            return settings.joinToString(", ")
        }

    override fun deepCopy() = ModMirror().also {
        it.reflection = reflection
    }

    enum class MirrorType {
        Horizontal,
        Vertical,
        Both
    }
}