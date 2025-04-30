package com.osudroid.ui.v2.modmenu

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
        inheritAncestorsColor = false

        val texture = ResourceManager.getInstance().getTexture(mod.textureName)

        if (texture is BlankTextureRegion || texture == null) {
            background = Box().apply {
                cornerRadius = 12f
                applyTheme = { color = it.accentColor * 0.1f }
            }

            attachChild(ExtendedText().apply {
                width = FillParent
                height = FillParent
                text = mod.acronym
                font = ResourceManager.getInstance().getFont("smallFont")
                alignment = Anchor.Center
                applyTheme = { color = it.accentColor }
            })
        } else {
            attachChild(ExtendedSprite(texture).apply {
                width = FillParent
                height = FillParent
            })
        }
    }

}