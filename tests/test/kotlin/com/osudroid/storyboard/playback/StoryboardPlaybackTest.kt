package com.osudroid.storyboard.playback

import com.edlplan.framework.easing.Easing
import com.osudroid.beatmaps.constants.SampleBank
import com.osudroid.beatmaps.hitobjects.BankHitSampleInfo
import com.osudroid.storyboard.model.AnimationLoopType
import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.model.StoryboardAnimation
import com.osudroid.storyboard.model.StoryboardColor
import com.osudroid.storyboard.model.StoryboardElement
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardOrigin
import com.osudroid.storyboard.model.StoryboardSample
import com.osudroid.storyboard.model.StoryboardSprite
import com.osudroid.storyboard.model.commands.StoryboardLoop
import com.osudroid.storyboard.model.commands.StoryboardTrigger
import com.osudroid.storyboard.model.commands.StoryboardTriggerType
import com.reco1l.framework.interpolate
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoryboardPlaybackTest {
    private fun sprite(layer: StoryboardLayerType = StoryboardLayerType.Foreground) =
        StoryboardSprite(layer, StoryboardOrigin.Centre, "sprite.png", 320f, 240f)

    private fun storyboardOf(vararg elements: StoryboardElement) = Storyboard().also {
        for (element in elements) {
            it.add(element)
        }
    }

    @Test
    fun `Test easing boundary values`() {
        for (easing in Easing.entries) {
            Assert.assertEquals("$easing at 0", 0f, easing.interpolate(0f), 1e-6f)
            Assert.assertEquals("$easing at 1", 1f, easing.interpolate(1f), 1e-6f)
        }
    }

    @Test
    fun `Test easing spot values`() {
        Assert.assertEquals(0.5f, Easing.None.interpolate(0.5f), 0f)
        Assert.assertEquals(0.25f, Easing.InQuad.interpolate(0.5f), 1e-6f)
        Assert.assertEquals(0.75f, Easing.OutQuad.interpolate(0.5f), 1e-6f)
        Assert.assertEquals(0.125f, Easing.InCubic.interpolate(0.5f), 1e-6f)
        Assert.assertEquals(0.5f, Easing.InOutSine.interpolate(0.5f), 1e-6f)
    }

    @Test
    fun `Test value evaluation before between and after commands`() {
        val element = sprite()
        element.commands.alpha.add(Easing.None, 1000.0, 2000.0, 0.2f, 0.8f)
        element.commands.alpha.add(Easing.None, 3000.0, 4000.0, 0.4f, 1f)

        val playable = PlayableSprite(element)

        // Before the first command, the property takes on its start value.
        playable.update(0.0)
        Assert.assertEquals(0.2f, playable.alpha, 1e-6f)

        // Linear interpolation within a command.
        playable.update(1500.0)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        // Between commands, the previous command's end value persists.
        playable.update(2500.0)
        Assert.assertEquals(0.8f, playable.alpha, 1e-6f)

        // After the last command, its end value persists.
        playable.update(10000.0)
        Assert.assertEquals(1f, playable.alpha, 1e-6f)

        // Properties without commands keep their initial values.
        Assert.assertEquals(320f, playable.x, 0f)
        Assert.assertEquals(240f, playable.y, 0f)
        Assert.assertEquals(1f, playable.scaleX, 0f)
        Assert.assertEquals(0f, playable.rotation, 0f)
    }

    @Test
    fun `Test scale and vector scale combination`() {
        val element = sprite()
        element.commands.scale.add(Easing.None, 0.0, 1000.0, 2f, 2f)
        element.commands.vectorScaleX.add(Easing.None, 0.0, 1000.0, 3f, 3f)
        element.commands.vectorScaleY.add(Easing.None, 0.0, 1000.0, 0.5f, 0.5f)

        val playable = PlayableSprite(element)
        playable.update(500.0)

        Assert.assertEquals(6f, playable.scaleX, 1e-6f)
        Assert.assertEquals(1f, playable.scaleY, 1e-6f)
    }

    @Test
    fun `Test color evaluation`() {
        val element = sprite()
        element.commands.color.add(
            Easing.None, 0.0, 1000.0,
            StoryboardColor(1f, 0f, 0f), StoryboardColor(0f, 1f, 0f)
        )

        val playable = PlayableSprite(element)
        playable.update(500.0)

        Assert.assertEquals(0.5f, playable.red, 1e-6f)
        Assert.assertEquals(0.5f, playable.green, 1e-6f)
        Assert.assertEquals(0f, playable.blue, 1e-6f)
    }

    @Test
    fun `Test parameter command evaluation`() {
        val element = sprite()
        // An interval P command is active during its interval only.
        element.commands.flipHorizontal.add(Easing.None, 1000.0, 2000.0, true, false)
        // A zero-duration P command applies permanently.
        element.commands.additiveBlend.add(Easing.None, 3000.0, 3000.0, true, true)

        val playable = PlayableSprite(element)

        playable.update(500.0)
        Assert.assertFalse(playable.flipHorizontal)
        Assert.assertFalse(playable.additiveBlend)

        playable.update(1500.0)
        Assert.assertTrue(playable.flipHorizontal)

        playable.update(2500.0)
        Assert.assertFalse(playable.flipHorizontal)

        playable.update(5000.0)
        Assert.assertTrue(playable.additiveBlend)
    }

    @Test
    fun `Test display start time optimization`() {
        val element = sprite()
        element.commands.x.add(Easing.None, 0.0, 5000.0, 0f, 100f)
        // The earliest alpha command starts with zero alpha, so the sprite only becomes visible
        // at its start time.
        element.commands.alpha.add(Easing.None, 2000.0, 3000.0, 0f, 1f)

        val playable = PlayableSprite(element)

        Assert.assertEquals(2000.0, playable.displayStartTime, 0.0)
        Assert.assertEquals(5000.0, playable.endTime, 0.0)
        Assert.assertFalse(playable.isActive(1000.0))
        Assert.assertTrue(playable.isActive(2500.0))
        Assert.assertFalse(playable.isActive(6000.0))
    }

    @Test
    fun `Test loop unrolling`() {
        val element = sprite()

        val loop = StoryboardLoop(1000.0, 3)
        loop.alpha.add(Easing.None, 0.0, 200.0, 0f, 1f)
        element.commands.loops.add(loop)

        val playable = PlayableSprite(element)

        // First iteration.
        playable.update(1100.0)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        // Second iteration restarts at loopStart + iterationDuration.
        playable.update(1300.0)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        // Third iteration.
        playable.update(1500.0)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        // After the last iteration the final value persists.
        playable.update(2000.0)
        Assert.assertEquals(1f, playable.alpha, 1e-6f)

        Assert.assertEquals(1000.0, playable.displayStartTime, 0.0)
        Assert.assertEquals(1600.0, playable.endTime, 0.0)
    }

    @Test
    fun `Test loop with delayed first command`() {
        val element = sprite()

        val loop = StoryboardLoop(1000.0, 2)
        loop.alpha.add(Easing.None, 500.0, 1000.0, 0f, 1f)
        element.commands.loops.add(loop)

        val playable = PlayableSprite(element)

        // First iteration runs from 1500 to 2000, the second one follows immediately.
        playable.update(1750.0)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        playable.update(2250.0)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        Assert.assertEquals(2500.0, playable.endTime, 0.0)
    }

    @Test
    fun `Test sample playback`() {
        val storyboard = storyboardOf(sprite().also {
            it.commands.alpha.add(Easing.None, 0.0, 10000.0, 1f, 1f)
        })

        storyboard.samples.add(StoryboardSample(StoryboardLayerType.Background, 1000.0, "a.wav", 100))
        storyboard.samples.add(StoryboardSample(StoryboardLayerType.Fail, 2000.0, "b.wav", 100))
        storyboard.samples.add(StoryboardSample(StoryboardLayerType.Background, 3000.0, "c.wav", 100))
        storyboard.samples.add(StoryboardSample(StoryboardLayerType.Background, -500.0, "intro.wav", 100))

        val playback = StoryboardPlayback(storyboard)
        val played = mutableListOf<String>()
        playback.onSamplePlayed = { played.add(it.filePath) }

        Assert.assertEquals(-500.0, playback.earliestEventTime!!, 0.0)

        playback.setTime(-1000.0)
        Assert.assertTrue(played.isEmpty())

        playback.setTime(-490.0)
        playback.setTime(1010.0)
        Assert.assertEquals(listOf("intro.wav", "a.wav"), played)

        // Samples of the Fail layer are not played while passing, and samples that are passed by
        // a large margin are skipped.
        playback.setTime(5000.0)
        Assert.assertEquals(listOf("intro.wav", "a.wav"), played)

        // Samples are played again after seeking backwards.
        playback.setTime(900.0)
        playback.setTime(1000.0)
        Assert.assertEquals(listOf("intro.wav", "a.wav", "a.wav"), played)
    }

    @Test
    fun `Test hit object hit trigger and playback reset`() {
        val element = sprite()

        val trigger = StoryboardTrigger(StoryboardTriggerType.parse("HitObjectHit")!!, 0.0, 10000.0, 0)
        trigger.alpha.add(Easing.None, 0.0, 500.0, 1f, 0f)
        element.commands.triggers.add(trigger)

        val playback = StoryboardPlayback(storyboardOf(element))
        val playable = playback.layers[StoryboardLayerType.Foreground]!![0]

        Assert.assertTrue(playback.hasHitObjectHitTriggers)

        // A sprite that is only displayed by triggers does not define the start of the storyboard.
        Assert.assertNull(playback.earliestEventTime)

        playback.update(1000.0)
        playback.onHitObjectHit(1000.0)
        playback.update(1250.0)
        Assert.assertTrue(playable.isVisible)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        playback.reset()
        playback.update(1250.0)
        Assert.assertFalse(playable.isVisible)
    }

    @Test
    fun `Test animation frame evaluation`() {
        fun animation(loopType: AnimationLoopType): StoryboardAnimation {
            val element = StoryboardAnimation(
                StoryboardLayerType.Foreground, StoryboardOrigin.Centre, "anim.png",
                0f, 0f, 4, 100.0, loopType
            )
            element.commands.alpha.add(Easing.None, 1000.0, 2000.0, 1f, 1f)
            return element
        }

        val once = PlayableSprite(animation(AnimationLoopType.LoopOnce))
        once.update(1250.0)
        Assert.assertEquals(2, once.frameIndex)
        once.update(1950.0)
        Assert.assertEquals(3, once.frameIndex)

        val forever = PlayableSprite(animation(AnimationLoopType.LoopForever))
        forever.update(1450.0)
        Assert.assertEquals(0, forever.frameIndex)
        forever.update(1550.0)
        Assert.assertEquals(1, forever.frameIndex)
    }

    @Test
    fun `Test hit sound trigger activation`() {
        val element = sprite()
        element.commands.alpha.add(Easing.None, 0.0, 10000.0, 0.2f, 0.2f)

        val trigger = StoryboardTrigger(
            StoryboardTriggerType.parse("HitSoundClap")!!, 0.0, 10000.0, 0
        )
        trigger.alpha.add(Easing.None, 0.0, 500.0, 1f, 0.5f)
        element.commands.triggers.add(trigger)

        val storyboard = storyboardOf(element)
        val playback = StoryboardPlayback(storyboard)

        Assert.assertTrue(playback.hasHitSoundTriggers)

        playback.update(1000.0)
        val playable = playback.layers[StoryboardLayerType.Foreground]!![0]
        Assert.assertEquals(0.2f, playable.alpha, 1e-6f)

        // A matching hit sound activates the trigger; a non-matching one does not.
        playback.onHitSound(listOf(BankHitSampleInfo("hitwhistle", SampleBank.Soft)), 1000.0)
        playback.update(1000.0)
        Assert.assertEquals(0.2f, playable.alpha, 1e-6f)

        playback.onHitSound(listOf(BankHitSampleInfo("hitclap", SampleBank.Soft)), 1000.0)
        playback.update(1000.0)
        Assert.assertEquals(1f, playable.alpha, 1e-6f)

        // Trigger commands are evaluated relative to the activation time.
        playback.update(1250.0)
        Assert.assertEquals(0.75f, playable.alpha, 1e-6f)

        // A backward seek resets trigger activations.
        playback.update(500.0)
        Assert.assertEquals(0.2f, playable.alpha, 1e-6f)
    }

    @Test
    fun `Test hit sound trigger with distinct normal and addition banks`() {
        val element = sprite()
        element.commands.alpha.add(Easing.None, 0.0, 10000.0, 0.2f, 0.2f)

        val trigger = StoryboardTrigger(
            StoryboardTriggerType.parse("HitSoundNormalSoftClap")!!, 0.0, 10000.0, 0
        )
        trigger.alpha.add(Easing.None, 0.0, 500.0, 1f, 0.5f)
        element.commands.triggers.add(trigger)

        val storyboard = storyboardOf(element)
        val playback = StoryboardPlayback(storyboard)
        val playable = playback.layers[StoryboardLayerType.Foreground]!![0]

        // The normal sample is on the wrong bank (Drum instead of Normal), so the trigger must not
        // activate even though the clap addition is correctly on the Soft bank.
        playback.onHitSound(
            listOf(
                BankHitSampleInfo("hitnormal", SampleBank.Drum),
                BankHitSampleInfo("hitclap", SampleBank.Soft)
            ),
            1000.0
        )
        playback.update(1000.0)
        Assert.assertEquals(0.2f, playable.alpha, 1e-6f)

        // The full sample set of a single hit object satisfies both constraints together: the
        // normal sample on the Normal bank AND the clap addition on the Soft bank.
        playback.onHitSound(
            listOf(
                BankHitSampleInfo("hitnormal", SampleBank.Normal),
                BankHitSampleInfo("hitclap", SampleBank.Soft)
            ),
            1000.0
        )
        playback.update(1000.0)
        Assert.assertEquals(1f, playable.alpha, 1e-6f)
    }

    @Test
    fun `Test passing and failing state`() {
        val passElement = sprite(StoryboardLayerType.Pass)
        passElement.commands.alpha.add(Easing.None, 0.0, 10000.0, 1f, 1f)

        val failElement = sprite(StoryboardLayerType.Fail)
        failElement.commands.alpha.add(Easing.None, 0.0, 10000.0, 1f, 1f)

        val playback = StoryboardPlayback(storyboardOf(passElement, failElement))

        Assert.assertTrue(playback.isLayerVisible(StoryboardLayerType.Pass))
        Assert.assertFalse(playback.isLayerVisible(StoryboardLayerType.Fail))

        playback.update(1000.0)
        playback.setPassing(false, 1000.0)

        Assert.assertFalse(playback.isLayerVisible(StoryboardLayerType.Pass))
        Assert.assertTrue(playback.isLayerVisible(StoryboardLayerType.Fail))
        Assert.assertTrue(playback.isLayerVisible(StoryboardLayerType.Background))
    }

    @Test
    fun `Test failing trigger activation`() {
        val element = sprite()
        element.commands.alpha.add(Easing.None, 0.0, 10000.0, 0.5f, 0.5f)

        val trigger = StoryboardTrigger(StoryboardTriggerType.Failing, 0.0, 10000.0, 0)
        trigger.color.add(
            Easing.None, 0.0, 0.0,
            StoryboardColor(1f, 0f, 0f), StoryboardColor(1f, 0f, 0f)
        )
        element.commands.triggers.add(trigger)

        val playback = StoryboardPlayback(storyboardOf(element))
        val playable = playback.layers[StoryboardLayerType.Foreground]!![0]

        playback.update(1000.0)
        Assert.assertEquals(1f, playable.red, 0f)
        Assert.assertEquals(1f, playable.green, 0f)

        playback.setPassing(false, 1000.0)
        playback.update(1500.0)
        Assert.assertEquals(1f, playable.red, 0f)
        Assert.assertEquals(0f, playable.green, 0f)
    }

    @Test
    fun `Test animation starts at earliest command`() {
        val element = StoryboardAnimation(
            StoryboardLayerType.Foreground, StoryboardOrigin.Centre, "anim.png",
            0f, 0f, 4, 100.0, AnimationLoopType.LoopForever
        )
        element.commands.scale.add(Easing.None, 1000.0, 1500.0, 1f, 1f)
        element.commands.alpha.add(Easing.None, 2000.0, 3000.0, 0f, 1f)

        val playable = PlayableSprite(element)

        // The sprite only becomes visible with the fade, but the animation has been running
        // since the scale command.
        Assert.assertEquals(2000.0, playable.displayStartTime, 0.0)

        playable.update(2150.0)
        Assert.assertEquals(3, playable.frameIndex)
    }

    @Test
    fun `Test sprite with only trigger commands is hidden until triggered`() {
        val element = sprite()

        val trigger = StoryboardTrigger(StoryboardTriggerType.Passing, 0.0, 10000.0, 0)
        trigger.alpha.add(Easing.None, 0.0, 500.0, 1f, 0f)
        element.commands.triggers.add(trigger)

        val playback = StoryboardPlayback(storyboardOf(element))
        val playable = playback.layers[StoryboardLayerType.Foreground]!![0]

        playback.setPassing(false, 0.0)
        playback.update(1000.0)
        Assert.assertFalse(playable.isVisible)

        playback.setPassing(true, 1000.0)
        playback.update(1250.0)
        Assert.assertTrue(playable.isVisible)
        Assert.assertEquals(0.5f, playable.alpha, 1e-6f)

        // The sprite is hidden again once the trigger's commands have finished.
        playback.update(2000.0)
        Assert.assertFalse(playable.isVisible)
    }

    @Test
    fun `Test sprite without commands is never active`() {
        val playable = PlayableSprite(sprite())

        Assert.assertFalse(playable.isActive(0.0))
        Assert.assertFalse(playable.isActive(100000.0))
    }
}
