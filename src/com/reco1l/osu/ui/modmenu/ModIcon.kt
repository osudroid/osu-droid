package com.reco1l.osu.ui.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.texture.*
import com.reco1l.framework.*
import com.rian.osu.mods.*
import ru.nsu.ccfit.zuev.osu.*

/**
 * The icon for a mod in the mod menu.
 */
class ModIcon(mod: Mod) : Container() {

    init {

        val texture = ResourceManager.getInstance().getTexture(mod.textureName)

        if (texture is BlankTextureRegion || texture == null) {
            background = RoundedBox().apply {
                cornerRadius = 14f
                color = ColorARGB(0xFF222234)
            }

            attachChild(ExtendedText().apply {
                width = FitParent
                height = FitParent
                text = mod.acronym
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.Center
            })
        } else {
            attachChild(ExtendedSprite(texture).apply {
                width = FitParent
                height = FitParent
            })
        }
    }

}