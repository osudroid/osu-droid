package com.reco1l.andengine

import android.util.Log
import com.reco1l.andengine.component.*
import com.reco1l.andengine.ui.*
import org.andengine.engine.camera.Camera
import org.andengine.entity.IEntity
import org.andengine.entity.scene.ITouchArea
import org.andengine.entity.scene.Scene
import org.andengine.entity.shape.IShape
import org.andengine.input.touch.TouchEvent
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.IVertexBufferObject
import org.andengine.opengl.vbo.VertexBufferObjectManager
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IClockReceiver
import com.rian.andengine.timing.IFrameBasedClock


/**
 * Scene with extended functionality.
 * @author Reco1l
 */
@Suppress("MemberVisibilityCanBePrivate")
open class UIScene : Scene(), IShape, IClockProvider<IFrameBasedClock?>, IClockReceiver<IFrameBasedClock?> {
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
        super.setTouchAreaBindingOnActionDownEnabled(true)
        super.setOnAreaTouchTraversalFrontToBack()
    }


    //region Update

    final override fun onUpdate(deltaTimeSec: Float) {
        if (loadState == LoadState.NotLoaded) {
            return
        }

        if (processCustomClock) {
            customClock?.processFrame()
        }

        if (loadState == LoadState.Ready) {
            loadState = LoadState.Loaded
            onLoadComplete()
        }

        if (!isIgnoreUpdate) {
            // Fallback to parent or engine-provided delta time in case clock is not present.
            onManagedUpdate(clock?.elapsedFrameTime ?: deltaTimeSec)
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        onUpdateTick?.invoke(deltaTimeSec)

        super.onManagedUpdate(deltaTimeSec)
    }

    /**
     * Whether this [UIScene] is currently loaded and part of the active update loop.
     *
     * This becomes `true` after [onLoadComplete] is called and stays `true` until this [UIScene] is unloaded.
     */
    val isLoaded
        get() = loadState == LoadState.Loaded

    private var loadState = LoadState.NotLoaded

    /**
     * Called when this [UIScene] is fully loaded and ready for use.
     *
     * This is invoked when [onUpdate] is called for the first time after this [UIScene] receives a valid [clock]
     * **and** before [onManagedUpdate]. It is safe to start animations and modifiers here.
     *
     * Note that this is called regardless of [isIgnoreUpdate], and can be called multiple times during this
     * [UIScene]'s lifetime if it is detached and re-attached.
     */
    protected open fun onLoadComplete() {}

    /**
     * Called when this [UIScene] is being unloaded.
     *
     * This is invoked when this [UIScene] loses its [clock], usually when it is detached from its [parent]. If this
     * [UIScene] is re-attached, [onLoadComplete] will be called again.
     */
    protected open fun onUnload() {}

    //endregion

    //region Timekeeping

    /**
     * Whether [IFrameBasedClock.processFrame] should be automatically invoked on this [UIScene]'s [clock] in
     * [onManagedUpdate]. This should only be set to false in scenarios where the clock is updated elsewhere.
     */
    var processCustomClock = true

    private var customClock: IFrameBasedClock? = null
    // Cache inherited clock here to avoid parent tree climbing.
    private var inheritedClock: IFrameBasedClock? = null

    /**
     * The [IFrameBasedClock] of this [UIScene]. Used for keeping track of time across frames. By default, this is
     * inherited from [parent] or [UIEngine].
     *
     * If set, then the provided value is used as a custom clock and [parent] or [UIEngine]'s [IFrameBasedClock] is
     * ignored.
     */
    override var clock: IFrameBasedClock?
        get() = customClock ?: inheritedClock
        set(value) {
            customClock = value
            updateClock(inheritedClock)
        }

    /**
     * The current frame's time as observed by this [UIScene]'s [clock].
     */
    val time
        get() = clock?.timeInfo

    override fun updateClock(clock: IFrameBasedClock?) {
        inheritedClock = clock
        val currentClock = this.clock

        if (currentClock != null) {
            if (loadState == LoadState.NotLoaded) {
                loadState = LoadState.Ready
            }
        } else {
            if (loadState == LoadState.Loaded) {
                onUnload()
            }
            loadState = LoadState.NotLoaded
        }

        mChildren?.fastForEach {
            @Suppress("UNCHECKED_CAST")
            (it as? IClockReceiver<IFrameBasedClock?>)?.updateClock(currentClock)
        }

        @Suppress("UNCHECKED_CAST")
        (mChildScene as? IClockReceiver<IFrameBasedClock?>)?.updateClock(currentClock)
    }

    //endregion

    override fun setChildScene(childScene: Scene?, modalDraw: Boolean, modalUpdate: Boolean, modalTouch: Boolean) {
        this.childScene?.onDetached()
        super.setChildScene(childScene, modalDraw, modalUpdate, modalTouch)
        childScene?.onAttached()
    }

    override fun clearChildScene() {
        childScene?.onDetached()
        super.clearChildScene()
    }

    override fun onAttached() {
        val inheritedClockProvider = (parent as? IClockProvider<*>) ?: (mParentScene as? IClockProvider<*>)
        updateClock(inheritedClockProvider?.clock as? IFrameBasedClock ?: UIEngine.current.clock)

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
        updateClock(null)

        super.onDetached()
    }

    //endregion

    //region Drawing

    override fun onManagedDraw(pGLState: GLState, pCamera: Camera) {
        cameraWidth = pCamera.widthRaw
        cameraHeight = pCamera.heightRaw

        if (!isVisible) {
            return
        }

        if (clipToBounds) {
            val wasScissorTestEnabled = pGLState.isScissorTestEnabled

            // Entity coordinates in screen's space.
            val (topLeftX, topLeftY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, 0f))
            val (topRightX, topRightY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(cameraWidth, 0f))
            val (bottomRightX, bottomRightY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(cameraWidth, cameraHeight))
            val (bottomLeftX, bottomLeftY) = pCamera.convertSceneToSurfaceCoordinates(convertLocalToSceneCoordinates(0f, cameraHeight))

            val minX = minOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val minY = minOf(topLeftY, bottomLeftY, bottomRightY, topRightY)
            val maxX = maxOf(topLeftX, bottomLeftX, bottomRightX, topRightX)
            val maxY = maxOf(topLeftY, bottomLeftY, bottomRightY, topRightY)

            pGLState.enableScissorTest()
            ScissorStack.pushScissor(minX, minY, maxX - minX, maxY - minY)
            super.onManagedDraw(pGLState, pCamera)
            ScissorStack.pop()

            if (!wasScissorTestEnabled) {
                pGLState.disableScissorTest()
            }
        } else {
            super.onManagedDraw(pGLState, pCamera)
        }
    }

    //endregion

    //region Input

    override fun registerTouchArea(area: ITouchArea) {

        if (area == this) {
            throw IllegalArgumentException("Cannot register an area to itself.")
        }

        super.registerTouchArea(area)
    }

    override fun setTouchAreaBindingOnActionDownEnabled(pTouchAreaBindingEnabled: Boolean) {
        Log.w("ExtendedScene", "Touch area binding is always enabled for ExtendedScene.")
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        // This should never be called, as the scene itself is not a touch area.
        return true
    }

    //endregion

    //region Delegation

    override fun contains(pX: Float, pY: Float): Boolean = true

    override fun collidesWith(shape: IShape): Boolean = false

    override fun isCullingEnabled(): Boolean = false

    //endregion

    //region IShape stubs

    override fun isBlendingEnabled(): Boolean = true

    override fun setBlendingEnabled(pBlendingEnabled: Boolean) { }

    override fun getBlendFunctionSource(): Int = IShape.BLENDFUNCTION_SOURCE_DEFAULT

    override fun getBlendFunctionDestination(): Int = IShape.BLENDFUNCTION_DESTINATION_DEFAULT

    override fun setBlendFunctionSource(pBlendFunctionSource: Int) { }

    override fun setBlendFunctionDestination(pBlendFunctionDestination: Int) { }

    override fun getVertexBufferObjectManager(): VertexBufferObjectManager? = null

    override fun getVertexBufferObject(): IVertexBufferObject? = null

    override fun getShaderProgram(): ShaderProgram? = null

    override fun setShaderProgram(pShaderProgram: ShaderProgram?) { }

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