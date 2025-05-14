package com.rian.osu.mods

import com.edlplan.framework.easing.Easing
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ModApproachDifferentTest {
    @Test
    fun `Test serialization`() {
        ModApproachDifferent().apply {
            serialize().getJSONObject("settings").apply {
                Assert.assertTrue(has("scale"))
                Assert.assertTrue(has("style"))
            }

            scale = 5f
            style = ModApproachDifferent.AnimationStyle.Gravity

            serialize().getJSONObject("settings").apply {
                Assert.assertEquals(5f, getDouble("scale").toFloat(), 1e-2f)
                Assert.assertEquals(ModApproachDifferent.AnimationStyle.Gravity.ordinal, getInt("style"))
            }
        }
    }

    @Test
    fun `Test animation style`() {
        val mod = ModApproachDifferent()

        fun test(style: ModApproachDifferent.AnimationStyle, easing: Easing) {
            mod.style = style
            Assert.assertEquals(easing, mod.easing)
        }

        test(ModApproachDifferent.AnimationStyle.Linear, Easing.None)
        test(ModApproachDifferent.AnimationStyle.Gravity, Easing.InBack)
        test(ModApproachDifferent.AnimationStyle.InOut1, Easing.InOutCubic)
        test(ModApproachDifferent.AnimationStyle.InOut2, Easing.InOutQuint)
        test(ModApproachDifferent.AnimationStyle.Accelerate1, Easing.In)
        test(ModApproachDifferent.AnimationStyle.Accelerate2, Easing.InCubic)
        test(ModApproachDifferent.AnimationStyle.Accelerate3, Easing.InQuint)
        test(ModApproachDifferent.AnimationStyle.Decelerate1, Easing.Out)
        test(ModApproachDifferent.AnimationStyle.Decelerate2, Easing.OutCubic)
        test(ModApproachDifferent.AnimationStyle.Decelerate3, Easing.OutQuint)
    }
}