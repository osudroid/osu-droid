package org.andengine.graphics.gles

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every GLSL shader must declare `#version 300 es`, the lowest version of the ES 3.0 - 3.2
 * context range the GL core supports, so the ES 3.0/3.1 fallback compiles all shaders. Shader
 * source must also not use GLSL ES 1.00 keywords, which `#version 300 es` rejects.
 */
class ShaderVersionTest {

    private val roots = listOf(File("src"), File("AndEngine-GLES2/src"))

    @Test
    fun shadersUseEs300() {
        roots.forEach { assertTrue("Missing source root ${it.absolutePath}", it.isDirectory) }

        var found = 0
        val offenders = mutableListOf<String>()

        for (root in roots) {
            root.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val path = file.invariantSeparatorsPath
                    file.readLines().forEachIndexed { index, line ->
                        val trimmed = line.trimStart()
                        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                            return@forEachIndexed
                        }
                        if (!line.contains("#version")) return@forEachIndexed

                        found++
                        if (!ES_300.containsMatchIn(line)) {
                            offenders += "$path:${index + 1}: ${line.trim()}"
                        }
                    }
                }
        }

        assertTrue("No #version directive found; the scan is vacuous", found > 0)
        assertTrue(
            "Shaders must use '#version 300 es':\n" + offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    /**
     * Flags GLSL ES 1.00 keywords in shader-like string literals: regular `"..."` literals on
     * non-comment lines, and every line inside a Kotlin raw (triple-quoted) string. Deliberately
     * narrow (`attribute <type>`, `varying `, `gl_FragColor`) so comments and prose do not match.
     */
    @Test
    fun shadersUseNoGlslEs100Keywords() {
        // The detector itself must work, or the scan below would be vacuous.
        assertTrue(isEs100Source("\t\"attribute vec4 \" + ATTRIBUTE_POSITION + \";\\n\" +", inRawString = false))
        assertTrue(isEs100Source("\"attribute highp vec2 a;\"", inRawString = false))
        assertTrue(isEs100Source("\"varying vec2 v;\"", inRawString = false))
        assertTrue(isEs100Source("    gl_FragColor = color;", inRawString = true))
        assertFalse(isEs100Source("// varying attribute vec4 gl_FragColor", inRawString = false))
        assertFalse(isEs100Source(" * results may be varying across devices", inRawString = false))
        assertFalse(isEs100Source("\"in vec2 v;\" // varying ", inRawString = false))

        val offenders = mutableListOf<String>()

        for (root in roots) {
            assertTrue("Missing source root ${root.absolutePath}", root.isDirectory)
            root.walkTopDown()
                .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
                .forEach { file ->
                    val path = file.invariantSeparatorsPath
                    var inRawString = false
                    file.readLines().forEachIndexed { index, line ->
                        // Segments between triple quotes alternate between code and raw string.
                        val segments = line.split(RAW_QUOTE)
                        for (s in segments.indices) {
                            if (isEs100Source(segments[s], inRawString)) {
                                offenders += "$path:${index + 1}: ${line.trim()}"
                                break
                            }
                            if (s < segments.lastIndex) inRawString = !inRawString
                        }
                    }
                }
        }

        assertTrue(
            "Shaders must not use GLSL ES 1.00 keywords (attribute/varying/gl_FragColor):\n" +
                offenders.joinToString("\n"),
            offenders.isEmpty()
        )
    }

    /** Whether [segment] holds GLSL ES 1.00 source; [inRawString] when it lies inside a raw string. */
    private fun isEs100Source(segment: String, inRawString: Boolean): Boolean {
        if (inRawString) return ES_100.containsMatchIn(segment)

        val trimmed = segment.trimStart()
        if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) return false

        return STRING_LITERAL.findAll(segment).any { ES_100.containsMatchIn(it.value) }
    }

    private companion object {
        val ES_300 = Regex("""#version 300 es(?![\w])""")

        val RAW_QUOTE = "\"".repeat(3)

        /** A single-line string literal, honouring escaped characters. */
        val STRING_LITERAL = Regex("\"(?:[^\"\\\\]|\\\\.)*\"")

        val ES_100 = Regex(
            """\battribute\s+(?:(?:lowp|mediump|highp)\s+)?(?:float|vec[234]|mat[234])\b|\bvarying\s|\bgl_FragColor\b"""
        )
    }
}
