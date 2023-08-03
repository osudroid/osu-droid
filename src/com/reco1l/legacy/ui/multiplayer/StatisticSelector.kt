package com.reco1l.legacy.ui.multiplayer

import android.opengl.GLES20
import com.reco1l.api.ibancho.data.WinCondition.ACCURACY
import com.reco1l.legacy.ui.entity.ScrollableList
import com.reco1l.legacy.ui.multiplayer.RoomScene.room
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import java.text.NumberFormat.getNumberInstance
import java.util.Locale.ENGLISH
import java.util.Locale.US
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * Scoreboard list used for multiplayer scores in results screen.
 */
class StatisticSelector(stats: Array<StatisticV2>?) : ScrollableList()
{

    private var selected: StatisticV2? = getGlobal().scoring.currentStatistic
        set(value)
        {
            if (value != field && value != null)
            {
                field = value

                getGlobal().scoring.load(value, getGlobal().selectedTrack, getGlobal().songService, null, null, null)
                getGlobal().engine.scene = getGlobal().scoring.scene
            }
        }

    init
    {
        stats?.forEachIndexed { i, stat -> addItem(i, stat) }
    }

    private fun addItem(index: Int, stats: StatisticV2)
    {
        camY = -146f

        val item = BoardItem(index, stats)

        if (stats.playerName == selected?.playerName)
            item.setColor(0.5f, 0.5f, 1f)

        attachChild(item)
        getGlobal().scoring.scene.registerTouchArea(item)

        itemHeight = 100f
    }


    inner class BoardItem(val index: Int, private val stats: StatisticV2) :

            Sprite(570f, 0f, getResources().getTexture("menu-button-background").deepCopy())
    {
        private var moved = false
        private var dx = 0f
        private var dy = 0f

        private val text = ChangeableText(10f, 15f, getResources().getFont("font"), "", 100)

        private val rank = ChangeableText(10f, 15f, getResources().getFont("CaptionFont"), "", 5)

        init
        {
            this.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            this.setColor(0.5f, 0.5f, 0.5f)
            this.width = 140f
            this.height = 100f

            text.setColor(0.85f, 0.85f, 0.9f)
            text.text = "${stats.playerName}\n${getNumberInstance(US).format(stats.modifiedTotalScore)}\n"
            text.text += when (room!!.winCondition)
            {
                ACCURACY -> String.format(ENGLISH, "%2.2f%%", stats.accuracyForServer * 100f)
                else -> "${getNumberInstance(US).format(stats.maxCombo)}x"
            }
            text.setScaleCenter(0f, 0f)
            text.setScale(0.65f)

            rank.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            rank.setColor(0.6f, 0.6f, 0.6f, 0.9f)
            rank.text = "#${index + 1}"
            rank.setScaleCenter(0f, 0f)
            rank.setScale(1.7f)
            rank.setPosition(100 - rank.width, 30f)

            this.attachChild(rank)
            this.attachChild(text)
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
        {
            handleScrolling(event)

            if (event.isActionDown)
            {
                moved = false
                dx = localX
                dy = localY

                return true
            }

            if (event.isActionUp)
            {

                if (moved || isScroll)
                    return false

                selected = stats
            }

            if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10)
            {
                moved = true
            }
            return false
        }
    }
}
