package com.reco1l.andengine

import android.util.Log
import com.reco1l.andengine.component.*
import com.reco1l.andengine.ui.*
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.andengine.timing.FramedClock
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IFrameBasedClock
import com.rian.andengine.timing.StopwatchClock
import javax.microedition.khronos.opengles.GL10
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.entity.scene.Scene
import org.anddev.andengine.entity.shape.IShape
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.opengl.util.GLHelper


/**
 * Scene with extended functionality.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
open class UIScene : Scene(), IShape, IClockProvider<IFrameBasedClock> {
    /**
     * Whether this [UIScene] should clip its children.
     */
    open var clipToBounds = false


    private var cameraWidth = 0f
    private var cameraHeight = 0f

    //region Events

    /**
     * Called every update thread tick, avoid heavy operations here.
     */
    var onUpdateTick: OnUpdateEvent? = null

    //endregion


    init {
        super.setTouchAreaBindingEnabled(true)
        super.setOnAreaTouchTraversalFrontToBack()
    }


    //region Update

    final override fun onUpdate(deltaTimeSec: Float) {
        if (isIgnoreUpdate) {
            return
        }

        originalClock.processFrame()

        if (processCustomClock) {
            customClock?.processFrame()
        }

        onManagedUpdate(clock.elapsedFrameTime)
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        onUpdateTick?.invoke(deltaTimeSec)

        super.onManagedUpdate(deltaTimeSec)
    }

    //region Timekeeping

    /**
     * Whether [IFrameBasedClock.processFrame] should be automatically invoked on this [UIScene]'s [clock] in
     * [onManagedUpdate]. This should only be set to false in scenarios where the clock is updated elsewhere.
     */
    var processCustomClock = true

    /**
     * The original [IFrameBasedClock] of this [UIScene]. This clock is always kept running.
     *
     * May be overridden by a custom clock or its parent's clock if available.
     */
    val originalClock: IFrameBasedClock = FramedClock(StopwatchClock(true))

    private var customClock: IFrameBasedClock? = null
    // Cache inherited clock here to avoid parent tree climbing.
    private var inheritedClock: IFrameBasedClock? = null

    override var clock
        get() = customClock ?: inheritedClock ?: originalClock
        set(value) {
            customClock = value
            updateClock(inheritedClock)
        }

    /**
     * The current frame's time as observed by this [UIScene]'s [clock].
     */
    val time by clock::timeInfo

    /**
     * Updates the [IFrameBasedClock] to be used as the parent-inherited clock of this [UIScene].
     *
     * To update the custom clock that this [UIScene] uses, set the [clock] property instead.
     *
     * @param clock The [IFrameBasedClock] to use.
     */
    protected open fun updateClock(clock: IFrameBasedClock?) {
        inheritedClock = clock
        val currentClock = this.clock

        mChildren?.fastForEach {
            (it as? UIComponent)?.updateClock(currentClock)
            (it as? UIScene)?.updateClock(currentClock)
        }

        (mChildScene as? UIScene)?.updateClock(currentClock)
    }

    //endregion

    override fun setChildScene(childScene: Scene?, modalDraw: Boolean, modalUpdate: Boolean, modalTouch: Boolean) {
        childScene?.onDetached()
        super.setChildScene(childScene, modalDraw, modalUpdate, modalTouch)
        childScene?.onAttached()
    }

    override fun clearChildScene() {
        childScene?.onDetached()
        super.clearChildScene()
    }

    override fun onAttached() {
        val parentClock = (parent as? IClockProvider<*>)?.clock ?: (parent as? UIComponent)?.clock
        updateClock(parentClock as? IFrameBasedClock)

        fun IEntity.propagateSkinChanges() {

            if (this is UIComponent) {
                onThemeChanged(Theme.current)
            }

            if (this is ISkinnable) {
                onSkinChanged()
            }

            forEach { it.propagateSkinChanges() }
        }

        propagateSkinChanges()
    }

    override fun onDetached() {
        super.onDetached()
        updateClock(null)
    }

    //endregion

    //region Drawing

    override fun onDraw(gl: GL10, camera: Camera) {
        if (!isVisible) {
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = GLHelper.isEnableScissorTest()
            GLHelper.enableScissorTest(gl)

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, 0f))
            val (bottomRightX, bottomRightY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(width, height))
            val (bottomLeftX, bottomLeftY) = camera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, height))

            val minX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)
            val maxX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            ScissorStack.pushScissor(minX, minY, maxX - minX, maxY - minY)
            onManagedDraw(gl, camera)
            ScissorStack.pop()

            if (!wasScissorTestEnabled) {
                GLHelper.disableScissorTest(gl)
            }
        } else {
            onManagedDraw(gl, camera)
        }
    }

    override fun onManagedDrawChildren(gl: GL10, camera: Camera) {
        cameraWidth = camera.widthRaw
        cameraHeight = camera.heightRaw
        super.onManagedDrawChildren(gl, camera)
    }

    //endregion

    //region Input

    override fun registerTouchArea(area: ITouchArea) {

        if (area == this) {
            throw IllegalArgumentException("Cannot register an area to itself.")
        }

        super.registerTouchArea(area)
    }

    override fun setTouchAreaBindingEnabled(pTouchAreaBindingEnabled: Boolean) {
        Log.w("ExtendedScene", "Touch area binding is always enabled for ExtendedScene.")
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        // This should never be called, as the scene itself is not a touch area.
        return true
    }

    //endregion

    //region Delegation

    override fun contains(pX: Float, pY: Float): Boolean = true

    override fun getWidth(): Float = cameraWidth

    override fun getHeight(): Float = cameraHeight

    override fun collidesWith(shape: IShape): Boolean = false

    override fun getBaseWidth(): Float = cameraWidth

    override fun getBaseHeight(): Float = cameraHeight

    override fun getWidthScaled(): Float = cameraWidth * scaleX

    override fun getHeightScaled(): Float = cameraHeight * scaleY

    override fun isCullingEnabled(): Boolean = false

    //endregion

    //region Unsupported methods

    override fun setColor(pRed: Float, pGreen: Float, pBlue: Float) {
        Log.w("ExtendedScene", "Color is not supported for scenes.")
    }

    override fun setCullingEnabled(pCullingEnabled: Boolean) {
        Log.w("ExtendedScene", "Culling is not supported for scenes.")
    }

    override fun setBlendFunction(pSourceBlendFunction: Int, pDestinationBlendFunction: Int) {
        Log.w("ExtendedScene", "Blend functions are not supported for scenes.")
    }

    //endregion

    open fun show() {
        UIEngine.current.scene = this
    }
}