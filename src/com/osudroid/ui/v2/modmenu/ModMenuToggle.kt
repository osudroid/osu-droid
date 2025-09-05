package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.*
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.*

class ModMenuToggle(var mod: Mod): UIButton() {

    /**
     * Whether the [Mod] represented by this [ModMenuToggle] is incompatible with one or more enabled [Mod]s.
     */
    var hasIncompatibility = false
        set(value) {
            if (field != value) {
                field = value
                applyCompatibilityState()
            }
        }

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
                ModMenu.addMod(mod)
                ResourceManager.getInstance().getSound("check-on")?.play()
            }
        }

        updateVisibility()
    }

    fun updateVisibility() {
        isVisible = if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
            mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                    (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
        } else {
            true
        }
    }

    fun applyCompatibilityState() {
        // Intentionally not using isEnabled here, otherwise the button will not be clickable.
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (hasIncompatibility) 0.5f else 1f, 0.2f)
    }

    companion object {

        private val sharedButtonVBO = UIBox.BoxVBO(12f, UICircle.approximateSegments(12f, 12f, 90f), PaintStyle.Fill)
        private val sharedTextCB = CompoundBuffer(UIText.TextTextureBuffer(256), UIText.TextVertexBuffer(256)).asSharedDynamically()

    }

}