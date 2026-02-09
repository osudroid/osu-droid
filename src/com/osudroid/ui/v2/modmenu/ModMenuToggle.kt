package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.*
import com.osudroid.utils.searchContiguously
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.Vec4
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.*


private val backgroundBufferRef = MutableReference<UIBox.BoxVBO?>(null)
private val textBufferRef = MutableReference<CompoundBuffer?>(null)

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
        style += {
            padding = Vec4(3f.srem, 2f.srem)
        }
        background?.apply {
            bufferReference = backgroundBufferRef
            bufferSharingMode = BufferSharingMode.Static
        }

        fillContainer {
            width = Size.Full
            cullingMode = CullingMode.CameraBounds
            style = {
                spacing = 3f.srem
            }

            +ModIcon(mod).apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = {
                    width = 1.35f.rem
                    height = 1.35f.rem
                }
            }

            linearContainer {
                width = Size.Full
                orientation = Orientation.Vertical
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                text {
                    text = mod.name
                    bufferReference = textBufferRef
                    bufferSharingMode = BufferSharingMode.Dynamic
                }

                text {
                    width = Size.Full
                    text = mod.description
                    clipToBounds = true
                    bufferReference = textBufferRef
                    bufferSharingMode = BufferSharingMode.Dynamic
                    style = {
                        fontSize = FontSize.XS
                        alpha = 0.75f
                    }
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

}