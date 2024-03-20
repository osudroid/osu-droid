package com.reco1l.legacy.graphics.entity

import org.andengine.engine.camera.Camera
import org.andengine.entity.shape.RectangularShape
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.IVertexBufferObject
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.util.Constants.*
import kotlin.math.max
import kotlin.math.min


/**
 * Entity with extended features.
 *
 * * Supports clipping for both axes X and Y.
 * * Supports origin offset for both axes X and Y.
 * * Scale, rotation and skew centers are now determined by size factors rather than absolute values.
 *
 * @author Reco1l
 */
open class ExtendedEntity @JvmOverloads constructor(

    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    shader: ShaderProgram? = null

): RectangularShape(x, y, width, height, shader)
{

    /**
     * The origin factor of the entity in the X axis.
     */
    var originX = 0f

    /**
     * The origin factor of the entity in the Y axis.
     */
    var originY = 0f

    /**
     * The percent of clipping on the X axis.
     */
    var clipX = 0f

    /**
     * The percent of clipping on the Y axis.
     */
    var clipY = 0f


    init
    {
        mRotationCenterX = 0.5f
        mRotationCenterY = 0.5f
        mScaleCenterX = 0.5f
        mScaleCenterY = 0.5f
        mSkewCenterX = 0.5f
        mSkewCenterY = 0.5f
    }



    @JvmOverloads
    fun setClip(x: Float, y: Float = x)
    {
        clipX = x
        clipY = y
    }

    @JvmOverloads
    fun setOrigin(x: Float, y: Float = x)
    {
        originX = x
        originY = y
    }


    override fun resetSkewCenter() = Unit

    override fun resetScaleCenter() = Unit

    override fun resetRotationCenter() = Unit



    override fun onUpdateVertices() = Unit

    override fun getVertexBufferObject(): IVertexBufferObject? = null

    override fun getVertexBufferObjectManager() = VertexBufferObjectManager.GLOBAL!!



    // Transformations

    override fun applyTranslation(gl: GLState)
    {
        if (x == 0f && y == 0f && originX == 0f && originY == 0f)
            return

        val offsetX = width * originX
        val offsetY = height * originY

        gl.translateModelViewGLMatrixf(x - offsetX, y - offsetY, 0f)
    }

    override fun applyRotation(gl: GLState)
    {
        if (rotation == 0f)
            return

        val offsetX = width * rotationCenterX
        val offsetY = height * rotationCenterY

        gl.translateModelViewGLMatrixf(offsetX, offsetY, 0f)
        gl.rotateModelViewGLMatrixf(rotation, 0f, 0f, 1f)
        gl.translateModelViewGLMatrixf(-offsetX, -offsetY, 0f)
    }

    override fun applyScale(gl: GLState)
    {
        if (scaleX == 1f && scaleY == 1f)
            return

        val offsetX = width * scaleCenterX
        val offsetY = height * scaleCenterY

        gl.translateModelViewGLMatrixf(offsetX, offsetY, 0f)
        gl.scaleModelViewGLMatrixf(scaleX, scaleY, 1)
        gl.translateModelViewGLMatrixf(-offsetX, -offsetY, 0f)
    }

    override fun applySkew(gl: GLState)
    {
        if (skewX == 0f && skewY == 0f)
            return

        val offsetX = width * skewCenterX
        val offsetY = height * skewCenterY

        gl.translateModelViewGLMatrixf(offsetX, offsetY, 0f)
        gl.skewModelViewGLMatrixf(skewX, skewY)
        gl.translateModelViewGLMatrixf(-offsetX, -offsetY, 0f)
    }



    // Draw

    override fun onManagedDraw(gl: GLState, camera: Camera) {

        if (clipX == 0f && clipY == 0f) {
            super.onManagedDraw(gl, camera)
            return
        }

        val wasScissorTestEnabled = gl.enableScissorTest()


        // In order to apply clipping, we need to determine the axis aligned bounds in OpenGL coordinates.
        // Determine clipping coordinates of each corner in surface coordinates.
        val lowerLeftSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(0f, 0f))
        val lowerLeftX = lowerLeftSurfaceCoordinates[VERTEX_INDEX_X].toInt()
        val lowerLeftY = camera.surfaceHeight - lowerLeftSurfaceCoordinates[VERTEX_INDEX_Y].toInt()

        val upperLeftSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(0f, height * (1 - clipY)))
        val upperLeftX = upperLeftSurfaceCoordinates[VERTEX_INDEX_X].toInt()
        val upperLeftY = camera.surfaceHeight - upperLeftSurfaceCoordinates[VERTEX_INDEX_Y].toInt()

        val upperRightSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(width * (1 - clipX), height * (1 - clipY)))
        val upperRightX = upperRightSurfaceCoordinates[VERTEX_INDEX_X].toInt()
        val upperRightY = camera.surfaceHeight - upperRightSurfaceCoordinates[VERTEX_INDEX_Y].toInt()

        val lowerRightSurfaceCoordinates = camera.getSurfaceCoordinatesFromSceneCoordinates(convertLocalToSceneCoordinates(width * (1 - clipX), 0f))
        val lowerRightX = lowerRightSurfaceCoordinates[VERTEX_INDEX_X].toInt()
        val lowerRightY = camera.surfaceHeight - lowerRightSurfaceCoordinates[VERTEX_INDEX_Y].toInt()

        // Determine minimum and maximum x clipping coordinates.
        val minClippingX = min(lowerLeftX, min(upperLeftX, min(upperRightX, lowerRightX)))
        val maxClippingX = max(lowerLeftX, max(upperLeftX, max(upperRightX, lowerRightX)))

        // Determine minimum and maximum y clipping coordinates.
        val minClippingY = min(lowerLeftY, min(upperLeftY, min(upperRightY, lowerRightY)))
        val maxClippingY = max(lowerLeftY, max(upperLeftY, max(upperRightY, lowerRightY)))

        // Determine clipping width and height.
        val clippingWidth = maxClippingX - minClippingX
        val clippingHeight = maxClippingY - minClippingY


        gl.glPushScissor(minClippingX, minClippingY, clippingWidth, clippingHeight)
        super.onManagedDraw(gl, camera)
        gl.glPopScissor()


        gl.setScissorTestEnabled(wasScissorTestEnabled)
    }
}