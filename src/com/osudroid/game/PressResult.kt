package com.osudroid.game

import ru.nsu.ccfit.zuev.osu.game.GameObject
import org.anddev.andengine.input.touch.TouchEvent

/**
 * The outcome of checking a [GameObject]s against pressing [CursorEvent]s (those with [TouchEvent.ACTION_DOWN]) of the current frame.
 */
sealed interface PressResult {
    /**
     * No press concerns the [GameObject].
     */
    data object Ignore : PressResult

    /**
     * A press landed on the [GameObject] but cannot hit it, either because it was too early or because the [GameObject] is
     * locked behind an earlier [GameObject] (notelock). The [GameObject] should shake.
     */
    data object Shake : PressResult

    /**
     * A press hit the [GameObject].
     *
     * Each [GameObject] owns a single [Hit] that is updated and returned on every hit to avoid allocations. As such,
     * [event] is only valid until the next check of the same [GameObject] and must not be held onto.
     */
    class Hit : PressResult {
        /**
         * The [CursorEvent] that hit the [GameObject].
         */
        lateinit var event: CursorEvent
            private set

        /**
         * Sets [event] to the given [CursorEvent].
         *
         * @param event The [CursorEvent] that hit the [GameObject].
         * @return This [Hit].
         */
        fun withEvent(event: CursorEvent) = apply { this.event = event }
    }
}
