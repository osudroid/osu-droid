package com.rian.osu.mods

import com.rian.osu.beatmap.hitobject.HitObject
import org.json.JSONObject

/**
 * Represents a [Mod] that adjusts the size of [HitObject]s during their fade in animation.
 */
abstract class ModObjectScaleTween : Mod() {
    override val type = ModType.Fun
    override val incompatibleMods = super.incompatibleMods + arrayOf(ModObjectScaleTween::class, ModTraceable::class)

    /**
     * The initial size multiplier applied to all [HitObject]s.
     */
    abstract var startScale: Float

    /**
     * The final size multiplier applied to all [HitObject]s.
     */
    open var endScale = 1f

    override fun copySettings(settings: JSONObject) {
        super.copySettings(settings)

        startScale = settings.optDouble("startScale", startScale.toDouble()).toFloat()
    }

    override fun serializeSettings() = JSONObject().apply {
        put("startScale", startScale)
    }
}