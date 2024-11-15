package com.reco1l.osu.playfield

import com.reco1l.andengine.text.*
import org.anddev.andengine.opengl.texture.region.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*


open class SpriteFont(private val texturePrefix: StringSkinData) : TextureFont(mutableMapOf<Char, TextureRegion>().also {

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

})