package org.andengine.opengl.texture.atlas.bitmap.source

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SolidColorBitmapTextureAtlasSourceTest {

    @Test
    fun onLoadBitmap_fillsTheWholeBitmapWithTheColor() {
        val color = Color.argb(200, 10, 20, 30)
        val bitmap = SolidColorBitmapTextureAtlasSource(3, 2, color).onLoadBitmap(Bitmap.Config.ARGB_8888)

        assertEquals(3, bitmap.width)
        assertEquals(2, bitmap.height)
        for (y in 0 until 2) {
            for (x in 0 until 3) {
                assertEquals(color, bitmap.getPixel(x, y))
            }
        }
    }

    @Test
    fun deepCopy_keepsSizeAndColor() {
        val copy = SolidColorBitmapTextureAtlasSource(1, 2, 3, 4, Color.RED).deepCopy()
        val bitmap = copy.onLoadBitmap(Bitmap.Config.ARGB_8888)

        assertEquals(1, copy.textureX)
        assertEquals(2, copy.textureY)
        assertEquals(3, bitmap.width)
        assertEquals(4, bitmap.height)
        assertEquals(Color.RED, bitmap.getPixel(0, 0))
    }
}
