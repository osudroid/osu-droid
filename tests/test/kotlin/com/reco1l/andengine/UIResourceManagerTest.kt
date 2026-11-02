package com.reco1l.andengine

import org.robolectric.RuntimeEnvironment
import org.andengine.opengl.font.Font
import org.andengine.opengl.font.FontManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UIResourceManagerTest {

    private class RecordingFontManager : FontManager() {
        val loaded = ArrayList<Font>()
        val unloaded = ArrayList<Font>()

        override fun loadFont(pFont: Font) {
            loaded += pFont
            super.loadFont(pFont)
        }

        override fun unloadFont(pFont: Font) {
            unloaded += pFont
            super.unloadFont(pFont)
        }
    }

    @Test
    fun fontStaysLoadedAfterLastUnsubscribe() {
        val manager = RecordingFontManager()
        val resources = UIResourceManager(RuntimeEnvironment.getApplication()) { manager }
        val component = UIDummyComponent()

        val font = resources.getOrStoreFont(16f, "nunito-medium.ttf")
        resources.subscribeToFont(font, component)
        resources.unsubscribeFromFont(font, component)

        assertSame(font, resources.getOrStoreFont(16f, "nunito-medium.ttf"))
        assertEquals(listOf(font), manager.loaded)
        assertEquals(0, manager.unloaded.size)
    }
}
