package com.rian.andengine

import com.reco1l.andengine.UIEngine
import com.reco1l.andengine.component.UIComponent
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IClockReceiver
import com.rian.andengine.timing.IFrameBasedClock
import org.anddev.andengine.engine.camera.hud.HUD as AndEngineHUD
import org.anddev.andengine.entity.scene.Scene

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

        mChildren?.fastForEach {
            @Suppress("UNCHECKED_CAST")
            (it as? IClockReceiver<IFrameBasedClock?>)?.updateClock(currentClock)
        }

        @Suppress("UNCHECKED_CAST")
        (mChildScene as? IClockReceiver<IFrameBasedClock?>)?.updateClock(currentClock)
    }

    final override fun onUpdate(deltaTimeSec: Float) {
        if (processCustomClock) {
            customClock?.processFrame()
        }

        if (isIgnoreUpdate) {
            return
        }

        // Fallback to parent or engine-provided delta time in case clock is not present.
        onManagedUpdate(clock?.elapsedFrameTime ?: deltaTimeSec)
    }

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
        val parent = parent
        val parentClock =
            if (parent != null) (parent as? IClockProvider<*>)?.clock as? IFrameBasedClock
            else UIEngine.current.clock

        updateClock(parentClock)

        super.onAttached()
    }

    override fun onDetached() {
        updateClock(null)

        super.onDetached()
    }
}