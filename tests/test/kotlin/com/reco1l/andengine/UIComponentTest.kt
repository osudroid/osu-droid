package com.reco1l.andengine

import com.reco1l.andengine.container.*
import com.reco1l.andengine.theme.pct
import com.reco1l.framework.math.*
import junit.framework.TestCase.*
import org.junit.Test
import org.junit.runner.*
import org.robolectric.*

@RunWith(RobolectricTestRunner::class)
class UIComponentTest {

    @Test
    fun `Test container auto-size`() {

        val container = UIContainer().apply {
            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            onHandleInvalidations()
        }

        assertEquals(100f, container.width)
        assertEquals(100f, container.height)
    }

    @Test
    fun `Test container auto-size with padding`() {

        val container = UIContainer().apply {
            padding = Vec4(20f)

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            onHandleInvalidations()
        }

        assertEquals(100f + 20f * 2, container.width)
        assertEquals(100f + 20f * 2, container.height)
    }

    @Test
    fun `Test linear container auto-size`() {

        val container = UILinearContainer().apply {
            spacing = 10f

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            onHandleInvalidations()
        }

        assertEquals(100f * 2 + 10f, container.width)
        assertEquals(100f, container.height)
    }

    @Test
    fun `Test linear container auto-size with padding`() {

        val container = UILinearContainer().apply {
            padding = Vec4(20f)

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            onHandleInvalidations()
        }

        assertEquals(100f * 2 + 20f * 2, container.width)
        assertEquals(100f + 20f * 2, container.height)
    }

    @Test
    fun `Test linear container spacing`() {

        val container = UILinearContainer().apply {
            spacing = 10f

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            onHandleInvalidations()
        }

        assertEquals(100f * 2 + 10f, container.width)
        assertEquals(100f, container.height)
    }

    @Test
    fun `Test linear container spacing with padding`() {

        val container = UILinearContainer().apply {
            padding = Vec4(20f)
            spacing = 10f

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            +UIDummyComponent().apply {
                width = 100f
                height = 100f
            }

            onHandleInvalidations()
        }

        assertEquals(100f * 2 + 10f + 20f * 2, container.width)
        assertEquals(100f + 20f * 2, container.height)
    }


    @Test
    fun `Test child's relative sizing`() {

        val component = UIDummyComponent().apply {
            width = 0.5f.pct
            height = 100f
        }

        UIContainer().apply {
            width = 200f
            height = 200f
            +component
            onHandleInvalidations()
        }

        assertEquals(component.width, 100f)
    }
}