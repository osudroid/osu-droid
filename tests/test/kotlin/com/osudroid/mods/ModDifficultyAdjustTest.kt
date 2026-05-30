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
import kotlin.math.pow
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonObject
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
    fun `Test serialization embeds original value`() {
        // Set adjusted values after construction so value != defaultValue (isDefault = false).
        ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
            it.setOriginals(cs = 4f, od = 8f)
        }.toAPIMod().settings!!.apply {
            Assert.assertEquals(7f, get("cs")!!.jsonObject["adjusted"]!!.jsonPrimitive.float, 0f)
            Assert.assertEquals(4f, get("cs")!!.jsonObject["original"]!!.jsonPrimitive.float, 0f)
            Assert.assertEquals(9f, get("od")!!.jsonObject["adjusted"]!!.jsonPrimitive.float, 0f)
            Assert.assertEquals(8f, get("od")!!.jsonObject["original"]!!.jsonPrimitive.float, 0f)
        }
    }

    @Test
    fun `Test serialization writes null original when beatmap is unknown`() {
        ModDifficultyAdjust().also { it.cs = 7f }.toAPIMod().settings!!.apply {
            Assert.assertEquals(7f, get("cs")!!.jsonObject["adjusted"]!!.jsonPrimitive.float, 0f)
            Assert.assertSame(JsonNull, get("cs")!!.jsonObject["original"])
        }
    }

    @Test
    fun `Test deserialization of new format`() {
        val mod = ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
            it.setOriginals(cs = 4f, od = 8f)
        }

        val deserialized = mod.roundTrip()

        Assert.assertEquals(7f, deserialized.cs)
        Assert.assertEquals(4f, deserialized.csDelegate().originalValue)
        Assert.assertEquals(9f, deserialized.od)
        Assert.assertEquals(8f, deserialized.odDelegate().originalValue)
    }

    @Test
    fun `Test deserialization of old scalar format`() {
        val json = """[{"acronym":"DA","settings":{"cs":7.0,"od":9.0}}]"""
        val deserialized = ModUtils.deserializeMods(json).ofType<ModDifficultyAdjust>()!!

        Assert.assertEquals(7f, deserialized.cs)
        Assert.assertNull(deserialized.csDelegate().originalValue)
        Assert.assertEquals(9f, deserialized.od)
        Assert.assertNull(deserialized.odDelegate().originalValue)
    }

    @Test
    fun `Test deserialization of new format with null original`() {
        val json = """[{"acronym":"DA","settings":{"cs":{"adjusted":7.0,"original":null}}}]"""
        val deserialized = ModUtils.deserializeMods(json).ofType<ModDifficultyAdjust>()!!

        Assert.assertEquals(7f, deserialized.cs)
        Assert.assertNull(deserialized.csDelegate().originalValue)
    }

    @Test
    fun `Test score multiplier uses embedded original`() {
        val mod = ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
            it.setOriginals(cs = 4f, od = 8f)
        }

        val csDiff = 7f - 4f
        val odDiff = 9f - 8f
        val expected = (1 + 0.0075f * csDiff.toDouble().pow(1.5).toFloat()) *
                       (1 + 0.005f  * odDiff.toDouble().pow(1.3).toFloat())

        Assert.assertEquals(expected, mod.scoreMultiplier, 1e-6f)
    }

    @Test
    fun `Test score multiplier is 1 without original or default`() {
        // value is set but both originalValue and defaultValue are null, so there is no delta to compute.
        val mod = ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
        }

        Assert.assertEquals(1f, mod.scoreMultiplier, 0f)
    }

    @Test
    fun `Test score multiplier falls back to defaultValue when original is absent`() {
        val mod = ModDifficultyAdjust().also {
            it.cs = 7f
            // Simulate applyFromBeatmap without embedded original.
            it.csDelegate().defaultValue = 4f
        }

        val diff = 7f - 4f
        val expected = 1 + 0.0075f * diff.toDouble().pow(1.5).toFloat()

        Assert.assertEquals(expected, mod.scoreMultiplier, 1e-6f)
    }

    @Test
    fun `Test deep copy preserves original values`() {
        val mod = ModDifficultyAdjust().also {
            it.cs = 7f
            it.od = 9f
            it.setOriginals(cs = 4f, od = 8f)
        }

        val copy = mod.deepCopy() as ModDifficultyAdjust

        Assert.assertEquals(7f, copy.cs)
        Assert.assertEquals(4f, copy.csDelegate().originalValue)
        Assert.assertEquals(9f, copy.od)
        Assert.assertEquals(8f, copy.odDelegate().originalValue)
    }

    private fun ModDifficultyAdjust.csDelegate() = getModSettingDelegate<DifficultyAdjustModSetting>(::cs)
    private fun ModDifficultyAdjust.odDelegate() = getModSettingDelegate<DifficultyAdjustModSetting>(::od)

    /**
     * Sets both [DifficultyAdjustModSetting.originalValue] and [DifficultyAdjustModSetting.defaultValue] for the
     * named dimensions, mirroring what [ModDifficultyAdjust.updateBeatmapValue] does when
     * [com.osudroid.mods.IModRequiresOriginalBeatmap.applyFromBeatmap] is called.
     */
    private fun ModDifficultyAdjust.setOriginals(cs: Float? = null, ar: Float? = null, od: Float? = null, hp: Float? = null) {
        cs?.let { getModSettingDelegate<DifficultyAdjustModSetting>(::cs).also { d -> d.defaultValue = it; d.originalValue = it } }
        ar?.let { getModSettingDelegate<DifficultyAdjustModSetting>(::ar).also { d -> d.defaultValue = it; d.originalValue = it } }
        od?.let { getModSettingDelegate<DifficultyAdjustModSetting>(::od).also { d -> d.defaultValue = it; d.originalValue = it } }
        hp?.let { getModSettingDelegate<DifficultyAdjustModSetting>(::hp).also { d -> d.defaultValue = it; d.originalValue = it } }
    }

    private fun ModDifficultyAdjust.roundTrip() =
        ModUtils.deserializeMods(ModUtils.serializeMods(listOf(this))).ofType<ModDifficultyAdjust>()!!
}