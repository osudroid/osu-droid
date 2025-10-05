package com.osudroid.ui.v2.multi

import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*

class RoomPlayerButton(room: Room, player: RoomPlayer) : UIButton() {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        color = theme.accentColor
        alpha = if (isEnabled) 1f else 0.5f
    }


    init {
        width = FillParent
        orientation = Orientation.Horizontal
        padding = Vec4(12f)
        spacing = 6f

        background = UIBox().apply {
            cornerRadius = 12f
            applyTheme = {
                color = it.accentColor * 0.1f
                alpha = 0.5f
            }
        }

        linearContainer {
            orientation = Orientation.Vertical
            inheritAncestorsColor = false

            text {
                text = player.name
                applyTheme = { color = it.accentColor }
            }

            if (!room.gameplaySettings.isFreeMod) {
                +ModsIndicator().apply {
                    minHeight = 24f // Force to take space even if no mods are enabled
                    iconSize = 18f
                    mods = player.mods.json
                }
            }
        }

        onActionLongPress = {
            UIDropdown(this@RoomPlayerButton).apply {
                addButton {
                    text = "View profile"
                }
            }.show()
        }
    }
}