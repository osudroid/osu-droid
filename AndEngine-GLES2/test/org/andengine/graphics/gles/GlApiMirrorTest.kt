package org.andengine.graphics.gles

import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@RunWith(RobolectricTestRunner::class)
class GlApiMirrorTest {

    /**
     * Broken overloads the runtime keeps public but the SDK hides, so no caller can compile against them.
     */
    private val hiddenInSdk = setOf(
        "GLES20.glGetActiveAttrib(int, int, int, IntBuffer, IntBuffer, IntBuffer, byte)",
        "GLES20.glGetActiveUniform(int, int, int, IntBuffer, IntBuffer, IntBuffer, byte)",
        "GLES20.glGetShaderSource(int, int, IntBuffer, byte)",
    )

    private fun publicStaticMethods(type: Class<*>): List<Method> =
        type.declaredMethods.filter { Modifier.isStatic(it.modifiers) && Modifier.isPublic(it.modifiers) }

    private fun describe(method: Method) =
        "${method.declaringClass.simpleName}.${method.name}(${method.parameterTypes.joinToString { it.simpleName }})"

    private fun assertMirrored(level: Class<*>, gl: Class<*>) {
        val missing = publicStaticMethods(level).filter { describe(it) !in hiddenInSdk }.filter { method ->
            val mirror = runCatching { gl.getMethod(method.name, *method.parameterTypes) }.getOrNull()
            mirror == null || mirror.returnType != method.returnType
        }
        assertTrue("Not mirrored on ${gl.simpleName}: ${missing.map(::describe)}", missing.isEmpty())
    }

    @Test
    fun gl30_declaresEveryGles20AndGles30Function() {
        assertMirrored(GLES20::class.java, GL30::class.java)
        assertMirrored(GLES30::class.java, GL30::class.java)
    }

    @Test
    fun gl31_declaresEveryGles31Function() {
        assertMirrored(GLES31::class.java, GL31::class.java)
    }

    @Test
    fun gl32_declaresEveryGles32Function() {
        assertMirrored(GLES32::class.java, GL32::class.java)
    }
}
