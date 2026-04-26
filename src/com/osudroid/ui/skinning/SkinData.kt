package com.osudroid.ui.skinning

import com.reco1l.framework.Color4
import com.reco1l.framework.HexComposition
import org.json.JSONObject

sealed class SkinData<T>(val tag: String, val defaultValue: T) {
    var currentValue = defaultValue

    open val isDefault
        get() = currentValue == defaultValue

    abstract fun setFromJson(data: JSONObject)
}

class IntegerSkinData(tag: String, defaultValue: Int) : SkinData<Int>(tag, defaultValue) {
    constructor(tag: String) : this(tag, 0)

    override fun setFromJson(data: JSONObject) {
        currentValue = data.optInt(tag, defaultValue)
    }
}

class FloatSkinData(tag: String, defaultValue: Float) : SkinData<Float>(tag, defaultValue) {
    constructor(tag: String) : this(tag, 0f)

    override fun setFromJson(data: JSONObject) {
        currentValue = data.optDouble(tag, defaultValue.toDouble()).toFloat()
    }
}

class BooleanSkinData(tag: String, defaultValue: Boolean) : SkinData<Boolean>(tag, defaultValue) {
    constructor(tag: String) : this(tag, false)

    override fun setFromJson(data: JSONObject) {
        currentValue = data.optBoolean(tag, defaultValue)
    }
}

class StringSkinData(tag: String, defaultValue: String) : SkinData<String>(tag, defaultValue) {
    constructor(tag: String) : this(tag, "")

    override fun setFromJson(data: JSONObject) {
        currentValue = data.optString(tag, defaultValue) ?: defaultValue
    }
}

class ColorSkinData(tag: String, defaultValue: Color4?) : SkinData<Color4?>(tag, defaultValue) {
    constructor(tag: String) : this(tag, null)

    override fun setFromJson(data: JSONObject) {
        val hex = data.optString(tag).trim()

        currentValue =
            if (hex.isEmpty()) defaultValue
            else Color4(hex, HexComposition.RRGGBB)
    }
}
