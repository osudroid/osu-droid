package org.andengine.graphics.gles

/** An OpenGL ES version as reported by `GL_VERSION`. */
data class GlesVersion(val major: Int, val minor: Int) : Comparable<GlesVersion> {

    override fun compareTo(other: GlesVersion): Int =
        if (major != other.major) major.compareTo(other.major) else minor.compareTo(other.minor)

    override fun toString(): String = "$major.$minor"

    companion object {

        @JvmField val ES30 = GlesVersion(3, 0)
        @JvmField val ES31 = GlesVersion(3, 1)
        @JvmField val ES32 = GlesVersion(3, 2)

        private val PATTERN = Regex("""OpenGL ES (\d+)\.(\d+)""")

        /** Parses a `GL_VERSION` string such as `OpenGL ES 3.2 V@0530.57`, or returns null if unreadable. */
        @JvmStatic
        fun parse(glVersion: String?): GlesVersion? {
            if (glVersion == null) return null
            val match = PATTERN.find(glVersion) ?: return null
            val major = match.groupValues[1].toIntOrNull() ?: return null
            val minor = match.groupValues[2].toIntOrNull() ?: return null
            return GlesVersion(major, minor)
        }
    }
}
