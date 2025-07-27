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
            Assert.assertEquals(color.redInt, red)
            Assert.assertEquals(color.greenInt, green)
            Assert.assertEquals(color.blueInt, blue)
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
            Assert.assertEquals(timing.controlPoints.size, 1)
        }
    }

    @Test
    fun `Test version 14 beatmap control points section`() {
        v14Beatmap.controlPoints.apply {
            Assert.assertEquals(difficulty.controlPoints.size, 14)
            Assert.assertEquals(effect.controlPoints.size, 14)
            Assert.assertEquals(sample.controlPoints.size, 526)
            Assert.assertEquals(timing.controlPoints.size, 5)
        }
    }

    @Test
    fun `Test version 3 beatmap difficulty section`() {
        v3Beatmap.difficulty.apply {
            Assert.assertEquals(ar, 6f, 0f)
            Assert.assertEquals(difficultyCS, 4f, 0f)
            Assert.assertEquals(gameplayCS, 4f, 0f)
            Assert.assertEquals(hp, 6f, 0f)
            Assert.assertEquals(od, 6f, 0f)
            Assert.assertEquals(sliderMultiplier, 1.4, 0.0)
            Assert.assertEquals(sliderTickRate, 2.0, 0.0)
        }
    }

    @Test
    fun `Test version 14 beatmap difficulty section`() {
        v14Beatmap.difficulty.apply {
            Assert.assertEquals(ar, 9f, 0f)
            Assert.assertEquals(difficultyCS, 4f, 0f)
            Assert.assertEquals(gameplayCS, 4f, 0f)
            Assert.assertEquals(hp, 5f, 0f)
            Assert.assertEquals(od, 8f, 0f)
            Assert.assertEquals(sliderMultiplier, 1.9, 0.0)
            Assert.assertEquals(sliderTickRate, 1.0, 0.0)
        }
    }

    @Test
    fun `Test version 3 beatmap events section`() {
        v3Beatmap.events.apply {
            Assert.assertEquals(backgroundFilename, "katamari2.jpg")

            Assert.assertNotNull(backgroundColor)
            Assert.assertEquals(backgroundColor!!.redInt, 54)
            Assert.assertEquals(backgroundColor!!.greenInt, 140)
            Assert.assertEquals(backgroundColor!!.blueInt, 191)

            Assert.assertEquals(breaks.size, 3)
            Assert.assertNull(videoFilename)
            Assert.assertEquals(videoStartTime, 0)
        }
    }

    @Test
    fun `Test version 14 beatmap events section`() {
        v14Beatmap.events.apply {
            Assert.assertEquals(backgroundFilename, "school.jpg")
            Assert.assertNull(backgroundColor)
            Assert.assertTrue(breaks.isEmpty())
            Assert.assertEquals(videoFilename, "Yoasobi.mp4")
            Assert.assertEquals(videoStartTime, -150)
        }
    }

    @Test
    fun `Test version 3 beatmap general section`() {
        v3Beatmap.general.apply {
            Assert.assertEquals(audioFilename, "20.mp3")
            Assert.assertEquals(audioLeadIn, 0)
            Assert.assertEquals(countdown, BeatmapCountdown.Normal)
            Assert.assertFalse(letterboxInBreaks)
            Assert.assertEquals(mode, 0)
            Assert.assertEquals(previewTime, -1)
            Assert.assertEquals(sampleBank, SampleBank.Normal)
            Assert.assertEquals(sampleVolume, 100)
            Assert.assertFalse(samplesMatchPlaybackRate)
        }
    }

    @Test
    fun `Test version 14 beatmap general section`() {
        v14Beatmap.general.apply {
            Assert.assertEquals(audioFilename, "audio.mp3")
            Assert.assertEquals(audioLeadIn, 0)
            Assert.assertEquals(countdown, BeatmapCountdown.NoCountdown)
            Assert.assertFalse(letterboxInBreaks)
            Assert.assertEquals(mode, 0)
            Assert.assertEquals(previewTime, 49037)
            Assert.assertEquals(sampleBank, SampleBank.Soft)
            Assert.assertEquals(sampleVolume, 100)
            Assert.assertFalse(samplesMatchPlaybackRate)
        }
    }

    @Test
    fun `Test version 3 beatmap hit object counters`() {
        v3Beatmap.hitObjects.apply {
            Assert.assertEquals(objects.size, 194)
            Assert.assertEquals(circleCount, 160)
            Assert.assertEquals(sliderCount, 30)
            Assert.assertEquals(sliderTickCount, 0)
            Assert.assertEquals(spinnerCount, 4)
        }
    }

    @Test
    fun `Test version 14 beatmap hit object counters`() {
        v14Beatmap.hitObjects.apply {
            Assert.assertEquals(objects.size, 592)
            Assert.assertEquals(circleCount, 198)
            Assert.assertEquals(sliderCount, 393)
            Assert.assertEquals(sliderTickCount, 0)
            Assert.assertEquals(spinnerCount, 1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Test version 3 beatmap hit object samples`() {
        val circle = v3Beatmap.hitObjects.objects[0] as HitCircle
        Assert.assertEquals(circle.samples.size, 2)

        val (firstSample, lastSample) = circle.samples as MutableList<BankHitSampleInfo>

        firstSample.apply {
            Assert.assertEquals(name, BankHitSampleInfo.HIT_NORMAL)
            Assert.assertEquals(bank, SampleBank.Normal)
            Assert.assertEquals(customSampleBank, 0)
            Assert.assertEquals(volume, 100)
            Assert.assertTrue(isLayered)
        }

        lastSample.apply {
            Assert.assertEquals(name, BankHitSampleInfo.HIT_FINISH)
            Assert.assertEquals(bank, SampleBank.Normal)
            Assert.assertEquals(customSampleBank, 0)
            Assert.assertEquals(volume, 100)
            Assert.assertFalse(isLayered)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun `Test version 14 beatmap hit object samples`() {
        val slider = v14Beatmap.hitObjects.objects[1] as Slider
        Assert.assertEquals(slider.samples.size, 2)

        val (firstSample, lastSample) = slider.samples as MutableList<BankHitSampleInfo>

        firstSample.apply {
            Assert.assertEquals(name, BankHitSampleInfo.HIT_NORMAL)
            Assert.assertEquals(bank, SampleBank.Soft)
            Assert.assertEquals(customSampleBank, 1)
            Assert.assertEquals(volume, 40)
            Assert.assertTrue(isLayered)
        }

        lastSample.apply {
            Assert.assertEquals(name, BankHitSampleInfo.HIT_CLAP)
            Assert.assertEquals(bank, SampleBank.Drum)
            Assert.assertEquals(customSampleBank, 1)
            Assert.assertEquals(volume, 40)
            Assert.assertFalse(isLayered)
        }
    }

    @Test
    fun `Test version 3 beatmap hit object node samples`() {
        val slider = v3Beatmap.hitObjects.objects[19] as Slider

        for (nodeSample in slider.nodeSamples) {
            (nodeSample[0] as BankHitSampleInfo).apply {
                Assert.assertEquals(name, BankHitSampleInfo.HIT_NORMAL)
                Assert.assertEquals(bank, SampleBank.Normal)
                Assert.assertEquals(customSampleBank, 0)
                Assert.assertEquals(volume, 100)
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
                Assert.assertEquals(name, BankHitSampleInfo.HIT_NORMAL)
                Assert.assertEquals(bank, SampleBank.Soft)
                Assert.assertEquals(customSampleBank, 1)
                Assert.assertEquals(volume, 40)
                Assert.assertTrue(isLayered)
            }

            lastSample.apply {
                Assert.assertEquals(name, BankHitSampleInfo.HIT_CLAP)
                Assert.assertEquals(bank, SampleBank.Drum)
                Assert.assertEquals(customSampleBank, 1)
                Assert.assertEquals(volume, 40)
                Assert.assertFalse(isLayered)
            }
        }
    }

    @Test
    fun `Test version 3 beatmap metadata section`() {
        v3Beatmap.metadata.apply {
            Assert.assertEquals(artist, "Kenji Ninuma")
            Assert.assertTrue(artistUnicode.isEmpty())
            Assert.assertEquals(beatmapId, -1)
            Assert.assertEquals(beatmapSetId, -1)
            Assert.assertEquals(creator, "peppy")
            Assert.assertEquals(title, "DISCO★PRINCE")
            Assert.assertTrue(titleUnicode.isEmpty())
            Assert.assertEquals(version, "Normal")
            Assert.assertTrue(source.isEmpty())
            Assert.assertTrue(tags.isEmpty())
        }
    }

    @Test
    fun `Test version 14 beatmap metadata section`() {
        v14Beatmap.metadata.apply {
            Assert.assertEquals(artist, "YOASOBI")
            Assert.assertEquals(artistUnicode, "YOASOBI")
            Assert.assertEquals(beatmapId, 3324715)
            Assert.assertEquals(beatmapSetId, 1585863)
            Assert.assertEquals(creator, "ohm002")
            Assert.assertEquals(title, "Love Letter")
            Assert.assertEquals(titleUnicode, "ラブレター")
            Assert.assertEquals(version, "Please accept my overflowing emotions.")
            Assert.assertTrue(source.isEmpty())
            Assert.assertTrue(tags.isNotEmpty())
        }
    }

    @Test
    fun `Test max combo`() {
        Assert.assertEquals(v3Beatmap.maxCombo, 314)
        Assert.assertEquals(v14Beatmap.maxCombo, 1033)
    }

    @Test
    fun `Test NaN control points`() {
        nanBeatmap.controlPoints.apply {
            Assert.assertEquals(timing.controlPoints.size, 1)
            Assert.assertEquals(difficulty.controlPoints.size, 2)

            Assert.assertEquals(timing.controlPointAt(1000.0).msPerBeat, 500.0, 0.0)

            difficulty.controlPointAt(2000.0).apply {
                Assert.assertEquals(speedMultiplier, 1.0, 0.0)
                Assert.assertFalse(generateTicks)
            }

            difficulty.controlPointAt(3000.0).apply {
                Assert.assertEquals(speedMultiplier, 1.0, 0.0)
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