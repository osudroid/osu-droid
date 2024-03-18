package com.reco1l.legacy.graphics.sprite

import org.andengine.entity.Entity
import org.andengine.opengl.util.GLState

/**
 * Entity with extended features.
 */
abstract class ExtendedEntity(x: Float, y: Float): Entity(x, y)
{

    /**
     * The origin factor of the entity in the X axis.
     *
     * When applying X translation it will be calculated as `x - (width * originX)`.
     */
    var originX = 0f
        set(value)
        {
            field = value.coerceIn(0f, 1f)
        }

    /**
     * The origin factor of the entity in the Y axis.
     *
     * When applying Y translation it will be calculated as `y - (height * originY)`.
     */
    var originY = 0f
        set(value)
        {
            field = value.coerceIn(0f, 1f)
        }

    /**
     * Internal reference for the width of the entity.
     */
    protected abstract var _width: Float

    /**
     * Internal reference for the height of the entity.
     */
    protected abstract var _height: Float


    override fun applyTranslation(pGLState: GLState)
    {
        pGLState.translateModelViewGLMatrixf(
                mX - (_width * scaleX * originX),
                mY - (_height * scaleY * originY),
                0f
        )
    }


    fun getWidth() = _width

    fun getHeight() = _height
}