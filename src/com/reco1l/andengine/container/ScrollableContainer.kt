package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.input.touch.TouchEvent.*
import javax.microedition.khronos.opengles.*
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
     * The maximum velocity in px/s on the x-axis.
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
     * The scroll indicator for the x-axis that shows the current scroll position.
     */
    var indicatorX: ExtendedEntity? = RoundedBox().also {
        it.color = ColorARGB.White
        it.height = 10f
        it.alpha = 0.5f
        it.cornerRadius = 5f
    }

    /**
     * The scroll indicator for the y-axis that shows the current scroll position.
     */
    var indicatorY: ExtendedEntity? = RoundedBox().also {
        it.color = ColorARGB.White
        it.width = 10f
        it.alpha = 0.5f
        it.cornerRadius = 5f
    }


    /**
     * The maximum scroll position on the x-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollX
        get() = max(0f, scrollableContentWidth - width)

    /**
     * The maximum scroll position on the y-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollY
        get() = max(0f, scrollableContentHeight - height)

    /**
     * The width of the content that can be scrolled. That is [contentWidth] minus
     * the width of the vertical indicator.
     */
    val scrollableContentWidth
        get() = max(0f, contentWidth - (indicatorY?.width ?: 0f))

    /**
     * The height of the content that can be scrolled. That is [contentHeight] minus
     * the height of the horizontal indicator.
     */
    val scrollableContentHeight
        get() = max(0f, contentHeight - (indicatorX?.height ?: 0f))


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

            if (abs(velocityX) < INSIGNIFICANT_DISTANCE) {
                velocityX = 0f
            }

            if (abs(velocityY) < INSIGNIFICANT_DISTANCE) {
                velocityY = 0f
            }
        }

        // Back smoothly to the max scroll position if the scroll position is out of bounds.
        if (!isDragging) {

            if (scrollX > maxScrollX) {
                velocityX = 0f

                val deltaX = scrollX - maxScrollX
                scrollX -= deltaX * 0.1f

                if (abs(deltaX) < INSIGNIFICANT_DISTANCE) {
                    scrollX = maxScrollX
                }
            }

            if (scrollY > maxScrollY) {
                velocityY = 0f

                val deltaY = scrollY - maxScrollY
                scrollY -= deltaY * 0.1f

                if (abs(deltaY) < INSIGNIFICANT_DISTANCE) {
                    scrollY = maxScrollY
                }
            }

            if (scrollY < 0) {
                velocityY = 0f
                scrollY += -scrollY * 0.1f

                if (abs(scrollY) < INSIGNIFICANT_DISTANCE) {
                    scrollY = 0f
                }
            }

            if (scrollX < 0) {
                velocityX = 0f
                scrollX += -scrollX * 0.1f

                if (abs(scrollX) < INSIGNIFICANT_DISTANCE) {
                    scrollX = 0f
                }
            }

        }

        // Updating progress indicators
        indicatorY?.let {

            it.isVisible = scrollAxes == Axes.Both || scrollAxes == Axes.Y
            it.y = scrollY * (height / scrollableContentHeight)

            if (it.alpha > 0f && velocityY == 0f) {
                it.alpha -= deltaTimeSec * 0.5f

                if (it.alpha < 0f) {
                    it.alpha = 0f
                }
            }

            if (it.isVisible) {
                it.onUpdate(deltaTimeSec)
            }
        }

        indicatorX?.let {

            it.isVisible = scrollAxes == Axes.Both || scrollAxes == Axes.X

            it.x = scrollX * (width / scrollableContentWidth)
            it.y = height - it.height

            if (it.alpha > 0f && velocityX == 0f) {
                it.alpha -= deltaTimeSec * 0.5f

                if (it.alpha < 0f) {
                    it.alpha = 0f
                }
            }

            if (it.isVisible) {
                it.onUpdate(deltaTimeSec)
            }
        }

        elapsedTimeSec += deltaTimeSec
    }


    override fun onMeasureContentSize() {
        super.onMeasureContentSize()

        indicatorY?.let {
            it.height = height * (height / scrollableContentHeight)
            it.x = contentWidth + 5f

            contentWidth += it.width + 5f
        }

        indicatorX?.let {
            it.width = width * (width / scrollableContentWidth)
            it.y = contentHeight + 5f

            contentHeight += it.height + 5f
        }

        if (indicatorX != null || indicatorY != null) {
            onContentSizeMeasured()
        }
    }


    override fun onDrawChildren(pGL: GL10, pCamera: Camera) {
        super.onDrawChildren(pGL, pCamera)

        indicatorX?.onDraw(pGL, pCamera)
        indicatorY?.onDraw(pGL, pCamera)
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

                val length = hypot(deltaX, deltaY)

                // Slow down the scroll when reaching the bounds.
                if (scrollX + deltaX < 0 || scrollX + deltaX > maxScrollX) {
                    deltaX *= if (length <= 0) 0f else length.pow(0.7f) / length
                }

                if (scrollY + deltaY < 0 || scrollY + deltaY > maxScrollY) {
                    deltaY *= if (length <= 0) 0f else length.pow(0.7f) / length
                }

                if (deltaX.isNaN() || abs(deltaX) < INSIGNIFICANT_DISTANCE) {
                    deltaX = 0f
                }

                if (deltaY.isNaN() || abs(deltaY) < INSIGNIFICANT_DISTANCE) {
                    deltaY = 0f
                }

                if (deltaX != 0f || deltaY != 0f) {
                    indicatorX?.alpha = 0.5f
                    indicatorY?.alpha = 0.5f
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

        if (child == indicatorX || child == indicatorY) {
            return super.getChildDrawX(child)
        }

        return -scrollX + child.x - child.originOffsetX + child.translationX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        if (child == indicatorX || child == indicatorY) {
            return super.getChildDrawY(child)
        }

        return -scrollY + child.y - child.originOffsetY + child.translationY
    }


    companion object {

        const val DEFAULT_DECELERATION = 0.98f

        const val INSIGNIFICANT_DISTANCE = 0.05f

    }
}