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

class IntegerSkinData @JvmOverloads constructor(tag: String, default: Int = 0) : SkinData<Int>(tag, default) {
    override fun setFromJson(data: JSONObject) {
        currentValue = data.optInt(tag, defaultValue)
    }
}

class FloatSkinData @JvmOverloads constructor(tag: String, default: Float = 0f) : SkinData<Float>(tag, default) {
    override fun setFromJson(data: JSONObject) {
        currentValue = data.optDouble(tag, defaultValue.toDouble()).toFloat()
    }
}

class BooleanSkinData @JvmOverloads constructor(tag: String, default: Boolean = false) : SkinData<Boolean>(tag, default) {
    override fun setFromJson(data: JSONObject) {
        currentValue = data.optBoolean(tag, defaultValue)
    }
}

class StringSkinData @JvmOverloads constructor(tag: String, default: String = "") : SkinData<String>(tag, default) {
    override fun setFromJson(data: JSONObject) {
        currentValue = data.optString(tag, defaultValue)
    }
}

class ColorSkinData @JvmOverloads constructor(tag: String, default: Color4? = null) : SkinData<Color4?>(tag, default) {
    override fun setFromJson(data: JSONObject) {
        val hex = if (data.isNull(tag)) "" else data.optString(tag)

        currentValue = if (hex.isEmpty()) {
            defaultValue
        } else {
            try {
                Color4(hex, HexComposition.RRGGBB)
            } catch (_: NumberFormatException) {
                defaultValue
            }
        }
    }
}
