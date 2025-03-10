package com.reco1l.osu.hud.elements

import com.reco1l.andengine.text.ExtendedText
import com.reco1l.osu.hud.HUDElement
import com.reco1l.osu.playfield.SpriteFont
import com.rian.osu.beatmap.hitobject.HitObject
import kotlin.collections.ArrayDeque
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

class HUDNotesPerSecondCounter : HUDElement() {

    override val name = "Notes per second counter"

    private val label = ExtendedText().apply {
        font = ResourceManager.getInstance().getFont("smallFont")
        text = "Notes/sec"
    }

    private val value = SpriteFont(OsuSkin.get().scorePrefix).apply {
        text = "0"
    }

    private val startTimes = ArrayDeque<Double>()

    init {
        attachChild(label)
        attachChild(value)
    }

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        startTimes.add(obj.startTime)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        val elapsedTimeMs = getGlobal().gameScene.elapsedTime * 1000

        while (startTimes.isNotEmpty() && startTimes.first() + 1000 < elapsedTimeMs) {
            startTimes.removeFirst()
        }

        value.text = startTimes.size.toString()
        value.y = label.drawHeight

        super.onManagedUpdate(pSecondsElapsed)
    }
}