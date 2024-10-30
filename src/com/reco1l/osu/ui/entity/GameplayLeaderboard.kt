package com.reco1l.osu.ui.entity

import android.opengl.GLES20
import com.reco1l.osu.multiplayer.Multiplayer.isMultiplayer
import org.anddev.andengine.entity.Entity
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoardItem
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2

class GameplayLeaderboard(var playerName: String, private val stats: StatisticV2) : Entity(0f, 0f) {


    var nextItems: List<ScoreBoardItem>? = null


    private var playerSprite: BoardItem? = null

    private var lastTimeDataChange = 0L


    // This determines the max amount of sprites that can be shown according to the user screen height.
    private val maxAllowed = (Config.getRES_HEIGHT() - VERTICAL_PADDING * 2).toInt() / SPRITE_HEIGHT

    private val replayId get() = GlobalManager.getInstance().scoring.replayID

    private val isReplaying get() = replayId != -1

    private val isGlobalLeaderboard get() = GlobalManager.getInstance().songMenu.isBoardOnline


    init {
        isChildrenIgnoreUpdate = true
    }


    override fun onManagedUpdate(secondsElapsed: Float) {

        if (!isMultiplayer) {
            val items = GlobalManager.getInstance().songMenu.board

            // We consider that if it's in replay mode the length should be the same, in case it's not then the
            // length should be +1 greater (because of the new score).
            if (items != null && childCount == 0) {
                nextItems = items
            }
        }

        val isInvalidated = nextItems != null

        if (isInvalidated) {
            val items = nextItems
            nextItems = null
            invalidate(items)
        }

        val spriteCount = childCount

        if (spriteCount == 0 || playerSprite == null) {
            return
        }

        val player = playerSprite!!

        player.apply {

            // Animating rank change
            val elapsed = (System.currentTimeMillis() - lastTimeDataChange) * 0.001f

            when {
                elapsed < 1 -> setColor(
                    1 + (r - 1) * elapsed,
                    1 + (g - 1) * elapsed,
                    1 + (b - 1) * elapsed,
                    1 + (a - 1) * elapsed
                )

                else -> setColor(r, g, b, a)
            }

            // Updating score data, we skip this on multiplayer because the data must be already updated at this point.
            if (!isMultiplayer) {
                data.apply {

                    // Updating info only if needed.
                    if (playScore != stats.totalScoreWithMultiplier || maxCombo != stats.scoreMaxCombo || accuracy != stats.accuracy) {
                        playScore = stats.totalScoreWithMultiplier
                        maxCombo = stats.scoreMaxCombo
                        accuracy = stats.accuracy

                        updateInfo()
                    }
                }
            }
        }

        val lastPlayerPosition = getChildIndex(player)
        var playerPosition = lastPlayerPosition

        if (!isMultiplayer) {

            var i = lastPlayerPosition - 1
            while (i >= 0) {
                val sprite = getChild(i) as BoardItem

                if (player.data.playScore >= sprite.data.playScore) {
                    player.data.rank = sprite.data.rank
                    sprite.data.rank++

                    playerPosition--

                    sprite.updateRank()
                    player.updateRank()

                    lastTimeDataChange = System.currentTimeMillis()
                }
                --i
            }
        }

        // Updating positions only if needed.
        if (playerPosition != lastPlayerPosition || isInvalidated) {

            if (playerPosition != lastPlayerPosition) {
                setChildIndex(player, playerPosition)
            }

            val maxY = VERTICAL_PADDING + SPRITE_HEIGHT * (maxAllowed - 1)

            if (playerPosition < maxAllowed) {

                var i = 0
                while (i < spriteCount) {
                    val sprite = getChild(i)

                    sprite.setPosition(0f, if (i >= maxAllowed) maxY else VERTICAL_PADDING + SPRITE_HEIGHT * i)
                    sprite.isVisible = i < maxAllowed
                    ++i
                }

            } else {

                // Computing the bound from player position towards the limit of sprites that can be shown.
                val minBound: Int = playerPosition - maxAllowed + 1

                var i = 0
                while (i < spriteCount) {

                    val sprite = getChild(i)
                    val isInBounds = i in minBound..<playerPosition

                    // Showing only sprites that are between the bound index exclusive up to player position inclusive, the
                    // first sprite will always be shown so that's why the bound index is exclusive.
                    sprite.isVisible = i == 0 || i == playerPosition || isInBounds

                    sprite.setPosition(0f, when {

                        // First always on top
                        i == 0 -> VERTICAL_PADDING

                        // Player always on bottom
                        i == playerPosition -> maxY

                        // Sprites outside the bounds will be placed at its respective limit, at this point this sprite
                        // shouldn't be visible.
                        !isInBounds -> if (i < minBound) VERTICAL_PADDING else maxY

                        // Placing sprites respectively from maxY accounting for first sprite
                        else -> maxY - SPRITE_HEIGHT * (playerPosition - i)
                    })
                    i++
                }
            }
        }
    }


    private fun invalidate(items: List<ScoreBoardItem>?) {

        detachChildren()

        var list: List<ScoreBoardItem> = items ?: emptyList()

        fun appendNewItem() = ScoreBoardItem().apply {

            userName = playerName

            // Setting the initial rank as the last rank, in local leaderboard it'll always be the last index because
            // it's based on the local database. In online the server database provides up to 50 scores, so we can't know
            // the actual last rank of it unless the provided leaderboard size is lower than 50.
            if (!isGlobalLeaderboard || items.isNullOrEmpty() || items.size < 50)
                rank = list.size + 1

            list = list + this
        }

        val playerItem = when {

            // In replay mode we try to find the corresponding data according to the replay ID, once we find it we append
            // it to the last index as empty score.
            isReplaying -> {
                if (list.isNotEmpty()) {
                    list = list.mapNotNull { if (it.scoreId == replayId) null else it.clone() }

                    // Reordering ranks according to indexes.
                    list.forEachIndexed { i, it -> it.rank = i + 1 }
                }

                // Setting replay data rank as empty score and shifting the rest of scores, it will eventually set back
                // to the corresponding values.
                appendNewItem()
            }

            // In multiplayer, we try to find the corresponding data according to the username.
            isMultiplayer -> list.find { it.userName == playerName }

            // In solo we just append a new data.
            else -> {
                if (list.isNotEmpty()) {
                    list = list.map { it.clone() }
                }

                appendNewItem()
            }
        }

        var i = list.size - 1
        while (i >= 0) {
            val it = list[i]
            val sprite = BoardItem(it)

            if (it == playerItem) {

                // This is only used for multiplayer when the list gets invalidated we try to figure if the rank was
                // changed by referencing the old player sprite.
                if (playerSprite?.data?.rank != it.rank) {
                    lastTimeDataChange = System.currentTimeMillis()
                }

                // Determining if the player 'isAlive' was changed from the last time the leaderboard was invalidated,
                // this is only used for multiplayer.
                if (playerSprite?.data?.isAlive != it.isAlive) {
                    lastTimeDataChange = System.currentTimeMillis()
                }

                playerSprite = sprite
            }

            sprite.updateColors()
            attachChild(sprite, 0)
            --i
        }
    }


    private inner class BoardItem(val data: ScoreBoardItem) : Sprite(0f, 0f, ResourceManager.getInstance().getTexture("menu-button-background")) {

        val info: ChangeableText

        val rank: ChangeableText

        // Storing target values, this is used when animating color changes.
        var r = 0.5f
        var g = 0.5f
        var b = 0.5f
        var a = 0.5f

        init {
            isVisible = false
            height = 90f
            width = 130f

            info = ChangeableText(10f, 15f, ResourceManager.getInstance().getFont("font"), "", 100)
            info.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            info.setScaleCenter(0f, 0f)
            info.setScale(0.65f)

            rank = ChangeableText(10f, 15f, ResourceManager.getInstance().getFont("CaptionFont"), "", 5)
            rank.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
            rank.setPosition(100 - rank.width, 30f)
            rank.setScaleCenter(0f, 0f)
            rank.setScale(1.7f)

            attachChild(rank)
            attachChild(info)

            updateInfo()
            updateRank()
        }

        fun updateInfo() {
            info.text = data.get()
            info.setScaleCenter(0f, 0f)
            info.setScale(0.65f)
        }

        fun updateRank() {
            rank.text = if (data.rank == -1) "#?" else "#${data.rank}"
            rank.setPosition(100 - rank.width, 30f)
        }

        fun updateColors() {
            r = 0.5f
            g = 0.5f
            b = 0.5f
            a = 0.5f

            if (data.isAlive || !isMultiplayer) {
                val isOwnScore = !isMultiplayer && isGlobalLeaderboard && data.userName == playerName

                info.setColor(0.9f, 0.9f, 0.9f)
                rank.setColor(0.6f, 0.6f, 0.6f, 0.9f)

                if (playerSprite == this) b = 1f else {
                    if (isOwnScore)
                        r = 1f

                    setColor(r, g, b, a)
                }
                return
            }

            rank.setColor(1f, 0.6f, 0.6f, 0.9f)
            info.setColor(1f, 0.8f, 0.8f)
            a = 0.25f

            if (playerSprite == this) {
                r = 1f
                g = 0.4f
                b = 0.4f
            } else {
                r = 1f
                g = 0.6f
                b = 0.6f

                setColor(r, g, b, a)
            }
        }
    }


    companion object {
        private const val SPRITE_HEIGHT = 83

        private const val VERTICAL_PADDING = SPRITE_HEIGHT.toFloat()
    }
}