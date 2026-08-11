@file:JvmName("SkinFileCatalog")

package com.osudroid.resources.skin

import java.io.File

/**
 * How a single texture name should be resolved while (re)loading a skin.
 *
 * This mirrors the three outcomes `ResourceManager.loadCustomSkin` used to decide inline for each
 * texture name: use a file from the skin/beatmap folder, fall back to the bundled default-skin
 * asset, or unload a previously loaded texture because nothing provides it anymore.
 */
sealed class TextureResolution {
    /** Load from this specific file found in the skin/beatmap folder. */
    data class FromFile(val file: File) : TextureResolution()

    /**
     * Load the bundled default-skin asset "gfx/[assetFileName]". Callers must additionally parse
     * the frame index for this texture name afterwards (matching `loadCustomSkin`'s
     * `parseFrameIndex(textureName, false, false)` call, which historically only happens on this
     * default-asset fallback branch, not when a custom skin file is used).
     */
    data class FromDefaultAsset(val assetFileName: String) : TextureResolution()

    /** Unload the texture if it was previously loaded; nothing provides it anymore. */
    object Unload : TextureResolution()
}

/** How a single sound name should be resolved while (re)loading a skin. */
sealed class SoundResolution {
    /** Load from this specific file found in the skin/beatmap folder. */
    data class FromFile(val file: File) : SoundResolution()

    /** Load the bundled default-skin asset "sfx/[assetFileName]". */
    data class FromDefaultAsset(val assetFileName: String) : SoundResolution()
}

/**
 * Scans a skin folder's files into a name -> file map, mirroring `loadCustomSkin`'s file-list
 * construction:
 * - `comboburst*.wav`/`comboburst*.mp3` files are skipped (comboburst sounds are resolved
 *   separately, unconditionally, from the skin folder path - see [comboburstSoundFiles]).
 * - Filenames shorter than 5 characters (i.e. not even "x.ext") or empty files are skipped.
 * - A dedicated `hitcircle`/`hitcircleoverlay` file is aliased onto `sliderstartcircle(overlay)`/
 *   `sliderendcircle(overlay)` when the skin doesn't provide those textures directly. Because the
 *   alias is only applied with `putIfAbsent` while the direct entry is always overwritten, this is
 *   safe regardless of file iteration order: a dedicated slider file always wins over the alias,
 *   whichever one is scanned first.
 */
fun buildAvailableFiles(skinFiles: List<File>): Map<String, File> {
    val availableFiles = LinkedHashMap<String, File>()

    for (f in skinFiles) {
        if (!f.isFile) continue

        val name = f.name
        if (name.startsWith("comboburst") && (name.endsWith(".wav") || name.endsWith(".mp3"))) continue
        if (name.length < 5) continue
        if (f.length() == 0L) continue

        val filename = name.substring(0, name.length - 4)
        availableFiles[filename] = f

        if (filename == "hitcircle") {
            availableFiles.putIfAbsent("sliderstartcircle", f)
            availableFiles.putIfAbsent("sliderendcircle", f)
        }
        if (filename == "hitcircleoverlay") {
            availableFiles.putIfAbsent("sliderstartcircleoverlay", f)
            availableFiles.putIfAbsent("sliderendcircleoverlay", f)
        }
    }

    return availableFiles
}

/**
 * The subset of [availableFiles]' names that are animatable texture frames (per
 * [ANIMATABLE_TEXTURES]) - these are managed by [resolveAnimatableTextures] instead of the regular
 * per-asset resolution in [resolveDefaultTextures].
 */
fun animatableFilenames(availableFiles: Map<String, File>): List<String> =
    availableFiles.keys.filter { name -> ANIMATABLE_TEXTURES.any { name.startsWith(it) } }

/**
 * Resolves one bundled default-skin gfx asset (e.g. "cursor.png") against the skin folder's files.
 *
 * Returns `null` if this texture is an animatable frame that the skin folder itself provides
 * frames for (it's handled separately by [resolveAnimatableTextures] instead).
 */
fun resolveGfxAsset(
    assetFileName: String,
    availableFiles: Map<String, File>,
    animatableFilenames: List<String>,
    isDefaultSkin: Boolean,
): Pair<String, TextureResolution>? {
    val textureName = assetFileName.substring(0, assetFileName.length - 4)

    val managedByAnimatableFrames = ANIMATABLE_TEXTURES.any { animatable ->
        textureName.startsWith(animatable) && animatableFilenames.any { it.startsWith(animatable) }
    }
    if (managedByAnimatableFrames) return null

    val file = availableFiles[textureName]
    val resolution = when {
        file != null -> TextureResolution.FromFile(file)
        !isDefaultSkin && OPTIONAL_TEXTURES.any { textureName.startsWith(it) } -> TextureResolution.Unload
        else -> TextureResolution.FromDefaultAsset(assetFileName)
    }

    return textureName to resolution
}

/** Resolves every bundled default-skin gfx asset via [resolveGfxAsset], in the given order. */
fun resolveDefaultTextures(
    gfxAssetFileNames: List<String>,
    availableFiles: Map<String, File>,
    animatableFilenames: List<String>,
    isDefaultSkin: Boolean,
): List<Pair<String, TextureResolution>> =
    gfxAssetFileNames.mapNotNull { resolveGfxAsset(it, availableFiles, animatableFilenames, isDefaultSkin) }

/**
 * Resolves "scorebar-kidanger"/"scorebar-kidanger2", applied *after* [resolveDefaultTextures] (whose
 * OPTIONAL_TEXTURES handling already loads/unloads both individually). This only produces an
 * override when the skin folder provides a dedicated "scorebar-kidanger" file: if
 * "scorebar-kidanger2" isn't separately provided, it reuses the "scorebar-kidanger" file for both -
 * matching `loadCustomSkin`'s behavior exactly. Produces nothing if the skin provides neither.
 */
fun resolveKidangerTextures(availableFiles: Map<String, File>): List<Pair<String, TextureResolution>> {
    val kidangerFile = availableFiles["scorebar-kidanger"] ?: return emptyList()
    val kidanger2File = availableFiles["scorebar-kidanger2"] ?: kidangerFile

    return listOf(
        "scorebar-kidanger" to TextureResolution.FromFile(kidangerFile),
        "scorebar-kidanger2" to TextureResolution.FromFile(kidanger2File),
    )
}

/**
 * Resolves "comboburst" and the numbered "comboburst-0".."comboburst-9" textures. Unlike
 * [resolveDefaultTextures], there is no default-asset fallback here - comboburst textures are
 * entirely opt-in via skin files, so a missing file always means [TextureResolution.Unload].
 */
fun resolveComboburstTextures(availableFiles: Map<String, File>): List<Pair<String, TextureResolution>> {
    val result = ArrayList<Pair<String, TextureResolution>>(11)

    fun resolve(name: String) {
        val file = availableFiles[name]
        result += name to (if (file != null) TextureResolution.FromFile(file) else TextureResolution.Unload)
    }

    resolve("comboburst")
    for (i in 0 until 10) {
        resolve("comboburst-$i")
    }

    return result
}

/**
 * Resolves every animatable texture frame the skin folder provides (per [animatableFilenames]).
 * Unlike [resolveDefaultTextures], a [TextureResolution.FromFile] result here additionally requires
 * a `parseFrameIndex` call for the frame to register (matching `loadCustomSkin`'s
 * `parseFrameIndex(filename, false, false)` call in this branch).
 */
fun resolveAnimatableTextures(
    availableFiles: Map<String, File>,
    animatableFilenames: List<String>,
): List<Pair<String, TextureResolution>> =
    animatableFilenames.map { filename ->
        val file = availableFiles[filename]
        filename to (if (file != null) TextureResolution.FromFile(file) else TextureResolution.Unload)
    }

/** Resolves one bundled default-skin sfx asset (e.g. "hitnormal.wav") against the skin folder's files. */
fun resolveSfxAsset(assetFileName: String, availableFiles: Map<String, File>): Pair<String, SoundResolution> {
    val name = assetFileName.substring(0, assetFileName.length - 4)
    val file = availableFiles[name]

    return name to (if (file != null) SoundResolution.FromFile(file) else SoundResolution.FromDefaultAsset(assetFileName))
}

/** Resolves every bundled default-skin sfx asset via [resolveSfxAsset], in the given order. */
fun resolveDefaultSounds(
    sfxAssetFileNames: List<String>,
    availableFiles: Map<String, File>,
): List<Pair<String, SoundResolution>> =
    sfxAssetFileNames.map { resolveSfxAsset(it, availableFiles) }

/**
 * The (name, path) pairs for the comboburst sound set, resolved unconditionally against
 * [skinFolderPath] regardless of whether the files actually exist - matching `loadCustomSkin`'s
 * behavior, which always attempts `folder + "comboburst.wav"` etc. as an external load (whose own
 * failure-fallback to the bundled sfx asset is handled by the sound-loading primitive itself, not
 * by file resolution). Only produced when the skin folder itself exists.
 */
fun comboburstSoundPaths(skinFolderPath: String): List<Pair<String, String>> {
    val folder = if (skinFolderPath.endsWith("/")) skinFolderPath else "$skinFolderPath/"

    val result = ArrayList<Pair<String, String>>(11)
    result += "comboburst" to "${folder}comboburst.wav"
    for (i in 0 until 10) {
        result += "comboburst-$i" to "${folder}comboburst-$i.wav"
    }

    return result
}
