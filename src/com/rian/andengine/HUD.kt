package com.rian.andengine

import com.reco1l.andengine.LoadState
import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.component.UIComponent
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IClockReceiver
import com.rian.andengine.timing.IFrameBasedClock
import org.andengine.engine.camera.hud.HUD as AndEngineHUD
import org.andengine.entity.scene.Scene

/**
 * An [AndEngineHUD] that provides an [IFrameBasedClock] to its [UIComponent] children.
 */
open class HUD : AndEngineHUD(), IClockProvider<IFrameBasedClock?>, IClockReceiver<IFrameBasedClock?> {
    /**
     * Whether [IFrameBasedClock.processFrame] should be automatically invoked on this [HUD]'s [clock] in
     * [onManagedUpdate]. This should only be set to false in scenarios where the clock is updated elsewhere.
     */
    var processCustomClock = true

    private var customClock: IFrameBasedClock? = null
    // Cache inherited clock here to avoid parent tree climbing.
    private var inheritedClock: IFrameBasedClock? = null

    /**
     * The [IFrameBasedClock] of this [HUD]. Used for keeping track of time across frames. By default, this is
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
     * The current frame's time as observed by this [HUD]'s [clock].
     */
    val time
        get() = clock?.timeInfo

    init {
        setOnAreaTouchTraversalFrontToBack()
    }

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

    /**
     * Whether this [HUD] is currently loaded and part of the active update loop.
     *
     * This becomes `true` after [onLoadComplete] is called and stays `true` until this [HUD] is unloaded.
     */
    val isLoaded
        get() = loadState == LoadState.Loaded

    private var loadState = LoadState.NotLoaded

    /**
     * Called when this [HUD] is fully loaded and ready for use.
     *
     * This is invoked when [onUpdate] is called for the first time after this [HUD] receives a valid [clock] **and**
     * before [onManagedUpdate]. It is safe to start animations and modifiers here.
     *
     * Note that this is called regardless of [isIgnoreUpdate], and can be called multiple times during this [HUD]'s
     * lifetime if it is detached and re-attached.
     */
    protected open fun onLoadComplete() {}

    /**
     * Called when this [HUD] is being unloaded.
     *
     * This is invoked when this [HUD] loses its [clock], usually when it is detached from its [parent]. If this [HUD]
     * is re-attached, [onLoadComplete] will be called again.
     */
    protected open fun onUnload() {}

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

        super.onAttached()
    }

    override fun onDetached() {
        updateClock(null)

        super.onDetached()
    }
}