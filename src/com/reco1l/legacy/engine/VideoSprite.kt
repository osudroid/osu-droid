package com.reco1l.legacy.engine

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.Sprite
import javax.microedition.khronos.opengles.GL10
import javax.microedition.khronos.opengles.GL10.GL_TEXTURE_2D

class VideoSprite(source: String, private val engine: Engine) : Sprite(0f, 0f, VideoTexture(source).toRegion())
{

    val texture = textureRegion.texture as VideoTexture

    init
    {
        engine.textureManager.loadTexture(texture)
    }

    fun release()
    {
        texture.release()
        engine.textureManager.unloadTexture(texture)
    }

    override fun doDraw(pGL: GL10, pCamera: Camera?)
    {
        onInitDraw(pGL)

        if (pGL.glGetString(GL10.GL_RENDERER).contains("Mali", true)) {
            pGL.glDisable(GL_TEXTURE_2D)
        }

        pGL.glEnable(GL_TEXTURE_EXTERNAL_OES)

        textureRegion.onApply(pGL)

        onApplyVertices(pGL)
        drawVertices(pGL, pCamera)

        pGL.glDisable(GL_TEXTURE_EXTERNAL_OES)

        if (pGL.glGetString(GL10.GL_RENDERER).contains("Mali", true)) {
            pGL.glEnable(GL_TEXTURE_2D)
        }
    }

    override fun finalize()
    {
        release()
        super.finalize()
    }
}