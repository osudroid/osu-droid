package com.osudroid.replay

import com.osudroid.GameMode
import com.osudroid.beatmaps.Beatmap
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.hitobjects.sliderobject.SliderTick
import com.osudroid.math.Vector2
import java.util.BitSet
import org.junit.Assert
import org.junit.Test
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import ru.nsu.ccfit.zuev.osu.scoring.ResultType
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class ReplayPopulateStatisticsTest {
    @Test
    fun `Test hit slider ticks count as tick hits in a non-300 slider`() {
        val beatmap = Beatmap(GameMode.Droid)
        val slider = Slider(
            0.0,
            Vector2(0),
            0,
            SliderPath(SliderPathType.Linear, listOf(Vector2(0), Vector2(200, 0)), 200.0),
            true,
            0,
            mutableListOf()
        )

        slider.applyDefaults(beatmap.controlPoints, beatmap.difficulty, beatmap.mode)
        beatmap.hitObjects.add(slider)

        // Sanity check: head, tick, tail.
        Assert.assertEquals(3, slider.nestedHitObjects.size)
        Assert.assertTrue(slider.nestedHitObjects[1] is SliderTick)

        val replay = Replay()
        replay.replayVersion = 9
        replay.setObjectCount(1)

        // Head hit on time, tick hit, tail missed - a 100.
        val tickSet = BitSet()
        tickSet.set(0, true)
        tickSet.set(1, false)

        replay.addObjectResult(0, 0, tickSet)
        replay.addObjectScore(0, ResultType.HIT100)

        // StatisticV2 reads the online username when constructed.
        Config.setOnlineUsername("tester")

        val stat = StatisticV2()
        replay.populateStatistics(beatmap, stat)

        Assert.assertEquals(1, stat.sliderHeadHits)
        Assert.assertEquals(1, stat.sliderTickHits)
        Assert.assertEquals(0, stat.sliderRepeatHits)
        Assert.assertEquals(0, stat.sliderEndHits)
    }
}
