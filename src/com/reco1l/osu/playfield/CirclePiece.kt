package com.reco1l.osu.playfield

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.skins.*

open class CirclePiece(

    circleTexture: String,
    overlayTexture: String

) : Container() {


    override var originX = 0.5f

    override var originY = 0.5f


    private val circle = ExtendedSprite().also {

        it.setOrigin(Anchor.Center)
        it.setAnchor(Anchor.Center)
        it.textureRegion = ResourceManager.getInstance().getTexture(circleTexture)

        attachChild(it)
    }

    private val overlay = ExtendedSprite().also {

        it.setOrigin(Anchor.Center)
        it.setAnchor(Anchor.Center)
        it.textureRegion = ResourceManager.getInstance().getTexture(overlayTexture)

        attachChild(it)
    }


    fun setCircleColor(red: Float, green: Float, blue: Float) {
        circle.setColor(red, green, blue)
    }
}

class NumberedCirclePiece(circleTexture: String, overlayTexture: String) : CirclePiece(circleTexture, overlayTexture) {


    private val number = SpriteFont(OsuSkin.get().hitCirclePrefix).also {

        it.setOrigin(Anchor.Center)
        it.setAnchor(Anchor.Center)
        it.spacing = -OsuSkin.get().hitCircleOverlap

        attachChild(it)
    }


    fun setNumberText(value: Int) {
        number.text = value.toString()
    }

    fun setNumberScale(value: Float) {
        number.setScale(value)
    }

}