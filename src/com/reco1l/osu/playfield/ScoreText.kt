package com.reco1l.osu.playfield

import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*


class ScoreText(private val texturePrefix: StringSkinData) : LinearContainer() {

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                isTextInvalid = true
            }
        }


    /**
     * The characters to display.
     */
    val characters: Map<Char, TextureRegion> = mutableMapOf<Char, TextureRegion>().also {

        for (i in 0..9) {
            it['0' + i] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, i.toString())
        }

        it['.'] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, "comma")
        it['%'] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, "percent")
        it['x'] = ResourceManager.getInstance().getTextureWithPrefix(texturePrefix, "x")

    }.toMap()


    private var isTextInvalid = true


    private fun allocateSprites(count: Int) {
        if (count > childCount) {
            for (i in childCount until count) {
                attachChild(ExtendedSprite())
            }
        } else {
            for (i in childCount - 1 downTo count) {
                detachChild(getChild(i))
            }
        }
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {

        if (isTextInvalid) {
            isTextInvalid = false

            allocateSprites(text.length)

            for (i in text.indices) {

                val char = text[i]
                val sprite = getChild(i) as? ExtendedSprite ?: continue

                sprite.textureRegion = characters[char]
            }
        }

        super.onManagedUpdate(pSecondsElapsed)
    }

}