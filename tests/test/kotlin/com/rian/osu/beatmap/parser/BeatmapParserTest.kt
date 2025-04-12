package com.rian.osu.beatmap.parser

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
import ru.nsu.ccfit.zuev.osu.RGBColor

@RunWith(RobolectricTestRunner::class)
class BeatmapParserTest {
    @Test
    fun testFormatVersionHeader() {
        Assert.assertEquals(v3Beatmap.formatVersion, 3)
        Assert.assertEquals(v14Beatmap.formatVersion, 14)
    }

    @Test
    fun testV3BeatmapColorsSection() {
        Assert.assertTrue(v3Beatmap.colors.comboColors.isEmpty())
    }

    @Test
    fun testV14BeatmapColorsSection() {
        fun test(color: RGBColor, red: Float, green: Float, blue: Float) {
            Assert.assertEquals(color.r(), red, 0f)
            Assert.assertEquals(color.g(), green, 0f)
            Assert.assertEquals(color.b(), blue, 0f)
        }

        val colors = v14Beatmap.colors

        Assert.assertEquals(colors.comboColors.size, 4)

        test(colors.comboColors[0], 98f, 243f, 255f)
        test(colors.comboColors[1], 251f, 170f, 251f)
        test(colors.comboColors[2], 102f, 171f, 255f)
        test(colors.comboColors[3], 162f, 205f, 232f)

        Assert.assertNull(colors.sliderBorderColor)
    }

    @Test
    fun testV3BeatmapControlPointsSection() {
        v3Beatmap.controlPoints.apply {
            Assert.assertTrue(difficulty.controlPoints.isEmpty())
            Assert.assertTrue(effect.controlPoints.isEmpty())
            Assert.assertTrue(sample.controlPoints.isEmpty())
            Assert.assertEquals(timing.controlPoints.size, 1)
        }
    }

    @Test
    fun testV14BeatmapControlPointsSection() {
        v14Beatmap.controlPoints.apply {
            Assert.assertEquals(difficulty.controlPoints.size, 14)
            Assert.assertEquals(effect.controlPoints.size, 14)
            Assert.assertEquals(sample.controlPoints.size, 526)
            Assert.assertEquals(timing.controlPoints.size, 5)
        }
    }

    @Test
    fun testV3BeatmapDifficultySection() {
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
    fun testV14BeatmapDifficultySection() {
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
    fun testV3BeatmapEventsSection() {
        v3Beatmap.events.apply {
            Assert.assertEquals(backgroundFilename, "katamari2.jpg")

            Assert.assertNotNull(backgroundColor)
            Assert.assertEquals(backgroundColor!!.r(), 54f, 0f)
            Assert.assertEquals(backgroundColor!!.g(), 140f, 0f)
            Assert.assertEquals(backgroundColor!!.b(), 191f, 0f)

            Assert.assertEquals(breaks.size, 3)
            Assert.assertNull(videoFilename)
            Assert.assertEquals(videoStartTime, 0)
        }
    }

    @Test
    fun testV14BeatmapEventsSection() {
        v14Beatmap.events.apply {
            Assert.assertEquals(backgroundFilename, "school.jpg")
            Assert.assertNull(backgroundColor)
            Assert.assertTrue(breaks.isEmpty())
            Assert.assertEquals(videoFilename, "Yoasobi.mp4")
            Assert.assertEquals(videoStartTime, -150)
        }
    }

    @Test
    fun testV3BeatmapGeneralSection() {
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
    fun testV14BeatmapGeneralSection() {
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
    fun testV3BeatmapHitObjectCounters() {
        v3Beatmap.hitObjects.apply {
            Assert.assertEquals(objects.size, 194)
            Assert.assertEquals(circleCount, 160)
            Assert.assertEquals(sliderCount, 30)
            Assert.assertEquals(spinnerCount, 4)
        }
    }

    @Test
    fun testV14BeatmapHitObjectCounters() {
        v14Beatmap.hitObjects.apply {
            Assert.assertEquals(objects.size, 592)
            Assert.assertEquals(circleCount, 198)
            Assert.assertEquals(sliderCount, 393)
            Assert.assertEquals(spinnerCount, 1)
        }
    }

    @Suppress("UNCHECKED_CAST")
    @Test
    fun testV3BeatmapHitObjectSamples() {
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
    fun testV14BeatmapHitObjectSamples() {
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
    fun testV3BeatmapHitObjectNodeSamples() {
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
    fun testV14BeatmapHitObjectNodeSamples() {
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
    fun testV3BeatmapMetadataSection() {
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
    fun testV14BeatmapMetadataSection() {
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
    fun testMaxCombo() {
        Assert.assertEquals(v3Beatmap.maxCombo, 314)
        Assert.assertEquals(v14Beatmap.maxCombo, 1033)
    }

    @Test
    fun testNaNControlPoints() {
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