package com.osudroid.storyboard.parser

import com.osudroid.beatmaps.constants.SampleBank
import com.osudroid.storyboard.model.AnimationLoopType
import com.osudroid.storyboard.model.Storyboard
import com.osudroid.storyboard.model.StoryboardAnimation
import com.osudroid.storyboard.model.StoryboardEasing
import com.osudroid.storyboard.model.StoryboardLayerType
import com.osudroid.storyboard.model.StoryboardOrigin
import com.osudroid.storyboard.model.commands.StoryboardTriggerType
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StoryboardParserTest {
    private fun parse(name: String): Storyboard {
        val file = TestResourceManager.getTestResource("beatmaps/storyboards/$name/map.osu")!!
        return StoryboardParser(file).parse()!!
    }

    @Test
    fun `Test basic storyboard parsing`() {
        val storyboard = parse("basic")

        Assert.assertTrue(storyboard.widescreen)
        Assert.assertEquals("bg.jpg", storyboard.backgroundFilename)
        Assert.assertTrue(storyboard.usesBackgroundImage())

        // The .osb elements are merged after the .osu elements within the Background layer, so
        // they render on top (matching osu!lazer's decode order).
        val background = storyboard.layers[StoryboardLayerType.Background]!!
        Assert.assertEquals(2, background.size)
        Assert.assertEquals("bg.jpg", background[0].filePath)
        Assert.assertEquals("sb/osb-sprite.png", background[1].filePath)

        val foreground = storyboard.layers[StoryboardLayerType.Foreground]!!
        Assert.assertEquals(1, foreground.size)

        val sprite = foreground[0]
        Assert.assertEquals(StoryboardOrigin.Centre, sprite.origin)
        // Backslashes in paths are normalized.
        Assert.assertEquals("sb/osu.png", sprite.filePath)
        Assert.assertEquals(320f, sprite.initialX, 0f)
        Assert.assertEquals(240f, sprite.initialY, 0f)

        // M commands feed both the X and Y timelines.
        Assert.assertEquals(1, sprite.commands.x.size)
        Assert.assertEquals(1, sprite.commands.y.size)
        Assert.assertEquals(320f, sprite.commands.x[0].startValue, 0f)
        Assert.assertEquals(400f, sprite.commands.x[0].endValue, 0f)
        Assert.assertEquals(1, sprite.commands.scale.size)

        val overlay = storyboard.layers[StoryboardLayerType.Overlay]!!
        Assert.assertEquals(1, overlay.size)

        val animation = overlay[0] as StoryboardAnimation
        Assert.assertEquals(4, animation.frameCount)
        Assert.assertEquals(50.0, animation.frameDelay, 0.0)
        Assert.assertEquals(AnimationLoopType.LoopOnce, animation.loopType)
        Assert.assertEquals("sb/anim2.png", animation.framePath(2))

        Assert.assertEquals(1, storyboard.samples.size)
        Assert.assertEquals(5000.0, storyboard.samples[0].time, 0.0)
        Assert.assertEquals(80, storyboard.samples[0].volume)

        // Texture usage counts include every animation frame.
        val counts = storyboard.textureUsageCounts()
        Assert.assertEquals(1, counts["sb/anim0.png"])
        Assert.assertEquals(1, counts["sb/osu.png"])
    }

    @Test
    fun `Test variable substitution`() {
        val storyboard = parse("variables")

        val foreground = storyboard.layers[StoryboardLayerType.Foreground]!!
        Assert.assertEquals(1, foreground.size)

        val sprite = foreground[0]
        Assert.assertEquals("sb/flash.png", sprite.filePath)

        // The $fadeCommand variable expands to a full command line.
        Assert.assertEquals(1, sprite.commands.alpha.size)
        Assert.assertEquals(0f, sprite.commands.alpha[0].startValue, 0f)
        Assert.assertEquals(1f, sprite.commands.alpha[0].endValue, 0f)
    }

    @Test
    fun `Test command edge cases`() {
        val storyboard = parse("commands")

        val sprites = storyboard.layers[StoryboardLayerType.Foreground]!!

        Assert.assertEquals(3, sprites.size)

        // Unknown layers are rendered above the Foreground layer, and the Video layer below the
        // Background layer.
        Assert.assertEquals("sb/c.png", storyboard.layers[StoryboardLayerType.Custom]!!.single().filePath)
        Assert.assertEquals("sb/d.png", storyboard.layers[StoryboardLayerType.Video]!!.single().filePath)
        Assert.assertTrue(StoryboardLayerType.Video < StoryboardLayerType.Background)
        Assert.assertTrue(StoryboardLayerType.Custom > StoryboardLayerType.Foreground)
        Assert.assertTrue(StoryboardLayerType.Custom < StoryboardLayerType.Overlay)

        // The iteration duration of a loop spans from the start of its earliest command to the
        // end of its latest command.
        val delayedLoop = sprites[2].commands.loops[0]
        Assert.assertEquals(500.0, delayedLoop.iterationDuration, 0.0)
        Assert.assertEquals(1500.0, delayedLoop.startTime, 0.0)
        Assert.assertEquals(1500.0 + 3 * 500.0, delayedLoop.endTime, 0.0)

        val commands = sprites[0].commands

        // Single value group: start value = end value.
        Assert.assertEquals(1, commands.scale.size)
        Assert.assertEquals(0.5f, commands.scale[0].startValue, 0f)
        Assert.assertEquals(0.5f, commands.scale[0].endValue, 0f)
        Assert.assertEquals(1000.0, commands.scale[0].endTime, 0.0)

        // Alpha timeline: shorthand with empty end time plus a chained command.
        Assert.assertEquals(3, commands.alpha.size)
        Assert.assertEquals(2000.0, commands.alpha[0].startTime, 0.0)
        Assert.assertEquals(2000.0, commands.alpha[0].endTime, 0.0)

        // Chained command `F,0,3000,4000,0,1,0` produces two commands of equal duration.
        Assert.assertEquals(3000.0, commands.alpha[1].startTime, 0.0)
        Assert.assertEquals(4000.0, commands.alpha[1].endTime, 0.0)
        Assert.assertEquals(0f, commands.alpha[1].startValue, 0f)
        Assert.assertEquals(1f, commands.alpha[1].endValue, 0f)
        Assert.assertEquals(4000.0, commands.alpha[2].startTime, 0.0)
        Assert.assertEquals(5000.0, commands.alpha[2].endTime, 0.0)
        Assert.assertEquals(1f, commands.alpha[2].startValue, 0f)
        Assert.assertEquals(0f, commands.alpha[2].endValue, 0f)

        // MX command.
        Assert.assertEquals(2, commands.x.size)
        Assert.assertEquals(100f, commands.x[0].startValue, 0f)

        // Rotation in radians.
        Assert.assertEquals(-1.5708f, commands.rotation[0].startValue, 0f)

        // Colors are normalized to [0, 1].
        Assert.assertEquals(1f, commands.color[0].startValue.red, 0f)
        Assert.assertEquals(0f, commands.color[0].startValue.green, 0f)
        Assert.assertEquals(1f, commands.color[0].endValue.green, 0f)

        // P commands: interval command ends with false, zero-duration command stays true.
        Assert.assertEquals(true, commands.flipHorizontal[0].startValue)
        Assert.assertEquals(false, commands.flipHorizontal[0].endValue)
        Assert.assertEquals(true, commands.additiveBlend[0].startValue)
        Assert.assertEquals(true, commands.additiveBlend[0].endValue)

        // Underscore indentation and vector scale.
        Assert.assertEquals(2f, commands.vectorScaleX[0].startValue, 0f)
        Assert.assertEquals(3f, commands.vectorScaleY[0].startValue, 0f)

        // Scientific notation (M,1,15000,16000,1e2,2.5E1,200,50).
        Assert.assertEquals(100f, commands.x[1].startValue, 0f)
        Assert.assertEquals(200f, commands.x[1].endValue, 0f)
        Assert.assertEquals(25f, commands.y[0].startValue, 0f)

        // Easing parsing, including out-of-range fallback.
        val second = sprites[1].commands
        Assert.assertEquals(StoryboardEasing.OutElastic, second.alpha[0].easing)
        Assert.assertEquals(StoryboardEasing.None, second.alpha[1].easing)
    }

    @Test
    fun `Test loop parsing`() {
        val storyboard = parse("loops")

        val sprites = storyboard.layers[StoryboardLayerType.Foreground]!!
        val loop = sprites[0].commands.loops[0]

        Assert.assertEquals(1000.0, loop.loopStartTime, 0.0)
        Assert.assertEquals(3, loop.totalIterations)

        // Loop command times stay relative to the loop start.
        Assert.assertEquals(1, loop.alpha.size)
        Assert.assertEquals(0.0, loop.alpha[0].startTime, 0.0)

        Assert.assertEquals(400.0, loop.iterationDuration, 0.0)
        Assert.assertEquals(1000.0, loop.startTime, 0.0)
        Assert.assertEquals(1000.0 + 3 * 400.0, loop.endTime, 0.0)

        // Loop commands do not leak into the sprite's own timelines.
        Assert.assertEquals(0, sprites[0].commands.alpha.size)
    }

    @Test
    fun `Test trigger parsing`() {
        val storyboard = parse("triggers")

        val sprite = storyboard.layers[StoryboardLayerType.Foreground]!![0]
        val triggers = sprite.commands.triggers

        Assert.assertEquals(4, triggers.size)

        val clap = triggers[0].type as StoryboardTriggerType.HitSound
        Assert.assertNull(clap.sampleBank)
        Assert.assertEquals("hitclap", clap.addition)
        Assert.assertEquals(0, triggers[0].groupNumber)
        Assert.assertEquals(0.0, triggers[0].triggerStartTime, 0.0)
        Assert.assertEquals(10000.0, triggers[0].triggerEndTime, 0.0)

        // A single sample set immediately followed by an addition name describes that addition's
        // bank, not the normal sample's bank (matching stable/lazer trigger name parsing).
        val softWhistle = triggers[1].type as StoryboardTriggerType.HitSound
        Assert.assertNull(softWhistle.sampleBank)
        Assert.assertEquals(SampleBank.Soft, softWhistle.additionsSampleBank)
        Assert.assertEquals("hitwhistle", softWhistle.addition)
        Assert.assertEquals(1, triggers[1].groupNumber)

        Assert.assertTrue(triggers[2].type is StoryboardTriggerType.Passing)
        Assert.assertTrue(triggers[3].type is StoryboardTriggerType.Failing)

        // Trigger command times are relative to the activation.
        Assert.assertEquals(1, triggers[0].alpha.size)
        Assert.assertEquals(0.0, triggers[0].alpha[0].startTime, 0.0)
    }

    @Test
    fun `Test unsupported trigger and trailing comments`() {
        val storyboard = parse("triggers")

        val sprite = storyboard.layers[StoryboardLayerType.Foreground]!![1]
        val triggers = sprite.commands.triggers

        // The trailing comment of the declaration does not break parsing.
        Assert.assertEquals(240f, sprite.initialY, 0f)
        Assert.assertEquals(2, triggers.size)

        // The commands of a trigger with an unknown condition stay within the trigger rather than
        // leaking into the sprite's own timelines.
        Assert.assertTrue(triggers[0].type is StoryboardTriggerType.Unsupported)
        Assert.assertEquals(1, triggers[0].alpha.size)
        Assert.assertEquals(0, sprite.commands.alpha.size)

        // Hit sound trigger names are case-insensitive, and the trigger window is optional.
        val clap = triggers[1].type as StoryboardTriggerType.HitSound
        Assert.assertEquals("hitclap", clap.addition)
        Assert.assertEquals(1, triggers[1].alpha.size)
        Assert.assertEquals(0f, triggers[1].alpha[0].endValue, 0f)
    }

    @Test
    fun `Test trigger type parsing`() {
        val allSoft = StoryboardTriggerType.parse("HitSoundAllSoft2") as StoryboardTriggerType.HitSound
        Assert.assertNull(allSoft.sampleBank)
        Assert.assertEquals(SampleBank.Soft, allSoft.additionsSampleBank)
        Assert.assertNull(allSoft.addition)
        Assert.assertEquals(2, allSoft.customSampleBank)

        val bare = StoryboardTriggerType.parse("HitSound") as StoryboardTriggerType.HitSound
        Assert.assertNull(bare.sampleBank)
        Assert.assertNull(bare.addition)

        // Two explicit banks assign the first to the normal sample and the second to the addition.
        val normalSoftClap = StoryboardTriggerType.parse("HitSoundNormalSoftClap") as StoryboardTriggerType.HitSound
        Assert.assertEquals(SampleBank.Normal, normalSoftClap.sampleBank)
        Assert.assertEquals(SampleBank.Soft, normalSoftClap.additionsSampleBank)
        Assert.assertEquals("hitclap", normalSoftClap.addition)

        Assert.assertNull(StoryboardTriggerType.parse("NotATrigger"))
        Assert.assertNull(StoryboardTriggerType.parse("HitSoundBogus"))
    }

    @Test
    fun `Test old format version time offset`() {
        val storyboard = parse("old-version")

        val sprite = storyboard.layers[StoryboardLayerType.Foreground]!![0]

        // Version 4 and lower have a 24 ms offset on absolute times.
        Assert.assertEquals(1024.0, sprite.commands.alpha[0].startTime, 0.0)
        Assert.assertEquals(2024.0, sprite.commands.alpha[0].endTime, 0.0)

        // The offset applies to the loop start, but not to the relative command times inside.
        val loop = sprite.commands.loops[0]
        Assert.assertEquals(3024.0, loop.loopStartTime, 0.0)
        Assert.assertEquals(0.0, loop.alpha[0].startTime, 0.0)
    }

    @Test
    fun `Test quick storyboard detection`() {
        fun resource(name: String) = TestResourceManager.getTestResource("beatmaps/storyboards/$name/map.osu")!!

        // Declared in the .osu file.
        Assert.assertTrue(StoryboardParser.hasStoryboard(resource("commands")))

        val withoutStoryboard = TestResourceManager.getBeatmapFile("Kenji Ninuma - DISCOPRINCE (peppy) [Normal]")!!
        Assert.assertFalse(StoryboardParser.hasStoryboard(withoutStoryboard))

        // Agrees with the parser for every test storyboard.
        for (name in arrayOf("basic", "loops", "triggers", "variables", "old-version")) {
            Assert.assertEquals(name, StoryboardParser(resource(name)).parse() != null, StoryboardParser.hasStoryboard(resource(name)))
        }
    }

    @Test
    fun `Test beatmap without storyboard returns null`() {
        val file = TestResourceManager.getBeatmapFile("Kenji Ninuma - DISCOPRINCE (peppy) [Normal]")!!
        Assert.assertNull(StoryboardParser(file).parse())
    }
}
