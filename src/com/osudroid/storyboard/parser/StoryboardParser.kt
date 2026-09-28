package com.osudroid.storyboard.parser

import android.util.Log
import com.osudroid.storyboard.model.Storyboard
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import okio.buffer
import okio.source
import ru.nsu.ccfit.zuev.osu.helper.FileUtils

/**
 * A parser for the storyboard of a beatmap.
 *
 * Storyboard data is read from the `[Events]` section of both the beatmap's `.osu` file and the
 * beatmap set's `.osb` file (if present), with `.osb` elements rendering above `.osu` elements
 * within each layer. `$variable` definitions from the `[Variables]` section are substituted, and the
 * `WidescreenStoryboard` setting is read from the `.osu` file's `[General]` section.
 */
class StoryboardParser @JvmOverloads constructor(
    /**
     * The `.osu` file of the beatmap.
     */
    private val osuFile: File,

    /**
     * The [CoroutineScope] to use for cancellation checkpoints.
     */
    private val scope: CoroutineScope? = null
) {
    @JvmOverloads
    constructor(osuPath: String, scope: CoroutineScope? = null) : this(File(osuPath), scope)

    /**
     * Parses the storyboard of the beatmap.
     *
     * @return The parsed [Storyboard], or `null` if the beatmap has no storyboard elements.
     * @throws IOException If an I/O error occurs while reading the files.
     */
    @Throws(IOException::class)
    fun parse(): Storyboard? {
        val storyboard = Storyboard()

        // The .osu is parsed first and the .osb second, so that the .osb's elements render above
        // the .osu's elements within each layer. This matches osu!lazer, which prepends the
        // beatmap stream to the storyboard stream when decoding.
        parseFile(osuFile, storyboard)
        FileUtils.listFiles(osuFile.parentFile, ".osb").firstOrNull()?.let { parseFile(it, storyboard) }

        return if (storyboard.isEmpty) null else storyboard
    }

    private fun parseFile(file: File, storyboard: Storyboard) {
        var currentSection: Section? = null
        var timeOffset = 0.0
        var formatVersion = LATEST_FORMAT_VERSION
        val variables = mutableListOf<Pair<String, String>>()
        val eventsParser = StoryboardEventsParser(storyboard)

        file.source().buffer().use { source ->
            var isFirstLine = true

            while (true) {
                scope?.ensureActive()

                var line = source.readUtf8Line() ?: break

                if (isFirstLine) {
                    // Strip the UTF-8 byte order mark, if present.
                    line = line.removePrefix("\uFEFF")
                    isFirstLine = false
                }

                if (line.trimStart().startsWith("//")) {
                    continue
                }

                // Trailing comments are stripped, as in osu!lazer.
                val commentIndex = line.indexOf("//")

                if (commentIndex > 0) {
                    line = line.substring(0, commentIndex)
                }

                val trimmed = line.trim { it <= ' ' }

                if (trimmed.isEmpty()) {
                    continue
                }

                // The header may be absent in .osb files, in which case the latest behavior
                // (no additional time offset) is assumed. It can only appear before the first
                // section.
                if (currentSection == null) {
                    val versionMatch = FORMAT_VERSION_REGEX.find(trimmed)

                    if (versionMatch != null) {
                        formatVersion = versionMatch.groupValues[1].toIntOrNull() ?: continue

                        // Beatmap version 4 and lower had an incorrect offset. Stable has this
                        // set as 24ms off (see IBeatmap.getOffsetTime).
                        timeOffset = if (formatVersion < 5) 24.0 else 0.0
                        continue
                    }
                }

                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    eventsParser.finalizeElement()
                    currentSection = Section.parse(trimmed.substring(1, trimmed.length - 1))
                    continue
                }

                try {
                    when (currentSection) {
                        Section.Variables -> {
                            val separator = trimmed.indexOf('=')

                            if (trimmed.startsWith("$") && separator > 0) {
                                val name = trimmed.substring(0, separator)

                                // A redefinition replaces the earlier value.
                                variables.removeAll { it.first == name }
                                variables.add(name to trimmed.substring(separator + 1))
                                // Substitute longer names first so that a variable does not
                                // partially replace another variable it is a prefix of.
                                variables.sortByDescending { it.first.length }
                            }
                        }

                        Section.General -> {
                            val separator = trimmed.indexOf(':')

                            if (separator > 0) {
                                val value = trimmed.substring(separator + 1).trim()

                                when (trimmed.substring(0, separator).trim()) {
                                    "WidescreenStoryboard" -> storyboard.widescreen = value == "1"
                                    "UseSkinSprites" -> storyboard.useSkinSprites = value == "1"
                                }
                            }
                        }

                        // Events lines must keep their leading whitespace, as it encodes the
                        // command nesting depth.
                        Section.Events ->
                            eventsParser.parse(substituteVariables(line, variables), timeOffset, formatVersion)

                        else -> continue
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }

                    Log.e("StoryboardParser", "Unable to parse line: $trimmed", e)
                }
            }
        }

        eventsParser.finalizeElement()
    }

    private fun substituteVariables(line: String, variables: List<Pair<String, String>>): String {
        if (variables.isEmpty()) {
            return line
        }

        var result = line

        // Variables may expand to text containing further variables, so substitution is repeated
        // a bounded number of times (matching osu!lazer's single-pass behavior, extended to allow
        // a few levels of nesting). This must NOT loop until the string stabilizes: a
        // self-referencing definition (e.g. `$a=x$a`) would then never terminate, since each pass
        // makes the string longer while still containing a `$`.
        repeat(MAX_VARIABLE_SUBSTITUTION_PASSES) {
            if ('$' !in result) {
                return result
            }

            for ((name, value) in variables) {
                result = result.replace(name, value)
            }
        }

        return result
    }

    private enum class Section {
        General,
        Events,
        Variables;

        companion object {
            fun parse(name: String) = entries.firstOrNull { it.name == name }
        }
    }

    companion object {
        /**
         * Quickly determines whether a beatmap has a storyboard, without parsing it. Reading stops
         * at the first element or sample that is found.
         *
         * @param osuFile The `.osu` file of the beatmap.
         * @return Whether the `.osu` file or the `.osb` file of the beatmap set declares at least
         * one storyboard element or sample.
         */
        @JvmStatic
        fun hasStoryboard(osuFile: File): Boolean {
            if (declaresElements(osuFile)) {
                return true
            }

            val osbFile = FileUtils.listFiles(osuFile.parentFile, ".osb")?.firstOrNull()

            return osbFile != null && declaresElements(osbFile)
        }

        private fun declaresElements(file: File): Boolean {
            if (!file.isFile) {
                return false
            }

            var isInEvents = false

            file.source().buffer().use { source ->
                while (true) {
                    val line = source.readUtf8Line() ?: break

                    // Element declarations are not indented.
                    if (line.startsWith("[")) {
                        // The events section is not split up, so nothing follows after it.
                        if (isInEvents) {
                            break
                        }

                        isInEvents = line.trimEnd() == "[Events]"
                        continue
                    }

                    if (isInEvents && ELEMENT_PREFIXES.any { line.startsWith(it) }) {
                        return true
                    }
                }
            }

            return false
        }

        private val ELEMENT_PREFIXES = arrayOf("Sprite,", "Animation,", "Sample,", "4,", "5,", "6,")

        private const val LATEST_FORMAT_VERSION = 14
        private val FORMAT_VERSION_REGEX = "^osu file format v(\\d+)".toRegex()

        /**
         * The maximum number of `$variable` substitution passes performed on a single line, to
         * bound self-referencing or deeply nested definitions.
         */
        private const val MAX_VARIABLE_SUBSTITUTION_PASSES = 8
    }
}
