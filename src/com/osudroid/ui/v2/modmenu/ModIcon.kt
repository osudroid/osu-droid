package com.osudroid.ui.v2.modmenu

import com.osudroid.ui.v2.*
import com.osudroid.utils.updateThread
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.texture.*
import com.reco1l.andengine.ui.*
import com.rian.osu.mods.*
import com.rian.osu.utils.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import javax.microedition.khronos.opengles.*

/**
 * The icon for a mod in the mod menu.
 */
class ModIcon(val mod: Mod) : UIContainer(), ISkinnable {

    constructor(acronym: String): this(ModUtils.allModsInstances.find { it.acronym.equals(acronym, ignoreCase = true) }!!)


    init {
        inheritAncestorsColor = false
        onSkinChanged()
    }


    private fun fetchTextureRegion(): TextureRegion? {
        return ResourceManager.getInstance().getTexture(mod.iconTextureName)?.takeUnless { it is BlankTextureRegion }
    }


    override fun onManagedDraw(gl: GL10, camera: Camera) {

        val acronymText = get<UIComponent>(0)
        if (acronymText is UIText) {
            acronymText.setScale(height * 0.6f / acronymText.contentHeight)
        }

        (background as? UIBox)?.cornerRadius = height * 0.2f

        super.onManagedDraw(gl, camera)
    }

    override fun onSkinChanged() = updateThread {
        detachChildren()

        val texture = fetchTextureRegion()

        if (texture == null) {
            background = UIBox().apply {
                applyTheme = { color = it.accentColor * 0.1f }
            }

            attachChild(UIText().apply {
                anchor = Anchor.Center
                origin = Anchor.Center
                text = mod.acronym
                font = ResourceManager.getInstance().getFont("smallFont")
                applyTheme = { color = it.accentColor }
            })

            return@updateThread
        }

        background = null

        attachChild(OsuSkinnableSprite(mod.iconTextureName).apply {
            width = FillParent
            height = FillParent
            buffer = sharedSpriteVBO
        })
    }


    companion object {
        private val sharedSpriteVBO = UISprite.SpriteVBO().asSharedDynamically()
    }
}