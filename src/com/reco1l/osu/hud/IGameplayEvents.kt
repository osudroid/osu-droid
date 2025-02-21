package com.reco1l.osu.hud

import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

interface IGameplayEvents {

    fun onGameplayUpdate(gameScene: GameScene, statistics: StatisticV2, secondsElapsed: Float) {}

    fun onNoteHit(statistics: StatisticV2) {}

    fun onBreakStateChange(isBreak: Boolean) {}

    fun onAccuracyRegister(accuracy: Float) {}

}