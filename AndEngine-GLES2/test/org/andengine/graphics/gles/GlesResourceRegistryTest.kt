package org.andengine.graphics.gles

import android.opengl.GLES20
import android.util.Log
import org.andengine.graphics.GpuResource
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLog

@RunWith(RobolectricTestRunner::class)
class GlesResourceRegistryTest {

    private val log = ArrayList<String>()
    private val registered = ArrayList<GlesRestorable>()
    private lateinit var gl: RecordingGL

    private open inner class Fake(val name: String, override val restorePhase: RestorePhase) : GlesRestorable {
        override fun onContextLost() {
            log += "lost:$name"
        }

        override fun restore(backend: GlesBackend) {
            log += "restore:$name"
        }
    }

    private inner class Throwing(
        name: String,
        phase: RestorePhase,
        private val throwOnLost: Boolean,
        private val throwOnRestore: Boolean
    ) : Fake(name, phase) {
        override fun onContextLost() {
            super.onContextLost()
            if (throwOnLost) throw IllegalStateException("lost boom")
        }

        override fun restore(backend: GlesBackend) {
            super.restore(backend)
            if (throwOnRestore) throw IllegalStateException("restore boom")
        }

        override fun toString() = "Throwing($name)"
    }

    private inner class FakeResource(name: String, phase: RestorePhase) : Fake(name, phase), GpuResource {
        override val label: String get() = name
        override var isDisposed: Boolean = false

        override fun dispose() {
            isDisposed = true
        }
    }

    private fun register(r: GlesRestorable) {
        GlesBackend.registry.register(r)
        registered += r
    }

    @Before
    fun setUp() {
        gl = RecordingGL()
        gl.intValues[GLES20.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS] = 16
        GlesBackend.onContextCreatedForTest(gl)
    }

    @After
    fun tearDown() {
        for (r in registered) GlesBackend.registry.unregister(r)
    }

    @Test
    fun restoreAll_orderAndCacheInvalidated() {
        register(Fake("tex", RestorePhase.Textures))
        register(Fake("prog", RestorePhase.Programs))
        register(Fake("legacy", RestorePhase.Legacy))

        GlesBackend.state.useProgram(1)
        gl.calls.clear()

        GlesBackend.onContextCreatedForTest(gl)

        assertEquals(
            listOf("lost:tex", "lost:prog", "lost:legacy", "restore:prog", "restore:tex", "restore:legacy"),
            log
        )

        gl.calls.clear()
        GlesBackend.state.useProgram(1)
        assertTrue(gl.calls.contains("glUseProgram(1)"))
    }

    @Test
    fun register_duplicate_isIgnored() {
        val before = GlesBackend.registry.size
        val fake = Fake("once", RestorePhase.Textures)
        register(fake)
        register(fake)
        assertEquals(before + 1, GlesBackend.registry.size)

        GlesBackend.onContextCreatedForTest(gl)

        assertEquals(1, log.count { it == "lost:once" })
        assertEquals(1, log.count { it == "restore:once" })
    }

    @Test
    fun disposedResource_notRestored() {
        val disposed = FakeResource("gone", RestorePhase.Textures)
        register(disposed)
        register(Fake("alive", RestorePhase.Textures))
        disposed.dispose()

        GlesBackend.onContextCreatedForTest(gl)

        assertTrue("restore:gone" !in log)
        assertTrue("restore:alive" in log)
    }

    @Test
    fun throwingEntry_doesNotStopOthers() {
        ShadowLog.clear()
        register(Fake("progA", RestorePhase.Programs))
        register(Throwing("badLost", RestorePhase.Programs, throwOnLost = true, throwOnRestore = false))
        register(Throwing("badRestore", RestorePhase.Programs, throwOnLost = false, throwOnRestore = true))
        register(Fake("progB", RestorePhase.Programs))
        register(Fake("tex", RestorePhase.Textures))

        GlesBackend.onContextCreatedForTest(gl)

        assertEquals(
            listOf(
                "lost:progA", "lost:badLost", "lost:badRestore", "lost:progB", "lost:tex",
                "restore:progA", "restore:badLost", "restore:badRestore", "restore:progB", "restore:tex"
            ),
            log
        )
        val errors = ShadowLog.getLogsForTag(GlesResourceRegistry.TAG).filter { it.type == Log.ERROR }
        assertEquals(2, errors.size)
        assertTrue(errors[0].msg.contains("onContextLost") && errors[0].msg.contains("Throwing(badLost)"))
        assertEquals("lost boom", errors[0].throwable.message)
        assertTrue(errors[1].msg.contains("restore") && errors[1].msg.contains("Throwing(badRestore)"))
        assertEquals("restore boom", errors[1].throwable.message)
    }

    @Test
    fun unregister_removes() {
        val fake = Fake("x", RestorePhase.Buffers)
        register(fake)
        val before = GlesBackend.registry.size

        GlesBackend.registry.unregister(fake)

        assertEquals(before - 1, GlesBackend.registry.size)
        GlesBackend.onContextCreatedForTest(gl)
        assertTrue(log.isEmpty())
    }
}
