package com.rian.osu.mods

/**
 * Represents the ScoreV2 mod.
 */
class ModScoreV2 : Mod(), IModUserSelectable {
    override val droidString = "v"
    override val acronym = "V2"

    override fun equals(other: Any?) = other === this || other is ModScoreV2
    override fun hashCode() = super.hashCode()
}