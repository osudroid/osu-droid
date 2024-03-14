package com.reco1l.legacy.graphics.texture

import org.andengine.opengl.texture.PixelFormat
import org.andengine.opengl.texture.Texture
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.GlobalManager

class BlankTextureRegion : TextureRegion(BlankTexture(), 0f, 0f, 1f, 1f)

class BlankTexture : Texture(GlobalManager.getInstance().engine.textureManager, PixelFormat.RGBA_8888, TextureOptions.DEFAULT, null)
{
    override fun getWidth() = 1
    override fun getHeight() = 1

    override fun writeTextureToHardware(pGLState: GLState) = Unit

}