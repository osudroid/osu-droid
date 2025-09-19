package com.osudroid.game

import android.graphics.PointF
import android.os.SystemClock
import androidx.core.util.Pools.SynchronizedPool
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.Constants
import ru.nsu.ccfit.zuev.osu.game.GameHelper

/**
 * Represents a cursor event in gameplay.
 */
class CursorEvent {
    private constructor()

    /**
     * The time at which the [CursorEvent] occurred, in the [SystemClock.uptimeMillis] time base.
     */
    @JvmField
    var systemTime = 0L

    /**
     * The time at which the [CursorEvent] occurred, in the gameplay time base (milliseconds since the start of the track).
     */
    @JvmField
    var trackTime = 0f

    /**
     * The offset of the [CursorEvent] from the last frame, in milliseconds.
     */
    @JvmField
    var offset = 0.0

    /**
     * The action of the [CursorEvent], which can be one of [TouchEvent.ACTION_DOWN], [TouchEvent.ACTION_MOVE], or [TouchEvent.ACTION_UP].
     */
    @JvmField
    var action = TouchEvent.ACTION_DOWN

    /**
     * Whether the action is [TouchEvent.ACTION_DOWN].
     */
    val isActionDown
        get() = action == TouchEvent.ACTION_DOWN

    /**
     * Whether the action is [TouchEvent.ACTION_MOVE].
     */
    val isActionMove
        get() = action == TouchEvent.ACTION_MOVE

    /**
     * Whether the action is [TouchEvent.ACTION_UP].
     */
    val isActionUp
        get() = action == TouchEvent.ACTION_UP

    /**
     * The time at which this [CursorEvent] occurred, accounting for [Config.isFixFrameOffset], in the gameplay time
     * base (milliseconds since the start of the track).
     */
    val hitTime
        get() = trackTime + if (Config.isFixFrameOffset()) offset else 0.0

    /**
     * The position of the cursor in screen space.
     */
    @JvmField
    var position = PointF()

    /**
     * The position of the cursor in playfield space.
     */
    @JvmField
    var trackPosition = PointF()

    /**
     * Releases this [CursorEvent] back to the pool for reuse.
     */
    fun release() {
        pool.release(this)
    }

    /**
     * Copies this [CursorEvent] to a new instance.
     */
    fun copy() = obtain().also { copy ->
        copy.systemTime = systemTime
        copy.trackTime = trackTime
        copy.action = action
        copy.offset = offset
        copy.position.set(position)
        copy.trackPosition.set(trackPosition)
    }

    private fun apply(event: TouchEvent) {
        systemTime = event.motionEvent.eventTime
        trackTime = 0f
        action = event.action
        offset = 0.0

        val width = Config.getRES_WIDTH().toFloat()
        val height = Config.getRES_HEIGHT().toFloat()

        position.x = event.x.coerceIn(0f, width)
        position.y = event.y.coerceIn(0f, height)

        trackPosition.x = event.x
        trackPosition.y = event.y

        if (GameHelper.isHardRock()) {
            trackPosition.y -= height / 2f
            trackPosition.y *= -1
            trackPosition.y += height / 2f
        }

        trackPosition.x -= (width - Constants.MAP_ACTUAL_WIDTH) / 2f
        trackPosition.y -= (height - Constants.MAP_ACTUAL_HEIGHT) / 2f

        trackPosition.x *= Constants.MAP_WIDTH / Constants.MAP_ACTUAL_WIDTH.toFloat()
        trackPosition.y *= Constants.MAP_HEIGHT / Constants.MAP_ACTUAL_HEIGHT.toFloat()
    }

    companion object {
        private val pool = SynchronizedPool<CursorEvent>(50)

        /**
         * Obtains an instance of [CursorEvent] from the pool or creates a new one if the pool is empty.
         *
         * @return An instance of [CursorEvent].
         */
        @JvmStatic
        fun obtain() = pool.acquire() ?: CursorEvent()

        /**
         * Obtains an instance of [CursorEvent] from the pool or creates a new one if the pool is empty.
         *
         * @param event The [TouchEvent] to apply to the [CursorEvent].
         * @return An instance of [CursorEvent].
         */
        @JvmStatic
        fun obtain(event: TouchEvent) = obtain().also { it.apply(event) }
    }
}