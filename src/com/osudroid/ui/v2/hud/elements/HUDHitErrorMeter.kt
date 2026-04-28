package com.osudroid.ui.v2.hud.elements

import com.reco1l.framework.Color4
import com.osudroid.ui.v2.hud.HUDElement
import com.rian.osu.beatmap.HitWindow
import ru.nsu.ccfit.zuev.osu.GlobalManager
import kotlin.math.abs

sealed class HUDHitErrorMeter : HUDElement() {
    protected val hitWindow: HitWindow = GlobalManager.getInstance().gameScene.hitWindow

    protected val greatColor = Color4(70, 180, 220)
    protected val okColor = Color4(100, 220, 40)
    protected val mehColor = Color4(200, 180, 110)

    final override fun onAccuracyRegister(accuracy: Float) {
        val absAccuracy = abs(accuracy * 1000)

        val color = when {
            absAccuracy <= hitWindow.greatWindow -> greatColor
            absAccuracy <= hitWindow.okWindow -> okColor
            absAccuracy <= hitWindow.mehWindow -> mehColor
            else -> return
        }

        addResult(accuracy, color)
    }

    protected abstract fun addResult(accuracy: Float, color: Color4)
}