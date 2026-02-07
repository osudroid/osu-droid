package com.reco1l.andengine

import com.reco1l.andengine.container.*
import com.reco1l.andengine.theme.Size
import junit.framework.TestCase.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UIFillContainerTest {

    @Test
    fun `Test fill container`() {

        val component1: UIDummyComponent
        val component2: UIDummyComponent

        UIFillContainer().apply {
            width = 100f

            +UIDummyComponent().apply {
                width = Size.Full
                height = 100f
                component1 = this
            }

            +UIDummyComponent().apply {
                width = Size.Full
                height = 100f
                component2 = this
            }

            onHandleInvalidations()
        }

        assertEquals(100f / 2, component1.width)
        assertEquals(100f / 2, component2.width)
    }

}