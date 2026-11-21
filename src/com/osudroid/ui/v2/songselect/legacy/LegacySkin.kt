package com.osudroid.ui.v2.songselect.legacy

import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.ui.Theme
import com.reco1l.framework.Color4
import org.andengine.opengl.texture.region.TextureRegion
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.skins.OsuSkin

/**
 * The optional legacy look of the song select: its panels and buttons are drawn with the images and the colors
 * that the skin has for the legacy song menu.
 *
 * The colors that a skin sets for the items of the legacy song menu are used either way. Where it sets none, they
 * are the ones of the legacy song menu while the legacy look [isEnabled], and the accent color of the theme
 * otherwise.
 */
internal object LegacySkin {

    /**
     * Whether the legacy look is used, see [refresh].
     */
    var isEnabled = false
        private set

    /**
     * The color of the beatmap set panels.
     */
    val setColor: Color4
        get() = skinColor("MenuItemDefaultColor") ?: if (isEnabled) DEFAULT_SET_COLOR else Theme.current.accentColor

    /**
     * The color of the difficulty panels.
     */
    val difficultyColor: Color4
        get() = skinColor("MenuItemVersionsDefaultColor") ?: if (isEnabled) DEFAULT_DIFFICULTY_COLOR else Theme.current.accentColor

    /**
     * The color of the selected difficulty panel.
     */
    val selectedDifficultyColor: Color4
        get() = skinColor("MenuItemVersionsSelectedColor") ?: if (isEnabled) Colors.White else difficultyColor

    /**
     * The color of the texts on the difficulty panels, only for the legacy look.
     */
    val textColor: Color4
        get() = skinColor("MenuItemDefaultTextColor") ?: Colors.White

    /**
     * The color of the texts on the selected difficulty panel, only for the legacy look.
     */
    val selectedTextColor: Color4
        get() = skinColor("MenuItemSelectedTextColor") ?: Colors.Black

    /**
     * The item background of the skin, `null` without the legacy look.
     */
    val itemBackground: TextureRegion?
        get() = if (isEnabled) ResourceManager.getInstance().getTexture("menu-button-background") else null

    /**
     * Counts up whenever [refresh] finds the setting, the image or a color changed. Whatever is drawn with them
     * compares it with the version it was drawn for.
     */
    var version = 0
        private set

    private var state: List<Any?> = emptyList()


    /**
     * Reads the setting and the skin again, they may have been changed while the song select was not shown.
     */
    fun refresh() {
        isEnabled = Config.getBoolean(PREFERENCE, false)

        val current = listOf(isEnabled, itemBackground, setColor, difficultyColor, selectedDifficultyColor, textColor, selectedTextColor)

        if (current != state) {
            state = current
            version++
        }
    }

    /**
     * Black or white, whichever is easier to read on the [background].
     */
    fun contrastOf(background: Color4): Color4 {
        val luminance = 0.299f * background.red + 0.587f * background.green + 0.114f * background.blue

        return if (luminance > 0.5f) Colors.Black else Colors.White
    }

    private fun skinColor(name: String): Color4? = OsuSkin.get().getColor(name, null)


    private const val PREFERENCE = "songSelectLegacySkin"

    private val DEFAULT_SET_COLOR = Color4(240, 150, 0)
    private val DEFAULT_DIFFICULTY_COLOR = Color4(25, 25, 240)

}
