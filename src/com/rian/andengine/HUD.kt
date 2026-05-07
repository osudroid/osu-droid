package com.rian.andengine

import com.reco1l.andengine.UIScene
import com.reco1l.andengine.component.UIComponent
import com.reco1l.toolkt.kotlin.fastForEach
import com.rian.andengine.timing.FramedClock
import com.rian.andengine.timing.IClockProvider
import com.rian.andengine.timing.IFrameBasedClock
import com.rian.andengine.timing.StopwatchClock
import org.anddev.andengine.engine.camera.hud.HUD as AndEngineHUD
import org.anddev.andengine.entity.scene.Scene

/**
 * An [AndEngineHUD] that provides an [IFrameBasedClock] to its [UIComponent] children.
 */
open class HUD : AndEngineHUD(), IClockProvider<IFrameBasedClock> {
    override var clock: IFrameBasedClock = FramedClock(StopwatchClock(true))
        set(value) {
            field = value
            updateClock(value)
        }

    /**
     * The current frame's time as observed by this [HUD]'s [IFrameBasedClock].
     */
    val time by clock::timeInfo

    init {
        setOnAreaTouchTraversalFrontToBack()
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        clock.processFrame()

        super.onManagedUpdate(clock.elapsedFrameTime)
    }

    protected open fun updateClock(clock: IFrameBasedClock) {
        mChildren?.fastForEach {
            (it as? UIComponent)?.updateClock(clock)
            (it as? UIScene)?.updateClock(clock)
        }
    }

    override fun setChildScene(childScene: Scene?, modalDraw: Boolean, modalUpdate: Boolean, modalTouch: Boolean) {
        childScene?.onDetached()
        super.setChildScene(childScene, modalDraw, modalUpdate, modalTouch)
        childScene?.onAttached()
    }

    override fun clearChildScene() {
        childScene?.onDetached()
        super.clearChildScene()
    }
}