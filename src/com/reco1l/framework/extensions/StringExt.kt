@file:JvmName("StringUtil")
/*
 * @author Reco1l
 */

package com.reco1l.framework.extensions

import android.text.format.DateFormat
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*


/**
 * Converts a date pattern to a real date using [DateFormat].
 */
fun String.toDate() = DateFormat.format(this, Date()).toString()

/**
 * Decode in UTF8 the filename from an encoded URL String.
 *
 * @see [URLDecoder.decode]
 */
fun String.decodeUtf8(): String = URLDecoder.decode(this, StandardCharsets.UTF_8.name())

/**
 * Remove all characters in a filename that are invalid in the filesystem.
 */
fun String.forFilesystem() = replace("[^a-zA-Z0-9.\\-]".toRegex(), "_")
