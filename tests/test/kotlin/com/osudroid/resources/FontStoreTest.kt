package com.osudroid.resources

import android.graphics.Color
import org.andengine.opengl.font.FontManager
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class FontStoreTest {

    private val fontManager = FontManager()
    private val store = FontStore()

    @Before
    fun setUp() {
        store.init(fontManager, RuntimeEnvironment.getApplication().assets)
    }

    @Test
    fun loadFont_sameParameters_reusesFont() {
        val first = store.loadFont("font", null, 20, Color.WHITE)
        val second = store.loadFont("font", null, 20, Color.WHITE)

        assertSame(first, second)
        assertSame(first, store.get("font"))
    }

    @Test
    fun loadFont_otherSize_createsNewFont() {
        val first = store.loadFont("font", null, 20, Color.WHITE)
        val second = store.loadFont("font", null, 24, Color.WHITE)

        assertNotSame(first, second)
        assertSame(second, store.get("font"))
    }

    @Test
    fun loadFont_otherColor_createsNewFont() {
        val first = store.loadFont("font", null, 20, Color.WHITE)
        val second = store.loadFont("font", null, 20, Color.BLACK)

        assertNotSame(first, second)
    }

    @Test
    fun loadStrokeFont_sameParameters_reusesFont() {
        val first = store.loadStrokeFont("stroke", null, 20, Color.WHITE, Color.BLACK)
        val second = store.loadStrokeFont("stroke", null, 20, Color.WHITE, Color.BLACK)

        assertSame(first, second)
    }

    @Test
    fun loadStrokeFont_otherStrokeColor_createsNewFont() {
        val first = store.loadStrokeFont("stroke", null, 20, Color.WHITE, Color.BLACK)
        val second = store.loadStrokeFont("stroke", null, 20, Color.WHITE, Color.RED)

        assertNotSame(first, second)
    }
}
