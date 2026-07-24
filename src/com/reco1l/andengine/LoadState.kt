package com.reco1l.andengine

import com.reco1l.andengine.component.UIComponent
import com.rian.andengine.timing.IFrameBasedClock

/**
 * Possible states of a [UIComponent] or [UIScene] within the loading pipeline.
 */
enum class LoadState {
    /**
     * Not loaded, and no [IFrameBasedClock] has been provided yet. The [UIComponent] or [UIScene] will not process
     * [UIScene.onUpdate] or [UIComponent.onUpdate].
     */
    NotLoaded,

    /**
     * An [IFrameBasedClock] has been provided. The [UIComponent] or [UIScene] is ready to be finalized on the update
     * thread.
     */
    Ready,

    /**
     * Loading is fully completed and the [UIComponent] or [UIScene] is now part of the active update loop.
     */
    Loaded
}
