package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * A small tab below a button of the [SongSelectFilterBar], showing the state the button stands for.
 */
class FilterBarTab : UIClickableContainer() {

    private val textEntity = UIText().apply {
        font = ResourceManager.getInstance().getFont("xs")
        anchor = Anchor.Center
        origin = Anchor.Center
        applyTheme = { color = it.accentColor }
    }

    var text by textEntity::text


    init {
        height = HEIGHT
        background = UIBox().apply {
            cornerRadius = 0.5f.srem
            applyTheme = { color = it.accentColor * 0.1f }
        }
        +textEntity
    }


    companion object {
        private const val HEIGHT = 32f

        /**
         * The space between a tab and its button.
         */
        const val SPACING = 6f
    }

}
