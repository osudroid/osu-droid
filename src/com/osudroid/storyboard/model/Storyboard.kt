package com.osudroid.storyboard.model

/**
 * Represents a parsed storyboard, merged from a beatmap's `.osu` file and the beatmap set's
 * `.osb` file, if present.
 */
class Storyboard {
    /**
     * The elements of this storyboard, grouped per layer in declaration order.
     */
    @JvmField
    val layers = LinkedHashMap<StoryboardLayerType, MutableList<StoryboardElement>>().also {
        for (layer in StoryboardLayerType.entries) {
            it[layer] = mutableListOf()
        }
    }

    /**
     * The audio samples of this storyboard.
     */
    @JvmField
    val samples = mutableListOf<StoryboardSample>()

    /**
     * Whether this storyboard should be displayed in the full 16:9 widescreen area instead of
     * being letterboxed to 4:3, per the `WidescreenStoryboard` setting of the beatmap.
     */
    @JvmField
    var widescreen = false

    /**
     * Whether textures of the current skin are preferred over the textures in the beatmap folder,
     * per the `UseSkinSprites` setting of the beatmap.
     */
    @JvmField
    var useSkinSprites = false

    /**
     * The file name of the beatmap's background image, as declared in the `[Events]` section.
     */
    @JvmField
    var backgroundFilename: String? = null

    /**
     * All elements of this storyboard across all layers.
     */
    val elements
        get() = layers.values.asSequence().flatten()

    /**
     * Whether this storyboard has neither elements nor samples.
     */
    val isEmpty
        get() = samples.isEmpty() && layers.values.all { it.isEmpty() }

    /**
     * Adds an element to its layer.
     *
     * @param element The element to add.
     */
    fun add(element: StoryboardElement) {
        layers[element.layer]!!.add(element)
    }

    /**
     * Counts how often each texture path is referenced by the elements of this storyboard.
     * Used to decide which textures are worth packing into a shared atlas.
     */
    fun textureUsageCounts(): Map<String, Int> {
        val counts = HashMap<String, Int>()

        for (element in elements) {
            for (path in element.texturePaths) {
                counts[path] = (counts[path] ?: 0) + 1
            }
        }

        return counts
    }

    /**
     * Determines whether this storyboard displays the beatmap's background image as one of its
     * own elements, in which case the static background should be hidden during gameplay.
     */
    fun usesBackgroundImage(): Boolean {
        val background = backgroundFilename?.let { normalizePath(it) } ?: return false

        return layers[StoryboardLayerType.Background]!!.any { normalizePath(it.filePath) == background }
    }

    private fun normalizePath(path: String) = path.replace('\\', '/').trim('"').lowercase()
}
