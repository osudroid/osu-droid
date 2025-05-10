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

        // We cannot verify old gameplay stack height here as it is dependent on screen space gameplay scale, which is
        // not available without emulation. Additionally, the result will be device-dependent (since it depends on the
        // screen height of the device). It goes to show just how broken it was, huh.
        Assert.assertEquals(0, objects[0].difficultyStackHeight)
        Assert.assertEquals(1, objects[1].difficultyStackHeight)
    }
}