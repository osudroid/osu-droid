package com.osudroid.game.cursor.trail

import android.opengl.GLES20
import java.lang.reflect.Proxy
import com.osudroid.resources.TextureStore
import org.andengine.engine.camera.Camera
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GL32
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.TextureManager
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas
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

    private val calls = ArrayList<String>()
    private val drawnIndices = ArrayList<Int>()
    private var nextId = 1

    /** Records every GL call by name; just enough answers for the batcher to build its resources. */
    private fun fakeGl(): GL32 = Proxy.newProxyInstance(GL32::class.java.classLoader, arrayOf(GL32::class.java)) { _, method, args ->
        val name = method.name
        calls += name
        if (name == "glDrawElements") drawnIndices += args!![1] as Int
        when {
            name.startsWith("glGen") && args != null && args.size == 3 && args[1] is IntArray -> {
                val ids = args[1] as IntArray
                for (i in 0 until args[0] as Int) ids[args[2] as Int + i] = nextId++
                null
            }
            name == "glCreateShader" || name == "glCreateProgram" -> nextId++
            name == "glGetShaderiv" || name == "glGetProgramiv" -> {
                (args!![2] as IntArray)[args[3] as Int] = 1
                null
            }
            name == "glGetIntegerv" -> {
                val value = when (args!![0] as Int) {
                    GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS -> 16
                    GLES20.GL_MAX_TEXTURE_SIZE -> 4096
                    else -> 0
                }
                (args[1] as IntArray)[args[2] as Int] = value
                null
            }
            name == "glGetString" -> if (args!![0] == GLES20.GL_VERSION) "OpenGL ES 3.2 test" else ""
            name == "glGetShaderInfoLog" || name == "glGetProgramInfoLog" -> ""
            name == "hashCode" -> System.identityHashCode(this)
            name == "equals" -> false
            name == "toString" -> "FakeGL"
            else -> when (method.returnType) {
                Int::class.javaPrimitiveType -> 0
                Long::class.javaPrimitiveType -> 0L
                Boolean::class.javaPrimitiveType -> false
                Float::class.javaPrimitiveType -> 0f
                else -> null
            }
        }
    } as GL32

    private class Page : BitmapTextureAtlas(TextureManager(), 16, 16, TextureOptions.BILINEAR) {
        init {
            mHardwareTextureID = 7
        }
    }

    @Before
    fun setUp() {
        GlesBackend.onContextCreatedForTest(fakeGl())
        GlesBackend.surfaceWidth = 800
        GlesBackend.surfaceHeight = 480
        calls.clear()
        drawnIndices.clear()
    }

    @After
    fun tearDown() {
        QuadBatcher.releaseResources()
    }

    @Test
    fun trailParts_oneDraw() {
        val region = TextureRegion(Page(), 0f, 0f, 16f, 16f)
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

        assertEquals(1, calls.count { it == "glDrawElements" })
        assertEquals(listOf(512 * 6), drawnIndices)
        assertEquals(0, calls.count { it == "glDrawArrays" })
    }
}
