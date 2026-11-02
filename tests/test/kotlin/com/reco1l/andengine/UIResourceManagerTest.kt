package com.reco1l.andengine

import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class UIResourceManagerTest {

    private class RecordingFontManager : FontManager() {
        val loaded = ArrayList<Font>()

        override fun loadFont(pFont: Font) {
            loaded += pFont
            super.loadFont(pFont)
        }
    }

    private val manager = RecordingFontManager()
    private val resources = UIResourceManager(RuntimeEnvironment.getApplication()) { manager }

    @Test
    fun sameFamilyAndSize_returnsTheCachedFont() {
        val font = resources.getOrStoreFont(16f, "nunito-medium.ttf")

        assertSame(font, resources.getOrStoreFont(16f, "nunito-medium.ttf"))
        assertEquals(listOf(font), manager.loaded)
    }

    @Test
    fun otherSize_loadsAnotherFont() {
        val small = resources.getOrStoreFont(16f, "nunito-medium.ttf")
        val large = resources.getOrStoreFont(24f, "nunito-medium.ttf")

        assertNotSame(small, large)
        assertEquals(listOf(small, large), manager.loaded)
    }
}
