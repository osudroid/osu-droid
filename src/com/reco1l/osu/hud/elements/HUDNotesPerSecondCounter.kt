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

    private val objects = ArrayDeque<HitObject>()

    init {
        attachChild(label)
        attachChild(value)
    }

    override fun onHitObjectLifetimeStart(obj: HitObject) {
        objects.add(obj)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        val elapsedTimeMs = getGlobal().gameScene.elapsedTime * 1000

        while (objects.isNotEmpty() && objects.first().startTime + 1000 < elapsedTimeMs) {
            objects.removeFirst()
        }

        value.text = objects.size.toString()
        value.y = label.drawHeight

        super.onManagedUpdate(pSecondsElapsed)
    }
}