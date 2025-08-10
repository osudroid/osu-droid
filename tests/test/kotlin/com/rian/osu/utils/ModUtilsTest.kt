package com.rian.osu.utils

import com.rian.osu.GameMode
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModAutoplay
import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModNightCore
import com.rian.osu.mods.ModOldNightCore
import com.rian.osu.mods.ModPrecise
import com.rian.osu.mods.ModReplayV6
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModUtilsTest {
    @Test
    fun `Test mod serialization with non-user playable mods`() {
        ModUtils.serializeMods(listOf(ModAutoplay(), ModCustomSpeed(1.25f), ModHidden(), ModReplayV6())).apply {
            Assert.assertEquals(length(), 4)
            Assert.assertEquals(getJSONObject(0).getString("acronym"), "AT")
            Assert.assertEquals(getJSONObject(1).getString("acronym"), "CS")

            Assert.assertEquals(
                getJSONObject(1).getJSONObject("settings").getDouble("rateMultiplier").toFloat(),
                1.25f,
                0.01f
            )

            Assert.assertEquals(getJSONObject(2).getString("acronym"), "HD")
            Assert.assertEquals(getJSONObject(3).getString("acronym"), "RV6")
        }
    }

    @Test
    fun `Test mod serialization without non-user playable mods`() {
        ModUtils.serializeMods(listOf(ModAutoplay(), ModCustomSpeed(1.25f), ModHidden(), ModReplayV6()), false).apply {
            Assert.assertEquals(length(), 3)
            Assert.assertEquals(getJSONObject(0).getString("acronym"), "AT")
            Assert.assertEquals(getJSONObject(1).getString("acronym"), "CS")

            Assert.assertEquals(
                getJSONObject(1).getJSONObject("settings").getDouble("rateMultiplier").toFloat(),
                1.25f,
                0.01f
            )

            Assert.assertEquals(getJSONObject(2).getString("acronym"), "HD")
        }
    }

    @Test
    fun `Test mod deserialization`() {
        val serializedMods = ModUtils.serializeMods(
            listOf(
                ModAutoplay(),
                ModCustomSpeed(1.25f),
                ModHidden()
            )
        )

        val deserializedMods = ModUtils.deserializeMods(serializedMods)

        Assert.assertEquals(deserializedMods.size, 3)
        Assert.assertTrue(ModAutoplay::class in deserializedMods)
        Assert.assertTrue(ModCustomSpeed::class in deserializedMods)
        Assert.assertTrue(ModHidden::class in deserializedMods)

        val customSpeed = deserializedMods.ofType<ModCustomSpeed>()
        Assert.assertNotNull(customSpeed)
        Assert.assertEquals(customSpeed!!.trackRateMultiplier, 1.25f, 0f)
    }

    @Test
    fun `Test rate calculation with mods`() {
        fun test(expectedRate: Float, vararg mods: Mod) =
            Assert.assertEquals(expectedRate, ModUtils.calculateRateWithMods(mods.toList()), 0f)

        test(1f)
        test(1.25f, ModCustomSpeed(1.25f))
        test(1.39f, ModOldNightCore())
        test(1.5f, ModDoubleTime())
        test(3f, ModNightCore(), ModCustomSpeed(2f))
    }

    @Test
    fun `Test applying mods to beatmap difficulty`() {
        fun test(original: BeatmapDifficulty, expected: BeatmapDifficulty, mode: GameMode, vararg mods: Mod) {
            ModUtils.applyModsToBeatmapDifficulty(original, mode, mods.toList())

            Assert.assertEquals(original.difficultyCS, expected.difficultyCS, 1e-2f)
            Assert.assertEquals(original.gameplayCS, expected.gameplayCS, 1e-2f)
            Assert.assertEquals(original.ar, expected.ar, 1e-2f)
            Assert.assertEquals(original.od, expected.od, 1e-2f)
            Assert.assertEquals(original.hp, expected.hp, 1e-2f)
        }

        test(BeatmapDifficulty(cs = 5f), BeatmapDifficulty(cs = 5f), GameMode.Standard, ModAutoplay())

        test(
            BeatmapDifficulty(ar = 9f),
            BeatmapDifficulty(cs = 6.5f, ar = 10f, od = 7f, hp = 7f),
            GameMode.Standard,
            ModHardRock()
        )

        test(
            BeatmapDifficulty(ar = 9f, od = 9f), BeatmapDifficulty(ar = 9f, od = 9f), GameMode.Standard,
            ModDoubleTime()
        )

        test(
            BeatmapDifficulty(ar = 9f, od = 9f),
            BeatmapDifficulty(cs = 6.5f, ar = 10f, od = 10f, hp = 7f),
            GameMode.Standard,
            ModDoubleTime(),
            ModHardRock()
        )

        test(BeatmapDifficulty(od = 10f), BeatmapDifficulty(od = 10f), GameMode.Droid, ModPrecise())
    }

    @Test
    fun `Test score multiplier calculation with multiple ModRateAdjust mods`() {
        val mods = listOf(
            ModHidden(),
            ModDoubleTime(),
            ModCustomSpeed(0.85f),
            ModPrecise()
        )

        Assert.assertEquals(1.1977575f, ModUtils.calculateScoreMultiplier(mods), 1e-6f)
    }
}