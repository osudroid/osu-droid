package com.rian.osu.mods

import com.rian.osu.mods.settings.BooleanModSetting
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModTest {
    @Test
    fun `Test serialization with all default settings`() {
        TestMod().serialize().apply {
            Assert.assertEquals("TM", getString("acronym"))
            Assert.assertFalse(has("settings"))
        }
    }

    @Test
    fun `Test serialization with modified settings`() {
        TestMod().apply {
            testSetting1 = true
            testSetting2 = true
            testSetting3 = true

            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(getBoolean("test1"))
                Assert.assertTrue(getBoolean("test2"))
                Assert.assertFalse(has("test3"))
            }
        }
    }
}

private class TestMod : Mod() {
    override val name = "Test mod"
    override val acronym = "TM"
    override val description = "This is a test mod"
    override val type = ModType.Automation

    var testSetting1 by BooleanModSetting(name = "Test 1", key = "test1", defaultValue = false)
    var testSetting2 by BooleanModSetting(name = "Test 2", key = "test2", defaultValue = false)
    var testSetting3 by BooleanModSetting(name = "Test 3", defaultValue = false)

    override fun deepCopy() = TestMod()
}