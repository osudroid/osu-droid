package com.osudroid.game

import android.os.SystemClock

import org.anddev.andengine.input.touch.TouchEvent

/**
 * Represents a cursor in gameplay.
 */
class Cursor {
    /**
     * Whether this [Cursor] is blocked from a [TouchEvent.ACTION_DOWN].
     */
    @JvmField
    var mouseBlocked = false

    /**
     * Whether this [Cursor] is currently pressed down.
     */
    @get:JvmName("isMouseDown")
    val mouseDown
        get() = latestEvent?.isActionDown == true || latestEvent?.isActionMove == true

    /**
     * [TouchEvent.ACTION_DOWN] [CursorEvent]s of this [Cursor] that have not been processed yet.
     */
    @JvmField
    val downEvents = ArrayList<CursorEvent>(25)

    /**
     * The index of the latest processed [CursorEvent] in [downEvents].
     */
    @JvmField
    var latestProcessedDownEventIndex = 0

    /**
     * [CursorEvent]s of this [Cursor] that have not been processed yet.
     */
    @JvmField
    val events = ArrayList<CursorEvent>(25)

    /**
     * The index of the latest processed [CursorEvent] in [events].
     */
    @JvmField
    var latestProcessedEventIndex = 0

    /**
     * The latest [CursorEvent] of this [Cursor].
     */
    private var latestEvent: CursorEvent? = null

    /**
     * Adds a [CursorEvent] to this [Cursor].
     *
     * @param event The [CursorEvent] to add.
     */
    fun addEvent(event: CursorEvent) {
        if (events.isEmpty()) {
            // First time adding an event in this update tick.
            // Free the latest event if it exists.
            latestEvent?.release()
        }

        events.add(event)

        if (event.action == TouchEvent.ACTION_DOWN) {
            downEvents.add(event)
        }

        latestEvent = event
    }

    /**
     * Obtains the latest [CursorEvent] of this [Cursor].
     *
     * @param actions The actions to filter by. If none are provided, no filtering is done.
     * @return The latest [CursorEvent], or [latestEvent] or `null` if there are no [CursorEvent]s.
     */
    fun getLatestEvent(vararg actions: Int): CursorEvent? {
        if (actions.isEmpty()) {
            return latestEvent
        }

        if (!events.isEmpty()) {
            val size = events.size

            for (i in size - 1 downTo 0) {
                val event = events[i]

                for (j in actions.indices) {
                    if (event.action == actions[j]) {
                        return event
                    }
                }
            }
        }

        if (latestEvent != null) {
            for (i in actions.indices) {
                if (latestEvent!!.action == actions[i]) {
                    return latestEvent
                }
            }
        }

        return null
    }

    /**
     * Obtains the closest [CursorEvent] before the specified system time.
     *
     * @param systemTime The system time to search for, in the [SystemClock.uptimeMillis] time base.
     * @return The closest [CursorEvent] before [systemTime], or [latestEvent] if there are no events.
     */
    fun getClosestEventBefore(systemTime: Long): CursorEvent? {
        val size = events.size

        if (size == 0) {
            return latestEvent
        }

        var l = 0
        var r = size - 1

        while (l <= r) {
            val mid = (l + r) / 2
            val event = events[mid]

            if (event.systemTime < systemTime) {
                l = mid + 1
            } else if (event.systemTime > systemTime) {
                r = mid - 1
            } else {
                return event
            }
        }

        return if (r >= 0) events[r] else latestEvent
    }

    /**
     * Resets this [Cursor] for the next update tick.
     *
     * @param frameTime The time of the current frame, in the [SystemClock.uptimeMillis] time base.
     * @param trackElapsedTimeMs The elapsed time of the track in milliseconds.
     */
    fun reset(frameTime: Long, trackElapsedTimeMs: Float) {
        val size = events.size

        if (size == 0 && latestEvent != null) {
            // When there are no new events, update the state of the latest event to the current update tick.
            // This allows any input in the next update tick that rely on this event to function properly.
            latestEvent!!.systemTime = frameTime
            latestEvent!!.trackTime = trackElapsedTimeMs
            latestEvent!!.offset = 0.0
        }

        // The last event in the list is used as the latest event, so we don't release it.
        for (i in 0..<size - 1) {
            events[i].release()
        }

        downEvents.clear()
        events.clear()
        latestProcessedDownEventIndex = 0
        latestProcessedEventIndex = 0
    }
}
