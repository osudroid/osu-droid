package com.reco1l.andengine.buffered

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Buffered components draw only through the mesh renderer: the legacy path that bound a shader of
 * the component's own and uploaded its buffer to a VBO must not come back, neither as API on the
 * buffered component and its buffers nor as an override in the app.
 */
class LegacyPathGuardTest {

    @Test
    fun noComponentUsesLegacyShaderPath() {
        val findings = mutableListOf<String>()

        // The API itself: no hook a component could override, no GL upload in the buffers.
        for (method in UIBufferedComponent::class.java.declaredMethods) {
            if (method.name in LEGACY_COMPONENT_METHODS) findings += "UIBufferedComponent.${method.name}"
        }
        for (type in listOf(IBuffer::class.java, Buffer::class.java, VertexBuffer::class.java, TextureCoordinatesBuffer::class.java)) {
            for (method in type.declaredMethods) {
                if (method.name in LEGACY_BUFFER_METHODS) findings += "${type.simpleName}.${method.name}"
            }
        }

        // The app sources (where the buffered components live): nothing overrides, calls or
        // registers the legacy hooks.
        val root = File("src")
        assertTrue("Missing source root ${root.absolutePath}", root.isDirectory)
        root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .forEach { file ->
                file.readLines().forEachIndexed { index, line ->
                    val trimmed = line.trimStart()
                    if (trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")) {
                        return@forEachIndexed
                    }
                    if (LEGACY_SOURCE.containsMatchIn(line)) {
                        findings += "${file.invariantSeparatorsPath}:${index + 1}: ${line.trim()}"
                    }
                }
            }

        assertTrue(
            "Legacy buffered-component shader path in use (draw through the mesh renderer instead):\n" +
                findings.joinToString("\n"),
            findings.isEmpty()
        )
    }

    private companion object {
        val LEGACY_COMPONENT_METHODS = setOf("onBindShader", "getDrawsWithOwnShader", "onDeclarePointers")

        val LEGACY_BUFFER_METHODS = setOf(
            "beginDraw", "declarePointers", "draw", "bindAndUpload", "invalidateOnHardware",
            "setHardwareBufferNeedsUpdate", "unloadFromActiveBufferObjectManager", "onContextLost"
        )

        val LEGACY_SOURCE = Regex(
            """\b(?:onBindShader|drawsWithOwnShader|onDeclarePointers|declarePointers|bindAndUpload|invalidateOnHardware)\b|""" +
                """buffered\.Buffer::onContextLost"""
        )
    }
}
