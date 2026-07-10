package com.osudroid.mods

import com.osudroid.GameMode
import com.osudroid.beatmaps.hitobjects.Slider
import com.osudroid.beatmaps.hitobjects.SliderPath
import com.osudroid.beatmaps.hitobjects.SliderPathType
import com.osudroid.beatmaps.sections.BeatmapControlPoints
import com.osudroid.beatmaps.sections.BeatmapDifficulty
import com.osudroid.math.Vector2
import com.osudroid.mods.settings.DifficultyAdjustModSetting
import com.osudroid.utils.ModUtils
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test

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

    @Test
    fun `Test serialization writes scalar value`() {
        ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
        }.toAPIMod().settings!!.apply {
            Assert.assertEquals(7f, get("cs")!!.jsonPrimitive.float, 0f)
            Assert.assertEquals(9f, get("od")!!.jsonPrimitive.float, 0f)
        }
    }

    @Test
    fun `Test serialization omits null settings`() {
        ModDifficultyAdjust().also { it.cs = 7f }.toAPIMod().settings!!.apply {
            Assert.assertEquals(7f, get("cs")!!.jsonPrimitive.float, 0f)
            Assert.assertNull(get("od"))
        }
    }

    @Test
    fun `Test deserialization of old object format`() {
        val json = """[{"acronym":"DA","settings":{"cs":{"adjusted":7.0,"original":4.0},"od":{"adjusted":9.0,"original":8.0}}}]"""
        val deserialized = ModUtils.deserializeMods(json).ofType<ModDifficultyAdjust>()!!

        Assert.assertEquals(7f, deserialized.cs)
        Assert.assertEquals(4f, deserialized.csDelegate().defaultValue)
        Assert.assertEquals(9f, deserialized.od)
        Assert.assertEquals(8f, deserialized.odDelegate().defaultValue)
    }

    @Test
    fun `Test deserialization of old scalar format`() {
        val json = """[{"acronym":"DA","settings":{"cs":7.0,"od":9.0}}]"""
        val deserialized = ModUtils.deserializeMods(json).ofType<ModDifficultyAdjust>()!!

        Assert.assertEquals(7f, deserialized.cs)
        Assert.assertEquals(9f, deserialized.od)
    }

    @Test
    fun `Test deserialization of old object format with null original`() {
        val json = """[{"acronym":"DA","settings":{"cs":{"adjusted":7.0,"original":null}}}]"""
        val deserialized = ModUtils.deserializeMods(json).ofType<ModDifficultyAdjust>()!!

        Assert.assertEquals(7f, deserialized.cs)
        Assert.assertNull(deserialized.csDelegate().defaultValue)
    }

    @Test
    fun `Test deep copy preserves values`() {
        val mod = ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
        }

        val copy = mod.deepCopy() as ModDifficultyAdjust

        Assert.assertEquals(7f, copy.cs)
        Assert.assertEquals(9f, copy.od)
    }

    private fun ModDifficultyAdjust.csDelegate() = getModSettingDelegate<DifficultyAdjustModSetting>(::cs)
    private fun ModDifficultyAdjust.odDelegate() = getModSettingDelegate<DifficultyAdjustModSetting>(::od)
}
