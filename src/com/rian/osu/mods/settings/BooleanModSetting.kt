package com.rian.osu.mods.settings


open class BooleanModSetting(
    name: String,
    defaultValue: Boolean,
    orderPosition: Int? = null
) : ModSetting<Boolean>(name, null, defaultValue, orderPosition)
