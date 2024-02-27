package com.reco1l.legacy.graphics

import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.Sprite
import org.andengine.opengl.texture.region.ITextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.VertexBufferObjectManager

/**
 * Basic implementation of sprite using scissor features.
 */
class ClipSprite(

    pX: Float,
    pY: Float,
    pTextureRegion: ITextureRegion,
    pSpriteVertexBufferObjectManager: VertexBufferObjectManager?

) : Sprite(pX, pY, pTextureRegion, pSpriteVertexBufferObjectManager)
{

    /**
     * The percent of clipping on the X axis.
     */
    var clipX = 0f

    /**
     * The percent of clipping on the Y axis.
     */
    var clipY = 0f


    override fun onManagedDraw(pGLState: GLState, pCamera: Camera)
    {
        if (clipX != 0f || clipY != 0f)
        {
            pGLState.enableScissorTest()

            val clipWidth = width * (1 - clipX)
            val clipHeight = height * (1 - clipY)

            pGLState.glPushScissor(0, 0, clipWidth.toInt(), clipHeight.toInt())
            super.onManagedDraw(pGLState, pCamera)
            pGLState.glPopScissor()

            pGLState.disableScissorTest()
        }
        else super.onManagedDraw(pGLState, pCamera)
    }
}