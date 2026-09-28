package com.osudroid.storyboard

import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.parser.StoryboardParser
import com.osudroid.storyboard.playback.StoryboardPlayback
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Regression tests against real, unmodified storyboards (only the `.osu`/`.osb` text files are
 * included in the test resources). The expected numbers pin the parser's behavior - if they
 * change unexpectedly, command or element parsing has regressed.
 */
@RunWith(RobolectricTestRunner::class)
class RealStoryboardsTest {
    private fun parse(name: String): Storyboard {
        val file = TestResourceManager.getTestResource("beatmaps/storyboards/real/$name/map.osu")!!
        return StoryboardParser(file).parse()!!
    }

    private fun Storyboard.totalCommands() = elements.sumOf { element ->
        element.commands.timelines.sumOf { it.size } +
            element.commands.loops.sumOf { loop -> loop.timelines.sumOf { it.size } } +
            element.commands.triggers.sumOf { trigger -> trigger.timelines.sumOf { it.size } }
    }

    private fun verifyPlayback(storyboard: Storyboard) {
        val playback = StoryboardPlayback(storyboard)

        val sprites = playback.layers.values.flatten()
        val minTime = sprites.minOf { it.displayStartTime }
        val maxTime = sprites.filter { it.endTime < Double.MAX_VALUE }.maxOf { it.endTime }

        // Sample a forward playthrough and verify that sprites actually become active.
        var maxActive = 0

        for (i in 0..240) {
            playback.update(minTime + (maxTime - minTime) * i / 240)
            maxActive = maxOf(maxActive, playback.layers.keys.sumOf { playback.activeSprites(it).size })
        }

        Assert.assertTrue("No sprite ever became active", maxActive > 0)

        // A backward seek must remain consistent.
        playback.update(minTime + (maxTime - minTime) / 2)
        Assert.assertTrue(playback.currentTime > minTime)
    }

    @Test
    fun `Test Kuba Oms - My Love`() {
        val storyboard = parse("my-love")

        Assert.assertEquals(10656, storyboard.elements.count())
        Assert.assertEquals(105190, storyboard.totalCommands())
        Assert.assertTrue(storyboard.widescreen)
        Assert.assertTrue(storyboard.usesBackgroundImage())

        verifyPlayback(storyboard)
    }

    @Test
    fun `Test BlackY - PANAGIA`() {
        val storyboard = parse("panagia")

        Assert.assertEquals(5013, storyboard.elements.count())
        Assert.assertEquals(36080, storyboard.totalCommands())
        Assert.assertTrue(storyboard.widescreen)
        Assert.assertTrue(storyboard.usesBackgroundImage())

        verifyPlayback(storyboard)
    }

    @Test
    fun `Test Ryu - Sakura Reflection`() {
        val storyboard = parse("sakura-reflection")

        Assert.assertEquals(3198, storyboard.elements.count())
        Assert.assertEquals(10092, storyboard.totalCommands())
        Assert.assertFalse(storyboard.widescreen)
        Assert.assertFalse(storyboard.usesBackgroundImage())

        verifyPlayback(storyboard)
    }

    @Test
    fun `Test NOMA - LOUDER MACHINE`() {
        val storyboard = parse("louder-machine")

        Assert.assertEquals(59227, storyboard.elements.count())
        Assert.assertEquals(598021, storyboard.totalCommands())
        Assert.assertTrue(storyboard.widescreen)
        Assert.assertTrue(storyboard.usesBackgroundImage())

        verifyPlayback(storyboard)
    }
}
