package com.rian.osu.mods

/**
 * An interface for [Mod]s which can be selected by the user.
 */
interface IModUserSelectable {
    /**
     * The osu!droid string representation of this [Mod].
     */
    val droidString: String

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
}