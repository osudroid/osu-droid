package com.reco1l.andengine

import com.reco1l.andengine.container.*
import junit.framework.TestCase.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UIFlexContainerTest {

    @Test
    fun `Test flex container flex-grow`() {

        val component1: UIDummyComponent
        val component2: UIDummyComponent

        UIFlexContainer().apply {
            width = 100f

            +UIDummyComponent().apply {
                height = 100f
                flexRules {
                    grow = 2f
                }
                component1 = this
            }

            +UIDummyComponent().apply {
                width = 100f

                flexRules {
                    grow = 1f
                }
                component2 = this
            }

            onHandleInvalidations()
        }

        assertEquals(100f * (2f / 3f), component1.width)
        assertEquals(100f * (1f / 3f), component2.width)
    }

}