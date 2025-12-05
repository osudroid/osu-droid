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
    val accentColor: Color4 = Color4(0xFFC2CAFF)
) {
    companion object {

        /**
         * The current theme. This is used to apply the theme to all entities that support theming.
         */
        var current = Theme()
            set(value) {
                if (field != value) {
                    field = value
                    UIEngine.current.onThemeChange(value)
                }
            }

    }
}

typealias StyleApplier = UIComponent.(theme: Theme) -> Unit

operator fun StyleApplier?.plus(other: StyleApplier): StyleApplier {
    return { theme ->
        this@plus?.invoke(this, theme)
        other.invoke(this, theme)
    }
}