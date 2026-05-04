package com.osudroid.mods

import com.osudroid.mods.settings.BooleanModSetting
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert
import org.junit.Test

class ModTest {
    @Test
    fun `Test conversion to APIMod with default settings`() {
        _root_ide_package_.com.osudroid.mods.TestMod().toAPIMod().apply {
            Assert.assertEquals("TM", acronym)
            Assert.assertNull(settings)
        }
    }

    @Test
    fun `Test conversion to APIMod with one modified setting`() {
        _root_ide_package_.com.osudroid.mods.TestMod().apply {
            testSetting1 = true

            toAPIMod().apply {
                Assert.assertNotNull(settings)
                Assert.assertTrue(settings!!["test1"]!!.jsonPrimitive.boolean)
            }
        }
    }

    @Test
    fun `Test deep copy`() {
        val originalMod = _root_ide_package_.com.osudroid.mods.TestMod().apply {
            testSetting1 = true
            testSetting2 = true
            testSetting3 = true
        }

        val copiedMod = originalMod.deepCopy() as com.osudroid.mods.TestMod

        Assert.assertNotSame(originalMod, copiedMod)
        Assert.assertEquals(originalMod, copiedMod)
    }
}

internal class TestMod : Mod() {
    override val name = "Test mod"
    override val acronym = "TM"
    override val description = "This is a test mod"
    override val type = ModType.Automation

    var testSetting1 by BooleanModSetting(name = "Test 1", key = "test1", defaultValue = false)
    var testSetting2 by BooleanModSetting(name = "Test 2", key = "test2", defaultValue = false)
    var testSetting3 by BooleanModSetting(name = "Test 3", defaultValue = false)
}