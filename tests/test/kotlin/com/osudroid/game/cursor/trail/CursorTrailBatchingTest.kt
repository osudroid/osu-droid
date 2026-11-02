package com.osudroid.game.cursor.trail

import com.osudroid.resources.TextureStore
import com.osudroid.testing.Page
import com.osudroid.testing.RecordingGl
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.graphics.DrawContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.cursor.main.CursorSprite

@RunWith(RobolectricTestRunner::class)
class CursorTrailBatchingTest {

    private lateinit var gl: RecordingGl

    @Before
    fun setUp() {
        gl = RecordingGl().install()
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
    }

    @Test
    fun trailParts_oneDraw() {
        val region = TextureRegion(Page(7), 0f, 0f, 16f, 16f)
        // The cursor sprite looks its texture up in the resource manager; hand it one without assets.
        val resources = ResourceManager.getInstance()
        val store = ResourceManager::class.java.getDeclaredField("textureStore").apply { isAccessible = true }.get(resources) as TextureStore
        store.put("cursor", region)
        val trail = CursorTrail(region, CursorSprite())

        // Every part alive at once: stamped with the (still zero) clock of the trail.
        val spawn = CursorTrail::class.java.getDeclaredField("partSpawnMs").apply { isAccessible = true }.get(trail) as FloatArray
        spawn.fill(0f)

        val glState = DrawContext()
        trail.doDraw(glState, Camera(0f, 0f, 800f, 480f))
        QuadBatcher.flush()

        val draw = gl.calls.single { it.startsWith("glDrawElements(") }
        // The index count is the second argument.
        assertEquals(512 * 6, draw.split(",")[1].toInt())
        assertEquals(0, gl.count("glDrawArrays"))
    }
}
