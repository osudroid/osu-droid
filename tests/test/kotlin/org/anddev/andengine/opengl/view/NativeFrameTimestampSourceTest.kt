package org.anddev.andengine.opengl.view

import org.junit.Assert.assertFalse
import org.junit.Test

class NativeFrameTimestampSourceTest {
    @Test
    fun `Start reports unsupported below API 26`() {
        assertFalse(NativeFrameTimestampSource(25).start())
    }

    @Test
    fun `Start reports unsupported when the library cannot load`() {
        // The JVM has no osudroid_frame_timestamps library, so loading fails.
        // start() must not throw.
        assertFalse(NativeFrameTimestampSource(34).start())
        assertFalse(NativeFrameTimestampSource(34).start())
    }

    @Test
    fun `Stop does not throw without a successful start`() {
        val source = NativeFrameTimestampSource(34)

        source.stop()
        source.start()
        source.stop()
    }
}
