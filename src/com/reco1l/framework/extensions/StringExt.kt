@file:JvmName("StringUtil")
/*
 * @author Reco1l
 */

package com.reco1l.framework.extensions

import android.text.format.DateFormat
import org.json.JSONException
import org.json.JSONObject
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


/**
 * Converts a date pattern to a real date using [DateFormat].
 */
fun String.toDate() = DateFormat.format(this, Date()).toString()

/**
 * Decode in UTF8 the filename from an encoded URL String.
 *
 * @see [URLDecoder.decode]
 */
fun String.decodeUtf8() = URLDecoder.decode(this, StandardCharsets.UTF_8.name())

/**
 * Remove all characters in a filename that are invalid in the filesystem.
 */
fun String.forFilesystem() = replace("[^a-zA-Z0-9.\\-]".toRegex(), "_")


/**
 * Creates a JSON formatted String containing all class fields and its values.
 */
@Throws(JSONException::class)
fun Any.toStringExt(): String
{
    val json = JSONObject()

    this::class.memberProperties.forEach {
        try
        {
            it.isAccessible = true
            json.put(it.name, it.getter.call(this).toString())
        }
        catch (e: Exception)
        {
            json.put(it.name, "NO ACCESSIBLE")
        }
    }

    return "$className\n${json.toString(4)}"
}


inline fun <T : Any> T.toStringMap(block: (JSONObject, T) -> Unit): String
{
    val json = JSONObject()
    block(json, this)
    return json.toString(4)
}

fun <K, V> Map<out K, V>?.toStringEntries() = this?.toStringMap { json, map ->
    map.forEach { (key, value) -> json.put(key.toString(), value.toString()) }
} ?: "null"
