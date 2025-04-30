package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test

class ModReplayV6Test {
    @Test
    fun `Test old object stacking`() {
        val beatmap = Beatmap(GameMode.Droid).apply {
            hitObjects.add(HitCircle(0.0, Vector2(0), true, 0))
            hitObjects.add(HitCircle(75.0, Vector2(0), true, 0))
        }

        val playableBeatmap = beatmap.createDroidPlayableBeatmap(listOf(ModReplayV6()))
        val objects = playableBeatmap.hitObjects.objects

        Assert.assertEquals(4f, objects[0].stackOffsetMultiplier, 1e-5f)
        Assert.assertEquals(0, objects[0].difficultyStackHeight)
        Assert.assertEquals(0, objects[0].gameplayStackHeight)

        Assert.assertEquals(4f, objects[1].stackOffsetMultiplier, 1e-5f)
        Assert.assertEquals(1, objects[1].difficultyStackHeight)
        Assert.assertEquals(1, objects[1].gameplayStackHeight)
    }
}