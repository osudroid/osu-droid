package com.reco1l.andengine.container

import com.reco1l.andengine.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.input.touch.TouchEvent.*
import kotlin.math.*

open class ScrollableContainer : Container() {

    /**
     * Which axes the container can scroll on.
     */
    var scrollAxes: Axes = Axes.Both

    /**
     * The scroll position on the x-axis.
     */
    var scrollX = 0f
        set(value) {
            if (scrollAxes == Axes.Both || scrollAxes == Axes.X) {
                field = value
            }
        }

    /**
     * The scroll position on the y-axis.
     */
    var scrollY = 0f
        set(value) {
            if (scrollAxes == Axes.Both || scrollAxes == Axes.Y) {
                field = value
            }
        }

    /**
     * The deceleration factor for the scrollable container.
     */
    var deceleration = DEFAULT_DECELERATION

    /**
     * The flag to indicate if the container is being dragged.
     */
    var isDragging = false
        private set

    /**
     * The maximum velocity in px/s on the x-axis
     */
    var maxVelocityX = 10000f

    /**
     * The maximum velocity in px/s on the y-axis.
     */
    var maxVelocityY = 10000f

    /**
     * The velocity in px/s on the x-axis.
     */
    var velocityX = 0f
        private set(value) {
            if (scrollAxes == Axes.Both || scrollAxes == Axes.X) {
                field = value.coerceIn(-maxVelocityX, maxVelocityX)
            }
        }

    /**
     * The velocity in px/s on the y-axis.
     */
    var velocityY = 0f
        private set(value) {
            if (scrollAxes == Axes.Both || scrollAxes == Axes.Y) {
                field = value.coerceIn(-maxVelocityY, maxVelocityY)
            }
        }


    /**
     * The maximum scroll position on the x-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollX
        get() = contentWidth - width

    /**
     * The maximum scroll position on the y-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollY
        get() = contentHeight - height



    private var initialX = 0f

    private var initialY = 0f

    private var elapsedTimeSec = 0f

    private var lastDragTimeSec = 0f


    override fun onManagedUpdate(deltaTimeSec: Float) {

        super.onManagedUpdate(deltaTimeSec)

        // Seems like AndEngine doesn't handle ACTION_OUTSIDE events properly so we have to set a dragging timeout.
        if (isDragging && elapsedTimeSec - lastDragTimeSec > 1f) {
            isDragging = false
            velocityX = 0f
            velocityY = 0f
        }

        if (!isDragging && (velocityX != 0f || velocityY != 0f)) {

            scrollX -= velocityX * deltaTimeSec
            scrollY -= velocityY * deltaTimeSec

            velocityX *= deceleration
            velocityY *= deceleration

            if (abs(velocityX) < 0.01f) {
                velocityX = 0f
            }

            if (abs(velocityY) < 0.01f) {
                velocityY = 0f
            }
        }

        // Back smoothly to the max scroll position if the scroll position is out of bounds.
        if (!isDragging) {

            if (scrollX > maxScrollX) {
                velocityX = 0f

                val deltaX = scrollX - maxScrollX
                scrollX -= deltaX * 0.1f

                if (abs(deltaX) < 0.01f) {
                    scrollX = maxScrollX
                }
            }

            if (scrollY > maxScrollY) {
                velocityY = 0f

                val deltaY = scrollY - maxScrollY
                scrollY -= deltaY * 0.1f

                if (abs(deltaY) < 0.01f) {
                    scrollY = maxScrollY
                }
            }

            if (scrollY < 0) {
                velocityY = 0f
                scrollY += (-scrollY) * 0.1f

                if (abs(scrollY) < 0.01f) {
                    scrollY = 0f
                }
            }

            if (scrollX < 0) {
                velocityX = 0f
                scrollX += (-scrollX) * 0.1f

                if (abs(scrollX) < 0.01f) {
                    scrollX = 0f
                }
            }

        }

        elapsedTimeSec += deltaTimeSec
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        when (event.action) {

            ACTION_DOWN -> {
                isDragging = true

                initialX = event.x
                initialY = event.y

                velocityX = 0f
                velocityY = 0f

                lastDragTimeSec = elapsedTimeSec
            }

            ACTION_UP, ACTION_CANCEL, ACTION_OUTSIDE -> {
                isDragging = false
            }

            else -> {
                isDragging = true

                // Coerce the delta values to the width and height of the container because the user can't scroll more than that.
                var deltaX = (event.x - initialX).coerceAtMost(width)
                var deltaY = (event.y - initialY).coerceAtMost(height)

                val length = sqrt(deltaX * deltaX + deltaY * deltaY)

                // Slow down the scroll when reaching the bounds.
                if (scrollX + deltaX < 0 || scrollX + deltaX > maxScrollX) {
                    deltaX *= if (length <= 0) 0f else length.pow(0.7f) / length
                }

                if (scrollY + deltaY < 0 || scrollY + deltaY > maxScrollY) {
                    deltaY *= if (length <= 0) 0f else length.pow(0.7f) / length
                }

                // Preventing possible NaN values.
                if (deltaX.isNaN()) {
                    deltaX = 0f
                }

                if (deltaY.isNaN()) {
                    deltaY = 0f
                }

                scrollX -= deltaX
                scrollY -= deltaY

                val dragTime = elapsedTimeSec - lastDragTimeSec
                velocityX = deltaX / dragTime
                velocityY = deltaY / dragTime

                initialX = event.x
                initialY = event.y

                lastDragTimeSec = elapsedTimeSec
            }
        }

        return super.onAreaTouched(event, localX, localY)
    }


    override fun getChildDrawX(child: ExtendedEntity): Float {
        val originOffsetX = child.width * child.originX

        return -scrollX + child.x - originOffsetX + child.translationX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {
        val originOffsetY = child.height * child.originY

        return -scrollY + child.y - originOffsetY + child.translationY
    }


    companion object {

        const val DEFAULT_DECELERATION = 0.98f

    }
}