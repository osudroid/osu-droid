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
}