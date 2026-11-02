package com.reco1l.andengine.text

import com.reco1l.andengine.buffered.*
import org.andengine.engine.camera.*
import org.andengine.graphics.BlendMode
import org.andengine.graphics.batch.BatchTexture
import org.andengine.graphics.batch.QuadBatcher
import org.andengine.graphics.gles.GlesBackend
import org.andengine.opengl.texture.region.*
import org.andengine.graphics.DrawContext
import kotlin.math.*

/**
 * A text component that uses textures for each character.
 *
 * The glyphs are drawn as quads of the engine's [QuadBatcher], so a text whose glyphs share one
 * texture (for example one atlas page) costs a single draw, and consecutive texts of the same
 * texture and blend share it too. [depthInfo] is not applied: batched quads are drawn without a
 * depth test.
 */
open class UITextureText(val characters: MutableMap<Char, TextureRegion>) : UIBufferedComponent<IBuffer>() {

    /**
     * The spacing between glyphs.
     */
    var spacing = 0f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The text to display.
     */
    var text = ""
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The scale of the textures on the x-axis.
     */
    var textureScaleX = 1f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * The scale of the textures on the y-axis.
     */
    var textureScaleY = 1f
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * When set, each character is placed in a fixed-width cell (unscaled pixels) looked up by
     * character. Characters absent from the map use their natural texture width. Useful for
     * preventing layout shifts when glyphs in the same role have varying widths.
     */
    var fixedCharWidths: Map<Char, Float>? = null
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * When set, content size (width/height) is measured from this string instead of [text].
     * The rendered text is still [text]. Use this to give the component a stable bounding box
     * sized for the widest value it can display, so surrounding elements don't shift.
     */
    var measureText: String? = null
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    /**
     * Horizontal alignment of the rendered text within the content bounds.
     */
    var textAlign = TextAlign.Left
        set(value) {
            if (field != value) {
                field = value
                onUpdateText()
            }
        }

    private val textureRegions = mutableListOf<Pair<Char, TextureRegion>>()
    private var textContentWidth = 0f


    init {
        width = MatchContent
        height = MatchContent
    }


    fun setTextureScale(scale: Float) {
        textureScaleX = scale
        textureScaleY = scale
    }


    private fun onUpdateText() {
        textureRegions.clear()
        textContentWidth = 0f

        for (char in text) {
            val textureRegion = characters[char] ?: continue
            val cellWidth = (fixedCharWidths?.get(char) ?: textureRegion.width.toFloat()) * textureScaleX
            textureRegions.add(char to textureRegion)
            textContentWidth += cellWidth + spacing
        }

        if (textureRegions.isNotEmpty()) {
            textContentWidth -= spacing
        }

        var contentWidth = 0f
        var contentHeight = 0f

        for (char in measureText ?: text) {
            val textureRegion = characters[char] ?: continue
            val cellWidth = (fixedCharWidths?.get(char) ?: textureRegion.width) * textureScaleX

            contentWidth += cellWidth + spacing
            contentHeight = max(contentHeight, textureRegion.height * textureScaleY)
        }

        contentWidth -= spacing

        super.contentWidth = contentWidth
        super.contentHeight = contentHeight
    }

    override fun doDraw(pGLState: DrawContext, pCamera: Camera) {
        onApplyColor(pGLState)

        val clearMask = clearMask()
        if (clearMask != 0) {
            GlesBackend.state.clear(clearMask)
        }

        if (textureRegions.isEmpty()) {
            return
        }

        // Every GL blend factor the UI uses has a batch counterpart.
        val blend = batchBlendMode() ?: BlendMode.Alpha
        val color = QuadBatcher.packColor(drawRed, drawGreen, drawBlue, drawAlpha)

        QuadBatcher.setProjection(pGLState.projectionGLMatrix)
        val modelView = pGLState.modelViewGLMatrix

        var offsetX = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Center -> 0.5f
            TextAlign.Right -> 1f
        } * (contentWidth - textContentWidth)

        val fixedCharWidths = fixedCharWidths

        for (i in textureRegions.indices) {

            val (char, region) = textureRegions[i]
            val textureWidth = region.width * textureScaleX
            val textureHeight = region.height * textureScaleY
            val cellWidth = (fixedCharWidths?.get(char) ?: region.width) * textureScaleX
            val x = offsetX + (cellWidth - textureWidth) / 2f

            QuadBatcher.add(
                BatchTexture.of(region.texture),
                blend,
                modelView,
                x, 0f, x + textureWidth, textureHeight,
                region.u, region.v, region.u2, region.v2,
                color
            )

            offsetX += cellWidth + spacing
        }
    }

    override fun onCreateBuffer(): IBuffer? {
        // The glyphs go through the quad batch; there is no buffer of its own.
        return null
    }

    override fun onUpdateBuffer() {
        // Nothing to do here, there is no buffer.
    }
}

/**
 * Determines the horizontal alignment of the rendered text within the content bounds.
 */
enum class TextAlign { Left, Center, Right }