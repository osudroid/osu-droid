package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.mods.settings.*
import com.rian.osu.utils.HitObjectGenerationUtils
import kotlinx.coroutines.CoroutineScope

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
        key = "flippedAxes",
        valueFormatter = { it.name },
        defaultValue = MirrorType.Horizontal
    )

    override fun applyToHitObject(
        mode: GameMode,
        hitObject: HitObject,
        adjustmentMods: Iterable<IModFacilitatesAdjustment>,
        scope: CoroutineScope?
    ) {
        when (reflection) {
            MirrorType.Horizontal ->
                HitObjectGenerationUtils.reflectHorizontallyAlongPlayfield(hitObject, scope)

            MirrorType.Vertical ->
                HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject, scope)

            MirrorType.Both -> {
                HitObjectGenerationUtils.reflectHorizontallyAlongPlayfield(hitObject, scope)
                HitObjectGenerationUtils.reflectVerticallyAlongPlayfield(hitObject, scope)
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

    enum class MirrorType {
        Horizontal,
        Vertical,
        Both
    }
}