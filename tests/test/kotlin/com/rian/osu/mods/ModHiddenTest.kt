package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Spinner
import com.rian.osu.math.Vector2
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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

    @Test
    fun `Test first adjustable object`() {
        val beatmap = Beatmap(GameMode.Standard).apply {
            hitObjects.add(Spinner(0.0, 500.0, true))
            hitObjects.add(HitCircle(1000.0, Vector2(0), true, 0))
        }

        val hidden = ModHidden().apply { applyToBeatmap(beatmap) }

        Assert.assertTrue(hidden.firstObject is HitCircle)
    }

    @Test
    fun `Test serialization`() {
        ModHidden().apply {
            serialize().apply {
                Assert.assertEquals(getString("acronym"), "HD")
                Assert.assertFalse(has("settings"))
            }

            onlyFadeApproachCircles = true

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(getBoolean("onlyFadeApproachCircles"))
            }
        }
    }
}