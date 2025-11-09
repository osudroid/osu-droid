package com.rian.osu.mods

import com.edlplan.framework.easing.Easing
import org.junit.Assert
import org.junit.Test

class ModApproachDifferentTest {
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
        test(ModApproachDifferent.AnimationStyle.BounceIn, Easing.InBounce)
        test(ModApproachDifferent.AnimationStyle.BounceOut, Easing.OutBounce)
        test(ModApproachDifferent.AnimationStyle.BounceInOut, Easing.InOutBounce)
    }
}