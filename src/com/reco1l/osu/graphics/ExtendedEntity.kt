package com.reco1l.osu.graphics

import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.entity.shape.*
import org.anddev.andengine.opengl.util.*
import org.anddev.andengine.opengl.vertex.*
import javax.microedition.khronos.opengles.*


/**
 * Entity with extended features.
 *
 * @author Reco1l
 */
open class ExtendedEntity @JvmOverloads constructor(

    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f,
    vertexBuffer: VertexBuffer = RectangleVertexBuffer(GL11.GL_STATIC_DRAW, true)

) : RectangularShape(x, y, width, height, vertexBuffer) {

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


    @JvmOverloads
    fun setOrigin(x: Float, y: Float = x) {
        originX = x
        originY = y
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


    override fun onUpdateVertexBuffer() = Unit

}