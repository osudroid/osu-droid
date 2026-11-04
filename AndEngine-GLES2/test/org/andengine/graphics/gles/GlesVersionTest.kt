package org.andengine.graphics.gles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GlesVersionTest {

    @Test
    fun parse_typicalStrings() {
        assertEquals(GlesVersion(3, 2), GlesVersion.parse("OpenGL ES 3.2 V@0530.57 (GIT@4bbe300fc3)"))
        assertEquals(GlesVersion(3, 2), GlesVersion.parse("OpenGL ES 3.2 v1.r18p0-01rel0.1ba20d94cbd7"))
        assertEquals(GlesVersion(3, 0), GlesVersion.parse("OpenGL ES 3.0 build 1.13@5776728"))
        assertEquals(GlesVersion(2, 0), GlesVersion.parse("OpenGL ES 2.0"))
    }

    @Test
    fun parse_garbageReturnsNull() {
        assertNull(GlesVersion.parse(null))
        assertNull(GlesVersion.parse(""))
        assertNull(GlesVersion.parse("WebGL"))
    }
}
