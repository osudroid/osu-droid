package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModHiddenTest {
    @Test
    fun `Test object fade in adjustment`() {
        val beatmap = Beatmap(GameMode.Standard).apply {
            difficulty.ar = 5f
        }

        val hitCircle = HitCircle(0.0, Vector2(0), true, 0).apply {
            applyDefaults(beatmap.controlPoints, beatmap.difficulty, beatmap.mode)
        }

        beatmap.hitObjects.add(hitCircle)
        Assert.assertEquals(beatmap.hitObjects.objects.size, 1)

        ModHidden().applyToBeatmap(beatmap)

        Assert.assertEquals(hitCircle.timeFadeIn, 480.0, 1e-5)
    }
}