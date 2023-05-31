package ru.nsu.ccfit.zuev.skins

import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultString

class StringSkinData(tag: String, val default: String) : SkinData<String>(tag, DefaultString(default))
{
    override fun setFromJson(data: JSONObject?)
    {
        currentValue = data?.optString(tag, defaultValue) ?: defaultValue
    }
}