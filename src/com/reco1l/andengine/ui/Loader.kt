package com.reco1l.andengine.ui

import com.edlplan.framework.easing.Easing
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.PaintStyle
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.rem
import com.reco1l.framework.Interpolation
import com.reco1l.toolkt.kotlin.fastForEachIndexed

open class Loader : UILinearContainer() {

    private val triangles = arrayOf(
        triangle {
            color = Colors.White
            paintStyle = PaintStyle.Outline
            style = {
                lineWidth = 0.115f.rem
                width = TRIANGLE_SIZE.rem
                height = TRIANGLE_SIZE.rem
                y = (TRIANGLE_SIZE / 2.5f).rem
            }
        },
        triangle {
            color = Colors.White
            paintStyle = PaintStyle.Outline
            rotationCenter = Anchor.Center
            rotation = 180f
            style = {
                lineWidth = 0.115f.rem
                width = TRIANGLE_SIZE.rem
                height = TRIANGLE_SIZE.rem
            }
        },
        triangle {
            color = Colors.White
            paintStyle = PaintStyle.Outline
            style = {
                lineWidth = 0.115f.rem
                width = TRIANGLE_SIZE.rem
                height = TRIANGLE_SIZE.rem
                y = (TRIANGLE_SIZE / 2.5f).rem
            }
        }
    )

    /**
     * Tiempo acumulado para la animación en segundos
     */
    private var animationTime = 0f



    init {
        style = {
            color = it.accentColor
            spacing = -(TRIANGLE_SIZE / 6f).rem
        }
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        animationTime += deltaTimeSec

        val cycleDuration = PULSE_DURATION * triangles.size * 2

        if (animationTime >= cycleDuration) {
            animationTime -= cycleDuration
        }

        triangles.fastForEachIndexed { index, triangle ->
            val offset = index * PULSE_DELAY
            val localTime = (animationTime - offset + cycleDuration) % cycleDuration

            val normalizedTime = (localTime % (PULSE_DURATION * 2)) / PULSE_DURATION

            val alpha = if (normalizedTime <= 1f) {
                Interpolation.floatAt(normalizedTime, 1f, 0.3f, 0f, 1f, Easing.InOutQuad)
            } else {
                Interpolation.floatAt(normalizedTime - 1f, 0.3f, 1f, 0f, 1f, Easing.InOutQuad)
            }

            triangle.alpha = alpha
            triangle.translationY = (alpha - 1f) * (TRIANGLE_SIZE / 8f).rem

        }

        super.onManagedUpdate(deltaTimeSec)
    }


    companion object {
        private const val PULSE_DURATION = 0.7f
        private const val PULSE_DELAY = 0.4f
        private const val TRIANGLE_SIZE = 1.25f
    }

}