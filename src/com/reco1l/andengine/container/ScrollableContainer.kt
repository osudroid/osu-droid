package com.reco1l.andengine.container

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import com.rian.osu.math.Precision
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.input.touch.TouchEvent.*
import javax.microedition.khronos.opengles.*
import kotlin.math.*

open class ScrollableContainer : Container() {

    override var autoSizeAxes = Axes.None

    /**
     * Which axes the container can scroll on.
     */
    open var scrollAxes: Axes = Axes.Both

    /**
     * The scroll position on the x-axis.
     */
    var scrollX = 0f
        set(value) {
            if (Precision.almostEquals(value, 0f) || !scrollAxes.isHorizontal) {
                field = 0f
                return
            }

            if (Precision.almostEquals(value, maxScrollX)) {
                field = maxScrollX
                return
            }

            indicatorX?.alpha = 0.5f
            field = value
            invalidateTransformations()
        }

    /**
     * The scroll position on the y-axis.
     */
    var scrollY = 0f
        set(value) {
            if (Precision.almostEquals(value, 0f) || !scrollAxes.isVertical) {
                field = 0f
                return
            }

            if (Precision.almostEquals(value, maxScrollY)) {
                field = maxScrollY
                return
            }

            indicatorY?.alpha = 0.5f
            field = value
            invalidateTransformations()
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
    var maxVelocityX = 5000f

    /**
     * The maximum velocity in px/s on the y-axis.
     */
    var maxVelocityY = 5000f

    /**
     * The velocity in px/s on the x-axis.
     */
    var velocityX = 0f
        private set(value) {

            if (Precision.almostEquals(value, 0f) || !scrollAxes.isHorizontal) {
                field = 0f
                return
            }

            field = value.coerceIn(-maxVelocityX, maxVelocityX)
        }

    /**
     * The velocity in px/s on the y-axis.
     */
    var velocityY = 0f
        private set(value) {

            if (Precision.almostEquals(value, 0f) || !scrollAxes.isVertical) {
                field = 0f
                return
            }

            field = value.coerceIn(-maxVelocityY, maxVelocityY)
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
        get() = max(0f, scrollableContentWidth - drawWidth)

    /**
     * The maximum scroll position on the y-axis.
     *
     * This does not take into account the overscroll.
     */
    val maxScrollY
        get() = max(0f, scrollableContentHeight - drawHeight)

    /**
     * The width of the content that can be scrolled. That is [contentWidth] minus
     * the width of the vertical indicator.
     */
    val scrollableContentWidth
        get() = max(0f, contentWidth - (indicatorY?.drawWidth ?: 0f))

    /**
     * The height of the content that can be scrolled. That is [contentHeight] minus
     * the height of the horizontal indicator.
     */
    val scrollableContentHeight
        get() = max(0f, contentHeight - (indicatorX?.drawHeight ?: 0f))


    private var initialX = 0f

    private var initialY = 0f

    private var elapsedTimeSec = 0f

    private var lastDragTimeSec = 0f


    override fun onManagedUpdate(deltaTimeSec: Float) {

        super.onManagedUpdate(deltaTimeSec)

        // Seems like AndEngine doesn't handle ACTION_OUTSIDE events properly so we have to set a dragging timeout.
        if (isDragging && elapsedTimeSec - lastDragTimeSec > 0.1f) {
            isDragging = false
        }

        if (!isDragging && (velocityX != 0f || velocityY != 0f)) {

            scrollX -= velocityX * deltaTimeSec
            scrollY -= velocityY * deltaTimeSec

            velocityX *= deceleration
            velocityY *= deceleration
        }

        // Back smoothly to the max scroll position if the scroll position is out of bounds.
        if (!isDragging) {

            if (scrollX > maxScrollX) {
                velocityX = 0f
                scrollX -= (scrollX - maxScrollX) * 0.1f
            }

            if (scrollY > maxScrollY) {
                velocityY = 0f
                scrollY -= (scrollY - maxScrollY) * 0.1f
            }

            if (scrollY < 0) {
                velocityY = 0f
                scrollY += -scrollY * 0.1f
            }

            if (scrollX < 0) {
                velocityX = 0f
                scrollX += -scrollX * 0.1f
            }

        }

        // Updating progress indicators
        indicatorY?.let { indicator ->

            indicator.isVisible = scrollAxes == Axes.Both || scrollAxes == Axes.Y

            indicator.x = drawWidth - indicator.drawWidth
            indicator.y = scrollY * (drawHeight / scrollableContentHeight)

            if (indicator.alpha > 0f && velocityY == 0f) {
                indicator.alpha = (indicator.alpha - deltaTimeSec * 0.5f).coerceAtLeast(0f)
            }

            if (indicator.isVisible) {
                indicator.onUpdate(deltaTimeSec)
            }
        }

        indicatorX?.let { indicator ->

            indicator.isVisible = scrollAxes == Axes.Both || scrollAxes == Axes.X

            indicator.x = scrollX * (drawWidth / scrollableContentWidth)
            indicator.y = drawHeight - indicator.drawHeight

            if (indicator.alpha > 0f && velocityX == 0f) {
                indicator.alpha = (indicator.alpha - deltaTimeSec * 0.5f).coerceAtLeast(0f)
            }

            if (indicator.isVisible) {
                indicator.onUpdate(deltaTimeSec)
            }
        }

        elapsedTimeSec += deltaTimeSec
    }


    override fun onMeasureContentSize() {
        super.onMeasureContentSize()

        indicatorY?.let { indicator ->

            indicator.height = drawHeight * (drawHeight / scrollableContentHeight)
            indicator.x = contentWidth + 5f

            contentWidth += indicator.drawWidth + 5f
        }

        indicatorX?.let { indicator ->

            indicator.width = drawWidth * (drawWidth / scrollableContentWidth)
            indicator.y = contentHeight + 5f

            contentHeight += indicator.drawHeight + 5f
        }

        if (indicatorX != null || indicatorY != null) {
            onContentSizeMeasured()
        }
    }

    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        super.onManagedDrawChildren(pGL, pCamera)

        indicatorX?.onDraw(pGL, pCamera)
        indicatorY?.onDraw(pGL, pCamera)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (super.onAreaTouched(event, localX, localY)) {
            return false
        }
        invalidateInputBinding()

        when (event.action) {

            ACTION_DOWN -> {
                initialX = localX
                initialY = localY

                velocityX = 0f
                velocityY = 0f
                return true
            }

            ACTION_MOVE -> {

                var deltaX = if (scrollAxes.isHorizontal) localX - initialX else 0f
                var deltaY = if (scrollAxes.isVertical) localY - initialY else 0f

                isDragging = abs(deltaX) > 1f || abs(deltaY) > 1f

                if (!isDragging) {
                    return super.onAreaTouched(event, localX, localY)
                }

                val length = hypot(deltaX, deltaY)

                if (scrollX - deltaX < 0 || scrollX - deltaX > maxScrollX) {
                    deltaX *= if (length <= 0) 0f else length.pow(0.7f) / length
                }

                if (scrollY - deltaY < 0 || scrollY - deltaY > maxScrollY) {
                    deltaY *= if (length <= 0) 0f else length.pow(0.7f) / length
                }

                val dragTimeSec = elapsedTimeSec - lastDragTimeSec

                if (abs(deltaX) > 0.1f) {
                    scrollX -= deltaX
                    velocityX = abs(deltaX / dragTimeSec) * sign(deltaX)
                    initialX = localX
                }

                if (abs(deltaY) > 0.1f) {
                    scrollY -= deltaY
                    velocityY = abs(deltaY / dragTimeSec) * sign(deltaY)
                    initialY = localY
                }

                lastDragTimeSec = elapsedTimeSec
                return true
            }

            else -> {
                if (!isDragging) {
                    return super.onAreaTouched(event, localX, localY)
                }
                isDragging = false
                return false
            }
        }
    }


    override fun getChildDrawX(child: ExtendedEntity): Float {

        if (child == indicatorX || child == indicatorY || !scrollAxes.isHorizontal) {
            return super.getChildDrawX(child)
        }

        return -scrollX + child.x - child.originOffsetX + child.translationX
    }

    override fun getChildDrawY(child: ExtendedEntity): Float {

        if (child == indicatorX || child == indicatorY || !scrollAxes.isVertical) {
            return super.getChildDrawY(child)
        }

        return -scrollY + child.y - child.originOffsetY + child.translationY
    }


    companion object {

        const val DEFAULT_DECELERATION = 0.98f

    }
}