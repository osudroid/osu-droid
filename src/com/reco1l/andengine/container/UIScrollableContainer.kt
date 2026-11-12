package com.reco1l.andengine.container

import android.os.SystemClock
import org.andengine.graphics.ClipStack
import androidx.annotation.*
import com.osudroid.math.Precision
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.andengine.engine.camera.*
import org.andengine.entity.*
import org.andengine.graphics.DrawContext
import org.andengine.input.touch.*
import org.andengine.input.touch.TouchEvent.*

import kotlin.math.*

@Suppress("MemberVisibilityCanBePrivate")
open class UIScrollableContainer : UIContainer() {

    /**
     * Determines which axes can be scrolled by the user.
     */
    var scrollAxes: Axes = Axes.Both

    /**
     * Determines which axes can be overflowed by the scroll.
     */
    var overflowAxes: Axes = Axes.Both

    /**
     * The flag to indicate if the container is being scrolled by the user. Container
     * can be scrolled by the user or by itself (e.g. animations).
     */
    var isUserScrolling = false
        private set

    /**
     * Whether to prevent scrolling on this container and all its children.
     */
    var preventScrolling = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    velocityX = 0f
                    velocityY = 0f
                }
            }
        }

    /**
     * Whether the container is scrolling or not.
     */
    val isScrolling
        get() = velocityX != 0f || velocityY != 0f || isUserScrolling

    //region Scrolling properties

    /**
     * The scroll position on the x-axis.
     */
    var scrollX = 0f
        set(value) {
            val scroll = when {
                Precision.almostEquals(value, 0f) -> 0f
                Precision.almostEquals(value, maxScrollX) -> maxScrollX
                else -> value
            }

            if (field != scroll) {
                field = scroll
                onScroll(Axes.X)
            }
        }

    /**
     * The scroll position on the y-axis.
     */
    var scrollY = 0f
        set(value) {
            val scroll = when {
                Precision.almostEquals(value, 0f) -> 0f
                Precision.almostEquals(value, maxScrollY) -> maxScrollY
                else -> value
            }

            if (field != scroll) {
                field = scroll
                onScroll(Axes.Y)
            }
        }

    /**
     * The velocity in px/s on the x-axis.
     */
    var velocityX = 0f
        private set(value) {
            val velocity = if (Precision.almostEquals(value, 0f)) 0f else value.coerceIn(-maxVelocity.x, maxVelocity.x)

            if (field != velocity) {
                field = velocity
                setHierarchyScrollPrevention(velocity != 0f)
            }
        }

    /**
     * The velocity in px/s on the y-axis.
     */
    var velocityY = 0f
        private set(value) {
            val velocity = if (Precision.almostEquals(value, 0f)) 0f else value.coerceIn(-maxVelocity.y, maxVelocity.y)

            if (field != velocity) {
                field = velocity
                setHierarchyScrollPrevention(velocity != 0f)
            }
        }

    /**
     * The deceleration factor for the scrollable container.
     */
    var deceleration = Vec2(DEFAULT_DECELERATION)

    /**
     * When set, flings decelerate linearly by this many units per second squared (time based, frame rate
     * independent) instead of the per-frame multiplicative [deceleration]. This is the behavior of the
     * legacy song menu, a long, even glide that ends predictably.
     */
    var flingDeceleration: Vec2? = null

    /**
     * When `true`, the fling velocity on release is the average velocity of the whole drag gesture
     * (distance since touch down divided by its duration) instead of the velocity of the last move event.
     * Averaging ignores the jitter of the final touch samples and gives consistent flings.
     */
    var velocityFromGesture = false

    /**
     * When `true`, a touch that starts on the right edge of the container grabs the vertical indicator:
     * dragging then maps the finger position directly onto the whole scroll range.
     */
    var isVerticalIndicatorDraggable = false

    /**
     * The width of the touch area along the right edge that grabs the vertical indicator.
     */
    var indicatorTouchWidth = 48f

    /**
     * Whether the vertical indicator is currently being dragged.
     */
    var isDraggingIndicator = false
        private set

    private var indicatorGrabOffset = 0f

    /**
     * The maximum velocity in px/s for both axes.
     */
    var maxVelocity = Vec2(DEFAULT_MAX_VELOCITY)

    /**
     * The minimum travel distance in px for both axes.
     *
     * This is used to determine if the user has scrolled enough to start
     * scrolling the container.
     */
    var minimumTravel = Vec2(DEFAULT_MINIMUM_TRAVEL)

    //endregion

    //region Indicators

    /**
     * Whether to show the horizontal scroll indicator.
     */
    var showHorizontalIndicator = true

    /**
     * Whether to show the vertical scroll indicator.
     */
    var showVerticalIndicator = true

    /**
     * The scroll indicator for the x-axis that shows the current scroll position.
     */
    var horizontalIndicator: UIComponent? = UIBox().apply {
        color = Color4.White
        height = 6f
        alpha = 0.5f
        cornerRadius = 3f
    }
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value
                field?.setParent(this, AttachmentMode.Decorator)
            }
        }

    /**
     * The scroll indicator for the y-axis that shows the current scroll position.
     */
    var verticalIndicator: UIComponent? = UIBox().apply {
        color = Color4.White
        width = 6f
        alpha = 0.5f
        cornerRadius = 3f
    }
        set(value) {
            if (field != value) {
                field?.detachSelf()
                field = value
                field?.setParent(this, AttachmentMode.Decorator)
            }
        }

    //endregion

    //region Dimension properties

    /**
     * The padding for the scrollable content.
     */
    var scrollPadding = Vec2(0f, 0f)

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
        get() = max(0f, contentWidth) + scrollPadding.x

    /**
     * The height of the content that can be scrolled. That is [contentHeight] minus
     * the height of the horizontal indicator.
     */
    val scrollableContentHeight
        get() = max(0f, contentHeight) + scrollPadding.y

    //endregion


    private var initialX = 0f
    private var initialY = 0f
    private var deltaX = 0f
    private var deltaY = 0f

    private val velocityTracker = PointerVelocityTracker()

    private var gestureStartX = 0f
    private var gestureStartY = 0f
    private var gestureStartTimeMillis = 0L


    /**
     * Stops the scrolling of the container by setting the velocity to 0.
     */
    fun stopScroll() {
        velocityX = 0f
        velocityY = 0f
    }


    //region Callbacks

    @CallSuper
    open fun onScroll(axes: Axes) {

        if (axes.isVertical) {
            verticalIndicator?.alpha = 0.5f
        }

        if (axes.isHorizontal) {
            horizontalIndicator?.alpha = 0.5f
        }

        invalidate(InvalidationFlag.InputBindings)
    }

    //endregion

    //region Scrolling

    private fun decelerateProgressively(deltaTimeSec: Float) {
        scrollX -= velocityX * deltaTimeSec
        scrollY -= velocityY * deltaTimeSec

        val flingDeceleration = flingDeceleration

        if (flingDeceleration != null) {
            velocityX = decelerateLinearly(velocityX, flingDeceleration.x * deltaTimeSec)
            velocityY = decelerateLinearly(velocityY, flingDeceleration.y * deltaTimeSec)
        } else {
            velocityX *= deceleration.x
            velocityY *= deceleration.y
        }
    }

    private fun decelerateLinearly(velocity: Float, amount: Float): Float {
        return if (abs(velocity) > amount) velocity - amount * sign(velocity) else 0f
    }

    private fun handleOverflow() {

        if (scrollX < 0f || scrollX > maxScrollX) {
            velocityX = 0f

            if (overflowAxes.isHorizontal) {
                val bounceBack = (if (scrollX < 0f) -scrollX else -(scrollX - maxScrollX)) * 0.2f

                if (abs(bounceBack) < .5f) {
                    scrollX = if (scrollX < 0f) 0f else maxScrollX
                } else {
                    scrollX += bounceBack
                }
            } else {
                scrollX = if (scrollX < 0f) 0f else maxScrollX
            }
        }

        if (scrollY < 0f || scrollY > maxScrollY) {
            velocityY = 0f

            if (overflowAxes.isVertical) {
                val bounceBack = (if (scrollY < 0f) -scrollY else -(scrollY - maxScrollY)) * 0.2f

                if (abs(bounceBack) < .5f) {
                    scrollY = if (scrollY < 0f) 0f else maxScrollY
                } else {
                    scrollY += bounceBack
                }
            } else {
                scrollY = if (scrollY < 0f) 0f else maxScrollY
            }
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        super.onManagedUpdate(deltaTimeSec)

        if (!isUserScrolling) {
            if (isScrolling) {
                decelerateProgressively(deltaTimeSec)
            }
            handleOverflow()
        } else {
            handleUserScroll()
        }

        updateIndicators(deltaTimeSec)
    }

    override fun onManagedDraw(drawContext: DrawContext, pCamera: Camera) {

        firstChild?.also { child ->
            if (child.x != -scrollX || child.y != -scrollY) {
                child.setPosition(-scrollX, -scrollY)
                onScrollChanged()
            }
        }

        // Push the parent GL state, apply this container's own transform (which internally
        // uses absoluteX/absoluteY — the correct render position), then read back the resulting
        // model-view matrix to obtain the container's 4 corners in scene space.
        // This is more accurate than convertLocalToSceneCoordinates, which uses raw mX/mY and
        // misses parent padding, anchor, and origin offsets.
        drawContext.pushModelViewGLMatrix()
        onApplyTransformations(drawContext)

        val m = drawContext.modelViewGLMatrix

        drawContext.popModelViewGLMatrix()

        // Column-major 4×4: transform the 4 local corners through M to get scene coords.
        // (lx, ly) → scene = (M[0]*lx + M[4]*ly + M[12],  M[1]*lx + M[5]*ly + M[13])
        val w = width
        val h = height
        fun sceneX(lx: Float, ly: Float) = m[0] * lx + m[4] * ly + m[12]
        fun sceneY(lx: Float, ly: Float) = m[1] * lx + m[5] * ly + m[13]

        val (tlX, tlY) = pCamera.convertSceneToSurfaceCoordinates(sceneX(0f, 0f), sceneY(0f, 0f))
        val (trX, trY) = pCamera.convertSceneToSurfaceCoordinates(sceneX(w,  0f), sceneY(w,  0f))
        val (brX, brY) = pCamera.convertSceneToSurfaceCoordinates(sceneX(w,  h),  sceneY(w,  h))
        val (blX, blY) = pCamera.convertSceneToSurfaceCoordinates(sceneX(0f, h),  sceneY(0f, h))

        val minX = minOf(tlX, trX, brX, blX)
        val minY = minOf(tlY, trY, brY, blY)
        val maxX = maxOf(tlX, trX, brX, blX)
        val maxY = maxOf(tlY, trY, brY, blY)

        ClipStack.push(minX, minY, maxX - minX, maxY - minY)

        // Suppress UIComponent's own clipToBounds scissor to avoid double-scissoring.
        val savedClipToBounds = clipToBounds
        clipToBounds = false
        super.onManagedDraw(drawContext, pCamera)
        clipToBounds = savedClipToBounds

        // Draw indicators inside the scissor (already position-clamped to container bounds).
        val hasIndicators = horizontalIndicator != null || verticalIndicator != null
        if (hasIndicators) {
            drawContext.pushModelViewGLMatrix()
            onApplyTransformations(drawContext)
            horizontalIndicator?.onDraw(drawContext, pCamera)
            verticalIndicator?.onDraw(drawContext, pCamera)
            drawContext.popModelViewGLMatrix()
        }

        ClipStack.pop()
    }
    /**
     * Called when the scroll position has changed.
     */
    protected open fun onScrollChanged() {}

    /**
     * Called to update the scroll indicators based on the current scroll position.
     */
    protected open fun updateIndicators(deltaTimeSec: Float) {

        val verticalIndicator = verticalIndicator

        if (verticalIndicator != null) {
            verticalIndicator.isVisible = showVerticalIndicator && (scrollAxes == Axes.Both || scrollAxes == Axes.Y)

            if (verticalIndicator.alpha > 0f && velocityY == 0f && !isDraggingIndicator) {
                verticalIndicator.alpha = (verticalIndicator.alpha - deltaTimeSec * 0.75f).coerceAtLeast(0f)
            }

            if (verticalIndicator.isVisible) {
                verticalIndicator.x = width - verticalIndicator.width
                verticalIndicator.y = indicatorPosition(scrollY, maxScrollY, height, scrollableContentHeight, verticalIndicator.height)

                verticalIndicator.onUpdate(deltaTimeSec)
            }
        }

        val horizontalIndicator = horizontalIndicator

        if (horizontalIndicator != null) {
            horizontalIndicator.isVisible = showHorizontalIndicator && (scrollAxes == Axes.Both || scrollAxes == Axes.X)

            if (horizontalIndicator.alpha > 0f && velocityX == 0f) {
                horizontalIndicator.alpha = (horizontalIndicator.alpha - deltaTimeSec * 0.75f).coerceAtLeast(0f)
            }

            if (horizontalIndicator.isVisible) {
                horizontalIndicator.x = indicatorPosition(scrollX, maxScrollX, width, scrollableContentWidth, horizontalIndicator.width)
                horizontalIndicator.y = height - horizontalIndicator.height

                horizontalIndicator.onUpdate(deltaTimeSec)
            }
        }
    }

    //endregion

    /**
     * Computes the position of an indicator along one axis. The scroll range is mapped onto the free track so the
     * indicator also works with its minimum size, and overscroll moves the indicator past the track proportionally
     * so it follows the bounce. Without a minimum size this equals `scroll * (viewport / contentSize)`.
     */
    private fun indicatorPosition(scroll: Float, maxScroll: Float, viewport: Float, contentSize: Float, indicatorSize: Float): Float {
        val clampedScroll = scroll.coerceIn(0f, maxScroll)
        val trackPosition = if (maxScroll > 0f) clampedScroll / maxScroll * (viewport - indicatorSize) else 0f
        val overscroll = scroll - clampedScroll

        return trackPosition + if (contentSize > 0f) overscroll * (viewport / contentSize) else 0f
    }

    override fun onContentChanged() {
        super.onContentChanged()

        // Proportional size, but never smaller than MIN_INDICATOR_SIZE so long lists keep a usable indicator.
        verticalIndicator?.height = (height * (height / scrollableContentHeight).coerceAtMost(1f)).coerceIn(min(MIN_INDICATOR_SIZE, height), height)
        horizontalIndicator?.width = (width * (width / scrollableContentWidth).coerceAtMost(1f)).coerceIn(min(MIN_INDICATOR_SIZE, width), width)
    }

    //region Input

    private fun handleUserScroll() {

        val length = hypot(deltaX, deltaY)

        fun decreaseInBoundary(current: Float, delta: Float, max: Float): Float {
            if (current - delta > 0f && current - delta < max) {
                return delta
            }
            return delta * if (length > 0) length.pow(0.7f) / length else 0f
        }

        if (scrollAxes.isHorizontal && !Precision.almostEquals(deltaX, 0f)) {
            scrollX -= decreaseInBoundary(scrollX, deltaX, maxScrollX)

            if (!overflowAxes.isHorizontal) {
                scrollX = scrollX.coerceIn(0f, maxScrollX)
            }
        }

        if (scrollAxes.isVertical && !Precision.almostEquals(deltaY, 0f)) {
            scrollY -= decreaseInBoundary(scrollY, deltaY, maxScrollY)

            if (!overflowAxes.isVertical) {
                scrollY = scrollY.coerceIn(0f, maxScrollY)
            }
        }

        deltaX = 0f
        deltaY = 0f
    }
    
    /**
     * Handles dragging of the vertical indicator, see [isVerticalIndicatorDraggable].
     *
     * @return `true` if the event was consumed by the indicator drag.
     */
    private fun handleIndicatorDrag(event: TouchEvent, localX: Float, localY: Float): Boolean {
        val indicator = verticalIndicator

        if (!isVerticalIndicatorDraggable || indicator == null || !scrollAxes.isVertical || maxScrollY <= 0f) {
            isDraggingIndicator = false
            return false
        }

        val trackLength = height - indicator.height

        fun scrollToFinger() {
            if (trackLength > 0f) {
                scrollY = ((localY - indicatorGrabOffset) / trackLength).coerceIn(0f, 1f) * maxScrollY
            }
            indicator.alpha = 1f
        }

        when (event.action) {
            ACTION_DOWN -> {
                if (localX < width - indicatorTouchWidth) {
                    isDraggingIndicator = false
                    return false
                }

                isDraggingIndicator = true
                velocityX = 0f
                velocityY = 0f

                // Grabbing the indicator keeps the finger's offset on it, touching elsewhere on the track centers it.
                val indicatorTop = indicator.y
                indicatorGrabOffset = if (localY in indicatorTop..(indicatorTop + indicator.height))
                    localY - indicatorTop
                else
                    indicator.height / 2f

                scrollToFinger()
                return true
            }

            ACTION_MOVE -> {
                if (!isDraggingIndicator) {
                    return false
                }
                scrollToFinger()
                return true
            }

            else -> {
                if (!isDraggingIndicator) {
                    return false
                }
                isDraggingIndicator = false
                return true
            }
        }
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (preventScrolling) {
            return super.onAreaTouched(event, localX, localY)
        }

        if (handleIndicatorDrag(event, localX, localY)) {
            return true
        }

        when (event.action) {

            ACTION_DOWN -> {
                initialX = localX
                initialY = localY

                velocityX = 0f
                velocityY = 0f

                velocityTracker.clear()
                velocityTracker.addSample(event.sampleTimeMillis, localX, localY)

                gestureStartX = localX
                gestureStartY = localY
                gestureStartTimeMillis = event.sampleTimeMillis
            }

            ACTION_MOVE -> {
                val moveDeltaX = localX - initialX
                val moveDeltaY = localY - initialY

                val isScrollingHorizontal = scrollAxes.isHorizontal && abs(moveDeltaX) > minimumTravel.x
                val isScrollingVertical = scrollAxes.isVertical && abs(moveDeltaY) > minimumTravel.y

                velocityTracker.addSample(event.sampleTimeMillis, localX, localY)

                if (isScrolling || isScrollingHorizontal || isScrollingVertical) {

                    // If it was already scrolling we don't need to subtract the minimum travel.
                    if (isScrolling) {
                        deltaX = moveDeltaX
                        deltaY = moveDeltaY
                    } else {
                        // Only the axes that crossed the threshold start moving, and only by the distance
                        // traveled past it (in the direction of travel).
                        deltaX = if (isScrollingHorizontal) moveDeltaX - sign(moveDeltaX) * minimumTravel.x else 0f
                        deltaY = if (isScrollingVertical) moveDeltaY - sign(moveDeltaY) * minimumTravel.y else 0f
                    }
                    handleUserScroll()
                    updateVelocityFromTracker()

                    initialX = localX
                    initialY = localY
                    isUserScrolling = true
                } else {
                    isUserScrolling = false
                }
            }

            ACTION_UP -> {
                // Only a drag can leave a non-zero velocity here, since ACTION_DOWN resets it. Recompute it including
                // the release sample so that a finger which stopped before lifting does not fling with a stale velocity.
                if (velocityX != 0f || velocityY != 0f) {
                    velocityTracker.addSample(event.sampleTimeMillis, localX, localY)
                    updateVelocityFromTracker()
                }

                velocityTracker.clear()
                applyGestureVelocity(event, localX, localY)
                isUserScrolling = false
            }

            else -> {
                applyGestureVelocity(event, localX, localY)
                isUserScrolling = false
            }
        }

        if (!isScrolling) {
            super.onAreaTouched(event, localX, localY)
        }

        return true
    }

    /**
     * Replaces the fling velocity with the average velocity of the whole drag gesture when [velocityFromGesture] is set.
     * Must be called on release, before [isUserScrolling] is reset.
     */
    private fun applyGestureVelocity(event: TouchEvent, localX: Float, localY: Float) {
        if (!velocityFromGesture || !isUserScrolling) {
            return
        }

        val gestureTimeSeconds = (event.sampleTimeMillis - gestureStartTimeMillis) / 1000f

        if (gestureTimeSeconds > 0.01f) {
            if (scrollAxes.isHorizontal) {
                velocityX = (localX - gestureStartX) / gestureTimeSeconds
            }
            if (scrollAxes.isVertical) {
                velocityY = (localY - gestureStartY) / gestureTimeSeconds
            }
        }
    }

    private fun updateVelocityFromTracker() {
        // Velocity is measured over a time window rather than between consecutive samples. Consecutive samples can
        // be very close (or identical) in time, which would turn a tiny movement into a huge velocity. A window also
        // naturally decays to zero when the pointer stops moving for longer than the window.
        if (scrollAxes.isHorizontal) {
            velocityX = velocityTracker.computeVelocityX()
        }

        if (scrollAxes.isVertical) {
            velocityY = velocityTracker.computeVelocityY()
        }
    }

    /**
     * The time at which this [TouchEvent] was sampled, in [SystemClock.uptimeMillis] time base.
     *
     * Falls back to [SystemClock.uptimeMillis] for synthesized [TouchEvent]s.
     */
    private val TouchEvent.sampleTimeMillis: Long
        get() = motionEvent?.eventTime?.takeIf { it > 0 } ?: SystemClock.uptimeMillis()

    //endregion

    override fun attachChild(pEntity: IEntity?, pIndex: Int): Boolean {
        if (!mChildren.isNullOrEmpty()) {
            throw IllegalStateException("UIScrollableContainer can only have one child entity.")
        }
        return super.attachChild(pEntity, pIndex)
    }

    override fun attachChild(pEntity: IEntity?) {
        if (!mChildren.isNullOrEmpty()) {
            throw IllegalStateException("UIScrollableContainer can only have one child entity.")
        }
        super.attachChild(pEntity)
    }


    companion object {

        const val DEFAULT_DECELERATION = 0.98f
        const val MIN_INDICATOR_SIZE = 48f
        const val DEFAULT_MINIMUM_TRAVEL = 20f
        const val DEFAULT_MAX_VELOCITY = 3000f

    }
}

/**
 * Estimates pointer velocity from the samples within a short trailing time window.
 */
private class PointerVelocityTracker {

    private val times = LongArray(CAPACITY)
    private val xs = FloatArray(CAPACITY)
    private val ys = FloatArray(CAPACITY)

    private var start = 0
    private var count = 0

    fun clear() {
        start = 0
        count = 0
    }

    fun addSample(timeMillis: Long, x: Float, y: Float) {
        // Drop samples that fell out of the window, or all of them if time went backwards.
        while (count > 0) {
            val oldestTime = times[start]

            if (timeMillis - oldestTime > HORIZON_MILLIS || timeMillis < oldestTime) {
                start = (start + 1) % CAPACITY
                count--
            } else {
                break
            }
        }

        if (count == CAPACITY) {
            start = (start + 1) % CAPACITY
            count--
        }

        val index = (start + count) % CAPACITY

        times[index] = timeMillis
        xs[index] = x
        ys[index] = y
        count++
    }

    /**
     * Computes the velocity on the x-axis in px/s, or 0 if there is not enough data.
     */
    fun computeVelocityX() = computeVelocity(xs)

    /**
     * Computes the velocity on the y-axis in px/s, or 0 if there is not enough data.
     */
    fun computeVelocityY() = computeVelocity(ys)

    private fun computeVelocity(values: FloatArray): Float {
        if (count < 2) {
            return 0f
        }

        val newest = (start + count - 1) % CAPACITY
        val elapsedMillis = times[newest] - times[start]

        if (elapsedMillis <= 0) {
            return 0f
        }

        return (values[newest] - values[start]) * 1000f / elapsedMillis
    }

    companion object {
        private const val HORIZON_MILLIS = 100L
        private const val CAPACITY = 64
    }
}

/**
 * Sets the [UIScrollableContainer.preventScrolling] property for all scrollable containers
 * in the hierarchy of this entity.
 */
fun UIComponent.setHierarchyScrollPrevention(value: Boolean) {

    var parent = parent
    while (parent != null) {
        if (parent is UIScrollableContainer) {
            parent.preventScrolling = value
        }
        parent = parent.parent
    }

    for (i in 0 until childCount) {
        val child = getChildByIndex(i)
        if (child is UIScrollableContainer) {
            child.preventScrolling = value
        }
    }
}