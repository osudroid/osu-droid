package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.framework.*

/**
 * A theme is a set of colors and styles that can be applied to an entity.
 */
data class Theme(

    /**
     * The accent color of the theme.
     */
    val accentColor: Color4 = Color4(0xFFC2CAFF),

    ) {
    companion object {

        /**
         * The current theme. This is used to apply the theme to all entities that support theming.
         */
        var current = Theme()
            set(value) {
                if (field != value) {
                    field = value
                    ExtendedEngine.Current.onThemeChange(value)
                }
            }

    }
}

interface IThemeable {

    /**
     * Called when the theme is changed. This is used to apply the theme to the entity.
     */
    var applyTheme: UIComponent.(theme: Theme) -> Unit
}