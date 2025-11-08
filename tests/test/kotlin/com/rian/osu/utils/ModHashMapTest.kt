package com.rian.osu.utils

import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModReallyEasy
import com.rian.osu.mods.ModReplayV6
import org.junit.Assert
import org.junit.Test

class ModHashMapTest {
    @Test
    fun `Test insert mod by type`() {
        val map = ModHashMap()
        map.put(ModReallyEasy::class)

        Assert.assertTrue(map.ofType<ModReallyEasy>() is ModReallyEasy)
    }

    @Test
    fun `Test insert mod by value`() {
        val map = ModHashMap()
        map.put(ModReallyEasy())

        Assert.assertTrue(map.ofType<ModReallyEasy>() is ModReallyEasy)
    }

    @Test
    fun `Test incompatible mod removal`() {
        val map = ModHashMap()
        map.put(ModReallyEasy())
        map.put(ModHardRock())

        Assert.assertEquals(2, map.size)

        map.put(ModDifficultyAdjust(1f, 1f, 1f, 1f))

        Assert.assertEquals(1, map.size)
    }

    @Test
    fun `Test legacy mod string conversion`() {
        ModHashMap().apply {
            put(ModReallyEasy())
            put(ModHardRock())

            Assert.assertEquals("rl|", toLegacyModString())

            put(ModCustomSpeed(1.25f))

            Assert.assertEquals("rl|x1.25", toLegacyModString())

            put(ModDifficultyAdjust(cs = 4f, ar = 8f))

            Assert.assertEquals("rl|x1.25|AR8.0|CS4.0", toLegacyModString())
        }
    }

    @Test
    fun `Test display mod string conversion with non-user playable`() {
        ModHashMap().apply {
            put(ModHidden())
            put(ModHardRock())
            put(ModDoubleTime())
            put(ModReplayV6())

            Assert.assertEquals("HR,HD,DT,RV6", toDisplayModString())
        }
    }

    @Test
    fun `Test display mod string conversion without non-user playable`() {
        ModHashMap().apply {
            put(ModHidden())
            put(ModHardRock())
            put(ModDoubleTime())
            put(ModReplayV6())

            Assert.assertEquals("HR,HD,DT", toDisplayModString(false))
        }
    }

    @Test
    fun `Test same type mod contains`() {
        val firstMod = ModCustomSpeed(1.25f)
        val secondMod = ModCustomSpeed(1.2f)

        ModHashMap().apply {
            put(firstMod)

            Assert.assertTrue(firstMod::class in this)
            Assert.assertTrue(firstMod in this)

            Assert.assertTrue(secondMod::class in this)
            Assert.assertFalse(secondMod in this)
        }
    }

    @Test
    fun `Test same type mod removal`() {
        val firstMod = ModCustomSpeed(1.25f)
        val secondMod = ModCustomSpeed(1.2f)

        ModHashMap().apply {
            put(firstMod)

            Assert.assertTrue(firstMod in this)
            Assert.assertNull(remove(secondMod))
            Assert.assertNotNull(remove(firstMod))
        }
    }
}