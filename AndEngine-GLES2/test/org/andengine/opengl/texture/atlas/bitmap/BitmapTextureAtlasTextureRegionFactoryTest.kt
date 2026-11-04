package org.andengine.opengl.texture.atlas.bitmap

import org.andengine.graphics.atlas.TextureAtlasPages
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class BitmapTextureAtlasTextureRegionFactoryTest {

    @Test
    fun tiledFromAssetDirectory_emptyDirectory_returnsNull() {
        val pages = TextureAtlasPages(TextureManager(), 64, 32)
        pages.beginSession()
        val tiled = BitmapTextureAtlasTextureRegionFactory.createTiledFromAssetDirectory(
            pages, RuntimeEnvironment.getApplication().assets, "missing", TextureOptions.BILINEAR
        )
        pages.endSession()

        assertNull(tiled)
        assertEquals(0, pages.pageCount)
    }
}
