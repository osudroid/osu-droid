package com.reco1l.andengine.ui

interface ISizeVariable {

    /**
     * The size variant of the UI component.
     */
    var sizeVariant: SizeVariant


    /**
     * Should be called when the size variant changes.
     */
    fun onSizeVariantChanged() = Unit
}

/**
 * Represents the size variants for UI components.
 */
enum class SizeVariant {
    Small,
    Medium,
    Large;
}