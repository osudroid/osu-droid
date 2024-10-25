@file:JvmName("SkinConverter")

package com.reco1l.osu.skinning

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.Color
import com.reco1l.toolkt.data.putObject
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.math.min


fun convertToJson(ini: IniReader) = JSONObject().apply {

    fun convertToHex(ints: IntArray?): String?
    {
        if (ints == null || ints.isEmpty())
            return null

        return buildString {

            append('#')
            append("%02X".format(ints[0]))
            append("%02X".format(ints[1]))
            append("%02X".format(ints[2]))
        }
    }

    fun parseComboColors(ini: IniReader) = mutableListOf<String>().also {

        // osu! skins supports up to 7 combo colors
        for (i in 0 until 8)
        {
            val ints: IntArray = ini["Colours", "Combo${i + 1}"] ?: continue

            // At this point convertToHex() shouldn't return null, but we handle it anyway.
            convertToHex(ints)?.also { rgb -> it.add(rgb) }
        }

    }.takeUnless { it.isEmpty() }?.toTypedArray()


    putObject("Cursor") {

        put("rotateCursor", ini["General", "CursorRotate"] ?: true)
    }

    putObject("ComboColor") {

        parseComboColors(ini)?.also {

            val array = JSONArray(it)

            put("colors", array)
            put("forceOverride", true)
        }
    }

    putObject("Slider") {

        ini.get<IntArray?>("Colours", "SliderTrackOverride")?.also { trackColor ->

            val hintColor = IntArray(trackColor.size) { min(255, trackColor[it] + 15) }

            convertToHex(hintColor)?.also {

                put("sliderHintColor", it)
                put("sliderHintAlpha", 1f)
                put("sliderHintWidth", 25f)
                put("sliderHintEnable", true)
                put("sliderHintShowMinLength", 1f)
            }

            convertToHex(trackColor)?.also {

                put("sliderBodyColor", it)
                put("sliderBodyBaseAlpha", 1f)
                put("sliderFollowComboColor", false)
            }
        }

        put("sliderBorderColor", convertToHex(ini["Colours", "SliderBorder"]) ?: "#FFFFFF")
    }

    putObject("Color") {

        put("MenuItemSelectedTextColor", convertToHex(ini["Colours", "SongSelectActiveText"]) ?: "#FFFFFF")
        put("MenuItemDefaultTextColor", convertToHex(ini["Colours", "SongSelectInactiveText"]) ?: "#000000")
        put("MenuItemDefaultColor", "#EB4999") // Matching osu! stable inactive color.
    }

    putObject("Fonts") {

        put("hitCirclePrefix", ini["Fonts", "HitCirclePrefix"] ?: "default")
        put("hitCircleOverlap", ini["Fonts", "HitCircleOverlap"] ?: -2)
        put("scorePrefix", ini["Fonts", "ScorePrefix"] ?: "score")
        put("comboPrefix", ini["Fonts", "ComboPrefix"] ?: "score")
    }

    putObject("Utils") {
        put("comboTextScale", 0.8f)
        put("animationFramerate", ini["General", "AnimationFramerate"] ?: -1f)
        put("layeredHitSounds", ini.get<Boolean>("General", "LayeredHitSounds") ?: true)
        put("SliderBallFlip", ini.get<Boolean>("General", "SliderBallFlip") ?: true)
        put("spinnerFrequencyModulate", ini.get<Boolean>("General", "SpinnerFrequencyModulate") ?: true)
    }

    putObject("Layout") {

        putObject("BackButton") {
            put("scaleWhenHold", false)
        }
    }
}



fun ensureOptionalTexture(file: File)
{
    if (file.exists()) return

    val bitmap = Bitmap.createBitmap(1, 1, Config.ARGB_8888)
    bitmap.setHasAlpha(true)
    bitmap.setPixel(0, 0, Color.TRANSPARENT)

    try
    {
        FileOutputStream(file).use {
            bitmap.compress(CompressFormat.PNG, 100, it)
        }
    }
    catch (e: IOException) { e.printStackTrace() }
}

fun ensureTexture(file: File)
{
    if (!file.exists()) return

    BitmapFactory.decodeFile(file.path)?.apply {

        if (width <= 1 || height <= 1)
        {
            file.delete()
            return
        }

        val pixels = IntArray(width * height)
        getPixels(pixels, 0, width, 0, 0, width, height)

        if (pixels.all { Color.alpha(it) == 0 })
        {
            file.delete()
        }
    }
}
