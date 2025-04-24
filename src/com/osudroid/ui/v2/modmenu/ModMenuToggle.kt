package com.osudroid.ui.v2.modmenu

import com.osudroid.multiplayer.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.*

class ModMenuToggle(val mod: Mod): TextButton() {

    private val titleText = firstOf<ExtendedText>()!!
    private val descriptionText = ExtendedText()


    init {
        titleText.detachSelf()

        +LinearContainer().apply {
            width = FillParent
            padding = Vec4(0f, 6f)
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            orientation = Orientation.Vertical

            +titleText.apply {
                height = MatchContent
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.TopLeft
                anchor = Anchor.TopLeft
                origin = Anchor.TopLeft
            }

            +descriptionText.apply {
                width = FillParent
                font = ResourceManager.getInstance().getFont("xs")
                text = mod.description
                clipChildren = true
                alpha = 0.75f
            }
        }

        width = FillParent
        theme = TextButtonTheme(
            iconSize = 38f,
            backgroundColor = 0xFF1E1E2E
        )
        text = mod.name
        leadingIcon = ModIcon(mod)
        padding = Vec4(20f, 8f)

        onActionUp = {
            if (isSelected) {
                ModMenu.removeMod(mod)
                ResourceManager.getInstance().getSound("check-off")?.play()
            } else {
                ModMenu.addMod(mod)
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

    override fun onManagedUpdate(deltaTimeSec: Float) {

        // Match the description text color with the title text color during animations.
        if (!descriptionText.color.colorEquals(titleText.color)) {
            descriptionText.color = titleText.color.copy(alpha = descriptionText.alpha)
        }

        super.onManagedUpdate(deltaTimeSec)
    }
}