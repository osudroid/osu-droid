package com.reco1l.legacy.graphics.sprite

import android.opengl.GLES20.GL_TRIANGLE_STRIP
import com.reco1l.legacy.graphics.texture.BlankTextureRegion
import org.andengine.engine.camera.Camera
import org.andengine.entity.shape.RectangularShape
import org.andengine.entity.sprite.ISprite
import org.andengine.entity.sprite.Sprite
import org.andengine.entity.sprite.Sprite.SPRITE_SIZE
import org.andengine.entity.sprite.Sprite.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
import org.andengine.entity.sprite.Sprite.VERTICES_PER_SPRITE
import org.andengine.entity.sprite.vbo.HighPerformanceSpriteVertexBufferObject
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.texture.region.ITextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.DrawType.STATIC
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * Sprite that allows to change texture once created.
 */
class MutableSprite(

    x: Float,
    y: Float,

    texture: ITextureRegion = BlankTextureRegion(),

) : RectangularShape(x, y, texture.width, texture.height, PositionColorTextureCoordinatesShaderProgram.getInstance()), ISprite {


    /**
     * Whether the size of the sprite should be adjusted to the size of the texture.
     */
    var adjustSize = true
        set(value)
        {
            if (field != value)
            {
                field = value

                if (value)
                {
                    setSize(textureRegion.width, textureRegion.height)
                    onUpdateTextureCoordinates()
                }
            }
        }

    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false
        set(value)
        {
            if (field != value)
            {
                field = value
                onUpdateTextureCoordinates()
            }
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value)
        {
            if (field != value)
            {
                field = value
                onUpdateTextureCoordinates()
            }
        }


    private var _textureRegion = texture

    private val vbo = HighPerformanceSpriteVertexBufferObject(GlobalManager.getInstance().engine.vertexBufferObjectManager, SPRITE_SIZE, STATIC, true, VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT)


    init
    {
        isBlendingEnabled = true
        initBlendFunction(texture)

        onUpdateColor()
        onUpdateTextureCoordinates()
    }



    override fun getVertexBufferObject() = vbo


    fun setTextureRegion(textureRegion: ITextureRegion)
    {
        if (_textureRegion == textureRegion)
            return

        _textureRegion = textureRegion

        initBlendFunction(textureRegion.texture)

        if (adjustSize)
        {
            setSize(textureRegion.width, textureRegion.height)
            onUpdateTextureCoordinates()
        }
    }

    override fun getTextureRegion() = _textureRegion


    // Extracted from Sprite.java

    override fun isFlippedHorizontal() = flippedHorizontal

    override fun isFlippedVertical() = flippedVertical


    override fun preDraw(gl: GLState, camera: Camera)
    {
        super.preDraw(gl, camera)

        textureRegion.texture.bind(gl)

        vbo.bind(gl, mShaderProgram)
    }

    override fun draw(gl: GLState, camera: Camera) = vbo.draw(GL_TRIANGLE_STRIP, VERTICES_PER_SPRITE)

    override fun postDraw(gl: GLState, camera: Camera)
    {
        vbo.unbind(gl, mShaderProgram)

        super.postDraw(gl, camera)
    }


    protected fun onUpdateTextureCoordinates() = vbo.onUpdateTextureCoordinates(this)

    override fun onUpdateVertices() = vbo.onUpdateVertices(this)

    override fun onUpdateColor() = vbo.onUpdateColor(this)


    override fun reset()
    {
        super.reset()
        initBlendFunction(textureRegion.texture)
    }

}