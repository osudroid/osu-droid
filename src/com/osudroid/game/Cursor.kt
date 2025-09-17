package com.osudroid.game

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
    @JvmField
    var mouseDown = false

    /**
     * A list of [CursorEvent]s of this [Cursor] that are [TouchEvent.ACTION_DOWN] events.
     */
    @JvmField
    val downEvents = ArrayList<CursorEvent>(25)

    /**
     * The index of the latest processed [CursorEvent] in [downEvents].
     */
    @JvmField
    var latestProcessedDownEventIndex = 0

    /**
     * A list of [CursorEvent]s of this [Cursor] that has not been processed yet.
     */
    @JvmField
    val events = ArrayList<CursorEvent>(25)

    /**
     * The index of the latest processed [CursorEvent] in [.events].
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
     * Obtains the earliest [CursorEvent] of this [Cursor].
     *
     * @param actions The actions to filter by. If none are provided, no filtering is done.
     * @return The earliest [CursorEvent], or [latestEvent] or `null` if there are no [CursorEvent]s.
     */
    fun getEarliestEvent(vararg actions: Int): CursorEvent? {
        if (actions.isEmpty()) {
            return if (events.isEmpty()) latestEvent else events[0]
        }

        for (i in events.indices) {
            val event = events[i]

            for (j in actions.indices) {
                if (event.action == actions[j]) {
                    return event
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
     * @param systemTime The system time to search for.
     * @return The closest [CursorEvent] before the specified system time, or [latestEvent] if there are no events.
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
     */
    fun reset(previousFrameTime: Long, elapsedTimeMs: Float) {
        val size = events.size

        if (size == 0 && latestEvent != null) {
            // Update the state of the latest event to the current update tick.
            latestEvent!!.systemTime = previousFrameTime
            latestEvent!!.trackTime = elapsedTimeMs
            latestEvent!!.offset = 0.0
        }

        // Do not release the last event in the list because it may be used in latestEvent.
        for (i in 0..<size - 1) {
            events[i].release()
        }

        downEvents.clear()
        events.clear()
        latestProcessedDownEventIndex = 0
        latestProcessedEventIndex = 0
    }
}
