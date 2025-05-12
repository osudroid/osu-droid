package com.rian.osu.mods

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModObjectScaleTweenTest {
    @Test
    fun `Test serialization`() {
        TestModObjectScaleTween().serialize().getJSONObject("settings").apply {
            Assert.assertEquals(1f, getDouble("startScale").toFloat(), 1e-5f)
        }
    }
}

private class TestModObjectScaleTween : ModObjectScaleTween() {
    override val name = "Test"
    override val acronym = "TS"
    override val description = "Test mod"

    override var startScale = 1f

    override fun deepCopy() = TestModObjectScaleTween().also {
        it.startScale = startScale
    }
}