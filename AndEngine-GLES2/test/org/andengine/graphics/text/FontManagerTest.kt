package org.andengine.graphics.text

import android.graphics.Typeface
import org.andengine.graphics.gles.installRecordingGL
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class FontManagerTest {

    private class CountingFont(manager: FontManager) :
        Font(manager, AndroidGlyphRasterizer(Typeface.DEFAULT, 16f, true)) {
        var updates = 0

        override fun update() {
            updates++
            super.update()
        }
    }

    private val manager = FontManager()

    @Before
    fun setUp() {
        installRecordingGL()
    }

    @Test
    fun loadFont_twice_managesTheFontOnce() {
        val font = CountingFont(manager)

        manager.loadFont(font)
        manager.loadFont(font)
        manager.updateFonts()

        assertEquals(1, font.updates)
    }

    @Test
    fun unloadFont_afterLoadingTwice_stopsManagingTheFont() {
        val font = CountingFont(manager)

        manager.loadFont(font)
        manager.loadFont(font)
        manager.unloadFont(font)
        manager.updateFonts()

        assertEquals(0, font.updates)
    }
}
