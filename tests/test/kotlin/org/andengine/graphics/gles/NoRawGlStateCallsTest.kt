package org.andengine.graphics.gles

import com.osudroid.testing.codeLines
import com.osudroid.testing.sourceFiles
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fails when a state-setting `android.opengl.GLES2x/3x.gl*` call exists outside the GL backend
 * (`org.andengine.graphics.gles`). Such calls must go through [GlesBackend.state] instead.
 */
class NoRawGlStateCallsTest {

    @Test
    fun noRawStateCallsOutsideBackend() {
        val excluded = File("AndEngine-GLES2/src/org/andengine/graphics/gles")
        val roots = listOf(File("src"), File("AndEngine-GLES2/src"))
        roots.forEach { assertTrue("Missing source root ${it.absolutePath}", it.isDirectory) }

        val findings = sourceFiles(roots, excluded)
            .flatMap { codeLines(it) }
            .filter { (_, line) -> STATE_CALL.containsMatchIn(line) }
            .map { (location, line) -> "$location: ${line.trim()}" }
            .toList()

        assertTrue(
            "Raw GL state calls outside org.andengine.graphics.gles (use GlesBackend.state):\n" +
                findings.joinToString("\n"),
            findings.isEmpty()
        )
    }

    private companion object {
        val NAMES = listOf(
            "Enable", "Disable", "BindBuffer", "BindTexture", "BindFramebuffer", "BindRenderbuffer",
            "BindVertexArray", "UseProgram", "ActiveTexture", "BlendFunc", "BlendFuncSeparate",
            "BlendEquation", "DepthFunc", "DepthMask", "ClearDepthf", "CullFace", "FrontFace",
            "Scissor", "Viewport", "ClearColor", "Clear", "LineWidth", "EnableVertexAttribArray",
            "DisableVertexAttribArray", "VertexAttribPointer", "VertexAttrib4f"
        )

        val STATE_CALL = Regex("""(?:GLES2[0-9]?|GLES3[0-2])\.gl(?:${NAMES.joinToString("|")})\s*\(""")
    }
}
