package com.reco1l.andengine.ui

/**
 * An interface for themes.
 */
interface ITheme

/**
 * An interface for entities that have a theme.
 */
interface IWithTheme<T : ITheme?> {

    /**
     * The theme of the entity.
     */
    var theme: T


    /**
     * Called when the theme changes.
     */
    fun onThemeChanged()
}