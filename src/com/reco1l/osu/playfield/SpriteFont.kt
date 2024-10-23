package com.reco1l.osu.playfield

import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*


open class SpriteFont(private val texturePrefix: StringSkinData) : LinearContainer() {

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                isTextDirty = true
            }
        }


    /**
     * The characters to display.
     */
    val characters = mutableMapOf<Char, TextureRegion>().also {

        fun addChar(char: Char, textureName: String) {
            it[char] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, textureName)
        }

        for (i in 0..9) {
            addChar('0' + i, i.toString())
        }

        addChar('.', "comma")
        addChar('%', "percent")
        addChar('x', "x")
        addChar('d', "d")
        addChar('p', "p")

    }.toMap()


    private var isTextDirty = true


    /**
     * Called when the text needs to be updated.
     *
     * This method is called automatically when the text is changed on the update thread.
     * You can also call it manually if needed to force an update.
     */
    fun onUpdateText() {
        isTextDirty = false

        if (text.length > childCount) {
            for (i in childCount until text.length) {
                attachChild(ExtendedSprite())
            }
        } else {
            for (i in childCount - 1 downTo text.length) {
                detachChild(getChild(i))
            }
        }

        for (i in text.indices) {

            val char = text[i]
            val sprite = getChild(i) as? ExtendedSprite ?: continue

            sprite.textureRegion = characters[char]
        }

        onMeasureContentSize()
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (isTextDirty) {
            isTextDirty = false
            onUpdateText()
        }

        super.onManagedUpdate(pSecondsElapsed)
    }

}