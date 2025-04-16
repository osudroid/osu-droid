package com.rian.osu.mods

import androidx.annotation.*
import ru.nsu.ccfit.zuev.osuplus.*

/**
 * Available types of [Mod]s.
 */
enum class ModType(
    /**
     * The string resource ID of the lcoalised mod type's name.
     */
    @StringRes val stringId: Int
) {
    DifficultyReduction(R.string.mod_section_difficulty_reduction),
    DifficultyIncrease(R.string.mod_section_difficulty_increase),
    Automation(R.string.mod_section_difficulty_automation),
    Conversion(R.string.mod_section_difficulty_conversion)
}