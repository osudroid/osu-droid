package com.rian.osu.ui

import com.reco1l.andengine.sprite.*
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.multiplayer.RoomScene
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

class DifficultyAlgorithmSwitcher : ExtendedSprite() {
    private var moved = false
    private var initialX: Float? = null
    private var initialY: Float? = null

    private val textures = arrayOf(
        getResources().getTextureIfLoaded("selection-difficulty-droid"),
        getResources().getTextureIfLoaded("selection-difficulty-droid-over"),
        getResources().getTextureIfLoaded("selection-difficulty-standard"),
        getResources().getTextureIfLoaded("selection-difficulty-standard-over")
    )

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
            if (!moved) {
                getResources().getSound("click-short")?.play()
            }

            moved = true

            onDeselect()
        }

        if (moved || !event.isActionUp) {
            return true
        }

        getResources().getSound("click-short-confirm")?.play()

        Config.setDifficultyAlgorithm(
            if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) DifficultyAlgorithm.droid
            else DifficultyAlgorithm.standard
        )

        if (Multiplayer.isConnected) {
            RoomScene.switchDifficultyAlgorithm()
        } else {
            getGlobal().songMenu.reloadCurrentSelection()
        }

        onDeselect()

        return true
    }

    private fun onSelect() {
        textureRegion = textures[if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) 3 else 1]
    }

    private fun onDeselect() {
        textureRegion = textures[if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.standard) 2 else 0]
    }
}