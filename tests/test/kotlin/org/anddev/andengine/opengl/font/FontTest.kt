package org.anddev.andengine.opengl.font

import android.graphics.Color
import android.graphics.Typeface
import org.anddev.andengine.opengl.texture.ITexture
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.function.Supplier

@RunWith(RobolectricTestRunner::class)
class FontTest {
    /**
     * The starting point of the [CJK Unified Ideographs](https://www.compart.com/en/unicode/block/U+4E00) block.
     *
     * Specifically used in this test suite since the characters commonly overflow an atlas.
     */
    private val startingCodePoint = 0x4e00

    @Test
    fun `Created letters stay within their own atlas page`() {
        val font = createFont()

        repeat(1000) {
            val character = (startingCodePoint + it).toChar().toString()
            val letter = font.getLetter(character)

            assertTrue(
                "Letter ${letter.mCharacter} overflows its atlas page vertically",
                letter.mTextureY + letter.mTextureHeight <= 1f,
            )

            assertTrue(
                "Letter ${letter.mCharacter} overflows its atlas page horizontally",
                letter.mTextureX + letter.mTextureWidth <= 1f,
            )
        }
    }

    @Test
    fun `Overflowing a page allocates a new one instead of reusing the old`() {
        val pages = mutableListOf<ITexture>()
        val font = createFont(pages)

        repeat(2000) { font.getLetter((startingCodePoint + it).toChar().toString()) }

        assertTrue("Expected more than one atlas page to have been allocated", pages.size > 1)
        assertEquals("All allocated pages should be distinct instances", pages.size, pages.distinct().size)
    }

    @Test
    fun `No two letters are ever assigned the same page and position`() {
        // Regression test:
        // A destructive "reset the atlas and start over" fix (as opposed to allocating a new page) would let a later
        // letter claim the same (page, x, y) as an earlier one, corrupting any Text still referencing the earlier letter.
        val font = createFont()
        val claimedPositions = HashMap<Triple<ITexture, Float, Float>, String>()

        repeat(2000) {
            val character = (startingCodePoint + it).toChar().toString()
            val letter = font.getLetter(character)
            val position = Triple(letter.texture, letter.mTextureX, letter.mTextureY)

            val previousOccupant = claimedPositions.put(position, character)

            assertTrue(
                "Letters '$previousOccupant' and '$character' were both assigned position $position",
                previousOccupant == null,
            )
        }
    }

    private fun createFont(pages: MutableList<ITexture>? = null) = FixedLineHeightFont {
        BitmapTextureAtlas(512, 512).also { pages?.add(it) }
    }

    private class FixedLineHeightFont(pageFactory: Supplier<ITexture>) : Font(
        pageFactory,
        Typeface.DEFAULT,
        28f,
        true,
        Color.WHITE,
    ) {
        override fun getLineHeight() = 32
        override fun getLineGap() = 0
    }
}
