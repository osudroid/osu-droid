package com.osudroid.storyboard.playback

import com.edlplan.framework.easing.Easing
import com.osudroid.storyboard.model.StoryboardColor
import com.osudroid.storyboard.model.commands.CommandTimeline
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PropertyTrackTest {

    // The evaluation the tracks replace: search the last started command, interpolate it.

    private fun referenceFloat(timeline: CommandTimeline<Float>, time: Double): Float {
        val index = timeline.indexAt(time)
        if (index < 0) return timeline.startValue!!
        val command = timeline[index]
        val progress = command.progressAt(time).toFloat()
        return command.startValue + (command.endValue - command.startValue) * progress
    }

    private fun referenceColor(timeline: CommandTimeline<StoryboardColor>, time: Double): FloatArray {
        val index = timeline.indexAt(time)
        if (index < 0) {
            val first = timeline.startValue!!
            return floatArrayOf(first.red, first.green, first.blue)
        }
        val command = timeline[index]
        val progress = command.progressAt(time).toFloat()
        val start = command.startValue
        val end = command.endValue
        return floatArrayOf(
            start.red + (end.red - start.red) * progress,
            start.green + (end.green - start.green) * progress,
            start.blue + (end.blue - start.blue) * progress
        )
    }

    private fun referenceBoolean(timeline: CommandTimeline<Boolean>, time: Double): Boolean {
        val index = timeline.indexAt(time)
        if (index < 0) return false
        val command = timeline[index]
        return if (time < command.endTime) command.startValue else command.endValue
    }

    private val easings = Easing.values()

    /** Commands with gaps, overlaps, zero and negative durations, at whole and odd times. */
    private fun <T> randomTimeline(random: Random, value: () -> T): CommandTimeline<T> {
        val timeline = CommandTimeline<T>()
        var time = random.nextInt(-500, 500).toDouble()
        repeat(random.nextInt(1, 40)) {
            // Now and then the same start time as the command before.
            if (random.nextInt(5) != 0) time += random.nextInt(-100, 400) + if (random.nextBoolean()) 0.5 else 0.0
            val duration = when (random.nextInt(6)) {
                0 -> 0.0
                1 -> -random.nextInt(1, 200).toDouble()
                else -> random.nextInt(1, 800).toDouble()
            }
            timeline.add(easings[random.nextInt(easings.size)], time, time + duration, value(), value())
        }
        return timeline
    }

    /**
     * Mostly forward in frame-sized steps, with jumps back and far ahead, repeated times, and times
     * exactly at (or right next to) a command's start or end.
     */
    private fun times(random: Random, timeline: CommandTimeline<*>): DoubleArray {
        val result = DoubleArray(400)
        var time = -1200.0
        for (i in result.indices) {
            when (random.nextInt(20)) {
                0 -> time -= random.nextInt(0, 3000)
                1 -> {}
                2 -> time += random.nextInt(500, 20000)
                3, 4 -> {
                    val command = timeline[random.nextInt(timeline.size)]
                    val edge = if (random.nextBoolean()) command.startTime else command.endTime
                    time = when (random.nextInt(3)) {
                        0 -> Math.nextDown(edge)
                        1 -> Math.nextUp(edge)
                        else -> edge
                    }
                }
                else -> time += random.nextDouble(0.0, 17.0)
            }
            result[i] = time
        }
        return result
    }

    @Test
    fun floatTrack_matchesTimelineEvaluation() {
        val random = Random(1)
        repeat(300) {
            val timeline = randomTimeline(random) { random.nextInt(-1000, 1000) / 7f }
            val track = PropertyTrack.floatOf(timeline)!!
            for (time in times(random, timeline)) {
                val expected = referenceFloat(timeline, time)
                assertEquals("at $time", expected.toRawBits(), track.valueAt(time).toRawBits())
            }
        }
    }

    @Test
    fun colorTrack_matchesTimelineEvaluation() {
        val random = Random(2)
        repeat(200) {
            val timeline = randomTimeline(random) {
                StoryboardColor(random.nextFloat(), random.nextFloat(), random.nextFloat())
            }
            val track = PropertyTrack.colorOf(timeline)!!
            for (time in times(random, timeline)) {
                val expected = referenceColor(timeline, time)
                track.update(time)
                assertEquals("red at $time", expected[0].toRawBits(), track.red.toRawBits())
                assertEquals("green at $time", expected[1].toRawBits(), track.green.toRawBits())
                assertEquals("blue at $time", expected[2].toRawBits(), track.blue.toRawBits())
            }
        }
    }

    @Test
    fun booleanTrack_matchesTimelineEvaluation() {
        val random = Random(3)
        repeat(300) {
            val timeline = randomTimeline(random) { random.nextBoolean() }
            val track = PropertyTrack.booleanOf(timeline)!!
            for (time in times(random, timeline)) {
                assertEquals("at $time", referenceBoolean(timeline, time), track.valueAt(time))
            }
        }
    }

    @Test
    fun emptyTimeline_hasNoTrack() {
        assertNull(PropertyTrack.floatOf(CommandTimeline()))
        assertNull(PropertyTrack.colorOf(CommandTimeline()))
        assertNull(PropertyTrack.booleanOf(CommandTimeline()))
    }

    @Test
    fun valueHeldAfterCommand_untilNextStarts() {
        val timeline = CommandTimeline<Float>()
        timeline.add(Easing.None, 0.0, 100.0, 0f, 1f)
        timeline.add(Easing.None, 200.0, 300.0, 5f, 6f)
        val track = PropertyTrack.floatOf(timeline)!!

        assertEquals(0f, track.valueAt(-10.0), 0f)
        assertEquals(0.5f, track.valueAt(50.0), 0f)
        assertEquals(1f, track.valueAt(100.0), 0f)
        assertEquals(1f, track.valueAt(150.0), 0f)
        assertEquals(1f, track.valueAt(199.0), 0f)
        assertEquals(5f, track.valueAt(200.0), 0f)
        assertEquals(6f, track.valueAt(400.0), 0f)
        assertEquals(0.5f, track.valueAt(50.0), 0f)
    }

    @Test
    fun booleanTrack_switchesAtCommandEnd() {
        val timeline = CommandTimeline<Boolean>()
        timeline.add(Easing.None, 100.0, 200.0, true, false)
        val track = PropertyTrack.booleanOf(timeline)!!

        assertEquals(false, track.valueAt(99.0))
        assertEquals(true, track.valueAt(100.0))
        assertEquals(true, track.valueAt(Math.nextDown(200.0)))
        assertEquals(false, track.valueAt(200.0))
        assertEquals(true, track.valueAt(150.0))
    }
}
