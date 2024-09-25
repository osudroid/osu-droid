package com.reco1l.andengine.sprite

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import com.reco1l.andengine.texture.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.Sprite
import javax.microedition.khronos.opengles.GL10

class VideoSprite(source: String, private val engine: Engine) : Sprite(0f, 0f, VideoTexture(source).toRegion())
{

    val texture = textureRegion.texture as VideoTexture

    private var isMali: Boolean? = null

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
        if (isMali == null) {
            isMali = pGL.glGetString(GL10.GL_RENDERER).contains("Mali", true)
        }

        onInitDraw(pGL)

        // Apparently there is either a bug or unintended behavior in Mali GPUs' OpenGL ES implementation.
        // Causes the wrong texture to be displayed when GL_TEXTURE_2D is enabled before enabling GL_TEXTURE_EXTERNAL_OES.
        if (isMali == true) {
            pGL.glDisable(GL10.GL_TEXTURE_2D)
        }

        pGL.glEnable(GL_TEXTURE_EXTERNAL_OES)

        textureRegion.onApply(pGL)

        onApplyVertices(pGL)
        drawVertices(pGL, pCamera)

        pGL.glDisable(GL_TEXTURE_EXTERNAL_OES)

        if (isMali == true) {
            pGL.glEnable(GL10.GL_TEXTURE_2D)
        }
    }

    override fun finalize()
    {
        release()
        super.finalize()
    }
}


