package com.reco1l.osu.ui.entity

import android.opengl.GLES20
import com.reco1l.ibancho.data.WinCondition.HighestAccuracy
import com.reco1l.osu.multiplayer.Multiplayer
import org.anddev.andengine.entity.scene.Scene.ITouchArea
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.text.NumberFormat.getNumberInstance
import java.util.Locale.ENGLISH
import java.util.Locale.US
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * Scoreboard list used for multiplayer scores in results screen.
 */
class StatisticSelector(stats: Array<StatisticV2>?) : ScrollableList(), ITouchArea {


    private var selected: StatisticV2? = getGlobal().scoring.currentStatistic
        set(value) {
            if (value != field && value != null) {
                field = value

                getGlobal().scoring.load(value, getGlobal().scoring.beatmapInfo, getGlobal().songService, null, null, null)
                getGlobal().engine.scene = getGlobal().scoring.scene
            }
        }


    init {
        stats?.forEachIndexed { i, stat -> addItem(i, stat) }
    }


    private fun addItem(index: Int, stats: StatisticV2) {
        camY = -146f

        val item = BoardItem(index, stats)
        val isSelected = stats.playerName == selected?.playerName

        if (stats.isAlive) {

            item.text.setColor(0.9f, 0.9f, 0.9f)
            item.rank.setColor(0.6f, 0.6f, 0.6f, 0.9f)

            if (isSelected) {
                item.setColor(0.5f, 0.5f, 1f, 0.9f)
            } else {
                item.setColor(0.5f, 0.5f, 0.5f, 0.9f)
            }

        } else {

            item.text.setColor(1f, 0.8f, 0.8f)
            item.rank.setColor(1f, 0.6f, 0.6f, 0.9f)

            if (isSelected) {
                item.setColor(1f, 0.4f, 0.4f, 0.9f)
            } else {
                item.setColor(1f, 0.6f, 0.6f, 0.9f)
            }

        }

        attachChild(item)
        registerTouchArea(item)

        itemHeight = 100f
    }


    inner class BoardItem(val index: Int, private val stats: StatisticV2) : Sprite(570f, 0f, ResourceManager.getInstance().getTexture("menu-button-background")) {

        private var moved = false
        private var dx = 0f
        private var dy = 0f


        val text = ChangeableText(10f, 15f, ResourceManager.getInstance().getFont("font"), "", 100)

        val rank = ChangeableText(10f, 15f, ResourceManager.getInstance().getFont("CaptionFont"), "", 5)


        init {
            setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            width = 140f
            height = 100f

            text.text = """
                ${stats.playerName}
                ${getNumberInstance(US).format(stats.totalScoreWithMultiplier)}
                ${
                when (Multiplayer.room!!.winCondition) {
                    HighestAccuracy -> "%2.2f%%".format(ENGLISH, stats.accuracy * 100f)
                    else -> "${getNumberInstance(US).format(stats.scoreMaxCombo)}x"
                }
            }
            """.trimIndent()
            text.setScaleCenter(0f, 0f)
            text.setScale(0.65f)

            rank.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            rank.text = "#${index + 1}"
            rank.setScaleCenter(0f, 0f)
            rank.setScale(1.7f)
            rank.setPosition(100 - rank.width, 30f)

            this.attachChild(rank)
            this.attachChild(text)
        }


        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

            handleScrolling(event)

            if (event.isActionDown) {
                moved = false
                dx = localX
                dy = localY

                return true
            }

            if (event.isActionUp) {
                velocityY = 0f

                if (moved || isScroll)
                    return false

                selected = stats
            }

            if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10) {
                moved = true
            }

            return false
        }
    }


    override fun contains(pX: Float, pY: Float): Boolean {
        return pX in 570f..570f + 140f
    }

    override fun onAreaTouched(event: TouchEvent?, x: Float, y: Float): Boolean {
        return super.onSceneTouchEvent(event)
    }
}
