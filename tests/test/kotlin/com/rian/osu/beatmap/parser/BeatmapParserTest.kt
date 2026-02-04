package com.rian.osu.beatmap.parser

import com.reco1l.framework.*
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.BeatmapCountdown
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo
import com.rian.osu.beatmap.hitobject.HitCircle
import com.rian.osu.beatmap.hitobject.Slider
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BeatmapParserTest {
    @Test
    fun `Test format version header`() {
        Assert.assertEquals(v3Beatmap.formatVersion, 3)
        Assert.assertEquals(v14Beatmap.formatVersion, 14)
    }

    @Test
    fun `Test version 3 beatmap colors section`() {
        Assert.assertTrue(v3Beatmap.colors.comboColors.isEmpty())
    }

    @Test
    fun `Test version 14 beatmap colors section`() {
        fun test(color: Color4, red: Int, green: Int, blue: Int) {
            Assert.assertEquals(red, color.redInt)
            Assert.assertEquals(green, color.greenInt)
            Assert.assertEquals(blue, color.blueInt)
        }

        val colors = v14Beatmap.colors

        Assert.assertEquals(colors.comboColors.size, 4)

        test(colors.comboColors[0].color, 98, 243, 255)
        test(colors.comboColors[1].color, 251, 170, 251)
        test(colors.comboColors[2].color, 102, 171, 255)
        test(colors.comboColors[3].color, 162, 205, 232)

        Assert.assertNull(colors.sliderBorderColor)
    }

    @Test
    fun `Test version 3 beatmap control points section`() {
        v3Beatmap.controlPoints.apply {
            Assert.assertTrue(difficulty.controlPoints.isEmpty())
            Assert.assertTrue(effect.controlPoints.isEmpty())
            Assert.assertTrue(sample.controlPoints.isEmpty())
            Assert.assertEquals(1, timing.controlPoints.size)
        }
    }

    @Test
    fun `Test version 14 beatmap control points section`() {
        v14Beatmap.controlPoints.apply {
            Assert.assertEquals(14, difficulty.controlPoints.size)
            Assert.assertEquals(14, effect.controlPoints.size)
            Assert.assertEquals(526, sample.controlPoints.size)
            Assert.assertEquals(5, timing.controlPoints.size)
        }
    }

    @Test
    fun `Test version 3 beatmap difficulty section`() {
        v3Beatmap.difficulty.apply {
            Assert.assertEquals(6f, ar, 0f)
            Assert.assertEquals(4f, difficultyCS, 0f)
            Assert.assertEquals(4f, gameplayCS, 0f)
            Assert.assertEquals(6f, hp, 0f)
            Assert.assertEquals(6f, od, 0f)
            Assert.assertEquals(1.4, sliderMultiplier, 0.0)
            Assert.assertEquals(2.0, sliderTickRate, 0.0)
        }
    }

    @Test
    fun `Test version 14 beatmap difficulty section`() {
        v14Beatmap.difficulty.apply {
            Assert.assertEquals(9f, ar, 0f)
            Assert.assertEquals(4f, difficultyCS, 0f)
            Assert.assertEquals(4f, gameplayCS, 0f)
            Assert.assertEquals(5f, hp, 0f)
            Assert.assertEquals(8f, od, 0f)
            Assert.assertEquals(1.9, sliderMultiplier, 0.0)
            Assert.assertEquals(1.0, sliderTickRate, 0.0)
        }
    }

    @Test
    fun `Test version 3 beatmap events section`() {
        v3Beatmap.events.apply {
            Assert.assertEquals("katamari2.jpg", backgroundFilename)

            Assert.assertNotNull(backgroundColor)
            Assert.assertEquals(54, backgroundColor!!.redInt)
            Assert.assertEquals(140, backgroundColor!!.greenInt)
            Assert.assertEquals(191, backgroundColor!!.blueInt)

            Assert.assertEquals(breaks.size, 3)
            Assert.assertNull(videoFilename)
            Assert.assertEquals(videoStartTime, 0)
        }
    }

    @Test
    fun `Test version 14 beatmap events section`() {
        v14Beatmap.events.apply {
            Assert.assertEquals("school.jpg", backgroundFilename)
            Assert.assertNull(backgroundColor)
            Assert.assertTrue(breaks.isEmpty())
            Assert.assertEquals("Yoasobi.mp4", videoFilename)
            Assert.assertEquals(-150,  videoStartTime)
        }
    }

    @Test
    fun `Test version 3 beatmap general section`() {
        v3Beatmap.general.apply {
            Assert.assertEquals("20.mp3", audioFilename)
            Assert.assertEquals(0, audioLeadIn)
            Assert.assertEquals(BeatmapCountdown.Normal, countdown)
            Assert.assertFalse(letterboxInBreaks)
            Assert.assertEquals(0, mode)
            Assert.assertEquals(-1, previewTime)
            Assert.assertEquals(SampleBank.Normal, sampleBank)
            Assert.assertEquals(100, sampleVolume)
            Assert.assertFalse(samplesMatchPlaybackRate)
        }
    }

    @Test
    fun `Test version 14 beatmap general section`() {
        v14Beatmap.general.apply {
            Assert.assertEquals("audio.mp3", audioFilename)
            Assert.assertEquals(0, audioLeadIn)
            Assert.assertEquals(BeatmapCountdown.NoCountdown, countdown)
            Assert.assertFalse(letterboxInBreaks)
            Assert.assertEquals(0, mode)
            Assert.assertEquals(49037, previewTime)
            Assert.assertEquals(SampleBank.Soft, sampleBank)
            Assert.assertEquals(100, sampleVolume)
            Assert.assertFalse(samplesMatchPlaybackRate)
        }
    }

    @Test
    fun `Test version 3 beatmap hit object counters`() {
        v3Beatmap.hitObjects.apply {
            Assert.assertEquals(194, objects.size)
            Assert.assertEquals(160, circleCount)
            Assert.assertEquals(30, sliderCount)
            Assert.assertEquals(75, sliderTickCount)
            Assert.assertEquals(4, spinnerCount)
        }
    }

    @Test
    fun `Test version 14 beatmap hit object counters`() {
        v14Beatmap.hitObjects.apply {
            Assert.assertEquals(592, objects.size)
            Assert.assertEquals(198, circleCount)
            Assert.assertEquals(393, sliderCount)
            Assert.assertEquals(21, sliderTickCount)
            Assert.assertEquals(1, spinnerCount)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Test version 3 beatmap hit object samples`() {
        val circle = v3Beatmap.hitObjects.objects[0] as HitCircle
        Assert.assertEquals(2, circle.samples.size)

        val (firstSample, lastSample) = circle.samples as MutableList<BankHitSampleInfo>

        firstSample.apply {
            Assert.assertEquals(BankHitSampleInfo.HIT_NORMAL, name)
            Assert.assertEquals(SampleBank.Normal, bank)
            Assert.assertEquals(0, customSampleBank)
            Assert.assertEquals(100, volume)
            Assert.assertTrue(isLayered)
        }

        lastSample.apply {
            Assert.assertEquals(BankHitSampleInfo.HIT_FINISH, name)
            Assert.assertEquals(SampleBank.Normal, bank)
            Assert.assertEquals(0, customSampleBank)
            Assert.assertEquals(100, volume)
            Assert.assertFalse(isLayered)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Test version 14 beatmap hit object samples`() {
        val slider = v14Beatmap.hitObjects.objects[1] as Slider
        Assert.assertEquals(2, slider.samples.size)

        val (firstSample, lastSample) = slider.samples as MutableList<BankHitSampleInfo>

        firstSample.apply {
            Assert.assertEquals(BankHitSampleInfo.HIT_NORMAL, name)
            Assert.assertEquals(SampleBank.Soft, bank)
            Assert.assertEquals(1, customSampleBank)
            Assert.assertEquals(40, volume)
            Assert.assertTrue(isLayered)
        }

        lastSample.apply {
            Assert.assertEquals(BankHitSampleInfo.HIT_CLAP, name)
            Assert.assertEquals(SampleBank.Drum, bank)
            Assert.assertEquals(1, customSampleBank)
            Assert.assertEquals(40, volume)
            Assert.assertFalse(isLayered)
        }
    }

    @Test
    fun `Test version 3 beatmap hit object node samples`() {
        val slider = v3Beatmap.hitObjects.objects[19] as Slider

        for (nodeSample in slider.nodeSamples) {
            (nodeSample[0] as BankHitSampleInfo).apply {
                Assert.assertEquals(BankHitSampleInfo.HIT_NORMAL, name)
                Assert.assertEquals(SampleBank.Normal, bank)
                Assert.assertEquals(0, customSampleBank)
                Assert.assertEquals(100, volume)
                Assert.assertFalse(isLayered)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Test version 14 beatmap hit object node samples`() {
        val slider = v14Beatmap.hitObjects.objects[1] as Slider

        for (nodeSample in slider.nodeSamples) {
            val (firstSample, lastSample) = nodeSample as MutableList<BankHitSampleInfo>

            firstSample.apply {
                Assert.assertEquals(BankHitSampleInfo.HIT_NORMAL, name)
                Assert.assertEquals(SampleBank.Soft, bank)
                Assert.assertEquals(1, customSampleBank)
                Assert.assertEquals(40, volume)
                Assert.assertTrue(isLayered)
            }

            lastSample.apply {
                Assert.assertEquals(BankHitSampleInfo.HIT_CLAP, name)
                Assert.assertEquals(SampleBank.Drum, bank)
                Assert.assertEquals(1, customSampleBank)
                Assert.assertEquals(40, volume)
                Assert.assertFalse(isLayered)
            }
        }
    }

    @Test
    fun `Test version 3 beatmap metadata section`() {
        v3Beatmap.metadata.apply {
            Assert.assertEquals("Kenji Ninuma", artist)
            Assert.assertTrue(artistUnicode.isEmpty())
            Assert.assertEquals(-1, beatmapId)
            Assert.assertEquals(-1, beatmapSetId)
            Assert.assertEquals("peppy", creator)
            Assert.assertEquals("DISCO★PRINCE", title)
            Assert.assertTrue(titleUnicode.isEmpty())
            Assert.assertEquals("Normal", version)
            Assert.assertTrue(source.isEmpty())
            Assert.assertTrue(tags.isEmpty())
        }
    }

    @Test
    fun `Test version 14 beatmap metadata section`() {
        v14Beatmap.metadata.apply {
            Assert.assertEquals("YOASOBI", artist)
            Assert.assertEquals("YOASOBI", artistUnicode)
            Assert.assertEquals(3324715, beatmapId)
            Assert.assertEquals(1585863, beatmapSetId)
            Assert.assertEquals("ohm002", creator)
            Assert.assertEquals("Love Letter", title)
            Assert.assertEquals("ラブレター", titleUnicode)
            Assert.assertEquals("Please accept my overflowing emotions.", version)
            Assert.assertTrue(source.isEmpty())
            Assert.assertTrue(tags.isNotEmpty())
        }
    }

    @Test
    fun `Test max combo`() {
        Assert.assertEquals(314, v3Beatmap.maxCombo)
        Assert.assertEquals(1033, v14Beatmap.maxCombo)
    }

    @Test
    fun `Test NaN control points`() {
        nanBeatmap.controlPoints.apply {
            Assert.assertEquals(1, timing.controlPoints.size)
            Assert.assertEquals(2, difficulty.controlPoints.size)

            Assert.assertEquals(500.0, timing.controlPointAt(1000.0).msPerBeat, 0.0)

            difficulty.controlPointAt(2000.0).apply {
                Assert.assertEquals(1.0, speedMultiplier, 0.0)
                Assert.assertFalse(generateTicks)
            }

            difficulty.controlPointAt(3000.0).apply {
                Assert.assertEquals(1.0, speedMultiplier, 0.0)
                Assert.assertTrue(generateTicks)
            }
        }
    }

    companion object {
        private lateinit var v3Beatmap: Beatmap
        private lateinit var v14Beatmap: Beatmap
        private lateinit var nanBeatmap: Beatmap

        @BeforeClass
        @JvmStatic
        fun setup() {
            v3Beatmap = BeatmapParser(
                TestResourceManager.getBeatmapFile("Kenji Ninuma - DISCOPRINCE (peppy) [Normal]")!!
            ).parse(true)!!

            v14Beatmap =
                BeatmapParser(
                    TestResourceManager.getBeatmapFile(
                        "YOASOBI - Love Letter (ohm002) [Please accept my overflowing emotions.]"
                    )!!
                ).parse(true)!!

            nanBeatmap =
                BeatmapParser(TestResourceManager.getBeatmapFile("nan-control-points")!!).parse(true)!!
        }
    }
}