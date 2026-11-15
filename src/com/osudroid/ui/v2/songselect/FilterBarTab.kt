package com.osudroid.ui.v2.songselect

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager

/**
 * A small tab in the row below the buttons of the [SongSelectFilterBar], showing the state a button stands for.
 */
class FilterBarTab : UIContainer() {

    private val textEntity = UIText().apply {
        font = ResourceManager.getInstance().getFont("xs")
        anchor = Anchor.Center
        origin = Anchor.Center
        applyTheme = { color = it.accentColor }
    }

    var text by textEntity::text


    init {
        height = HEIGHT
        padding = Vec4(3f.srem, 0f)
        background = UIBox().apply {
            cornerRadius = 0.5f.srem
            applyTheme = { color = it.accentColor * 0.1f }
        }
        +textEntity
    }


    companion object {
        const val HEIGHT = 32f

        /**
         * The space between the buttons and the row below them.
         */
        const val SPACING = 6f
    }

}
