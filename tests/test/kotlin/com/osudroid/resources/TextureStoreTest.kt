package com.osudroid.resources

import org.andengine.graphics.atlas.TextureAtlasPages
import org.andengine.opengl.texture.ITexture
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.atlas.bitmap.source.EmptyBitmapTextureAtlasSource
import org.andengine.opengl.texture.region.TextureRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TextureStoreTest {

    private class RecordingTextureManager : TextureManager() {
        val unloaded = ArrayList<ITexture>()

        override fun unloadTexture(pTexture: ITexture): Boolean {
            unloaded.add(pTexture)
            return super.unloadTexture(pTexture)
        }
    }

    private val manager = RecordingTextureManager()
    private val atlas = TextureAtlasPages(manager, 64, 32)
    private val store = TextureStore()

    @Before
    fun setUp() {
        store.init(manager, atlas)
    }

    private fun ownRegion(): TextureRegion = TextureRegion(BitmapTextureAtlas(manager, 8, 8, TextureOptions.BILINEAR), 0f, 0f, 8f, 8f)

    private fun atlasRegion(): TextureRegion {
        atlas.beginSession()
        try {
            return atlas.add(EmptyBitmapTextureAtlasSource(8, 8), TextureOptions.BILINEAR)!!
        } finally {
            atlas.endSession()
        }
    }

    @Test
    fun replace_releasesPreviousAtlasRegion() {
        val old = atlasRegion()
        store.replace("hitcircle", old)
        store.replace("hitcircle", atlasRegion())

        assertEquals(listOf(old.texture), manager.unloaded)
        assertEquals(1, atlas.pageCount)
    }

    @Test
    fun replace_unloadsPreviousOwnTexture() {
        val old = ownRegion()
        store.replace("hitcircle", old)
        val new = ownRegion()
        store.replace("hitcircle", new)

        assertEquals(listOf(old.texture), manager.unloaded)
        assertSame(new, store.get("hitcircle"))
    }

    @Test
    fun replace_keepsPreviousStillUsedByOtherName() {
        val old = atlasRegion()
        store.replace("menu-background", old)
        store.put("::background", old)
        store.replace("menu-background", atlasRegion())

        assertTrue(manager.unloaded.isEmpty())
        assertSame(old, store.get("::background"))
    }

    @Test
    fun replace_sameRegionOrAbsent_releasesNothing() {
        val region = ownRegion()
        store.markAbsent("lighting")
        store.replace("lighting", region)
        store.replace("lighting", region)

        assertTrue(manager.unloaded.isEmpty())
    }

    @Test
    fun unload_atlasRegion_releasesPageNotTextureDirectly() {
        val a = atlasRegion()
        store.replace("a", a)
        store.unload("a")

        // Released through the atlas: the page is unloaded once, as the last region on it.
        assertEquals(listOf(a.texture), manager.unloaded)
        assertEquals(0, atlas.pageCount)
    }

    @Test
    fun clear_releasesAtlasRegionsOnly() {
        val a = atlasRegion()
        store.replace("a", a)
        store.replace("own", ownRegion())
        store.clear()

        assertEquals(listOf(a.texture), manager.unloaded)
        assertEquals(0, atlas.pageCount)
    }
}
