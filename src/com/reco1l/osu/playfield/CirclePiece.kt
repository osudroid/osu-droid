package com.reco1l.osu.playfield

import com.reco1l.osu.graphics.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.CircleNumber

open class CirclePiece(

    circleTexture: String,
    overlayTexture: String

) : ExtendedEntity() {


    private val circle = ExtendedSprite().also {

        it.originX = 0.5f
        it.originY = 0.5f
        it.textureRegion = ResourceManager.getInstance().getTexture(circleTexture)

        attachChild(it)
    }

    private val overlay = ExtendedSprite().also {

        it.originX = 0.5f
        it.originY = 0.5f
        it.textureRegion = ResourceManager.getInstance().getTexture(overlayTexture)

        attachChild(it)
    }


    fun setCircleColor(red: Float, green: Float, blue: Float) {
        circle.setColor(red, green, blue)
    }
}

class NumberedCirclePiece(circleTexture: String, overlayTexture: String) : CirclePiece(circleTexture, overlayTexture) {


    private val number = CircleNumber().also {

        it.originX = 0.5f
        it.originY = 0.5f
        attachChild(it)
    }


    fun setNumberText(value: Int) {
        number.setNumber(value)
    }

    fun setNumberScale(value: Float) {
        number.setScale(value)
    }

}