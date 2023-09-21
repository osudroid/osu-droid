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
        val spriteCount = childCount

        if (!isMultiplayer)
        {
            val items = getGlobal().songMenu.board

            // We consider that if it's in replay mode the length should be the same, in case it's not then the
            // length should be +1 greater (because of the new score).
            if (items != null && spriteCount == 0)
                nextItems = items
        }

        if (nextItems != null)
        {
            val items = nextItems
            nextItems = null
            invalidate(items)
        }

        if (spriteCount == 0 || playerSprite == null)
        {
            super.onManagedUpdate(secondsElapsed)
            return
        }

        val player = playerSprite!!

        player.apply {

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

        var playerPosition = getChildIndex(player)
        var positionChanged = false

        var index = 0
        while (index < spriteCount)
        {
            val sprite = getChild(index) as? BoardItem ?: break

            // We don't rely on 'i' because it may get changed.
            var position = index

            if (!isMultiplayer && index < playerPosition)
            {
                if (player.data.playScore >= sprite.data.playScore)
                {
                    player.data.rank = sprite.data.rank
                    sprite.data.rank++

                    playerPosition--
                    position++
                    positionChanged = true

                    sprite.updateRank()
                    player.updateRank()

                    lastRankChange = System.currentTimeMillis()
                }
            }

            sprite.isVisible = when
            {
                // First and player positions are always shown.
                position == 0 || sprite == player -> true

                // If player position is lower than max allowed bound then wee apply the same bound to this sprite
                playerPosition == maxAllowed && position < maxAllowed -> true
                playerPosition < maxAllowed && position <= maxAllowed -> true

                // If the player position is beyond the max allowed bound
                playerPosition > maxAllowed && position > playerPosition - maxAllowed + 1 && position < playerPosition -> true

                else -> false
            }
            ++index
        }

        if (positionChanged)
            setChildIndex(player, playerPosition)

        if (playerPosition <= maxAllowed)
        {
            var i = 0
            val limit = min(maxAllowed, spriteCount - 1)
            while (i < limit)
            {
                getChild(i).setPosition(0f, VERTICAL_PADDING + SPRITE_HEIGHT * i)
                ++i
            }
        } else {
            val maxY = VERTICAL_PADDING + SPRITE_HEIGHT * maxAllowed

            var i = playerPosition
            while (i >= 0)
            {
                val sprite = getChild(i)

                if (i >= playerPosition - maxAllowed)
                    sprite.setPosition(0f, maxY - SPRITE_HEIGHT * (1 + playerPosition - i))
                else
                    sprite.setPosition(0f, VERTICAL_PADDING)
                --i
            }
        }
        super.onManagedUpdate(secondsElapsed)
    }


    private fun invalidate(items: List<ScoreBoardItem>?)
    {
        detachChildren()

        if (items.isNullOrEmpty())
            return

        var list: List<ScoreBoardItem> = items

        fun appendNewItem() = ScoreBoardItem().apply {

            userName = playerName
            list = list + this
        }

        val playerItem = when
        {
            // In replay mode we try to find the corresponding data according to the replay ID, once we find it we append
            // it to the last index as empty score.
            isReplaying ->
            {
                list = items.mapNotNull { if (it.scoreId == replayId) null else it.clone() }

                // Reordering ranks according to indexes.
                list.forEachIndexed { i, it -> it.rank = i + 1 }

                // Setting replay data rank as empty score and shifting the rest of scores, it will eventually set back
                // to the corresponding values.
                appendNewItem()
            }

            // In multiplayer, we try to find the corresponding data according to the username.
            isMultiplayer -> items.find { it.userName == playerName }

            // In solo we just append a new data.
            else -> appendNewItem()
        }

        var i = list.size - 1
        while (i >= 0)
        {
            val it = list[i]
            val sprite = BoardItem(it)

            if (it == playerItem)
            {
                sprite.setColor(0.5f, 1f, 0.5f)

                // This is mostly used for multiplayer when the list gets invalidated we try to figure if the rank
                // was changed by referencing the old player sprite.
                if (playerSprite?.data?.rank != it.rank)
                    lastRankChange = System.currentTimeMillis()

                playerSprite = sprite
            }

            attachChild(sprite, 0)
            --i
        }
    }


    private inner class BoardItem(val data: ScoreBoardItem) :
        Sprite(0f, 0f, getResources().getTexture("menu-button-background"))
    {

        val info: ChangeableText

        val rank: ChangeableText


        init
        {
            isVisible = false
            height = 90f
            width = 130f
            alpha = 0.5f

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