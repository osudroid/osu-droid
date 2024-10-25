package com.rian.osu.ui

import org.anddev.andengine.entity.modifier.MoveYModifier
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.scene.Scene.ITouchArea
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.HorizontalAlign
import ru.nsu.ccfit.zuev.osu.Config
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.round
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

class SendingPanel(
    private val overallRank: Long,
    private val score: Long,
    private val accuracy: Float,
    private val pp: Float
) : Rectangle(0f, -200f, 800f, 200f) {
    val dismissTouchArea: ITouchArea
        get() = dismissButton

    private val overallCaptionText = Text(0f, 0f, getResources().getFont("CaptionFont"), "Overall Ranking").also {
        it.setPosition((width - it.width) / 2, height / 5)
        attachChild(it)
    }

    private val minWidth = width
    private val minHeight = height
    private val columnGap = 10

    private var moveModifier: MoveYModifier? = null

    private val dismissButton = object : DismissButton() {
        override fun onAreaTouched(touchEvent: TouchEvent, localX: Float, localY: Float): Boolean {
            if (canBeDismissed) {
                this@SendingPanel.let {
                    if (moveModifier != null) {
                        it.unregisterEntityModifier(moveModifier)
                    }

                    val offset = it.height + height
                    moveModifier = MoveYModifier(0.5f * (it.y + offset) / offset, it.y, -offset)
                    it.registerEntityModifier(moveModifier)
                }

                canBeDismissed = false
                return true
            }

            return false
        }
    }.also {
        attachChild(it)
        it.setPosition((width - it.width) / 2, height)
        it.setText("Sending...")
    }

    private val rowContainer = Rectangle(0f, 0f, width, height * 0.8f).also {
        it.setColor(0f, 0f, 0f, 0f)
        attachChild(it)
    }

    private val mapRankColumn = Column("Map Rank").also { rowContainer.attachChild(it) }
    private val overallRankColumn = Column("Overall Rank").also { rowContainer.attachChild(it) }
    private val accuracyColumn = Column("Accuracy").also { rowContainer.attachChild(it) }
    private val scoreColumn = Column("Score").also { rowContainer.attachChild(it) }
    private val ppColumn = Column("Performance").also { rowContainer.attachChild(it) }

    init {
        setColor(0f, 0f, 0f, 0.7f)
        resetPosition()
    }

    fun show(newMapRank: Long, newOverallRank: Long, newScore: Long, newAccuracy: Float, newPP: Float) {
        dismissButton.canBeDismissed = true

        // Map rank column is special, as its color is different when ranking up
        updateColumn(mapRankColumn, "#$newMapRank", 0f)
        if (newScore > score) {
            mapRankColumn.setValueRectColor(1f, 1f, 0f, 0.8f)
        }

        updateColumn(
            overallRankColumn,
            when {
                newOverallRank == overallRank -> "#$newOverallRank"
                newOverallRank < overallRank -> "#$newOverallRank\n(+${overallRank - newOverallRank})"
                else -> "#$newOverallRank\n(-${newOverallRank - overallRank})"
            },
            (overallRank - newOverallRank).toFloat(),
            mapRankColumn
        )

        updateColumn(
            accuracyColumn,
            when {
                abs(newAccuracy - accuracy) < 0.0001f -> String.format("%.2f%%", newAccuracy * 100f)
                newAccuracy < accuracy -> String.format("%.2f%%\n(%.2f%%)", newAccuracy * 100f, (newAccuracy - accuracy) * 100f)
                else -> String.format("%.2f%%\n(+%.2f%%)", newAccuracy * 100f, (newAccuracy - accuracy) * 100f)
            },
            newAccuracy - accuracy,
            overallRankColumn
        )

        updateColumn(
            scoreColumn,
            when {
                newScore == score -> formatScore(newScore)
                newScore < score -> "${formatScore(newScore)}\n(${formatScore(newScore - score)})"
                else -> "${formatScore(newScore)}\n(+${formatScore(newScore - score)})"
            },
            (newScore - score).toFloat(),
            accuracyColumn
        )

        updateColumn(
            ppColumn,
            // For PP, we only want to show significant changes.
            when {
                round(newPP) - round(pp) < 1 -> String.format("%.0fpp", round(newPP))
                round(newPP) - round(pp) < 0 -> String.format("%.0fpp\n(%.0fpp)", round(newPP), round(newPP) - round(pp))
                else -> String.format("%.0fpp\n(+%.0fpp)", round(newPP), round(newPP) - round(pp))
            },
            round(newPP - pp),
            scoreColumn
        )

        // Obtain the maximum height of all columns
        val columnMaxHeight = max(
            mapRankColumn.height,
            max(overallRankColumn.height,
                max(accuracyColumn.height,
                    max(scoreColumn.height, ppColumn.height)
                )
            )
        )

        // Update the height of all columns to the maximum height
        mapRankColumn.height = columnMaxHeight
        overallRankColumn.height = columnMaxHeight
        accuracyColumn.height = columnMaxHeight
        scoreColumn.height = columnMaxHeight
        ppColumn.height = columnMaxHeight

        rowContainer.let {
            it.width = ppColumn.x + ppColumn.width - mapRankColumn.x
            it.height = columnMaxHeight
        }

        width = max(minWidth, max(overallCaptionText.width, rowContainer.width) + 20)
        height = max(minHeight, minHeight / 5 + overallCaptionText.height + rowContainer.height + 10)

        rowContainer.let {
            it.setPosition((width - it.width) / 2, height - it.height - 10)
        }

        resetPosition()

        overallCaptionText.setPosition((width - overallCaptionText.width) / 2, (height - overallCaptionText.height) / 5)

        dismissButton.let {
            it.setText("Dismiss")
            it.setPosition((width - it.width) / 2, height)
        }

        moveModifier = MoveYModifier(0.5f, -height, 0f)
        registerEntityModifier(moveModifier)
    }

    fun setFail() {
        dismissButton.setText("Failed")
    }

    private fun resetPosition() {
        setPosition((Config.getRES_WIDTH() - width) / 2f, -height)
    }

    private fun updateColumn(column: Column, value: String, difference: Float, prevColumn: Column? = null) = column.apply {
        updateValue(value)

        when {
            difference > 0 -> setValueRectColor(0f, 1f, 0f, 0.5f)
            difference < 0 -> setValueRectColor(1f, 0f, 0f, 0.5f)
            else -> setValueRectColor(0.4f, 0.4f, 0.4f, 0.8f)
        }

        setPosition(if (prevColumn != null) prevColumn.x + prevColumn.width + columnGap else 0f, 0f)
    }

    private fun formatScore(score: Long) = score
        .toString()
        // Reverse the order
        .reversed()
        // Split into 3 characters each
        .chunked(3)
        // Join them
        .joinToString(" ") { it }
        // Reverse back to maintain original order
        .reversed()

    private open class DismissButton : Sprite(0f, 0f, getResources().getTexture("ranking_button")) {
        var canBeDismissed = false

        private val text = ChangeableText(0f, 0f, getResources().getFont("font"), "", 10).also {
            attachChild(it)
        }

        fun setText(text: String) {
            this.text.text = text

            updateTextPosition()
        }

        private fun updateTextPosition() = text.let {
            it.setPosition(
                (width - it.width) / 2,
                // For whatever reason, the text is not centered vertically,
                // so we need to manually adjust it by subtracting 10...
                (height - it.height) / 2 - 10
            )
        }
    }

    private class Column(caption: String) : Rectangle(0f, 0f, 100f, 80f) {
        private val minWidth = width
        private val minHeight = height

        private val captionText = Text(0f, 0f, getResources().getFont("font"), caption).also {
            attachChild(it)
        }

        private val valueRect = Rectangle(0f, 80f, width, 0f).also {
            it.setColor(0.4f, 0.4f, 0.4f, 0.8f)
            attachChild(it)
        }

        private val valueText = ChangeableText(0f, 0f, getResources().getFont("font"), "", HorizontalAlign.CENTER, 100).also {
            valueRect.attachChild(it)
        }

        init { setColor(0f, 0f, 0f, 0f) }

        fun updateValue(value: String) {
            valueText.text = value

            updateSizes()
        }

        fun setValueRectColor(pRed: Float, pGreen: Float, pBlue: Float, pAlpha: Float = 1f) {
            valueRect.setColor(pRed, pGreen, pBlue, pAlpha)
        }

        override fun setWidth(pWidth: Float) {
            super.setWidth(pWidth)

            // Center the caption text
            captionText.let { it.setPosition((width - it.width) / 2, 0f) }

            // Update the value rectangle width
            valueRect.let {
                it.width = width
                it.setPosition(0f, height - it.height)
            }

            // Center the value text
            valueText.let {
                it.setPosition((valueRect.width - it.width) / 2, (valueRect.height - it.height) / 2)
            }
        }

        override fun setHeight(pHeight: Float) {
            super.setHeight(pHeight)

            // Update the value rectangle height
            valueRect.let {
                it.height = height - captionText.y - captionText.height
                it.setPosition(0f, height - it.height)
            }

            // Center the value text
            valueText.let {
                it.setPosition((valueRect.width - it.width) / 2, (valueRect.height - it.height) / 2)
            }
        }

        private fun updateSizes() {
            width = max(minWidth, max(captionText.width + 10, valueText.width + 20))
            height = max(minHeight, captionText.height + valueText.height)
        }
    }
}