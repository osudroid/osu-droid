package com.reco1l.legacy.graphics.sprite

import com.reco1l.legacy.graphics.entity.ExtendedEntity
import com.reco1l.legacy.graphics.texture.BlankTextureRegion
import org.andengine.opengl.texture.region.ITextureRegion
import org.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.StringSkinData
import kotlin.math.max

/**
 * A type of text that renders with sprites.
 */
class SpriteText(

    x: Float,
    y: Float,

    private val textureProvider: (Char) -> TextureRegion?

) : ExtendedEntity(x, y)
{

    private val characterMap = mutableMapOf<Char, ITextureRegion>()


    /**
     * Set the text to be rendered.
     *
     * @see setText(String, Boolean)
     */
    var text: String = ""
        set(value)
        {
            if (field != value)
            {
                field = value
                shouldInvalidate = true
            }
        }

    /**
     * Set the character scale.
     */
    var characterScale: Float = 1f
        set(value)
        {
            if (field != value)
            {
                field = value
                shouldInvalidate = true
            }
        }


    private var shouldInvalidate = true


    override fun onManagedUpdate(pSecondsElapsed: Float)
    {
        if (shouldInvalidate)
            invalidate()

        super.onManagedUpdate(pSecondsElapsed)
    }


    /**
     * Set the text to be rendered.
     *
     * @param invalidateNow Whether to invalidate the text without waiting to update thread, if `true`
     * make sure to call this method inside update thread.
     */
    fun setText(text: String, invalidateNow: Boolean)
    {
        this.text = text

        if (invalidateNow)
            invalidate()
    }


    private fun invalidate()
    {
        val text = text
        shouldInvalidate = false

        if (text.length != childCount) when
        {
            text.length > childCount ->
            {
                for (i in childCount until text.length)
                    attachChild(ExtendedSprite(0f, 0f))
            }

            text.length < childCount ->
            {
                for (i in childCount - 1 downTo text.length)
                    getChildByIndex(i).detachSelf()
            }
        }

        mWidth = 0f
        mHeight = 0f

        for (i in text.indices)
        {
            val character = text[i]

            val sprite = getChildByIndex(i) as ExtendedSprite

            sprite.textureRegion = characterMap.getOrPut(character) { textureProvider(character) ?: BlankTextureRegion() }
            sprite.x = mWidth
            sprite.color = color
            sprite.setScale(characterScale)

            mWidth += sprite.widthScaled
            mHeight = max(mHeight, sprite.heightScaled)
        }
    }


    override fun onUpdateColor()
    {
        shouldInvalidate = true
    }


    companion object
    {

        /**
         * Creates a texture provider specially for osu! textures with a determined prefix where
         * special characters such as ´.´ or ´%´ are replaced by its full name.
         */
        @JvmStatic
        fun createOsuTextureProvider(prefix: StringSkinData) = { character: Char ->

            ResourceManager.getInstance().getTextureWithPrefix(prefix, when (character)
            {
                '.' -> "comma"
                '%' -> "percent"
                else -> character.toString()
            })
        }

    }
}