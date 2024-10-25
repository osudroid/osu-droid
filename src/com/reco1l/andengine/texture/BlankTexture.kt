package com.reco1l.andengine.texture

import org.anddev.andengine.opengl.texture.Texture
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.region.TextureRegion
import javax.microedition.khronos.opengles.GL10

class BlankTextureRegion : TextureRegion(BlankTexture(), 0, 0, 1, 1)

class BlankTexture : Texture(PixelFormat.RGBA_8888, TextureOptions.DEFAULT, null)
{
    override fun getWidth() = 1
    override fun getHeight() = 1
    override fun writeTextureToHardware(gl: GL10) = Unit
}