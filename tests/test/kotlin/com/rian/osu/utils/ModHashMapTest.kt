package com.rian.osu.utils

import com.rian.osu.mods.ModCustomSpeed
import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModDoubleTime
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModHidden
import com.rian.osu.mods.ModReallyEasy
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

        Assert.assertTrue(map.size == 2)

        map.put(ModDifficultyAdjust(1f, 1f, 1f, 1f))

        Assert.assertTrue(map.size == 1)
    }

    @Test
    fun `Test legacy mod string conversion`() {
        ModHashMap().apply {
            put(ModReallyEasy())
            put(ModHardRock())

            Assert.assertEquals(toLegacyModString(), "rl|")

            put(ModCustomSpeed(1.25f))

            Assert.assertEquals(toLegacyModString(), "rl|x1.25")

            put(ModDifficultyAdjust(cs = 4f, ar = 8f))

            Assert.assertEquals(toLegacyModString(), "rl|x1.25|AR8.0|CS4.0")
        }
    }

    @Test
    fun `Test display mod string conversion`() {
        ModHashMap().apply {
            put(ModHidden())
            put(ModHardRock())
            put(ModDoubleTime())

            Assert.assertEquals(toDisplayModString(), "HR,HD,DT")
        }
    }
}