@file:Suppress("LeakingThis")

package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Radius
import com.reco1l.andengine.theme.srem
import com.reco1l.framework.*
import com.reco1l.framework.math.*


/**
 * A badge is a small piece of information that can be used to display a value or a status.
 */
open class UIBadge : CompoundText(), ISizeVariable, IColorVariable {

    override var sizeVariant = SizeVariant.Medium
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }

    override var colorVariant = ColorVariant.Secondary
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }


    init {
        style = {
            when (colorVariant) {
                ColorVariant.Primary -> {
                    color = it.accentColor
                    backgroundColor = it.accentColor * 0.15f
                }
                ColorVariant.Secondary -> {
                    color = it.accentColor * 0.15f
                    backgroundColor = it.accentColor
                }
                ColorVariant.Tertiary -> {
                    color = it.accentColor
                    backgroundColor = Color4.Transparent
                }
            }

            applySizeStyle()
        }
    }

    protected fun applySizeStyle() {
        when (sizeVariant) {
            SizeVariant.Small -> {
                fontSize = FontSize.XS
                padding = Vec4(1.25f.srem, 0.75f.srem)
                spacing = 1.25f.srem
                radius = Radius.SM
            }
            SizeVariant.Medium -> {
                fontSize = FontSize.SM
                padding = Vec4(2f.srem, 1.25f.srem)
                spacing = 2f.srem
                radius = Radius.LG
            }
            SizeVariant.Large -> {
                fontSize = FontSize.MD
                padding = Vec4(2f.srem, 1.5f.srem)
                spacing = 2f.srem
                radius = Radius.LG
            }
        }
    }

}

/**
 * A statistic badge is a badge that displays a value next to a label.
 */
open class UILabeledBadge : UILinearContainer(), ISizeVariable, IColorVariable {

    override var sizeVariant = SizeVariant.Medium
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }

    override var colorVariant = ColorVariant.Secondary
        set(value) {
            if (field != value) {
                field = value
                applyStyle()
            }
        }


    /**
     * The entity of the badge's label.
     */
    val labelComponent = text {
        alignment = Anchor.Center
        backgroundColor = Color4.Black / 0.1f
    }

    /**
     * The value of the badge.
     */
    val valueComponent = text {
        fontSize = FontSize.SM
        alignment = Anchor.Center
    }

    //region Shortcuts

    /**
     * The label of the badge.
     */
    var label by labelComponent::text

    /**
     * The value of the badge.
     */
    var value by valueComponent::text

    //endregion


    init {
        orientation = Orientation.Horizontal
        style = {
            when (colorVariant) {
                ColorVariant.Primary -> {
                    labelComponent.color = it.accentColor * 0.15f
                    valueComponent.color = it.accentColor * 0.15f
                    backgroundColor = it.accentColor
                }
                ColorVariant.Secondary -> {
                    labelComponent.color = it.accentColor
                    valueComponent.color = it.accentColor
                    backgroundColor = it.accentColor * 0.15f
                }

                ColorVariant.Tertiary -> TODO()
            }

            when (sizeVariant) {
                SizeVariant.Small -> {
                    labelComponent.fontSize = FontSize.XS
                    valueComponent.fontSize = FontSize.XS
                    labelComponent.padding = Vec4(1.25f.srem, 0.75f.srem)
                    valueComponent.padding = Vec4(1.25f.srem, 0.75f.srem)
                    radius = Radius.MD
                    labelComponent.radius = Radius.MD
                }
                SizeVariant.Medium -> {
                    labelComponent.fontSize = FontSize.SM
                    valueComponent.fontSize = FontSize.SM
                    labelComponent.padding = Vec4(2f.srem, 1.25f.srem)
                    valueComponent.padding = Vec4(2f.srem, 1.25f.srem)
                    radius = Radius.LG
                    labelComponent.radius = Radius.LG
                }
                SizeVariant.Large -> {
                    labelComponent.fontSize = FontSize.MD
                    valueComponent.fontSize = FontSize.MD
                    labelComponent.padding = Vec4(2f.srem, 1.5f.srem)
                    valueComponent.padding = Vec4(2f.srem, 1.5f.srem)
                    radius = Radius.LG
                    labelComponent.radius = Radius.LG
                }
            }
        }
    }

}



