package com.reco1l.legacy.graphics.sprite

import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.Sprite
import org.andengine.opengl.texture.region.ITextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.util.Constants

/**
 * Basic implementation of sprite using scissor features.
 */
class ClipSprite(

    pX: Float,
    pY: Float,
    pTextureRegion: ITextureRegion,
    pSpriteVertexBufferObjectManager: VertexBufferObjectManager

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


    // Credits to:
    // https://github.com/nicolasgramlich/AndEngine/blob/GLES2-AnchorCenter/src/org/andengine/entity/clip/ClipEntity.java
    override fun onManagedDraw(gl: GLState, camera: Camera) {

        /* Enable scissor test, while remembering previous state. */
        val wasScissorTestEnabled = gl.enableScissorTest()
        val surfaceHeight = camera.surfaceHeight

        /* In order to apply clipping, we need to determine the the axis aligned bounds in OpenGL coordinates. */

        /* Determine clipping coordinates of each corner in surface coordinates. */
        val lowerLeftSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(0f, 0f))
        val lowerLeftX = Math.round(lowerLeftSurfaceCoordinates[Constants.VERTEX_INDEX_X])
        val lowerLeftY = surfaceHeight - Math.round(lowerLeftSurfaceCoordinates[Constants.VERTEX_INDEX_Y])

        val upperLeftSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(0f, mHeight * (1 - clipY)))
        val upperLeftX = Math.round(upperLeftSurfaceCoordinates[Constants.VERTEX_INDEX_X])
        val upperLeftY = surfaceHeight - Math.round(upperLeftSurfaceCoordinates[Constants.VERTEX_INDEX_Y])

        val upperRightSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(mWidth  * (1 - clipX), mHeight * (1 - clipY)))
        val upperRightX = Math.round(upperRightSurfaceCoordinates[Constants.VERTEX_INDEX_X])
        val upperRightY = surfaceHeight - Math.round(upperRightSurfaceCoordinates[Constants.VERTEX_INDEX_Y])

        val lowerRightSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(mWidth * (1 - clipX), 0f))
        val lowerRightX = Math.round(lowerRightSurfaceCoordinates[Constants.VERTEX_INDEX_X])
        val lowerRightY = surfaceHeight - Math.round(lowerRightSurfaceCoordinates[Constants.VERTEX_INDEX_Y])

        /* Determine minimum and maximum x clipping coordinates. */
        val minClippingX = minOf(lowerLeftX, upperLeftX, upperRightX, lowerRightX)
        val maxClippingX = maxOf(lowerLeftX, upperLeftX, upperRightX, lowerRightX)

        /* Determine minimum and maximum y clipping coordinates. */
        val minClippingY = minOf(lowerLeftY, upperLeftY, upperRightY, lowerRightY)
        val maxClippingY = maxOf(lowerLeftY, upperLeftY, upperRightY, lowerRightY)

        /* Determine clipping width and height. */
        val clippingWidth = maxClippingX - minClippingX
        val clippingHeight = maxClippingY - minClippingY

        /* Finally apply the clipping. */
        gl.glPushScissor(
            minClippingX,
            minClippingY,
            clippingWidth,
            clippingHeight
        )

        /* Draw children, etc... */
        super.onManagedDraw(gl, camera)

        /* Revert scissor test to previous state. */
        gl.glPopScissor()
        gl.setScissorTestEnabled(wasScissorTestEnabled)
    }
}