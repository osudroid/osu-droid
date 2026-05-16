package com.osudroid.game.replay

import com.reco1l.andengine.ui.UICard

/**
 * Controls playback of gameplay when replaying.
 */
class ReplayPlaybackControl : UICard() {
    /**
     * Controls for playback rate.
     */
    val rateControl = ReplayPlaybackRate()

    init {
        width = FillParent
        title = "Playback"

        content.apply {
            +rateControl
        }
    }
}
