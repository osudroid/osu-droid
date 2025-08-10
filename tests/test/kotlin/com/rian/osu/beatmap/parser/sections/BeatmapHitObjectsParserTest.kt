package com.rian.osu.beatmap.parser.sections

import com.rian.osu.GameMode
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.constants.SampleBank
import com.rian.osu.beatmap.hitobject.BankHitSampleInfo
import com.rian.osu.beatmap.hitobject.Slider
import org.junit.Assert
import org.junit.Test

class BeatmapHitObjectsParserTest {
    @Test
    fun `Test hit sample addition bank defaulting to normal bank if not specified`() {
        val beatmap = Beatmap(GameMode.Standard)
        val line = "461,307,72830,6,0,L|476:225,1,78.7500030040742,4|0,3:0|0:0,0:0:0:0:"

        BeatmapHitObjectsParser.parse(beatmap, line, null)

        val obj = beatmap.hitObjects.objects[0]
        Assert.assertTrue(obj is Slider)

        obj as Slider

        val samples = obj.nodeSamples[0]

        Assert.assertEquals(2, samples.size)

        val firstSample = samples[0]
        val secondSample = samples[1]

        Assert.assertTrue(firstSample is BankHitSampleInfo)
        Assert.assertTrue(secondSample is BankHitSampleInfo)

        firstSample as BankHitSampleInfo
        secondSample as BankHitSampleInfo

        Assert.assertEquals(SampleBank.Drum, firstSample.bank)
        Assert.assertEquals(SampleBank.Drum, secondSample.bank)
    }
}