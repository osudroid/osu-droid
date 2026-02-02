package com.rian.osu.beatmap

import com.rian.osu.beatmap.parser.BeatmapParser
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StackingTest {
    @Test
    fun `Test stacking edge case one`() {
        val beatmapFile = TestResourceManager.getBeatmapFile("stacking-edge-case-one")!!
        val beatmap = BeatmapParser(beatmapFile).parse(true)!!

        val objects = beatmap.hitObjects.objects

        // The last hitobject triggers the stacking.
        for (i in 0 until objects.size - 1) {
            Assert.assertEquals(0, objects[i].difficultyStackHeight)
            Assert.assertEquals(0, objects[i].gameplayStackHeight)
        }
    }

    @Test
    fun `Test stacking edge case two`() {
        val beatmapFile = TestResourceManager.getBeatmapFile("stacking-edge-case-two")!!
        val beatmap = BeatmapParser(beatmapFile).parse(true)!!

        val objects = beatmap.hitObjects.objects

        Assert.assertEquals(3, objects.size)

        // The last hitobject triggers the stacking.
        for (i in 0 until objects.size - 1) {
            Assert.assertEquals(0, objects[i].difficultyStackHeight)
            Assert.assertEquals(0, objects[i].gameplayStackHeight)
        }
    }
}