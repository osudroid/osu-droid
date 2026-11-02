package com.osudroid.storyboard.playback

import com.edlplan.framework.easing.Easing
import com.osudroid.storyboard.model.StoryboardColor
import com.osudroid.storyboard.model.commands.CommandTimeline
import com.reco1l.framework.interpolate

/**
 * The commands of one property of a sprite in flat arrays, evaluated with a cursor instead of a
 * search: playback mostly moves forward, so the command in effect is found by stepping on from the
 * one of the previous evaluation. Between commands, and after a command ended, the value stays the
 * same until the next command starts; that span is remembered, so a property that is not being
 * animated costs a single comparison per evaluation.
 *
 * The result equals evaluating the [CommandTimeline] the track was built from: the command in
 * effect is the last one that started at or before the time, interpolated by its eased progress.
 */
abstract class PropertyTrack(timeline: CommandTimeline<*>) {

    protected val size = timeline.size
    protected val startTimes = DoubleArray(size)
    protected val endTimes = DoubleArray(size)
    protected val easings: Array<Easing> = Array(size) { timeline[it].easing }

    // Index of the command in effect at the last evaluated time; -1 before the first command.
    private var cursor = -1

    // The last evaluated value holds for times in [constantFrom, constantUntil).
    protected var constantFrom = Double.POSITIVE_INFINITY
    protected var constantUntil = Double.NEGATIVE_INFINITY

    init {
        var i = 0
        for (command in timeline) {
            startTimes[i] = command.startTime
            endTimes[i] = command.endTime
            i++
        }
    }

    /**
     * Brings the value up to date with [time]. Returns false if the value from the previous call
     * still holds, so the caller can keep it.
     */
    protected fun advance(time: Double): Boolean {
        if (time >= constantFrom && time < constantUntil) {
            return false
        }

        var index = cursor
        if (index >= 0 && time < startTimes[index] ||
            index + FAR_AHEAD < size && startTimes[index + FAR_AHEAD] <= time
        ) {
            // Moved back, or far ahead (a seek): find the command again.
            index = search(time)
        } else {
            while (index + 1 < size && startTimes[index + 1] <= time) {
                index++
            }
        }
        cursor = index

        val nextStart = if (index + 1 < size) startTimes[index + 1] else Double.POSITIVE_INFINITY

        if (index < 0) {
            constantFrom = Double.NEGATIVE_INFINITY
            constantUntil = nextStart
            evaluateBefore()
        } else {
            evaluate(index, time)
            setConstantSpan(index, time, nextStart)
        }
        return true
    }

    /**
     * Sets [constantFrom] and [constantUntil] to the span around [time] in which command [index]
     * (in effect until [nextStart]) keeps the value just evaluated, or to an empty span while it
     * changes. For interpolated values: from the end of the command on, never before its start.
     */
    protected open fun setConstantSpan(index: Int, time: Double, nextStart: Double) {
        val start = startTimes[index]
        val end = endTimes[index]
        if (time >= end) {
            constantFrom = maxOf(start, end)
            constantUntil = nextStart
        } else {
            constantFrom = Double.POSITIVE_INFINITY
            constantUntil = Double.NEGATIVE_INFINITY
        }
    }

    /** Sets the value before the first command. */
    protected abstract fun evaluateBefore()

    /** Sets the value of command [index] at [time]. */
    protected abstract fun evaluate(index: Int, time: Double)

    /** The eased progress of command [index] at [time], computed like the command itself does. */
    protected fun progressAt(index: Int, time: Double): Float {
        val start = startTimes[index]
        val duration = endTimes[index] - start
        if (duration <= 0) {
            return 1f
        }
        val progress = ((time - start) / duration).coerceIn(0.0, 1.0)
        return easings[index].interpolate(progress.toFloat())
    }

    private fun search(time: Double): Int {
        var low = 0
        var high = size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) ushr 1
            if (startTimes[mid] <= time) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    companion object {
        // Commands to step over at most before searching instead.
        private const val FAR_AHEAD = 16

        /** A track for [timeline], or null if it has no commands. */
        @JvmStatic
        fun floatOf(timeline: CommandTimeline<Float>): FloatTrack? =
            if (timeline.hasCommands) FloatTrack(timeline) else null

        /** A track for [timeline], or null if it has no commands. */
        @JvmStatic
        fun colorOf(timeline: CommandTimeline<StoryboardColor>): ColorTrack? =
            if (timeline.hasCommands) ColorTrack(timeline) else null

        /** A track for [timeline], or null if it has no commands. */
        @JvmStatic
        fun booleanOf(timeline: CommandTimeline<Boolean>): BooleanTrack? =
            if (timeline.hasCommands) BooleanTrack(timeline) else null
    }
}

/** A float property; before its first command it takes the first command's start value. */
class FloatTrack(timeline: CommandTimeline<Float>) : PropertyTrack(timeline) {

    private val startValues = FloatArray(size)
    private val endValues = FloatArray(size)
    private var value = 0f

    init {
        var i = 0
        for (command in timeline) {
            startValues[i] = command.startValue
            endValues[i] = command.endValue
            i++
        }
    }

    /** The value at [time]. */
    fun valueAt(time: Double): Float {
        advance(time)
        return value
    }

    override fun evaluateBefore() {
        value = startValues[0]
    }

    override fun evaluate(index: Int, time: Double) {
        val start = startValues[index]
        value = start + (endValues[index] - start) * progressAt(index, time)
    }
}

/** A color property; before its first command it takes the first command's start color. */
class ColorTrack(timeline: CommandTimeline<StoryboardColor>) : PropertyTrack(timeline) {

    // Red, green and blue of the start and end colors, three floats per command.
    private val startValues = FloatArray(size * 3)
    private val endValues = FloatArray(size * 3)

    @JvmField var red = 1f
    @JvmField var green = 1f
    @JvmField var blue = 1f

    init {
        var i = 0
        for (command in timeline) {
            startValues[i] = command.startValue.red
            startValues[i + 1] = command.startValue.green
            startValues[i + 2] = command.startValue.blue
            endValues[i] = command.endValue.red
            endValues[i + 1] = command.endValue.green
            endValues[i + 2] = command.endValue.blue
            i += 3
        }
    }

    /** Brings [red], [green] and [blue] up to date with [time]. */
    fun update(time: Double) {
        advance(time)
    }

    override fun evaluateBefore() {
        red = startValues[0]
        green = startValues[1]
        blue = startValues[2]
    }

    override fun evaluate(index: Int, time: Double) {
        val progress = progressAt(index, time)
        val i = index * 3
        red = startValues[i] + (endValues[i] - startValues[i]) * progress
        green = startValues[i + 1] + (endValues[i + 1] - startValues[i + 1]) * progress
        blue = startValues[i + 2] + (endValues[i + 2] - startValues[i + 2]) * progress
    }
}

/**
 * A boolean property (flips, additive blending): false before the first command, the command's
 * start value during it and its end value after it.
 */
class BooleanTrack(timeline: CommandTimeline<Boolean>) : PropertyTrack(timeline) {

    private val startValues = BooleanArray(size)
    private val endValues = BooleanArray(size)
    private var value = false

    init {
        var i = 0
        for (command in timeline) {
            startValues[i] = command.startValue
            endValues[i] = command.endValue
            i++
        }
    }

    /** The value at [time]. */
    fun valueAt(time: Double): Boolean {
        advance(time)
        return value
    }

    // Constant during the command (start value) and after it (end value).
    override fun setConstantSpan(index: Int, time: Double, nextStart: Double) {
        val start = startTimes[index]
        val end = endTimes[index]
        if (time < end) {
            constantFrom = start
            constantUntil = minOf(end, nextStart)
        } else {
            constantFrom = maxOf(start, end)
            constantUntil = nextStart
        }
    }

    override fun evaluateBefore() {
        value = false
    }

    override fun evaluate(index: Int, time: Double) {
        value = if (time < endTimes[index]) startValues[index] else endValues[index]
    }
}
