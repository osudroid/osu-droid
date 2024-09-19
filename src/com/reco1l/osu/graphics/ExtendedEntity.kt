package com.reco1l.osu.graphics

import org.anddev.andengine.collision.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.primitive.*
import org.anddev.andengine.entity.shape.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*


/**
 * Entity with extended features.
 *
 * @author Reco1l
 */
open class ExtendedEntity(

    private var width: Float = 0f,

    private var height: Float = 0f,

    private val vertexBuffer: VertexBuffer? = null

) : Shape(0f, 0f) {

    /**
     * The origin factor of the entity in the X axis.
     */
    var originX = 0f

    /**
     * The origin factor of the entity in the Y axis.
     */
    var originY = 0f

    /**
     * The translation in the X axis.
     */
    var translationX = 0f

    /**
     * The translation in the Y axis.
     */
    var translationY = 0f

    /**
     * Whether the color should be inherited from all the parents in the hierarchy.
     */
    var inheritColor = true


    init {
        mRotationCenterX = 0.5f
        mRotationCenterY = 0.5f
        mScaleCenterX = 0.5f
        mScaleCenterY = 0.5f
    }


    fun setOrigin(origin: Origin) {
        originX = origin.factorX
        originY = origin.factorY
    }


    override fun applyTranslation(pGL: GL10) {

        if (x == 0f && y == 0f && originX == 0f && originY == 0f && translationX == 0f && translationY == 0f) {
            return
        }

        val offsetX = width * originX
        val offsetY = height * originY

        pGL.glTranslatef(x - offsetX + translationX, y - offsetY + translationY, 0f)
    }

    override fun applyRotation(pGL: GL10) {

        if (rotation == 0f) {
            return
        }

        val offsetX = width * rotationCenterX
        val offsetY = height * rotationCenterY

        pGL.glTranslatef(offsetX, offsetY, 0f)
        pGL.glRotatef(rotation, 0f, 0f, 1f)
        pGL.glTranslatef(-offsetX, -offsetY, 0f)
    }

    override fun applyScale(pGL: GL10) {

        if (scaleX == 1f && scaleY == 1f) {
            return
        }

        val offsetX = width * scaleCenterX
        val offsetY = height * scaleCenterY

        pGL.glTranslatef(offsetX, offsetY, 0f)
        pGL.glScalef(scaleX, scaleY, 1f)
        pGL.glTranslatef(-offsetX, -offsetY, 0f)
    }

    private fun applyColor(pGL: GL10) {

        var red = mRed
        var green = mGreen
        var blue = mBlue
        var alpha = mAlpha

        if (inheritColor) {

            var parent = parent
            while (parent != null) {

                red *= parent.red
                green *= parent.green
                blue *= parent.blue
                alpha *= parent.alpha

                parent = parent.parent
            }
        }

        GLHelper.setColor(pGL, red, green, blue, alpha)
    }


    override fun onInitDraw(pGL: GL10) {

        applyColor(pGL)

        GLHelper.enableVertexArray(pGL)
        GLHelper.blendFunction(pGL, mSourceBlendFunction, mDestinationBlendFunction)
    }

    override fun drawVertices(pGL: GL10, pCamera: Camera) {
        if (vertexBuffer != null) {
            pGL.glDrawArrays(GL10.GL_TRIANGLE_STRIP, 0, 4)
        }
    }


    override fun onUpdateVertexBuffer() {}

    override fun onApplyVertices(pGL: GL10) {
        if (vertexBuffer != null) {
            super.onApplyVertices(pGL)
        }
    }

    override fun getVertexBuffer(): VertexBuffer? {
        return vertexBuffer
    }


    fun setSize(width: Float, height: Float) {
        if (this.width != width || this.height != height) {
            this.width = width
            this.height = height
            onUpdateVertexBuffer()
        }
    }

    fun setWidth(value: Float) {
        if (width != value) {
            width = value
            onUpdateVertexBuffer()
        }
    }

    override fun getWidth(): Float {
        return width
    }

    fun setHeight(value: Float) {
        if (height != value) {
            height = value
            onUpdateVertexBuffer()
        }
    }

    override fun getHeight(): Float {
        return height
    }


    // Base width and height are not needed for this class.

    override fun getBaseWidth(): Float {
        return width
    }

    override fun getBaseHeight(): Float {
        return height
    }


    override fun collidesWith(pOtherShape: IShape?): Boolean {
        return when (pOtherShape) {
            is RectangularShape -> RectangularShapeCollisionChecker.checkCollision(this, pOtherShape)
            is Line -> RectangularShapeCollisionChecker.checkCollision(this, pOtherShape)
            else -> false
        }
    }

    override fun contains(pX: Float, pY: Float): Boolean {
        return RectangularShapeCollisionChecker.checkContains(this, pX, pY)
    }

    override fun isCulled(pCamera: Camera): Boolean {
        return mX > pCamera.maxX
            || mY > pCamera.maxY
            || mX + this.width < pCamera.minX
            || mY + this.height < pCamera.minY
    }

}


/**
 * Determines the origin of the sprite.
 */
enum class Origin(val factorX: Float, val factorY: Float) {

    TopLeft(0f, 0f),

    TopCenter(0.5f, 0f),

    TopRight(1f, 0f),

    CenterLeft(0f, 0.5f),

    Center(0.5f, 0.5f),

    CenterRight(1f, 0.5f),

    BottomLeft(0f, 1f),

    BottomCenter(0.5f, 1f),

    BottomRight(1f, 1f)

}