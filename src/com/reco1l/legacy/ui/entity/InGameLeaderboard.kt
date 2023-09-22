package com.reco1l.legacy.ui.entity

import android.opengl.GLES20
import com.reco1l.legacy.ui.multiplayer.Multiplayer.isMultiplayer
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

class InGameLeaderboard(var playerName: String, private val stats: StatisticV2) : Entity(0f, 0f)
{

    var nextItems: List<ScoreBoardItem>? = null


    private var playerSprite: BoardItem? = null

    private var lastRankChange = 0L


    // This determines the max amount of sprites that can be shown according to the user screen height.
    private val maxAllowed = (Config.getRES_HEIGHT() - VERTICAL_PADDING * 2).toInt() / SPRITE_HEIGHT

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

        val lastPlayerPosition = getChildIndex(player)
        var playerPosition = lastPlayerPosition

        if (!isMultiplayer)
        {
            var i = lastPlayerPosition - 1
            while (i >= 0)
            {
                val sprite = getChild(i) as BoardItem

                if (player.data.playScore >= sprite.data.playScore)
                {
                    player.data.rank = sprite.data.rank
                    sprite.data.rank++

                    playerPosition--

                    sprite.updateRank()
                    player.updateRank()

                    lastRankChange = System.currentTimeMillis()
                }
                --i
            }
        }

        if (playerPosition != lastPlayerPosition)
            setChildIndex(player, playerPosition)

        super.onManagedUpdate(secondsElapsed)

        val maxY = VERTICAL_PADDING + SPRITE_HEIGHT * (maxAllowed - 1)

        if (playerPosition < maxAllowed)
        {
            var i = 0
            while (i < spriteCount)
            {
                val sprite = getChild(i)

                sprite.setPosition(0f, if (i >= maxAllowed) maxY else VERTICAL_PADDING + SPRITE_HEIGHT * i)
                sprite.isVisible = i < maxAllowed
                ++i
            }
        } else
        {
            // Computing the bound from player position towards the limit of sprites that can be shown.
            val minBound: Int = playerPosition - maxAllowed + 1
            val showRange = minBound + 1 until playerPosition

            var i = 0
            while (i < spriteCount)
            {
                val sprite = getChild(i)

                // Showing only sprites that are between the bound index exclusive up to player position inclusive, the
                // first sprite will always be shown so that's why the bound index is exclusive.
                sprite.isVisible = i == 0 || i == playerPosition || i in showRange

                sprite.setPosition(0f, when (i) {

                    // First always on top
                    0 -> VERTICAL_PADDING

                    // Player always on bottom
                    playerPosition -> maxY

                    // Sprites outside the bounds will be placed at its respective limit, at this point this sprite
                    // shouldn't be visible.
                    !in showRange -> if (i < minBound) VERTICAL_PADDING else maxY

                    // Placing sprites respectively from maxY accounting for first sprite
                    else -> maxY - SPRITE_HEIGHT * (playerPosition - i)
                })
                i++
            }
        }
    }


    private fun invalidate(items: List<ScoreBoardItem>?)
    {
        detachChildren()

        if (items.isNullOrEmpty())
            return

        var list: List<ScoreBoardItem> = items

        fun appendNewItem() = ScoreBoardItem().apply {

            userName = playerName

            // When using local leaderboard we can know the last rank, so we apply it.
            if (!getGlobal().songMenu.isBoardOnline)
                rank = list.size + 1

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
            else ->
            {
                list = list.map { it.clone() }
                appendNewItem()
            }
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

        private const val VERTICAL_PADDING = SPRITE_HEIGHT.toFloat()
    }
}