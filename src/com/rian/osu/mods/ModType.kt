package com.rian.osu.mods

import androidx.annotation.*
import com.osudroid.resources.R

/**
 * Available types of [Mod]s.
 */
enum class ModType(
    /**
     * The string resource ID of the localized mod type's name.
     */
    @StringRes val stringId: Int
) {
    DifficultyReduction(R.string.mod_section_difficulty_reduction),
    DifficultyIncrease(R.string.mod_section_difficulty_increase),
    Automation(R.string.mod_section_difficulty_automation),
    Conversion(R.string.mod_section_difficulty_conversion),
    Fun(R.string.mod_section_fun),
    System(R.string.mod_section_system),
}