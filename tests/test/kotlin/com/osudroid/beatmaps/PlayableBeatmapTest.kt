package com.osudroid.beatmaps

import com.osudroid.GameMode
import com.osudroid.beatmaps.hitobjects.HitCircle
import com.osudroid.math.Vector2

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