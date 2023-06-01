@file:JvmName("SkinConverter")

package com.reco1l.legacy.data

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.Bitmap.Config
import android.graphics.BitmapFactory
import android.graphics.Color
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

//--------------------------------------------------------------------------------------------------------------------//

fun ensureOptionalTexture(file: File)
{
    if (!file.exists()) return

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

//--------------------------------------------------------------------------------------------------------------------//

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
