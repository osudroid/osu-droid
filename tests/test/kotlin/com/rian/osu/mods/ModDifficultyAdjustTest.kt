package com.rian.osu.mods

import com.rian.osu.GameMode
import com.rian.osu.beatmap.hitobject.Slider
import com.rian.osu.beatmap.hitobject.SliderPath
import com.rian.osu.beatmap.hitobject.SliderPathType
import com.rian.osu.beatmap.sections.BeatmapControlPoints
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.math.Vector2
import com.rian.osu.mods.settings.NullableFloatModSetting
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModDifficultyAdjustTest {
    @Test
    fun `Test beatmap setting override without additional mods`() {
        BeatmapDifficulty().apply {
            ModDifficultyAdjust(cs = 4f, ar = 8f, od = 7f, hp = 6f).applyToDifficulty(GameMode.Droid, this, listOf())

            Assert.assertEquals(4f, difficultyCS)
            Assert.assertEquals(4f, gameplayCS)
            Assert.assertEquals(8f, ar)
            Assert.assertEquals(7f, od)
            Assert.assertEquals(6f, hp)
        }
    }

    @Test
    fun `Test beatmap setting override with additional mods`() {
        BeatmapDifficulty().apply {
            ModDifficultyAdjust(cs = 6f, ar = 6f, od = 6f, hp = 6f).applyToDifficulty(
                GameMode.Droid, this, listOf(ModHardRock(), ModReallyEasy())
            )

            Assert.assertEquals(6f, difficultyCS)
            Assert.assertEquals(6f, gameplayCS)
            Assert.assertEquals(6f, ar)
            Assert.assertEquals(6f, od)
            Assert.assertEquals(6f, hp)
        }
    }

    @Test
    fun `Test AR override with non-1x speed multiplier`() {
        BeatmapDifficulty().apply {
            ModDifficultyAdjust(ar = 9f).applyToDifficulty(GameMode.Droid, this, listOf(ModDoubleTime()))

            Assert.assertEquals(9f, ar)
        }
    }

    @Test
    fun `Test AR override with non-1x speed multiplier with old scaling`() {
        BeatmapDifficulty().apply {
            ModDifficultyAdjust(ar = 9f).applyToDifficulty(GameMode.Droid, this, listOf(ModDoubleTime(), ModReplayV6()))

            Assert.assertEquals(7f, ar)
        }
    }

    @Test
    fun `Test object fade in adjustments with non-1x speed multiplier AR override`() {
        val mods = listOf(ModDoubleTime())
        val difficulty = BeatmapDifficulty()
        val difficultyAdjust = ModDifficultyAdjust(ar = 9f)

        difficultyAdjust.applyToDifficulty(GameMode.Droid, difficulty, mods)

        Slider(
            0.0, Vector2(0), 0, SliderPath(
                SliderPathType.Linear, listOf(Vector2(0), Vector2(256, 0)), 256.0
            ), true, 0, mutableListOf()
        ).apply {
            applyDefaults(BeatmapControlPoints(), difficulty, GameMode.Droid)
            difficultyAdjust.applyToHitObject(GameMode.Droid, this, mods)

            Assert.assertEquals(timePreempt, 600.0, 1e-2)
            Assert.assertEquals(timeFadeIn, 400.0, 1e-2)

            Assert.assertEquals(head.timePreempt, 600.0, 1e-2)
            Assert.assertEquals(head.timeFadeIn, 400.0, 1e-2)

            Assert.assertTrue(nestedHitObjects.size > 2)

            val tick = nestedHitObjects[1]

            Assert.assertEquals(tick.timePreempt, 896.0, 1e-2)
            Assert.assertEquals(tick.timeFadeIn, 400.0, 1e-2)
        }
    }

    @Test
    fun `Test object fade in adjustments with non-1x speed multiplier AR override with old scaling`() {
        val mods = listOf(ModDoubleTime(), ModReplayV6())
        val difficulty = BeatmapDifficulty()
        val difficultyAdjust = ModDifficultyAdjust(ar = 9f)

        difficultyAdjust.applyToDifficulty(GameMode.Droid, difficulty, mods)

        Slider(
            0.0, Vector2(0), 0, SliderPath(
                SliderPathType.Linear, listOf(Vector2(0), Vector2(256, 0)), 256.0
            ), true, 0, mutableListOf()
        ).apply {
            applyDefaults(BeatmapControlPoints(), difficulty, GameMode.Droid)
            difficultyAdjust.applyToHitObject(GameMode.Droid, this, mods)

            Assert.assertEquals(timePreempt, 900.0, 1e-2)
            Assert.assertEquals(timeFadeIn, 600.0, 1e-2)

            Assert.assertEquals(head.timePreempt, 900.0, 1e-2)
            Assert.assertEquals(head.timeFadeIn, 600.0, 1e-2)

            Assert.assertTrue(nestedHitObjects.size > 2)

            val tick = nestedHitObjects[1]

            Assert.assertEquals(tick.timePreempt, 1094.0, 1e-2)
            Assert.assertEquals(tick.timeFadeIn, 600.0, 1e-2)
        }
    }

    @Test
    fun `Test serialization`() {
        ModDifficultyAdjust().apply {
            serialize().apply {
                Assert.assertNull(optJSONObject("settings"))
            }

            cs = 4f
            ar = 9f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(4f, getDouble("cs").toFloat())
                Assert.assertEquals(9f, getDouble("ar").toFloat())
                Assert.assertTrue(optDouble("od").isNaN())
                Assert.assertTrue(optDouble("hp").isNaN())
            }

            od = 8f
            hp = 6f

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(4f, getDouble("cs").toFloat())
                Assert.assertEquals(9f, getDouble("ar").toFloat())
                Assert.assertEquals(8f, getDouble("od").toFloat())
                Assert.assertEquals(6f, getDouble("hp").toFloat())
            }
        }
    }

    @Test
    fun `Test toString`() {
        ModDifficultyAdjust().apply {
            Assert.assertEquals("DA", toString())

            cs = 4f
            ar = 9f
            od = 8f
            hp = 6f

            Assert.assertEquals("DA (CS4.0, AR9.0, OD8.0, HP6.0)", toString())
        }
    }

    @Test
    fun `Test deep copy`() {
        ModDifficultyAdjust(4f, 9f, 8f, 6f).apply {
            getModSettingDelegate<NullableFloatModSetting>(::cs).defaultValue = 2f
            getModSettingDelegate<NullableFloatModSetting>(::ar).defaultValue = 3f
            getModSettingDelegate<NullableFloatModSetting>(::od).defaultValue = 5f
            getModSettingDelegate<NullableFloatModSetting>(::hp).defaultValue = 1f

            val copy = deepCopy()

            Assert.assertNotSame(this, copy)

            Assert.assertEquals(4f, copy.cs)
            Assert.assertEquals(9f, copy.ar)
            Assert.assertEquals(8f, copy.od)
            Assert.assertEquals(6f, copy.hp)

            Assert.assertEquals(2f, getModSettingDelegate<NullableFloatModSetting>(copy::cs).defaultValue)
            Assert.assertEquals(3f, getModSettingDelegate<NullableFloatModSetting>(copy::ar).defaultValue)
            Assert.assertEquals(5f, getModSettingDelegate<NullableFloatModSetting>(copy::od).defaultValue)
            Assert.assertEquals(1f, getModSettingDelegate<NullableFloatModSetting>(copy::hp).defaultValue)
        }
    }

    @Test
    fun `Test compatibility with difficulty-adjusting mods`() {
        // Theoretically, Small Circle should be here, but it is only dependent on the CS value
        // and will be migrated into Difficulty Adjust anyway.
        val difficultyAdjustingMods = listOf(
            ModHardRock(),
            ModEasy(),
            ModReallyEasy()
        )

        val difficultyAdjust = ModDifficultyAdjust(cs = 4f, ar = 9f, od = 7f)
        difficultyAdjustingMods.forEach { Assert.assertTrue(difficultyAdjust.isCompatibleWith(it)) }

        difficultyAdjust.hp = 6f
        difficultyAdjustingMods.forEach { Assert.assertFalse(it.isCompatibleWith(difficultyAdjust)) }
    }

    @Test
    fun `Test default settings`() {
        Assert.assertTrue(ModDifficultyAdjust().usesDefaultSettings)
        Assert.assertFalse(ModDifficultyAdjust(cs = 4f).usesDefaultSettings)
    }
}