package com.reco1l.osu.multiplayer

import com.reco1l.ibancho.data.PlayerStatus.*
import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomPlayer
import com.reco1l.ibancho.data.RoomTeam.*
import com.reco1l.ibancho.data.TeamMode
import com.reco1l.osu.ui.entity.ScrollableList
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager

class RoomPlayerList(val room: Room) : ScrollableList(), IScrollDetectorListener {


    val menu = RoomPlayerMenu()


    private var isValid = false


    init {
        for (i in 0 until room.maxPlayers) {
            camY = -146f

            val item = PlayerItem()
            attachChild(item)
            RoomScene.registerTouchArea(item)

            itemHeight = item.height
        }
    }


    fun invalidate() {
        isValid = false
    }

    override fun detachSelf(): Boolean {

        for (i in 0 until childCount) {
            RoomScene.unregisterTouchArea(getChild(i) as ITouchArea)
        }

        return RoomScene.detachChild(this)
    }


    override fun onManagedUpdate(secondsElapsed: Float) {
        if (!isValid) {
            isValid = true
            room.players.forEachIndexed { i, player ->

                val item = getChild(i) as PlayerItem

                item.room = room
                item.player = player
                item.isHost = player != null && player.id == room.host

                item.load()
            }
        }

        super.onManagedUpdate(secondsElapsed)
    }


    inner class PlayerItem : Rectangle(40f, 0f, Config.getRES_WIDTH() * 0.4f, 80f) {


        var room: Room? = null

        var player: RoomPlayer? = null

        var isHost: Boolean = false


        private val playerStatusRect = Rectangle(0f, 0f, 5f, height)

        private val playerInfoText = ChangeableText(20f, 16f, ResourceManager.getInstance().getFont("smallFont"), "", 64)

        private var hostIcon: Sprite? = null

        private var missingIcon: Sprite? = null


        private var moved = false

        private var dx = 0f

        private var dy = 0f


        init {
            attachChild(playerStatusRect)
            attachChild(playerInfoText)
        }


        fun load() {
            setColor(1f, 1f, 1f, 0.15f)

            hostIcon?.detachSelf()
            missingIcon?.detachSelf()
            hostIcon = null
            missingIcon = null

            playerInfoText.text = ""
            playerInfoText.isVisible = false
            playerStatusRect.isVisible = false

            if (room == null || player == null) return

            playerStatusRect.isVisible = true
            playerInfoText.isVisible = true
            playerInfoText.text = "${player!!.name}\n${player!!.mods}"

            if (room!!.teamMode == TeamMode.TeamVersus) {
                when (player!!.team) {
                    Red -> setColor(1f, 0.2f, 0.2f, 0.15f)
                    Blue -> setColor(0.2f, 0.2f, 1f, 0.15f)
                    else -> setColor(1f, 1f, 1f, 0.15f)
                }
            } else {
                setColor(1f, 1f, 1f, 0.15f)
            }

            if (isHost) {
                val icon = ResourceManager.getInstance().getTexture("crown")

                hostIcon = Sprite(width - icon.width - 15f, (height - icon.height) / 2f, icon)
                attachChild(hostIcon)
            }

            when (player!!.status) {
                MissingBeatmap -> {
                    val icon = ResourceManager.getInstance().getTexture("missing")

                    missingIcon = Sprite(width - icon.width - 15f - (hostIcon?.let { it.width + 10f } ?: 0f), (height - icon.height) / 2f, icon)
                    attachChild(missingIcon)

                    playerStatusRect.setColor(1f, 0.1f, 0.1f)
                }

                NotReady -> playerStatusRect.setColor(1f, 0.1f, 0.1f)
                Ready -> playerStatusRect.setColor(0.1f, 1f, 0.1f)
                Playing -> playerStatusRect.setColor(0.1f, 0.1f, 1f)
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            handleScrolling(event)

            if (event.isActionDown) {
                moved = false
                dx = localX
                dy = localY

                alpha = 0.25f
                return true
            }

            if (event.isActionUp) {
                velocityY = 0f
                alpha = 0.15f

                if (moved || isScroll) return true

                ResourceManager.getInstance().getSound("menuclick")?.play()

                if (player != null && Multiplayer.player != player) {
                    menu.player = player
                    menu.show()
                }
                return true
            }

            if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10) {
                moved = true
                alpha = 0.15f
                return true
            }

            return false
        }
    }
}
