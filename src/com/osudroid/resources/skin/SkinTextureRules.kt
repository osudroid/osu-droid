@file:JvmName("SkinTextureRules")

package com.osudroid.resources.skin

/**
 * The textures that shouldn't fall back to the default skin if they're not present in the skin folder.
 */
@JvmField
val OPTIONAL_TEXTURES = arrayOf(
    "scorebar-marker",
    "scorebar-ki",
    "scorebar-kidanger",
    "scorebar-kidanger2",
)

/**
 * The textures that can be animated.
 */
@JvmField
val ANIMATABLE_TEXTURES = arrayOf(
    "followpoint-",
    "hit0-",
    "hit100-",
    "hit100k-",
    "hit300-",
    "hit300g-",
    "hit300k-",
    "hit50-",
    "menu-back-",
    "play-skip-",
    "scorebar-colour-",
    "sliderb",
    "sliderfollowcircle-",
)

/**
 * The first capturing group refers to the texture's base name. The name may contain one or more hyphens/dashes
 * in the name (e.g. `menu-back`), but it should never end with a hyphen/dash.
 *
 * The second capturing group refers to the frame index. A hyphen/dash may be present before the frame index
 * (e.g., `menu-back-0` (with hyphen) or `sliderb0` (without hyphen)).
 */
private val ANIMATABLE_TEXTURE_REGEX = Regex("^(${ANIMATABLE_TEXTURES.joinToString("|")})(\\d+)$")

/**
 * The result of successfully matching a filename (without extension) against [ANIMATABLE_TEXTURES].
 *
 * @param textureName The animatable texture's base name, with any trailing hyphen stripped.
 * @param frameIndex The frame index parsed from the filename.
 */
data class AnimatableFrameMatch(val textureName: String, val frameIndex: Int)

/**
 * Matches [filename] (without extension) against [ANIMATABLE_TEXTURES], returning the parsed base texture name
 * and frame index, or `null` if the filename does not belong to any animatable texture.
 */
@JvmName("matchAnimatableFrame")
fun matchAnimatableFrame(filename: String): AnimatableFrameMatch? {
    val result = ANIMATABLE_TEXTURE_REGEX.matchEntire(filename) ?: return null

    var textureName = result.groupValues[1]
    if (textureName.endsWith("-")) {
        textureName = textureName.dropLast(1)
    }

    val frameIndex = result.groupValues[2].toInt()

    return AnimatableFrameMatch(textureName, frameIndex)
}
