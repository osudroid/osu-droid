package com.reco1l.legacy.ui.entity

import android.opengl.GLES20
import com.reco1l.legacy.ui.multiplayer.Multiplayer.isMultiplayer
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import kotlin.math.min
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

class InGameLeaderboard(var playerName: String, private val stats: StatisticV2) : Entity(0f, 0f)
{

    var nextItems: List<ScoreBoardItem>? = null


    private var playerSprite: BoardItem? = null

    private var lastRankChange = 0L


    // This determines the max amount of sprites that can be shown according to the user screen height.
    private val maxAllowed = ((Config.getRES_HEIGHT() - VERTICAL_PADDING * 2) / SPRITE_HEIGHT).toInt()

    private val replayId get() = getGlobal().scoring.replayID

    private val isReplaying get() = replayId != -1


    init
    {
        isChildrenIgnoreUpdate = true
    }


    override fun onManagedUpdate(secondsElapsed: Float)
    {
        val count = childCount

        if (!isMultiplayer)
        {
            val items = getGlobal().songMenu.board

            // We consider that if it's in replay mode the length should be the same, in case it's not then the
            // length should be +1 greater (because of the new score).
            if (items == null || items.size + (if (isReplaying) 0 else 1) != count)
            {
                invalidate(items)
                return
            }
        }
        else if (nextItems != null) {

            val items = nextItems
            nextItems = null
            invalidate(items)
        }

        if (count == 0 || playerSprite == null)
        {
            super.onManagedUpdate(secondsElapsed)
            return
        }

        val playerSprite = playerSprite!!

        playerSprite.apply {

            // Animating rank change
            val elapsed = (System.currentTimeMillis() - lastRankChange) * 0.001f
            val component = if (elapsed < 1) 1 - 0.5f * elapsed else 0.5f
            setColor(component, component, 1f, component)

            // Updating score data, we skip this on multiplayer because the data must be already updated at this point.
            if (!isMultiplayer) data.apply {

                // Updating info only if needed.
                if (playScore != stats.autoTotalScore || maxCombo != stats.maxCombo || accuracy != stats.accuracy)
                {
                    playScore = stats.autoTotalScore
                    maxCombo = stats.maxCombo
                    accuracy = stats.accuracy

                    updateInfo()
                }
            }
        }

        var playerSpritePosition = playerSprite.bindingPosition

        for (i in 0 until count)
        {
            val sprite = getChild(i) as? BoardItem ?: break

            var position = sprite.bindingPosition

            if (!isMultiplayer && position < playerSpritePosition)
            {
                if (playerSprite.data.playScore >= sprite.data.playScore)
                {
                    // Setting the new player rank
                    val newPlayerPosition = position
                    val newPosition = playerSpritePosition

                    sprite.data.rank = newPosition + 1
                    playerSprite.data.rank = newPlayerPosition + 1

                    playerSpritePosition = newPlayerPosition
                    position = newPosition

                    sprite.updateRank()
                    playerSprite.updateRank()

                    lastRankChange = System.currentTimeMillis()
                }
            }

            sprite.isVisible = when
            {
                // First and player positions are always shown.
                position == 0 || sprite == playerSprite -> true

                // Showing only if the sprite index is lower than the limit of allowed sprites, if it corresponds to
                // the last index of allowed sprites it'll show only if the player sprite index isn't greater than
                // the max allowed (because the player sprite is always shown).
                position <= if (playerSpritePosition > maxAllowed) maxAllowed - 1 else maxAllowed -> true

                else -> false
            }

            // Updating position
            sprite.setPosition(0f, VERTICAL_PADDING + SPRITE_HEIGHT * min(position, maxAllowed))
        }
        super.onManagedUpdate(secondsElapsed)
    }


    private fun invalidate(items: List<ScoreBoardItem>?)
    {
        detachChildren()

        if (items.isNullOrEmpty())
            return

        // Removing replay score data from the list.
        val list = items.toMutableList()

        val playerIndex = when
        {
            isReplaying -> list.indexOfFirst { it.scoreId == replayId }
            isMultiplayer -> list.indexOfFirst { it.userName == playerName }

            // In single player we just append a new item corresponding to the new score.
            else ->
            {
                list.add(ScoreBoardItem().apply { userName = playerName })
                list.lastIndex
            }
        }

        list.mapIndexed { index, it ->

            // Setting replay data rank as empty score and shifting the rest of scores, it will eventually set back to
            // the corresponding values.
            if (isReplaying)
                BoardItem(it.clone().apply {

                    rank = when
                    {
                        index == playerIndex -> -1
                        index > playerIndex -> rank - 1
                        else -> rank
                    }

                    if (index == playerIndex)
                    {
                        playScore = 0
                        accuracy = 0f
                        maxCombo = 0
                    }
                })
            else BoardItem(it)

        }.forEachIndexed { index, it ->

            if (index == playerIndex)
            {
                // Setting blue color if it's the current score item.
                it.setColor(0.5f, 1f, 0.5f)

                playerSprite = it
            }

            // Showing once all scores has been added.
            it.alpha = 0.5f

            attachChild(it)
        }

        lastRankChange = System.currentTimeMillis()
    }


    private inner class BoardItem(val data: ScoreBoardItem) :
        Sprite(0f, 0f, getResources().getTexture("menu-button-background"))
    {

        val info: ChangeableText

        val rank: ChangeableText

        val bindingPosition get() = data.rank.let { if (it == -1) parent.childCount - 1 else it - 1 }


        init
        {
            height = 90f
            width = 130f
            alpha = 0f

            when
            {
                // Setting red color if the score belongs to the player, but it's not the current one.
                data.userName == playerName && playerName != "osu!" -> setColor(1f, 0.5f, 0.5f)

                else -> setColor(0.5f, 0.5f, 0.5f)
            }

            info = ChangeableText(10f, 15f, getResources().getFont("font"), "", 100)
            info.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            info.setColor(0.85f, 0.85f, 0.9f)
            info.setScaleCenter(0f, 0f)
            info.setScale(0.65f)

            rank = ChangeableText(10f, 15f, getResources().getFont("CaptionFont"), "", 5)
            rank.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            rank.setPosition(100 - rank.width, 30f)
            rank.setColor(0.6f, 0.6f, 0.6f, 0.9f)
            rank.setScaleCenter(0f, 0f)
            rank.setScale(1.7f)

            attachChild(rank)
            attachChild(info)

            if (data.isAlive)
                info.setColor(0.85f, 0.85f, 0.9f)
            else
                info.setColor(1f, 0.5f, 0.5f)

            updateInfo()
            updateRank()
        }

        fun updateInfo()
        {
            info.text = data.get()
            info.setScaleCenter(0f, 0f)
            info.setScale(0.65f)
        }

        fun updateRank()
        {
            rank.text = if (data.rank == -1) "#?" else "#${data.rank}"
            rank.setPosition(100 - rank.width, 30f)
        }
    }


    companion object
    {
        private const val SPRITE_HEIGHT = 83

        private const val VERTICAL_PADDING = SPRITE_HEIGHT * 1.5f
    }
}