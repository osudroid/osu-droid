package org.andengine.graphics.gles

import android.os.Build
import org.andengine.opengl.exception.GLESVersionUnsupportedException

/**
 * What the current GL context supports, derived from `GL_VERSION` and `GL_EXTENSIONS`, plus its
 * `GL_MAX_TEXTURE_SIZE` (0 while unknown).
 */
class GlesCapabilities(val version: GlesVersion, val extensions: Set<String>, val maxTextureSize: Int = 0) {

    val hasGl31: Boolean get() = version >= GlesVersion.ES31

    val hasGl32: Boolean get() = version >= GlesVersion.ES32

    /** Debug output is core in ES 3.2 and available through `GL_KHR_debug` before that. */
    val hasDebugOutput: Boolean get() = hasGl32 || "GL_KHR_debug" in extensions

    val hasTimerQuery: Boolean get() = "GL_EXT_disjoint_timer_query" in extensions

    val hasExternalTextureEssl3: Boolean get() = "GL_OES_EGL_image_external_essl3" in extensions

    companion object {

        /**
         * @throws GLESVersionUnsupportedException if the version is below ES 3.0 or unreadable.
         */
        @JvmStatic
        @JvmOverloads
        fun detect(
            versionString: String?,
            extensionsString: String?,
            maxTextureSize: Int = 0
        ): GlesCapabilities {
            val parsed = GlesVersion.parse(versionString)
            if (parsed == null || parsed < GlesVersion.ES30) {
                throw GLESVersionUnsupportedException(
                    "OpenGL ES 3.0",
                    versionString ?: "Unknown",
                    Build.MANUFACTURER + " " + Build.MODEL,
                    Build.HARDWARE,
                    Build.VERSION.RELEASE + " (SDK " + Build.VERSION.SDK_INT + ")"
                )
            }

            val version = if (GlesDebugOptions.simulateEs30) GlesVersion.ES30 else parsed
            val extensions = extensionsString
                ?.split(' ')
                ?.filterTo(HashSet()) { it.isNotEmpty() }
                ?: emptySet<String>()

            return GlesCapabilities(version, extensions, maxTextureSize)
        }
    }
}
