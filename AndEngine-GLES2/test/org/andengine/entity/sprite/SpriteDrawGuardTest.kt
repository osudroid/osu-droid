package org.andengine.entity.sprite

import android.opengl.GLES20
import org.andengine.engine.camera.Camera
import org.andengine.entity.Entity
import org.andengine.graphics.DrawContext
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.graphics.gles.Page
import org.andengine.graphics.gles.RecordingGL
import org.andengine.graphics.gles.installRecordingGL
import org.andengine.opengl.texture.region.TextureRegion
import org.junit.After
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
    private val glState = DrawContext()
    private val camera = Camera(0f, 0f, 100f, 100f)

    /** Counts the draws the sprite hands to the batch. */
    private class CountingSprite(region: TextureRegion) : Sprite(0f, 0f, 8f, 8f, region) {
        var draws = 0
        override fun draw(pGLState: DrawContext, pCamera: Camera) {
            draws++
            super.draw(pGLState, pCamera)
        }
    }

    @Before
    fun setUp() {
        gl = installRecordingGL()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
    }

    private fun sprite(textureId: Int) = CountingSprite(TextureRegion(Page(textureId), 2f, 2f, 8f, 8f))

    @Test
    fun unloadedTexture_skipsDrawWithoutTouchingState() {
        val s = sprite(-1)
        s.onDraw(glState, camera)

        assertEquals(0, s.draws)
        assertFalse(QuadBatcher.hasPending)
        assertFalse(gl.calls.any { it.startsWith("glBindTexture") })
        assertFalse(gl.calls.any { it.startsWith("glEnable(${GLES20.GL_BLEND})") })
    }

    @Test
    fun loadedTexture_draws() {
        val s = sprite(7)
        s.onDraw(glState, camera)

        assertEquals(1, s.draws)
        assertTrue(QuadBatcher.hasPending)
        QuadBatcher.flush()
        assertTrue(gl.calls.toString(), gl.calls.any { it.startsWith("glBindTexture(${GLES20.GL_TEXTURE_2D},7)") })
    }

    @Test
    fun unloadedTexture_childrenStillDrawn() {
        val parent = sprite(-1)
        val child = sprite(7)
        parent.attachChild(child)
        parent.onDraw(glState, camera)

        assertEquals(0, parent.draws)
        assertEquals(1, child.draws)
        assertTrue(QuadBatcher.hasPending)
    }

    @Test
    fun plainEntity_isSelfDrawable() {
        var drawn = false
        val entity = object : Entity() {
            override fun draw(pGLState: DrawContext, pCamera: Camera) {
                drawn = true
            }
        }
        entity.onDraw(glState, camera)
        assertTrue(drawn)
    }
}
