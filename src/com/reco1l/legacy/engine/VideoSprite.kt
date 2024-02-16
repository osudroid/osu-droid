package com.reco1l.legacy.engine

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.Sprite
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.GlobalManager

class VideoSprite(source: String, private val engine: Engine) : Sprite(
    0f,
    0f,
    VideoTexture(source).toRegion(),
    GlobalManager.getInstance().engine.vertexBufferObjectManager
)
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

    override fun draw(pGLState: GLState, pCamera: Camera?)
    {
        GLES20.glEnable(GL_TEXTURE_EXTERNAL_OES)

        // TODO: Fix video texture.
        textureRegion.texture.bind(pGLState)

        GLES20.glDisable(GL_TEXTURE_EXTERNAL_OES)
    }

    override fun finalize()
    {
        release()
        super.finalize()
    }
}