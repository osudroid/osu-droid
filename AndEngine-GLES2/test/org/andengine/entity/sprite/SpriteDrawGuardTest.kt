package org.andengine.entity.sprite

import android.opengl.GLES20
import org.andengine.engine.camera.Camera
import org.andengine.entity.Entity
import org.andengine.entity.sprite.vbo.ISpriteVertexBufferObject
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.RecordingGL
import org.andengine.opengl.shader.ShaderProgram
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import org.andengine.opengl.vbo.VertexBufferObjectManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SpriteDrawGuardTest {

    private lateinit var gl: RecordingGL
    private val glState = GLState()
    private val camera = Camera(0f, 0f, 100f, 100f)

    /** Records what the sprite asks its buffer to do instead of issuing GL calls. */
    private class RecordingSpriteVbo : ISpriteVertexBufferObject {
        val calls = ArrayList<String>()
        override fun onUpdateColor(pSprite: Sprite) = Unit
        override fun onUpdateVertices(pSprite: Sprite) = Unit
        override fun onUpdateTextureCoordinates(pSprite: Sprite) = Unit
        override fun isAutoDispose() = false
        override fun getHardwareBufferID() = 1
        override fun isLoadedToHardware() = true
        override fun setNotLoadedToHardware() = Unit
        override fun unloadFromHardware(pGLState: GLState) = Unit
        override fun isDirtyOnHardware() = false
        override fun setDirtyOnHardware() = Unit
        override fun getCapacity() = 0
        override fun getByteCapacity() = 0
        override fun getHeapMemoryByteSize() = 0
        override fun getNativeHeapMemoryByteSize() = 0
        override fun getGPUMemoryByteSize() = 0
        override fun bind(pGLState: GLState) { calls += "bind" }
        override fun bind(pGLState: GLState, pShaderProgram: ShaderProgram) { calls += "bind" }
        override fun unbind(pGLState: GLState, pShaderProgram: ShaderProgram) { calls += "unbind" }
        override fun getVertexBufferObjectManager(): VertexBufferObjectManager? = null
        override fun draw(pPrimitiveType: Int, pCount: Int) { calls += "draw" }
        override fun draw(pPrimitiveType: Int, pOffset: Int, pCount: Int) { calls += "draw" }
        override fun isDisposed() = false
        override fun dispose() = Unit
    }

    private class Page(loadedId: Int) : BitmapTextureAtlas(TextureManager(), 16, 16, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = loadedId
        }
    }

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
        gl.calls.clear()
    }

    private fun sprite(textureId: Int, vbo: RecordingSpriteVbo): Sprite {
        val region = TextureRegion(Page(textureId), 2f, 2f, 8f, 8f)
        return Sprite(0f, 0f, 8f, 8f, region, vbo)
    }

    @Test
    fun unloadedTexture_skipsDrawWithoutTouchingState() {
        val vbo = RecordingSpriteVbo()
        sprite(-1, vbo).onDraw(glState, camera)

        assertTrue(vbo.calls.isEmpty())
        assertFalse(gl.calls.any { it.startsWith("glBindTexture") })
        assertFalse(gl.calls.any { it.startsWith("glEnable(${GLES20.GL_BLEND})") })
    }

    @Test
    fun loadedTexture_draws() {
        val vbo = RecordingSpriteVbo()
        sprite(7, vbo).onDraw(glState, camera)

        assertEquals(listOf("bind", "draw", "unbind"), vbo.calls)
        assertTrue(gl.calls.contains("glBindTexture(${GLES20.GL_TEXTURE_2D},7)"))
    }

    @Test
    fun unloadedTexture_childrenStillDrawn() {
        val vbo = RecordingSpriteVbo()
        val childVbo = RecordingSpriteVbo()
        val parent = sprite(-1, vbo)
        parent.attachChild(sprite(7, childVbo))
        parent.onDraw(glState, camera)

        assertTrue(vbo.calls.isEmpty())
        assertEquals(listOf("bind", "draw", "unbind"), childVbo.calls)
    }

    @Test
    fun plainEntity_isSelfDrawable() {
        var drawn = false
        val entity = object : Entity() {
            override fun draw(pGLState: GLState, pCamera: Camera) {
                drawn = true
            }
        }
        entity.onDraw(glState, camera)
        assertTrue(drawn)
    }
}
