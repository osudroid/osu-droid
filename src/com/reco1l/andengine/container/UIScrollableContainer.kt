package com.reco1l.andengine.container

import androidx.annotation.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.shape.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.math.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.*
import org.anddev.andengine.input.touch.*
import org.anddev.andengine.input.touch.TouchEvent.*
import javax.microedition.khronos.opengles.*
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

    private var dragStartTimeMillis = 0L


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

        velocityX *= deceleration.x
        velocityY *= deceleration.y
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

    override fun onManagedDraw(gl: GL10, camera: Camera) {

        firstChild?.also { child ->
            if (child.x != -scrollX || child.y != -scrollY) {
                child.setPosition(-scrollX, -scrollY)
                onScrollChanged()
            }
        }

        super.onManagedDraw(gl, camera)
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
            verticalIndicator.isVisible = scrollAxes == Axes.Both || scrollAxes == Axes.Y

            if (verticalIndicator.alpha > 0f && velocityY == 0f) {
                verticalIndicator.alpha = (verticalIndicator.alpha - deltaTimeSec * 0.75f).coerceAtLeast(0f)
            }

            if (verticalIndicator.isVisible) {
                verticalIndicator.x = width - verticalIndicator.width
                verticalIndicator.y = scrollY * (height / scrollableContentHeight)

                verticalIndicator.onUpdate(deltaTimeSec)
            }
        }

        val horizontalIndicator = horizontalIndicator

        if (horizontalIndicator != null) {
            horizontalIndicator.isVisible = scrollAxes == Axes.Both || scrollAxes == Axes.X

            if (horizontalIndicator.alpha > 0f && velocityX == 0f) {
                horizontalIndicator.alpha = (horizontalIndicator.alpha - deltaTimeSec * 0.75f).coerceAtLeast(0f)
            }

            if (horizontalIndicator.isVisible) {
                horizontalIndicator.x = scrollX * (width / scrollableContentWidth)
                horizontalIndicator.y = height - horizontalIndicator.height

                horizontalIndicator.onUpdate(deltaTimeSec)
            }
        }
    }

    //endregion

    override fun onContentChanged() {
        super.onContentChanged()

        verticalIndicator?.height = height * (height / scrollableContentHeight).coerceAtMost(1f)
        horizontalIndicator?.width = width * (width / scrollableContentWidth).coerceAtMost(1f)
    }

    override fun onManagedDrawChildren(pGL: GL10, pCamera: Camera) {
        super.onManagedDrawChildren(pGL, pCamera)

        horizontalIndicator?.onDraw(pGL, pCamera)
        verticalIndicator?.onDraw(pGL, pCamera)
    }


    //region Input

    private fun handleUserScroll() {

        val dragTimeSeconds = (System.currentTimeMillis() - dragStartTimeMillis) / 1000f
        val length = hypot(deltaX, deltaY)

        fun decreaseInBoundary(current: Float, delta: Float, max: Float): Float {
            if (current - delta > 0f && current - delta < max) {
                return delta
            }
            return delta * if (length > 0) length.pow(0.7f) / length else 0f
        }

        if (scrollAxes.isHorizontal && !Precision.almostEquals(deltaX, 0f)) {
            velocityX = if (dragTimeSeconds > 0.3f) 0f else deltaX / dragTimeSeconds
            scrollX -= decreaseInBoundary(scrollX, deltaX, maxScrollX)

            if (!overflowAxes.isHorizontal) {
                scrollX = scrollX.coerceIn(0f, maxScrollX)
            }
        }

        if (scrollAxes.isVertical && !Precision.almostEquals(deltaY, 0f)) {
            velocityY = if (dragTimeSeconds > 0.3f) 0f else deltaY / dragTimeSeconds
            scrollY -= decreaseInBoundary(scrollY, deltaY, maxScrollY)

            if (!overflowAxes.isVertical) {
                scrollY = scrollY.coerceIn(0f, maxScrollY)
            }
        }

        deltaX = 0f
        deltaY = 0f
    }
    
    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (preventScrolling) {
            return super.onAreaTouched(event, localX, localY)
        }

        when (event.action) {

            ACTION_DOWN -> {
                initialX = localX
                initialY = localY

                velocityX = 0f
                velocityY = 0f
                dragStartTimeMillis = System.currentTimeMillis()
            }

            ACTION_MOVE -> {
                val moveDeltaX = localX - initialX
                val moveDeltaY = localY - initialY

                val isScrollingHorizontal = scrollAxes.isHorizontal && abs(moveDeltaX) > minimumTravel.x
                val isScrollingVertical = scrollAxes.isVertical && abs(moveDeltaY) > minimumTravel.y

                if (isScrolling || isScrollingHorizontal || isScrollingVertical) {

                    // If it was already scrolling we don't need to subtract the minimum travel.
                    if (isScrolling) {
                        deltaX = moveDeltaX
                        deltaY = moveDeltaY
                    } else {
                        deltaX = moveDeltaX - minimumTravel.x
                        deltaY = moveDeltaY - minimumTravel.y
                    }
                    handleUserScroll()

                    initialX = localX
                    initialY = localY
                    isUserScrolling = true
                    dragStartTimeMillis = System.currentTimeMillis()
                } else {
                    isUserScrolling = false
                }
            }

            else -> {
                isUserScrolling = false
            }
        }

        if (!isScrolling) {
            super.onAreaTouched(event, localX, localY)
        }

        return true
    }

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
        const val DEFAULT_MINIMUM_TRAVEL = 20f
        const val DEFAULT_MAX_VELOCITY = 3000f

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
        val child = getChild(i)
        if (child is UIScrollableContainer) {
            child.preventScrolling = value
        }
    }
}