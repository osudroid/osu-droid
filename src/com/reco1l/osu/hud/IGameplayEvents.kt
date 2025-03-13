package com.reco1l.osu.hud

import android.view.MotionEvent
import com.rian.osu.beatmap.hitobject.HitObject
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

interface IGameplayEvents {

    fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float)

    /**
     * Called when a [MotionEvent.ACTION_DOWN] event occurs in gameplay.
     *
     * @param time The time in seconds when the event occurred with respect to gameplay time.
     */
    fun onGameplayTouchDown(time: Float)

    fun onHitObjectLifetimeStart(obj: HitObject)

    fun onNoteHit(statistics: StatisticV2)

    fun onBreakStateChange(isBreak: Boolean)

    fun onAccuracyRegister(accuracy: Float)

}