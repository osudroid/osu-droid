package com.reco1l.legacy.graphics.sprite

import android.opengl.GLES20
import com.reco1l.legacy.graphics.entity.ExtendedEntity
import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.ISprite
import org.andengine.entity.sprite.Sprite
import org.andengine.entity.sprite.Sprite.SPRITE_SIZE
import org.andengine.entity.sprite.Sprite.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
import org.andengine.entity.sprite.vbo.HighPerformanceSpriteVertexBufferObject
import org.andengine.opengl.shader.PositionColorTextureCoordinatesShaderProgram
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.region.ITextureRegion
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.DrawType.STATIC
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.andengine.opengl.vbo.VertexBufferObjectManager.GLOBAL

/**
 * Sprite that allows to change texture once created.
 */
open class ExtendedSprite @JvmOverloads constructor(

    x: Float = 0f,
    y: Float = 0f,
    width: Float = 0f,
    height: Float = 0f

) :
    ExtendedEntity(
        x,
        y,
        width,
        height,
        PositionColorTextureCoordinatesShaderProgram.getInstance()
    ),
    ISprite
{

    /**
     * Whether the size of the sprite should be adjusted to the size of the texture.
     */
    var adjustSize = true
        set(value)
        {
            field = value

            if (value)
            {
                setSize(textureRegion?.width ?: 0f, textureRegion?.height ?: 0f)
                onUpdateTextureCoordinates()
            }
        }

    /**
     * Whether the texture should be flipped horizontally.
     */
    var flippedHorizontal = false
        set(value)
        {
            field = value
            onUpdateTextureCoordinates()
        }

    /**
     * Whether the texture should be flipped vertically.
     */
    var flippedVertical = false
        set(value)
        {
            field = value
            onUpdateTextureCoordinates()
        }


    private var _textureRegion: ITextureRegion? = null

    private val vbo = HighPerformanceSpriteVertexBufferObject(GLOBAL, SPRITE_SIZE, STATIC, true, VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT)



    init
    {
        isBlendingEnabled = true

        onUpdateColor()
        onUpdateTextureCoordinates()
    }



    // Texture

    /**
     * Set the current texture region from a [ITexture] object, this may reset blending in order to
     * adapt to the new texture.
     *
     * A new [TextureRegion] will be created by using this.
     */
    fun setTexture(texture: ITexture)
    {
        val region = TextureRegion(texture, 0f, 0f, texture.width.toFloat(), texture.height.toFloat())
        setTextureRegion(region)
    }

    /**
     * Set the current texture region, this may reset blending in order to adapt to the new texture.
     */
    fun setTextureRegion(textureRegion: ITextureRegion?)
    {
        if (_textureRegion == textureRegion)
            return

        _textureRegion = textureRegion

        if (textureRegion != null)
            initBlendFunction(textureRegion.texture)

        if (adjustSize)
        {
            setSize(textureRegion?.width ?: 0f, textureRegion?.height ?: 0f)
            onUpdateTextureCoordinates()
        }
    }


    // Buffer update

    override fun reset()
    {
        super.reset()

        if (textureRegion != null)
            initBlendFunction(textureRegion!!.texture)
    }

    private fun onUpdateTextureCoordinates()
    {
        if (textureRegion != null)
            vbo.onUpdateTextureCoordinates(this)
    }

    override fun onUpdateColor() = vbo.onUpdateColor(this)

    override fun onUpdateVertices() = vbo.onUpdateVertices(this)


    // Draw

    override fun preDraw(gl: GLState, camera: Camera)
    {
        super.preDraw(gl, camera)
        textureRegion?.texture?.bind(gl)
        vbo.bind(gl, shaderProgram)
    }

    override fun draw(gl: GLState, camera: Camera)
    {
        vbo.draw(GLES20.GL_TRIANGLE_STRIP, Sprite.VERTICES_PER_SPRITE)
    }

    override fun postDraw(gl: GLState, camera: Camera)
    {
        vbo.unbind(gl, shaderProgram)
        super.postDraw(gl, camera)
    }


    // Inherited

    override fun getVertexBufferObject() = vbo

    override fun isFlippedHorizontal() = flippedHorizontal

    override fun isFlippedVertical() = flippedVertical

    override fun getTextureRegion() = _textureRegion

}