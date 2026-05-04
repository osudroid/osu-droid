package com.osudroid.utils

import com.osudroid.GameMode
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.mods.Mod
import com.osudroid.mods.ModAutoplay
import com.osudroid.mods.ModCustomSpeed
import com.osudroid.mods.ModDoubleTime
import com.osudroid.mods.ModHardRock
import com.osudroid.mods.ModHidden
import com.osudroid.mods.ModNightCore
import com.osudroid.mods.ModOldNightCore
import com.osudroid.mods.ModPrecise
import com.osudroid.mods.ModReplayV6
import org.junit.Assert
import org.junit.Test

class ModUtilsTest {
    @Test
    fun `Test mod serialization with non-user playable mods`() {
        val serializedMods = ModUtils.serializeMods(
            listOf(ModAutoplay(), ModCustomSpeed(1.25f), ModHidden(), ModReplayV6())
        )

        ModUtils.deserializeMods(serializedMods).apply {
            Assert.assertEquals(4, size)
            Assert.assertTrue(ModAutoplay::class in this)
            Assert.assertTrue(ModCustomSpeed::class in this)
            Assert.assertTrue(ModHidden::class in this)
            Assert.assertTrue(ModReplayV6::class in this)

            Assert.assertEquals(1.25f, ofType<ModCustomSpeed>()!!.trackRateMultiplier, 0f)
        }
    }

    @Test
    fun `Test mod serialization without non-user playable mods`() {
        val serializedMods = ModUtils.serializeMods(
            listOf(ModAutoplay(), ModCustomSpeed(1.25f), ModHidden(), ModReplayV6()),
            false
        )

        ModUtils.deserializeMods(serializedMods).apply {
            Assert.assertEquals(3, size)
            Assert.assertTrue(ModAutoplay::class in this)
            Assert.assertTrue(ModCustomSpeed::class in this)
            Assert.assertTrue(ModHidden::class in this)
            Assert.assertFalse(ModReplayV6::class in this)

            Assert.assertEquals(1.25f, ofType<ModCustomSpeed>()!!.trackRateMultiplier, 0f)
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

        ModUtils.deserializeMods(serializedMods).apply {
            Assert.assertEquals(3, size)
            Assert.assertTrue(ModAutoplay::class in this)
            Assert.assertTrue(ModCustomSpeed::class in this)
            Assert.assertTrue(ModHidden::class in this)

            Assert.assertEquals(1.25f, ofType<ModCustomSpeed>()!!.trackRateMultiplier, 0f)
        }
    }

    @Test
    fun `Test rate calculation with mods`() {
        fun test(expectedRate: Float, vararg mods: Mod) =
            Assert.assertEquals(expectedRate, ModUtils.calculateRateWithMods(mods.toList()), 0f)

        test(1f)
        test(1.25f, ModCustomSpeed(1.25f))
        test(1.3781248f, ModOldNightCore())
        test(1.5f, ModDoubleTime())
        test(3f, ModNightCore(), ModCustomSpeed(2f))
    }

    @Test
    fun `Test applying mods to beatmap difficulty`() {
        data class Case(
            val original: BeatmapDifficulty,
            val expected: BeatmapDifficulty,
            val mode: GameMode,
            val mods: List<Mod>
        )

        listOf(
            Case(
                BeatmapDifficulty(cs = 5f),
                BeatmapDifficulty(cs = 5f),
                GameMode.Standard,
                listOf(ModAutoplay())
            ),
            Case(
                BeatmapDifficulty(ar = 9f),
                BeatmapDifficulty(cs = 6.5f, ar = 10f, od = 7f, hp = 7f),
                GameMode.Standard,
                listOf(ModHardRock())
            ),
            Case(
                BeatmapDifficulty(ar = 9f, od = 9f),
                BeatmapDifficulty(ar = 9f, od = 9f),
                GameMode.Standard,
                listOf(ModDoubleTime())
            ),
            Case(
                BeatmapDifficulty(ar = 9f, od = 9f),
                BeatmapDifficulty(cs = 6.5f, ar = 10f, od = 10f, hp = 7f),
                GameMode.Standard,
                listOf(ModDoubleTime(), ModHardRock())
            ),
            Case(
                BeatmapDifficulty(od = 10f),
                BeatmapDifficulty(od = 10f),
                GameMode.Droid,
                listOf(ModPrecise())
            )
        ).forEach { (original, expected, mode, mods) ->
            ModUtils.applyModsToBeatmapDifficulty(original, mode, mods)

            Assert.assertEquals(expected.difficultyCS, original.difficultyCS, 1e-2f)
            Assert.assertEquals(expected.gameplayCS, original.gameplayCS, 1e-2f)
            Assert.assertEquals(expected.ar, original.ar, 1e-2f)
            Assert.assertEquals(expected.od, original.od, 1e-2f)
            Assert.assertEquals(expected.hp, original.hp, 1e-2f)
        }
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