package com.reco1l.andengine.ui

interface ISizeVariable {

    /**
     * The size variant of the UI component.
     */
    var sizeVariant: SizeVariant

}

/**
 * Represents the size variants for UI components.
 */
enum class SizeVariant {
    Small,
    Medium,
    Large;
}