package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.*
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.rian.osu.mods.*
import kotlin.reflect.full.createInstance
import ru.nsu.ccfit.zuev.osu.*

class ModMenuToggle(val mod: Mod): UIButton() {


    init {
        orientation = Orientation.Horizontal
        width = FillParent
        spacing = 8f
        cullingMode = CullingMode.CameraBounds

        background = UIBox().apply {
            cornerRadius = 12f
            // Sharing the same VBO across all toggles to reduce memory usage.
            buffer = sharedButtonVBO
        }

        +ModIcon(mod).apply {
            width = 38f
            height = 38f
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
        }

        linearContainer {
            orientation = Orientation.Vertical
            width = FillParent
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft

            text {
                text = mod.name
                font = ResourceManager.getInstance().getFont("smallFont")
                buffer = sharedTextCB
            }

            text {
                width = FillParent
                font = ResourceManager.getInstance().getFont("xs")
                text = mod.description
                clipToBounds = true
                alpha = 0.75f
                buffer = sharedTextCB
            }
        }

        onActionUp = {
            if (isSelected) {
                ModMenu.removeMod(mod)
                ResourceManager.getInstance().getSound("check-off")?.play()
            } else {
                // Create a new instance in case the mod depends on some state that breaks mod compatibility
                // when the same instance is used in multiple places (i.e., Difficulty Adjust with Hard Rock).
                ModMenu.addMod(mod::class.createInstance())
                ResourceManager.getInstance().getSound("check-on")?.play()
            }
        }

        updateEnabledState()
    }

    fun updateEnabledState() {
        isVisible = if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
            mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                    (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
        } else {
            true
        }
    }


    companion object {

        private val sharedButtonVBO = UIBox.BoxVBO(12f, UICircle.approximateSegments(12f, 12f, 90f), PaintStyle.Fill)
        private val sharedTextCB = CompoundBuffer(UIText.TextTextureBuffer(256), UIText.TextVertexBuffer(256)).asSharedDynamically()

    }

}