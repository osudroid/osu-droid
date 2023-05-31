@file:JvmName("SkinIniConverter")

package com.reco1l.legacy.data

import com.reco1l.framework.data.IniReader
import com.reco1l.framework.net.JsonContent
import org.json.JSONArray
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.ToastLogger
import java.io.File
import java.io.FileWriter
import java.io.IOException

//--------------------------------------------------------------------------------------------------------------------//

fun convertToJson(ini: IniReader) = JsonContent().apply {

    putGroup("Cursor").apply {

        put("rotateCursor", ini["General", "RotateCursor"] ?: true)
    }

    putGroup("ComboColor").apply {

        parseComboColors(ini)?.also {

            val array = JSONArray(it)

            put("colors", array)
            put("forceOverride", true)
        }
    }

    putGroup("Slider").apply {

        convertToHex(ini["Colours", "SliderTrackOverride"])?.also {

            put("sliderBodyColor", it)
            put("sliderFollowComboColor", false)
        }

        convertToHex(ini["Colours", "SliderBorder"])?.also {

            put("sliderBorderColor", it)
        }
    }

    putGroup("Color").apply {

        convertToHex(ini["Colours", "SongSelectActiveText"])?.also {

            put("MenuItemSelectedTextColor", it)
        }

        convertToHex(ini["Colours", "SongSelectInactiveText"])?.also {

            put("MenuItemDefaultTextColor", it)
        }
    }

    putGroup("Fonts").apply {

        put("hitCirclePrefix", ini["Fonts", "HitCirclePrefix"] ?: "default")
        put("scorePrefix", ini["Fonts", "ScorePrefix"] ?: "score")
        put("comboPrefix", ini["Fonts", "ComboPrefix"] ?: "score")
    }
}

//--------------------------------------------------------------------------------------------------------------------//

private fun convertToHex(ints: IntArray?): String?
{
    if (ints == null || ints.isEmpty())
    {
        return null
    }
    val hex = StringBuilder("#")

    hex.append("%02X".format(ints[0]))
    hex.append("%02X".format(ints[1]))
    hex.append("%02X".format(ints[2]))

    return hex.toString()
}

private fun parseComboColors(ini: IniReader) = mutableListOf<String>().also {

    // osu! skins supports up to 7 combo colors
    for (i in 0 until 8)
    {
        val ints: IntArray = ini["Colours", "Combo${i + 1}"] ?: continue

        // At this point convertToHex() shouldn't return null, but we handle it anyway.
        convertToHex(ints)?.also { rgb -> it.add(rgb) }
    }

}.takeUnless { it.isEmpty() }?.toTypedArray()

//--------------------------------------------------------------------------------------------------------------------//

fun JSONObject.saveToFile(file: File) = try
{
    FileWriter(file).use {

        it.write(toString(4))
        it.flush()
    }
}
catch (e: IOException)
{
    e.printStackTrace()
    ToastLogger.showText("Failed to save converted JSON file.", true)
}