/*
 * @author Reco1l
 */

package com.reco1l.framework.data

import android.util.Log
import okhttp3.internal.toImmutableMap
import okio.buffer
import okio.source
import java.io.Closeable
import java.io.File

/**
 * Basic INI reader meant only for osu! skin.ini format.
 * It only supports [Boolean], [Int], [Float] and [IntArray] types.
 *
 * @author Reco1l
 */
class IniReader(file: File) : Closeable
{

    private val buffer = file.source().buffer()

    private val map = HashMap<String, HashMap<String, Any>>()

    private var currentLine: String? = null

    private var currentSection: HashMap<String, Any>? = null

    //----------------------------------------------------------------------------------------------------------------//

    init
    {
        if (file.extension.lowercase() != EXTENSION)
        {
            throw IniException("Not an INI file.")
        }
        read()
    }

    override fun close() = buffer.close()

    //----------------------------------------------------------------------------------------------------------------//

    private fun parseObject(input: String): Any
    {
        val value = input.trim()
        val sanitized = value.substringBefore("//").trim()

        // Boolean
        if (value == "0" || value == "1")
        {
            return value == "1"
        }

        // Integer
        if (Regex("^-?\\d+$").matches(sanitized))
        {
            return sanitized.toIntOrNull() ?: value
        }

        // Decimal
        if (Regex("^-?\\d+\\.\\d+$").matches(sanitized))
        {
            return sanitized.toFloatOrNull() ?: value
        }

        // IntArray
        if (Regex("^\\d+(?:\\s*,\\s*\\d+)*$").matches(sanitized))
        {
            return sanitized.split(",")
                    .mapNotNull { it.trim().toIntOrNull() }
                    .takeUnless { it.isEmpty() }
                    ?.toIntArray() ?: value
        }

        // String
        return value
    }

    //----------------------------------------------------------------------------------------------------------------//

    private fun readNextLine(): Boolean
    {
        currentLine = buffer.readUtf8Line()
        return currentLine != null
    }

    /**
     * Read the INI loaded, consider using it inside a try-catch statement.
     */
    fun read()
    {
        // Using mr. Rian code as reference.
        while (readNextLine()) currentLine?.let { input ->

            val line = input.trim()

            // Handle C++ style comments and empty lines
            if (line.startsWith("//") || line.isEmpty())
            {
                return@let
            }

            // [SectionName]
            if (line.startsWith("[") && line.endsWith("]"))
            {
                val sectionName = line.substring(1, line.length - 1)

                if (map[sectionName] == null)
                {
                    currentSection = HashMap()
                    map[sectionName] = currentSection!!

                    Log.i("IniReader", line)
                }
                return@let
            }

            val command = line.substringBefore(':').takeIf { Regex("^[a-zA-Z0-9]+\$").matches(it) } ?: return@let
            val value = line.substringAfter(':')

            val o = parseObject(value)
            currentSection?.put(command, o)

            Log.i("IniReader", "$command: ${if (o is IntArray) o.contentToString() else o}")
        }
        buffer.close()
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Returns the desired section map.
     */
    operator fun get(section: String): Map<String, Any>? = map[section]?.toImmutableMap()

    /**
     * Returns the value from the desired command and section cast as the inferred type or `null`.
     */
    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(section: String, key: String): T? = map[section]?.get(key) as? T

    //----------------------------------------------------------------------------------------------------------------//

    companion object
    {
        /**
         * `.ini` extension constant.
         */
        const val EXTENSION = "ini"
    }
}