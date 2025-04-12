import java.io.File

/**
 * Utility object to manage test resources.
 */
object TestResourceManager {
    /**
     * Obtains a beatmap file from the `test/resources/beatmaps` directory.
     *
     * @param name The name of the beatmap file (without the `.osu` extension).
     * @return The [File] if it exists, or `null` if it doesn't.
     */
    fun getBeatmapFile(name: String) = getTestResource("beatmaps/$name.osu")

    /**
     * Obtains a [File] from the `test/resources` directory.
     *
     * @param path The path to the file relative to the `test/resources` directory.
     * @return The [File] if it exists, or `null` if it doesn't.
     */
    fun getTestResource(path: String): File? {
        val classLoader = this::class.java.classLoader!!
        val resource = classLoader.getResource(path)

        return if (resource != null) File(resource.toURI()) else null
    }
}