package com.rian.osu.mods

import ru.nsu.ccfit.zuev.osu.game.mods.GameMod

/**
 * An interface for [Mod]s which can be selected by the user.
 */
interface IModUserSelectable {
    /**
     * The osu!droid character representation of this [Mod].
     */
    val droidChar: Char

    /**
     * The acronym of this [Mod].
     */
    val acronym: String

    /**
     * The suffix to append to the texture name of this [Mod].
     */
    val textureNameSuffix: String

    /**
     * The texture name of this [Mod].
     */
    val textureName
        get() = "selection-mod-${textureNameSuffix}"

    /**
     * The [GameMod] equivalent of this [Mod]. Used in replay serialization.
     */
    val enum: GameMod
}