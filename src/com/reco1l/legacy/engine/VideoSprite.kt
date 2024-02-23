package com.reco1l.legacy.engine

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import com.reco1l.legacy.graphics.ExternalTextureShaderProgram
import org.andengine.engine.Engine
import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.Sprite
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.GlobalManager

class VideoSprite(source: String, engine: Engine) : Sprite(
    0f,
    0f,
    VideoTexture(source, engine).toRegion(),
    engine.vertexBufferObjectManager,
    ExternalTextureShaderProgram
)
{

    val texture = textureRegion.texture as VideoTexture


    override fun preDraw(pGLState: GLState, pCamera: Camera)
    {
        // Since video dimensions are 0x0 initially due to the surface not being set yet, we have to
        // ensure once the surface is set the texture coordinates are updated.
        textureRegion.set(0f, 0f, texture.width.toFloat(), texture.height.toFloat())
        (textureRegion as TextureRegion).updateUV()

        GLES20.glEnable(GL_TEXTURE_EXTERNAL_OES)
        super.preDraw(pGLState, pCamera)
    }

    override fun postDraw(pGLState: GLState, pCamera: Camera)
    {
        super.postDraw(pGLState, pCamera)
        GLES20.glDisable(GL_TEXTURE_EXTERNAL_OES)
    }


    override fun dispose()
    {
        if (isDisposed)
            texture.release()

        super.dispose()
    }
}