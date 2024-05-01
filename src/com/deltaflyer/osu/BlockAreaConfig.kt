package com.deltaflyer.osu

import org.json.JSONObject


class BlockAreaConfig {
    var activated = false
    var rects = mutableListOf<Rect>()

    companion object {
        fun fromJson(jsonString: String?) = BlockAreaConfig().also {
            if (jsonString != null && jsonString !== "null") {
                val json = JSONObject(jsonString)
                it.activated = json.getBoolean("activated")
                it.rects = ArrayList()
                json.optJSONArray("rects")?.let { rectsJson ->
                    for (i in 0 until rectsJson.length()) {
                        it.rects.add(Rect.fromJson(rectsJson.getJSONObject(i)))
                    }
                }
            }
        }
    }
}


class Rect {
    var top = 0.0
    var bottom = 0.0
    var left = 0.0
    var right = 0.0

    companion object {
        fun fromJson(jsonObject: JSONObject) = Rect().also {
            it.top = jsonObject.getDouble("top")
            it.bottom = jsonObject.getDouble("bottom")
            it.left = jsonObject.getDouble("left")
            it.right = jsonObject.getDouble("right")
        }
    }
}

