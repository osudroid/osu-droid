package com.rian.osu.mods.settings


open class BooleanModSetting(
    name: String,
    key: String? = null,
    defaultValue: Boolean,
    orderPosition: Int? = null
) : ModSetting<Boolean>(name, key, null, defaultValue, orderPosition)