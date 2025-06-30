package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModTraceableTest {
    @Test
    fun `Test first adjustable object`() {
        val beatmap = Beatmap(GameMode.Standard).apply {
            hitObjects.add(Spinner(0.0, 500.0, true))
            hitObjects.add(HitCircle(1000.0, Vector2(0), true, 0))
        }

        val traceable = ModTraceable().apply { applyToBeatmap(beatmap) }

        Assert.assertTrue(traceable.firstObject is HitCircle)
    }
}