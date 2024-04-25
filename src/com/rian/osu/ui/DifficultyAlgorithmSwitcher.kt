package com.rian.osu.ui

import com.reco1l.osu.Multiplayer
import com.reco1l.osu.multiplayer.RoomScene
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.helper.AnimSprite
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

class DifficultyAlgorithmSwitcher : AnimSprite(
    0f, 0f, 0f,
    "selection-difficulty-droid",
    "selection-difficulty-droid-over",
    "selection-difficulty-standard",
    "selection-difficulty-standard-over"
) {
    private var moved = false
    private var initialX: Float? = null
    private var initialY: Float? = null

    init {
        onDeselect()
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (event.isActionDown) {
            moved = false
            initialX = localX
            initialY = localY

            onSelect()
        }

        if (event.isActionOutside || initialX == null || initialY == null ||
            event.isActionMove && MathUtils.distance(initialX!!, initialY!!, localX, localY) > 30) {
            moved = true

            onDeselect()
        }

        if (moved || !event.isActionUp) {
            return true
        }

        Config.setDifficultyAlgorithm(
            if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) DifficultyAlgorithm.droid
            else DifficultyAlgorithm.standard
        )

        if (Multiplayer.isConnected) {
            RoomScene.switchDifficultyAlgorithm()
        } else {
            getGlobal().songMenu.switchDifficultyAlgorithm()
        }

        onDeselect()

        return true
    }

    private fun onSelect() {
        frame = if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) 3 else 1
    }

    private fun onDeselect() {
        frame = if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) 2 else 0
    }
}