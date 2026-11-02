package org.andengine.entity.sprite

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.andengine.engine.camera.Camera
import org.andengine.entity.sprite.vbo.HighPerformanceSpriteVertexBufferObject
import org.andengine.entity.sprite.vbo.ISpriteVertexBufferObject
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.texture.region.TiledTextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.DrawType
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpriteBatchingTest {

    private lateinit var gl: RecordingGL
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    private class Page(id: Int) : BitmapTextureAtlas(TextureManager(), 16, 16, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = id
        }
    }

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        gl.intValues[GLES20.GL_MAX_TEXTURE_SIZE] = 4096
        GlesBackend.onContextCreatedForTest(gl)
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        gl.calls.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
    }

    private fun defaultVbo() = HighPerformanceSpriteVertexBufferObject(
        null, Sprite.SPRITE_SIZE, DrawType.STATIC, false, Sprite.VERTEXBUFFEROBJECTATTRIBUTES_DEFAULT
    )

    private fun sprite(page: Page, x: Float = 0f): Sprite =
        Sprite(x, 0f, 8f, 8f, TextureRegion(page, 2f, 2f, 8f, 8f), defaultVbo())

    private fun draws() = gl.calls.count { it.startsWith("glDrawElements") }
    private fun legacyDraws() = gl.calls.count { it.startsWith("glDrawArrays") }

    @Test
    fun engineSprite_goesThroughBatcher() {
        val s = sprite(Page(7), 10f)
        s.setColor(1f, 0f, 0f, 0.5f)
        s.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
        // Batching changes no GL state on its own.
        assertFalse(gl.calls.any { it.startsWith("glBindTexture") || it.startsWith("glBlendFunc") })

        QuadBatcher.flush()
        assertEquals(1, draws())

        val v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        // Corners in order around the quad: (x,y), (x,y2), (x2,y2), (x2,y); 20 bytes per vertex,
        // position at +0, UV at +8, color at +16.
        assertEquals(10f, v.getFloat(0), 0f)
        assertEquals(0f, v.getFloat(4), 0f)
        assertEquals(10f, v.getFloat(20), 0f)
        assertEquals(8f, v.getFloat(24), 0f)
        assertEquals(18f, v.getFloat(40), 0f)
        assertEquals(8f, v.getFloat(44), 0f)
        assertEquals(18f, v.getFloat(60), 0f)
        assertEquals(0f, v.getFloat(64), 0f)
        // UVs of the region (2..10 of 16): top-left and bottom-right.
        assertEquals(2f / 16f, v.getFloat(8), 1e-6f)
        assertEquals(2f / 16f, v.getFloat(12), 1e-6f)
        assertEquals(10f / 16f, v.getFloat(48), 1e-6f)
        assertEquals(10f / 16f, v.getFloat(52), 1e-6f)
        // Color bytes R, G, B, A.
        assertEquals(255, v.get(16).toInt() and 0xFF)
        assertEquals(0, v.get(17).toInt() and 0xFF)
        assertEquals(127, v.get(19).toInt() and 0xFF)
    }

    @Test
    fun blendFunction_followsTheSprite() {
        val s = sprite(Page(7))
        s.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE)
        s.onDraw(glState, camera)
        QuadBatcher.flush()

        val expected = "glBlendFunc(${GLES20.GL_SRC_ALPHA},${GLES20.GL_ONE})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun customShaderSprite_usesLegacyPath() {
        val shader = ShaderProgram("void main(){}", "void main(){}")
        val s = Sprite(0f, 0f, 8f, 8f, TextureRegion(Page(7), 2f, 2f, 8f, 8f), defaultVbo(), shader)
        s.onDraw(glState, camera)

        assertEquals(1, legacyDraws())
        assertFalse(QuadBatcher.hasPending)
    }

    private fun region() = TextureRegion(Page(7), 2f, 2f, 8f, 8f)

    @Test
    fun overriddenDraw_cannotBatch() {
        val s = object : Sprite(0f, 0f, 8f, 8f, region(), defaultVbo()) {
            override fun draw(pGLState: GLState, pCamera: Camera) = super.draw(pGLState, pCamera)
        }
        assertFalse(SpriteBatching.canBatch(s))
    }

    @Test
    fun overriddenPreDraw_cannotBatch() {
        val s = object : Sprite(0f, 0f, 8f, 8f, region(), defaultVbo()) {
            override fun preDraw(pGLState: GLState, pCamera: Camera) = super.preDraw(pGLState, pCamera)
        }
        assertFalse(SpriteBatching.canBatch(s))
    }

    @Test
    fun overriddenPostDraw_cannotBatch() {
        val s = object : Sprite(0f, 0f, 8f, 8f, region(), defaultVbo()) {
            override fun postDraw(pGLState: GLState, pCamera: Camera) = super.postDraw(pGLState, pCamera)
        }
        assertFalse(SpriteBatching.canBatch(s))
    }

    @Test
    fun plainSubclass_canBatch() {
        val s = object : Sprite(0f, 0f, 8f, 8f, region(), defaultVbo()) {}
        assertTrue(SpriteBatching.canBatch(s))
    }

    @Test
    fun subclassWithoutOverrides_stillBatches() {
        val s = object : Sprite(0f, 0f, 8f, 8f, TextureRegion(Page(7), 2f, 2f, 8f, 8f), defaultVbo()) {}
        s.onDraw(glState, camera)

        assertTrue(QuadBatcher.hasPending)
        assertEquals(0, legacyDraws())
    }

    @Test
    fun blendingDisabled_replacesDestination() {
        val s = sprite(Page(7))
        s.isBlendingEnabled = false
        s.onDraw(glState, camera)
        QuadBatcher.flush()

        val expected = "glBlendFunc(${GLES20.GL_ONE},${GLES20.GL_ZERO})"
        assertTrue(gl.calls.toString(), gl.calls.any { it == expected })
    }

    @Test
    fun batchedThenLegacy_batchIsDrawnFirst() {
        val page = Page(7)
        sprite(page).onDraw(glState, camera)
        val legacy = Sprite(0f, 0f, 8f, 8f, TextureRegion(page, 2f, 2f, 8f, 8f), defaultVbo(), ShaderProgram("void main(){}", "void main(){}"))
        legacy.onDraw(glState, camera)

        val indexed = gl.calls.indexOfFirst { it.startsWith("glDrawElements") }
        val arrays = gl.calls.indexOfFirst { it.startsWith("glDrawArrays") }
        assertTrue(gl.calls.toString(), indexed >= 0 && arrays > indexed)
    }

    @Test
    fun sameTexture_twoSprites_oneDraw() {
        val page = Page(7)
        sprite(page, 0f).onDraw(glState, camera)
        sprite(page, 20f).onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(1, draws())
        assertEquals(0, legacyDraws())
    }

    @Test
    fun tiledSprite_drawsCurrentTile() {
        val page = Page(7)
        val tiles = TiledTextureRegion(
            page, false, TextureRegion(page, 0f, 0f, 8f, 8f), TextureRegion(page, 8f, 0f, 8f, 8f)
        )
        val s = TiledSprite(0f, 0f, tiles, null as VertexBufferObjectManager?)
        s.currentTileIndex = 1
        s.onDraw(glState, camera)
        QuadBatcher.flush()

        assertEquals(1, draws())
        val v = ByteBuffer.wrap(gl.vertexUploads.last()).order(ByteOrder.nativeOrder())
        assertEquals(0.5f, v.getFloat(8), 1e-6f)
    }
}
