@file:Suppress("LeakingThis")

package com.reco1l.andengine.ui

import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.SizeVariant.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.engine.camera.*
import org.anddev.andengine.opengl.font.Font
import ru.nsu.ccfit.zuev.osu.ResourceManager
import javax.microedition.khronos.opengles.*


/**
 * A badge is a small piece of information that can be used to display a value or a status.
 */
open class UIBadge : CompoundText(), ISizeVariable {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        color = theme.accentColor
        background?.color = theme.accentColor * 0.15f
    }

    override var sizeVariant = Medium
        set(value) {
            if (field != value) {
                field = value
                onSizeVariantChanged()
            }
        }

    init {
        background = UIBox()
        onSizeVariantChanged()
    }


    override fun onSizeVariantChanged() {

        val cornerRadius: Float

        when (sizeVariant) {
            Small -> {
                font = ResourceManager.getInstance().getFont("xs")
                padding = Vec4(8f, 4f)
                spacing = 4f
                cornerRadius = 6f
            }
            Medium -> {
                font = ResourceManager.getInstance().getFont("smallFont")
                padding = Vec4(12f, 8f)
                spacing = 8f
                cornerRadius = 12f
            }
            Large -> {
                font = ResourceManager.getInstance().getFont("font")
                padding = Vec4(16f, 12f)
                spacing = 12f
                cornerRadius = 16f
            }
        }

        (background as? UIBox)?.cornerRadius = cornerRadius
    }
}

/**
 * A statistic badge is a badge that displays a value next to a label.
 */
open class UILabeledBadge : UILinearContainer(), ISizeVariable {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        color = theme.accentColor
        background?.color = theme.accentColor * 0.15f
    }

    override var sizeVariant = Medium
        set(value) {
            if (field != value) {
                field = value
                onSizeVariantChanged()
            }
        }


    /**
     * The entity of the badge's label.
     */
    val labelEntity = text {

        alignment = Anchor.Center
        background = UIBox().apply {
            color = Color4.Black
            alpha = 0.1f
        }
    }

    /**
     * The value of the badge.
     */
    val valueEntity = text {
        font = ResourceManager.getInstance().getFont("smallFont")
        padding = Vec4(12f, 8f)
        alignment = Anchor.Center
    }

    //region Shortcuts

    /**
     * The label of the badge.
     */
    var label by labelEntity::text

    /**
     * The value of the badge.
     */
    var value by valueEntity::text

    //endregion


    init {
        orientation = Orientation.Horizontal
        background = UIBox()

        onSizeVariantChanged()
    }


    override fun onSizeVariantChanged() {

        val font: Font
        val padding: Vec4
        val cornerRadius: Float

        when (sizeVariant) {
            Small -> {
                font = ResourceManager.getInstance().getFont("xs")
                padding = Vec4(8f, 4f)
                cornerRadius = 6f
            }
            Medium -> {
                font = ResourceManager.getInstance().getFont("smallFont")
                padding = Vec4(12f, 8f)
                cornerRadius = 12f
            }
            Large -> {
                font = ResourceManager.getInstance().getFont("font")
                padding = Vec4(16f, 12f)
                cornerRadius = 16f
            }
        }

        labelEntity.font = font
        valueEntity.font = font
        labelEntity.padding = padding
        valueEntity.padding = padding
        (background as? UIBox)?.cornerRadius = cornerRadius
    }

    override fun onManagedDraw(gl: GL10, camera: Camera) {
        (labelEntity.background as? UIBox)?.cornerRadius = (background as? UIBox)?.cornerRadius ?: 0f
        super.onManagedDraw(gl, camera)
    }
}



