package com.reco1l.andengine.ui

interface IColorVariable {

    /**
     * The color variant of the UI component.
     */
    var colorVariant: ColorVariant

}

enum class ColorVariant {
    Primary,
    Secondary,
    Tertiary
}