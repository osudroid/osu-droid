package com.osudroid.ui.skinning

import com.reco1l.framework.Color4
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntegerSkinDataTest {
    @Test
    fun `Test default value`() {
        val skinData = IntegerSkinData("testTag", 42)
        assertEquals(42, skinData.currentValue)
        assertEquals(42, skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test default value with no default specified`() {
        val skinData = IntegerSkinData("testTag")
        assertEquals(0, skinData.currentValue)
        assertEquals(0, skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with valid value`() {
        val skinData = IntegerSkinData("testTag", 10)
        val json = JSONObject()
        json.put("testTag", 100)

        skinData.setFromJson(json)

        assertEquals(100, skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with missing value`() {
        val skinData = IntegerSkinData("testTag", 10)
        val json = JSONObject()

        skinData.setFromJson(json)

        assertEquals(10, skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with negative value`() {
        val skinData = IntegerSkinData("testTag")
        val json = JSONObject()
        json.put("testTag", -50)

        skinData.setFromJson(json)

        assertEquals(-50, skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson updates currentValue correctly`() {
        val skinData = IntegerSkinData("testTag", 5)
        assertEquals(5, skinData.currentValue)

        val json = JSONObject()
        json.put("testTag", 15)
        skinData.setFromJson(json)
        assertEquals(15, skinData.currentValue)

        val newJson = JSONObject()
        newJson.put("testTag", 25)
        skinData.setFromJson(newJson)
        assertEquals(25, skinData.currentValue)
    }
}

@RunWith(RobolectricTestRunner::class)
class FloatSkinDataTest {
    @Test
    fun `Test default value`() {
        val skinData = FloatSkinData("testTag", 3.14f)
        assertEquals(3.14f, skinData.currentValue)
        assertEquals(3.14f, skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test default value with no default specified`() {
        val skinData = FloatSkinData("testTag")
        assertEquals(0f, skinData.currentValue)
        assertEquals(0f, skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with float value`() {
        val skinData = FloatSkinData("testTag", 1.0f)
        val json = JSONObject()
        json.put("testTag", 2.5)

        skinData.setFromJson(json)

        assertEquals(2.5f, skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with integer value converts to float`() {
        val skinData = FloatSkinData("testTag", 1.0f)
        val json = JSONObject()
        json.put("testTag", 5)

        skinData.setFromJson(json)

        assertEquals(5.0f, skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with missing value`() {
        val skinData = FloatSkinData("testTag", 1.5f)
        val json = JSONObject()

        skinData.setFromJson(json)

        assertEquals(1.5f, skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with negative float value`() {
        val skinData = FloatSkinData("testTag")
        val json = JSONObject()
        json.put("testTag", -3.14)

        skinData.setFromJson(json)

        assertEquals(-3.14f, skinData.currentValue, 0.001f)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with zero value`() {
        val skinData = FloatSkinData("testTag", 1.0f)
        val json = JSONObject()
        json.put("testTag", 0.0)

        skinData.setFromJson(json)

        assertEquals(0f, skinData.currentValue)
        assertFalse(skinData.isDefault)
    }
}

@RunWith(RobolectricTestRunner::class)
class BooleanSkinDataTest {
    @Test
    fun `Test default true value`() {
        val skinData = BooleanSkinData("testTag", true)
        assertTrue(skinData.currentValue)
        assertTrue(skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test default false value`() {
        val skinData = BooleanSkinData("testTag")
        assertFalse(skinData.currentValue)
        assertFalse(skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test default value with no default specified`() {
        val skinData = BooleanSkinData("testTag")
        assertFalse(skinData.currentValue)
        assertFalse(skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with true value`() {
        val skinData = BooleanSkinData("testTag")
        val json = JSONObject()
        json.put("testTag", true)

        skinData.setFromJson(json)

        assertTrue(skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with false value`() {
        val skinData = BooleanSkinData("testTag", true)
        val json = JSONObject()
        json.put("testTag", false)

        skinData.setFromJson(json)

        assertFalse(skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with missing value`() {
        val skinData = BooleanSkinData("testTag", true)
        val json = JSONObject()

        skinData.setFromJson(json)

        assertTrue(skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson toggles boolean value`() {
        val skinData = BooleanSkinData("testTag")

        val jsonTrue = JSONObject()
        jsonTrue.put("testTag", true)
        skinData.setFromJson(jsonTrue)
        assertTrue(skinData.currentValue)
        assertFalse(skinData.isDefault)

        val jsonFalse = JSONObject()
        jsonFalse.put("testTag", false)
        skinData.setFromJson(jsonFalse)
        assertFalse(skinData.currentValue)
        assertTrue(skinData.isDefault)
    }
}

@RunWith(RobolectricTestRunner::class)
class StringSkinDataTest {
    @Test
    fun `Test default empty string`() {
        val skinData = StringSkinData("testTag")
        assertEquals("", skinData.currentValue)
        assertEquals("", skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test default string value`() {
        val skinData = StringSkinData("testTag", "hello")
        assertEquals("hello", skinData.currentValue)
        assertEquals("hello", skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with string value`() {
        val skinData = StringSkinData("testTag", "default")
        val json = JSONObject()
        json.put("testTag", "custom")

        skinData.setFromJson(json)

        assertEquals("custom", skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with empty string`() {
        val skinData = StringSkinData("testTag", "default")
        val json = JSONObject()
        json.put("testTag", "")

        skinData.setFromJson(json)

        assertEquals("", skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with missing value`() {
        val skinData = StringSkinData("testTag", "default")
        val json = JSONObject()

        skinData.setFromJson(json)

        assertEquals("default", skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with special characters`() {
        val skinData = StringSkinData("testTag")
        val json = JSONObject()
        json.put("testTag", "hello@#$%^&*()")

        skinData.setFromJson(json)

        assertEquals("hello@#$%^&*()", skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with unicode characters`() {
        val skinData = StringSkinData("testTag")
        val json = JSONObject()
        json.put("testTag", "こんにちは世界")

        skinData.setFromJson(json)

        assertEquals("こんにちは世界", skinData.currentValue)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with whitespace`() {
        val skinData = StringSkinData("testTag")
        val json = JSONObject()
        json.put("testTag", "   spaces   ")

        skinData.setFromJson(json)

        assertEquals("   spaces   ", skinData.currentValue)
        assertFalse(skinData.isDefault)
    }
}

@RunWith(RobolectricTestRunner::class)
class ColorSkinDataTest {
    @Test
    fun `Test default null color`() {
        val skinData = ColorSkinData("testTag")
        assertNull(skinData.currentValue)
        assertNull(skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test custom default color`() {
        val defaultColor = Color4(255, 0, 0)
        val skinData = ColorSkinData("testTag", defaultColor)
        assertEquals(defaultColor, skinData.currentValue)
        assertEquals(defaultColor, skinData.defaultValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with valid RRGGBB hex`() {
        val defaultColor = Color4(0, 0, 0)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "FF0000")

        skinData.setFromJson(json)

        assertNotNull(skinData.currentValue)
        assertEquals(255, skinData.currentValue!!.redInt)
        assertEquals(0, skinData.currentValue!!.greenInt)
        assertEquals(0, skinData.currentValue!!.blueInt)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with hashtagged hex`() {
        val defaultColor = Color4(0, 0, 0)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "#00FF00")

        skinData.setFromJson(json)

        assertNotNull(skinData.currentValue)
        assertEquals(0, skinData.currentValue!!.redInt)
        assertEquals(255, skinData.currentValue!!.greenInt)
        assertEquals(0, skinData.currentValue!!.blueInt)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with empty string reverts to default`() {
        val defaultColor = Color4(100, 100, 100)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "FF0000")
        skinData.setFromJson(json)
        assertNotNull(skinData.currentValue)
        assertFalse(skinData.isDefault)

        // Now revert to default
        val emptyJson = JSONObject()
        json.put("testTag", "")
        skinData.setFromJson(emptyJson)

        assertEquals(defaultColor, skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with missing value reverts to default`() {
        val defaultColor = Color4(50, 50, 50)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "0000FF")
        skinData.setFromJson(json)
        assertFalse(skinData.isDefault)

        // Now revert to default by having no value in JSON
        val emptyJson = JSONObject()
        skinData.setFromJson(emptyJson)

        assertEquals(defaultColor, skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with null value reverts to default`() {
        val defaultColor = Color4(75, 75, 75)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "FFFFFF")
        skinData.setFromJson(json)
        assertFalse(skinData.isDefault)

        // Now set null value
        val nullJson = JSONObject()
        nullJson.put("testTag", JSONObject.NULL)
        skinData.setFromJson(nullJson)

        assertEquals(defaultColor, skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with invalid hex format falls back to default`() {
        val defaultColor = Color4(200, 200, 200)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "INVALIDHEX")

        skinData.setFromJson(json)

        assertEquals(defaultColor, skinData.currentValue)
        assertTrue(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with various valid hex colors`() {
        data class TestCase(val hex: String, val expectedRed: Int, val expectedGreen: Int, val expectedBlue: Int)

        val defaultColor = Color4(0, 0, 0)

        val testCases = listOf(
            TestCase("000000", 0, 0, 0),
            TestCase("FFFFFF", 255, 255, 255),
            TestCase("FF0000", 255, 0, 0),
            TestCase("00FF00", 0, 255, 0),
            TestCase("0000FF", 0, 0, 255),
            TestCase("808080", 128, 128, 128),
            TestCase("FFFF00", 255, 255, 0)
        )

        for ((hex, expectedRed, expectedGreen, expectedBlue) in testCases) {
            val skinData = ColorSkinData("testTag", defaultColor)
            val json = JSONObject()
            json.put("testTag", hex)

            skinData.setFromJson(json)

            assertNotNull("Color should not be null for hex: $hex", skinData.currentValue)
            assertEquals("Red mismatch for hex: $hex", expectedRed, skinData.currentValue!!.redInt)
            assertEquals("Green mismatch for hex: $hex", expectedGreen, skinData.currentValue!!.greenInt)
            assertEquals("Blue mismatch for hex: $hex", expectedBlue, skinData.currentValue!!.blueInt)
        }
    }

    @Test
    fun `Test setFromJson with lowercase hex`() {
        val defaultColor = Color4(0, 0, 0)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()

        json.put("testTag", "ff00ff")
        skinData.setFromJson(json)

        assertNotNull(skinData.currentValue)
        assertEquals(255, skinData.currentValue!!.redInt)
        assertEquals(0, skinData.currentValue!!.greenInt)
        assertEquals(255, skinData.currentValue!!.blueInt)
        assertFalse(skinData.isDefault)
    }

    @Test
    fun `Test setFromJson with mixed case hex`() {
        val defaultColor = Color4(0, 0, 0)
        val skinData = ColorSkinData("testTag", defaultColor)
        val json = JSONObject()
        json.put("testTag", "#FfAaBb")

        skinData.setFromJson(json)

        assertNotNull(skinData.currentValue)
        assertEquals(255, skinData.currentValue!!.redInt)
        assertEquals(170, skinData.currentValue!!.greenInt)
        assertEquals(187, skinData.currentValue!!.blueInt)
        assertFalse(skinData.isDefault)
    }
}

@RunWith(RobolectricTestRunner::class)
class SkinDataGeneralTest {
    @Test
    fun `Test tag is preserved in all SkinData types`() {
        val intData = IntegerSkinData("intTag", 10)
        val floatData = FloatSkinData("floatTag", 1.0f)
        val boolData = BooleanSkinData("boolTag")
        val stringData = StringSkinData("stringTag", "test")
        val colorData = ColorSkinData("colorTag", Color4(0, 0, 0))

        assertEquals("intTag", intData.tag)
        assertEquals("floatTag", floatData.tag)
        assertEquals("boolTag", boolData.tag)
        assertEquals("stringTag", stringData.tag)
        assertEquals("colorTag", colorData.tag)
    }

    @Test
    fun `Test isDefault property reflects current vs default value`() {
        val intData = IntegerSkinData("test", 42)
        assertTrue(intData.isDefault)

        intData.currentValue = 43
        assertFalse(intData.isDefault)

        intData.currentValue = 42
        assertTrue(intData.isDefault)
    }

    @Test
    fun `Test int and float can handle extreme values`() {
        val intData = IntegerSkinData("test", Int.MIN_VALUE)
        assertEquals(Int.MIN_VALUE, intData.currentValue)

        val json = JSONObject()
        json.put("test", Int.MAX_VALUE)
        intData.setFromJson(json)
        assertEquals(Int.MAX_VALUE, intData.currentValue)
    }

    @Test
    fun `Test string data with numeric values stays as string`() {
        val stringData = StringSkinData("test")
        val json = JSONObject()
        json.put("test", "12345")

        stringData.setFromJson(json)

        assertEquals("12345", stringData.currentValue)
    }
}

