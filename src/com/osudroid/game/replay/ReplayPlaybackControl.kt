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

    /**
     * Controls for seeking.
     */
    val seekControl = ReplayPlaybackSeek()

    init {
        width = FillParent
        title = "Playback"

        content.apply {
            +seekControl
            +rateControl
        }
    }
}
