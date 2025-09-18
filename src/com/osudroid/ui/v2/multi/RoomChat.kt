package com.osudroid.ui.v2.multi

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.*

// TODO: Finish multiplayer chat.
class RoomChat : UIContainer() {

    init {
        width = FillParent
        padding = Vec4(80f, 20f)
        background = UIBox().apply {
            applyTheme = {
                color = it.accentColor * 0.1f
                alpha = 0.9f
            }
        }

        compoundText {
            leadingIcon = UISprite(ResourceManager.getInstance().getTexture("chat"))
            applyTheme = { color = it.accentColor }
            spacing = 4f
            text = "System: Welcome to osu!droid multiplayer!"
        }

    }
}