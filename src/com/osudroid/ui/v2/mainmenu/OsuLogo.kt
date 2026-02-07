package com.osudroid.ui.v2.mainmenu

import android.opengl.GLES10
import com.osudroid.RythimManager
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.circle
import com.reco1l.andengine.component.ClearInfo
import com.reco1l.andengine.component.DepthInfo
import com.reco1l.andengine.component.rotationCenter
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.container
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.shape.UICircle
import com.reco1l.andengine.shape.UIGradientBox
import com.reco1l.andengine.text
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.Fonts
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.ui.UIClickableContainer
import com.reco1l.framework.Interpolation
import com.reco1l.framework.rgb

class OsuLogo(withExternalEffects: Boolean = true) : UIClickableContainer() {

    /**
     * Whether to play music effects.
     */
    var playEffects = withExternalEffects


    private lateinit var osuText: UIText
    private lateinit var inputFeedbackCircle: UICircle

    private val bounceContainer: UIContainer

    private var radialVisualizer: RadialVisualizer? = null
    private var rippleDispenser: RippleVisualizer? = null


    init {
        if (withExternalEffects) {
            +RippleVisualizer().apply {
                width = Size.Full
                height = Size.Full
                anchor = Anchor.Center
                origin = Anchor.Center
                alpha = 0.3f

                rippleDispenser = this
            }
        }

        bounceContainer = container {
            width = Size.Full
            height = Size.Full
            anchor = Anchor.Center
            origin = Anchor.Center

            if (withExternalEffects) {
                +RadialVisualizer().apply {
                    width = Size.Full
                    height = Size.Full
                    anchor = Anchor.Center
                    origin = Anchor.Center
                    alpha = 0.4f

                    radialVisualizer = this
                }
            }

            circle {
                width = Size.Full
                height = Size.Full
                color = Colors.White
            }

            circle {
                width = 0.9f.pct
                height = 0.9f.pct
                anchor = Anchor.Center
                origin = Anchor.Center
                color = OSU_COLOR
                clearInfo = ClearInfo.ClearDepthBuffer
                depthInfo = DepthInfo(test = true, mask = true, function = GLES10.GL_ALWAYS)
            }

            +TrianglesDispenser().apply {
                width = 0.9f.pct
                height = 0.9f.pct
                triangle.apply {
                    depthInfo = DepthInfo(test = true, mask = true, function = GLES10.GL_EQUAL)
                    paintStyle = PaintStyle.Outline
                }
                style = {
                    triangle.lineWidth = 0.25f.rem
                }

                colorPalette = arrayOf(
                    OSU_COLOR.lighten(0.1f),
                    OSU_COLOR.darken(0.1f),
                    OSU_COLOR.darken(0.125f),
                    OSU_COLOR.darken(0.15f)
                )
            }

            +UIGradientBox().apply {
                width = 0.9f.pct
                height = 0.9f.pct
                colorStart = Colors.Transparent
                colorEnd = Colors.Black
                gradientAngle = 90f
                anchor = Anchor.Center
                origin = Anchor.Center
                alpha = 0.175f
                depthInfo = DepthInfo(test = true, mask = true, function = GLES10.GL_EQUAL)
            }

            osuText = text {
                text = "OSU!"
                fontFamily = Fonts.TorusBold
                color = Colors.White
                anchor = Anchor.Center
                origin = Anchor.Center
            }

            inputFeedbackCircle = circle {
                width = Size.Full
                height = Size.Full
                color = Colors.White
                alpha = 0f
            }
        }

    }

    fun playClickEffect() {
        inputFeedbackCircle.alpha = 0.9f
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        osuText.fontSize = height * 0.295f

        if (playEffects) {
            val beatLengthSeconds = RythimManager.beatLength.toFloat() / 1000f

            if (RythimManager.beatElapsed / 1000f < beatLengthSeconds * 0.75f) {
                val threeQuarts = beatLengthSeconds * 0.75f
                bounceContainer.setScale(Interpolation.floatAt(deltaTimeSec.coerceIn(0f, threeQuarts), bounceContainer.scaleX, 1f, 0f, threeQuarts))
            } else {
                val oneQuart = beatLengthSeconds * 0.25f
                bounceContainer.setScale(Interpolation.floatAt(deltaTimeSec.coerceIn(0f, oneQuart), bounceContainer.scaleX, 0.9f, 0f, oneQuart))
            }
        } else {
            bounceContainer.setScale(Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), bounceContainer.scaleX, 1f, 0f, 0.1f))
            radialVisualizer?.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), radialVisualizer?.alpha ?: 0f, 0f, 0f, 0.1f)
            rippleDispenser?.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), rippleDispenser?.alpha ?: 0f, 0f, 0f, 0.1f)
        }

        inputFeedbackCircle.alpha = Interpolation.floatAt(deltaTimeSec.coerceIn(0f, 0.1f), inputFeedbackCircle.alpha, 0f, 0f, 0.1f)

        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {

        /**
         * The official osu! pink color.
         */
        val OSU_COLOR = rgb(255, 101, 170)

    }
}