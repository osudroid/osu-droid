package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.*
import com.osudroid.utils.searchContiguously
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.*

class ModMenuToggle(var mod: Mod) : UIButton() {

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
        width = Size.Full

        fillContainer {
            width = Size.Full
            cullingMode = CullingMode.CameraBounds
            style = {
                spacing = 2f.srem
            }

            +ModIcon(mod).apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = {
                    width = 1.25f.rem
                    height = 1.25f.rem
                }
            }

            linearContainer {
                width = Size.Full
                orientation = Orientation.Vertical
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                text {
                    text = mod.name
                    fontSize = FontSize.SM
                    buffer = sharedTextCB
                }

                text {
                    width = Size.Full
                    fontSize = FontSize.XS
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

        }

        updateVisibility()
    }

    @JvmOverloads
    fun updateVisibility(searchTerm: String = "") {
        var shouldBeVisible = if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
            mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
        } else {
            true
        }

        if (searchTerm.isNotBlank()) {
            shouldBeVisible = shouldBeVisible &&
                (mod.acronym.equals(searchTerm, true) ||
                    mod.name.searchContiguously(searchTerm, true))
        }

        isVisible = shouldBeVisible
    }

    fun applyCompatibilityState() {
        // Intentionally not using isEnabled here, otherwise the button will not be clickable.
        clearModifiers(ModifierType.Alpha)
        fadeTo(if (hasIncompatibility) 0.5f else 1f, 0.2f)
    }

    companion object {

        private val sharedButtonVBO = UIBox.BoxVBO(12f, UICircle.approximateSegments(12f, 12f, 90f), PaintStyle.Fill)
        private val sharedTextCB = UITextCompoundBuffer(256).asSharedDynamically()

    }

}