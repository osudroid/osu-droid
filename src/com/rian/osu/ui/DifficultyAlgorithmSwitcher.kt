package com.rian.osu.ui

import com.reco1l.legacy.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.HorizontalAlign
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

class DifficultyAlgorithmSwitcher : Rectangle(0f, 0f, 125f, 110f) {
    private val gameModeText = ChangeableText(
        10f, height / 2, getResources().getFont("font"),
        "", HorizontalAlign.LEFT, 13
    )

    private val star = Sprite(0f, 0f, getResources().getTextureIfLoaded("star"))

    private var moved = false
    private var initialX: Float? = null
    private var initialY: Float? = null

    init {
        updateState()

        attachChild(gameModeText)
        attachChild(star)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionDown) {
            moved = false
            initialX = localX
            initialY = localY

            updateSelectedColor()
        }

        if (event.isActionOutside || initialX == null || initialY == null ||
            event.isActionMove && MathUtils.distance(initialX!!, initialY!!, localX, localY) > 30) {
            moved = true
            updateDeselectedColor()
        }

        if (moved || !event.isActionUp) {
            return true
        }

        Config.setDifficultyAlgorithm(
            if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) DifficultyAlgorithm.droid
            else DifficultyAlgorithm.standard
        )

        if (Multiplayer.isConnected && Multiplayer.room != null) {
            RoomScene.switchDifficultyAlgorithm()
        } else {
            getGlobal().songMenu.switchDifficultyAlgorithm()
        }

        updateState()

        return true
    }

    private fun updateState() {
        updateDeselectedColor()

        gameModeText.let {
            it.text = if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid) "osu!droid" else "osu!standard"
            it.setPosition(10f, height / 5 - it.height / 2)
            it.scaleCenterX = 0f
            it.setScale(min(1f, (width - 20) / it.width))

            star.setPosition((width - star.width) / 2, it.y + it.height + 10)
        }
    }

    private fun updateSelectedColor() {
        val color = 65 / 255f

        setColor(color, color, color, 0.9f)
    }

    private fun updateDeselectedColor() {
        val color = 100 / 255f

        setColor(color, color, color, 0.75f)
    }
}