package com.deltaflyer.osu

import org.json.JSONObject


class BlockAreaConfig {
    var activated = false
    var rects: MutableList<Rect> = mutableListOf()

    companion object {
        fun fromJson(jsonString: String?): BlockAreaConfig {
            val config = BlockAreaConfig()
            if (jsonString != null && jsonString !== "null") {
                val json = JSONObject(jsonString)
                config.activated = json.getBoolean("activated")
                config.rects = ArrayList()
                json.optJSONArray("rects")?.let { rectsJson ->
                    for (i in 0 until rectsJson.length()) {
                        config.rects.add(Rect.fromJson(rectsJson.getJSONObject(i)))
                    }
                }
            }
            return config
        }
    }
}


class Rect {
    var top = 0.0
    var bottom = 0.0
    var left = 0.0
    var right = 0.0

    companion object {
        fun fromJson(jsonObject: JSONObject): Rect {
            val rect = Rect()
            rect.top = jsonObject.getDouble("top")
            rect.bottom = jsonObject.getDouble("bottom")
            rect.left = jsonObject.getDouble("left")
            rect.right = jsonObject.getDouble("right")
            return rect
        }
    }
}

