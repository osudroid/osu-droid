package com.rian.osu.utils

import com.rian.osu.mods.ModDifficultyAdjust
import com.rian.osu.mods.ModHardRock
import com.rian.osu.mods.ModReallyEasy
import org.junit.Assert
import org.junit.Test

class ModHashMapTest {
    @Test
    fun testInsertModByType() {
        val map = ModHashMap()
        map.put(ModReallyEasy::class)

        Assert.assertTrue(map.ofType<ModReallyEasy>() is ModReallyEasy)
    }

    @Test
    fun testInsertModByValue() {
        val map = ModHashMap()
        map.put(ModReallyEasy())

        Assert.assertTrue(map.ofType<ModReallyEasy>() is ModReallyEasy)
    }

    @Test
    fun testIncompatibleModRemoval() {
        val map = ModHashMap()
        map.put(ModReallyEasy())
        map.put(ModHardRock())

        Assert.assertTrue(map.size == 2)

        map.put(ModDifficultyAdjust(1f, 1f, 1f, 1f))

        Assert.assertTrue(map.size == 1)
    }
}