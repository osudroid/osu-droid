package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.texture.*
import com.reco1l.framework.*
import com.rian.osu.mods.*
import org.anddev.andengine.engine.camera.*
import ru.nsu.ccfit.zuev.osu.*
import javax.microedition.khronos.opengles.*

/**
 * The icon for a mod in the mod menu.
 */
class ModIcon(mod: Mod) : Container() {

    init {
        inheritAncestorsColor = false

        val texture = ResourceManager.getInstance().getTexture(mod.textureName)

        if (texture is BlankTextureRegion || texture == null) {
            background = Box().apply {
                applyTheme = { color = it.accentColor * 0.1f }
            }

            attachChild(ExtendedText().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                text = mod.acronym
                font = ResourceManager.getInstance().getFont("smallFont")
                applyTheme = { color = it.accentColor }
            })
        } else {
            attachChild(ExtendedSprite(texture).apply {
                width = FillParent
                height = FillParent
            })
        }
    }


    override fun onManagedDraw(gl: GL10, camera: Camera) {

        val acronymText = get<ExtendedEntity>(0)
        if (acronymText is ExtendedText) {
            acronymText.setScale(height * 0.6f / acronymText.contentHeight)
        }

        (background as? Box)?.cornerRadius = height * 0.2f

        super.onManagedDraw(gl, camera)
    }

}