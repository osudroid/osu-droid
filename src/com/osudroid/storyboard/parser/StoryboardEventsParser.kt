package com.osudroid.storyboard.parser

import com.edlplan.framework.easing.Easing
import com.osudroid.storyboard.model.AnimationLoopType
import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.model.StoryboardAnimation
import com.osudroid.storyboard.model.StoryboardColor
import com.osudroid.storyboard.model.StoryboardElement
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardOrigin
import com.osudroid.storyboard.model.StoryboardSample
import com.osudroid.storyboard.model.StoryboardSprite
import com.osudroid.storyboard.model.commands.CommandTimeline
import com.osudroid.storyboard.model.commands.StoryboardCommandGroup
import com.osudroid.storyboard.model.commands.StoryboardLoop
import com.osudroid.storyboard.model.commands.StoryboardTrigger
import com.osudroid.storyboard.model.commands.StoryboardTriggerType
import kotlin.math.max

/**
 * Parses the lines of an `[Events]` section into storyboard elements and commands.
 *
 * This parser is stateful: element declarations open an element, and subsequent indented lines
 * (leading spaces or underscores) add commands to it. A nesting depth of 2 adds commands to the
 * loop or trigger opened by the preceding `L`/`T` command.
 */
internal class StoryboardEventsParser(private val storyboard: Storyboard) {
    private var currentElement: StoryboardElement? = null
    private var currentSubGroup: StoryboardCommandGroup? = null

    /**
     * Parses a single line. The line must not be stripped of its leading whitespace.
     *
     * @param line The line to parse.
     * @param timeOffset The version-dependent time offset in milliseconds to add to absolute times.
     * @param formatVersion The format version of the file being parsed.
     */
    fun parse(line: String, timeOffset: Double, formatVersion: Int = 14) {
        var depth = 0

        while (depth < line.length && (line[depth] == ' ' || line[depth] == '_')) {
            ++depth
        }

        val content = line.substring(depth).trim { it <= ' ' }

        if (content.isEmpty()) {
            return
        }

        if (depth == 0) {
            finalizeElement()
            parseElement(content, timeOffset, formatVersion)
            return
        }

        val element = currentElement ?: return

        // A depth-1 line ends the loop/trigger opened by a preceding L/T command.
        if (depth == 1) {
            currentSubGroup = null
        }

        parseCommand(element, content, timeOffset)
    }

    /**
     * Closes the element currently under construction, if any.
     */
    fun finalizeElement() {
        currentElement = null
        currentSubGroup = null
    }

    private fun parseElement(content: String, timeOffset: Double, formatVersion: Int) {
        val parts = content.split(',')

        when (parts[0]) {
            "Sprite", "4" -> {
                currentElement = StoryboardSprite(
                    parseLayer(parts[1]),
                    StoryboardOrigin.parse(parts[2]),
                    cleanPath(parts[3]),
                    parts[4].toFloat(),
                    parts[5].toFloat()
                ).also { storyboard.add(it) }
            }

            "Animation", "6" -> {
                var frameDelay = parts[7].toDouble()

                if (formatVersion < 6) {
                    // This is random as hell but taken straight from osu!stable (via lazer).
                    frameDelay = Math.round(0.015 * frameDelay) * 1.186 * (1000 / 60.0)
                }

                currentElement = StoryboardAnimation(
                    parseLayer(parts[1]),
                    StoryboardOrigin.parse(parts[2]),
                    cleanPath(parts[3]),
                    parts[4].toFloat(),
                    parts[5].toFloat(),
                    max(1, parts[6].toInt()),
                    frameDelay,
                    AnimationLoopType.parse(parts.getOrNull(8))
                ).also { storyboard.add(it) }
            }

            "Sample", "5" -> {
                storyboard.samples.add(
                    StoryboardSample(
                        parseLayer(parts[2]),
                        parts[1].toDouble() + timeOffset,
                        cleanPath(parts[3]),
                        parts.getOrNull(4)?.toIntOrNull() ?: 100
                    )
                )
            }

            // The beatmap background is tracked for background replacement detection. Videos,
            // breaks and background colors are handled by the beatmap parser.
            "0" -> {
                if (parts.size >= 3 && storyboard.backgroundFilename == null) {
                    storyboard.backgroundFilename = cleanPath(parts[2])
                }
            }
        }
    }

    private fun parseCommand(element: StoryboardElement, content: String, timeOffset: Double) {
        val parts = content.split(',')

        when (parts[0]) {
            "L" -> {
                val loop = StoryboardLoop(
                    parts[1].toDouble() + timeOffset,
                    max(1, parts[2].toInt())
                )

                element.commands.loops.add(loop)
                currentSubGroup = loop
            }

            "T" -> {
                // Triggers with an unknown condition are kept (but never activate) so that their
                // commands are not mistaken for commands of the element itself.
                val type = StoryboardTriggerType.parse(parts[1]) ?: StoryboardTriggerType.Unsupported

                // The trigger window times are optional; a trigger without them can activate at
                // any point in time (matching osu!lazer).
                val trigger = StoryboardTrigger(
                    type,
                    parts.getOrNull(2)?.toDoubleOrNull()?.plus(timeOffset) ?: -Double.MAX_VALUE,
                    parts.getOrNull(3)?.toDoubleOrNull()?.plus(timeOffset) ?: Double.MAX_VALUE,
                    parts.getOrNull(4)?.toIntOrNull() ?: 0
                )

                element.commands.triggers.add(trigger)
                currentSubGroup = trigger
            }

            else -> parseValueCommand(element, parts, timeOffset)
        }
    }

    private fun parseValueCommand(element: StoryboardElement, parts: List<String>, timeOffset: Double) {
        val type = parts[0]

        val valueSize = when (type) {
            "M", "V" -> 2
            "C" -> 3
            "MX", "MY", "S", "R", "F", "P" -> 1
            else -> return
        }

        val group = currentSubGroup ?: element.commands

        // Command times inside loops and triggers are relative to the group's own start time, so
        // the version offset only applies to top-level commands.
        val offset = if (group === element.commands) timeOffset else 0.0

        // Out-of-range values fall back to no easing, matching osu!stable.
        val easing = Easing.entries.getOrElse(parts[1].toIntOrNull() ?: 0) { Easing.None }
        val startTime = parts[2].toDouble() + offset
        val endTime = (parts.getOrNull(3)?.toDoubleOrNull() ?: (startTime - offset)) + offset
        val duration = endTime - startTime

        if (type == "P") {
            // P commands are active during their interval. A zero-duration command applies
            // permanently, expressed by an end value of `true`.
            val timeline = when (parts.getOrNull(4)?.trim()) {
                "H" -> group.flipHorizontal
                "V" -> group.flipVertical
                "A" -> group.additiveBlend
                else -> return
            }

            timeline.add(easing, startTime, endTime, true, startTime == endTime)
            return
        }

        // Values are chunked into groups of `valueSize`. A single group means the value is held
        // (start = end), multiple groups produce a chain of commands of equal duration
        // (e.g. `F,0,0,1000,0,1,0` fades 0->1 over 0..1000, then 1->0 over 1000..2000).
        val values = parseValueGroups(parts, valueSize) ?: return

        fun addChained(timeline: CommandTimeline<Float>, index: Int) {
            if (values.size == 1) {
                timeline.add(easing, startTime, endTime, values[0][index], values[0][index])
                return
            }

            for (i in 0 until values.size - 1) {
                timeline.add(
                    easing,
                    startTime + i * duration, startTime + (i + 1) * duration,
                    values[i][index], values[i + 1][index]
                )
            }
        }

        when (type) {
            "M" -> {
                addChained(group.x, 0)
                addChained(group.y, 1)
            }

            "MX" -> addChained(group.x, 0)
            "MY" -> addChained(group.y, 0)
            "S" -> addChained(group.scale, 0)

            "V" -> {
                addChained(group.vectorScaleX, 0)
                addChained(group.vectorScaleY, 1)
            }

            "R" -> addChained(group.rotation, 0)
            "F" -> addChained(group.alpha, 0)

            "C" -> {
                fun colorAt(i: Int) = StoryboardColor(
                    values[i][0] / 255f,
                    values[i][1] / 255f,
                    values[i][2] / 255f
                )

                if (values.size == 1) {
                    group.color.add(easing, startTime, endTime, colorAt(0), colorAt(0))
                } else {
                    for (i in 0 until values.size - 1) {
                        group.color.add(
                            easing,
                            startTime + i * duration, startTime + (i + 1) * duration,
                            colorAt(i), colorAt(i + 1)
                        )
                    }
                }
            }
        }
    }

    /**
     * Chunks the value parameters of a command line (everything after the end time) into groups
     * of [valueSize] floats. Empty parameters repeat the value of the previous group at the same
     * position, matching osu!stable's behavior for shorthands like `S,0,0,1000,,0.5`.
     *
     * @return The value groups, or `null` if the line contains no usable values.
     */
    private fun parseValueGroups(parts: List<String>, valueSize: Int): List<FloatArray>? {
        val params = parts.drop(4)

        if (params.isEmpty()) {
            return null
        }

        val groupCount = (params.size + valueSize - 1) / valueSize
        val groups = ArrayList<FloatArray>(groupCount)

        for (i in 0 until groupCount) {
            val group = FloatArray(valueSize)

            for (j in 0 until valueSize) {
                val param = params.getOrNull(i * valueSize + j)?.trim()

                group[j] = when {
                    !param.isNullOrEmpty() -> param.toFloat()
                    // Missing or empty parameters repeat the previous group's value.
                    i > 0 -> groups[i - 1][j]
                    else -> return null
                }
            }

            groups.add(group)
        }

        return groups
    }

    /**
     * Parses a layer name. Unknown layers are rendered above the Foreground layer, as in
     * osu!lazer.
     */
    private fun parseLayer(value: String) =
        StoryboardLayerType.parse(value.trim()) ?: StoryboardLayerType.Custom

    private fun cleanPath(path: String) = path.trim().trim('"').replace('\\', '/')
}
