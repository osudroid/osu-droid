package com.rian.osu.beatmap

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.math.Vector2

sealed class PlayableBeatmapTest {
    protected fun createBasePlayableBeatmap(mode: GameMode) = Beatmap(mode).apply {
        hitObjects.add(HitCircle(1000.0, Vector2(256, 192), true, 0))
        hitObjects.add(HitCircle(2000.0, Vector2(320, 192), true, 0))
        hitObjects.add(HitCircle(3000.0, Vector2(384, 192), true, 0))

        for (obj in hitObjects.objects) {
            obj.applyDefaults(controlPoints, difficulty, mode)
            obj.applySamples(controlPoints)
        }
    }
}