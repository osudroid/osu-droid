package com.reco1l.legacy.ui.multiplayer

import com.reco1l.api.ibancho.data.*
import com.reco1l.api.ibancho.data.PlayerStatus.*
import com.reco1l.api.ibancho.data.RoomTeam.*
import com.reco1l.framework.lang.glThread
import com.reco1l.legacy.data.modsToReadable
import com.reco1l.legacy.ui.entity.ScrollableList
import org.anddev.andengine.entity.primitive.Rectangle
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.input.touch.detector.ScrollDetector.IScrollDetectorListener
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

class RoomPlayerList(val room: Room) : ScrollableList(), IScrollDetectorListener
{

    val menu = RoomPlayerMenu()

    init
    {
        for (i in 0 until room.maxPlayers)
        {
            camY = -146f

            val item = PlayerItem()
            attachChild(item)
            RoomScene.registerTouchArea(item)

            itemHeight = item.height
        }
    }

    fun updateItems() = room.players.forEachIndexed { i, player -> setItem(i, player) }

    private fun setItem(index: Int, player: RoomPlayer?)
    {
        val item = getChild(index) as PlayerItem

        item.room = room
        item.player = player
        item.isHost = player != null && player.id == room.host

        glThread { item.load() }
    }

    override fun detachSelf(): Boolean
    {
        for (i in 0 until childCount)
        {
            val item = getChild(i) as PlayerItem
            RoomScene.unregisterTouchArea(item)
        }
        glThread { detachChildren() }
        return super.detachSelf()
    }

    inner class PlayerItem : Rectangle(40f, 0f, Config.getRES_WIDTH() * 0.4f, 80f)
    {

        var room: Room? = null

        var player: RoomPlayer? = null

        var isHost: Boolean = false


        private val state = Rectangle(0f, 0f, 5f, height)

        private val text = ChangeableText(20f, 16f, getResources().getFont("smallFont"), "", 64)

        private var hostIcon: Sprite? = null

        private var missingIcon: Sprite? = null


        private var moved = false
        private var dx = 0f
        private var dy = 0f


        init
        {
            attachChild(state)
            attachChild(text)
        }


        fun load()
        {
            setColor(1f, 1f, 1f, 0.15f)

            hostIcon?.detachSelf()
            missingIcon?.detachSelf()
            hostIcon = null
            missingIcon = null

            text.text = ""
            text.isVisible = false
            state.isVisible = false

            if (room == null || player == null)
                return

            state.isVisible = true
            text.isVisible = true
            text.text = "${player!!.name}\n${modsToReadable(player!!.mods)}"

            if (room!!.teamMode == TeamMode.TEAM_VS_TEAM)
            {
                when (player!!.team)
                {
                    RED -> setColor(1f, 0.2f, 0.2f, 0.15f)
                    BLUE -> setColor(0.2f, 0.2f, 1f, 0.15f)
                    else -> setColor(1f, 1f, 1f, 0.15f)
                }
            }
            else setColor(1f, 1f, 1f, 0.15f)

            if (isHost)
            {
                val icon = getResources().getTexture("crown")

                hostIcon = Sprite(width - icon.width - 15f, (height - icon.height) / 2f, icon)
                attachChild(hostIcon)
            }

            when (player!!.status)
            {
                MISSING_BEATMAP ->
                {
                    val icon = getResources().getTexture("missing")

                    missingIcon = Sprite(width - icon.width - 15f - (hostIcon?.let { it.width + 10f } ?: 0f), (height - icon.height) / 2f, icon)
                    attachChild(missingIcon)

                    state.setColor(1f, 0.1f, 0.1f)
                }

                NOT_READY -> state.setColor(1f, 0.1f, 0.1f)
                READY -> state.setColor(0.1f, 1f, 0.1f)
                PLAYING -> state.setColor(0.1f, 0.1f, 1f)
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
        {
            handleScrolling(event)

            if (event.isActionDown)
            {
                moved = false
                dx = localX
                dy = localY

                alpha = 0.25f
                return true
            }

            if (event.isActionUp)
            {
                alpha = 0.15f

                if (moved || isScroll)
                    return true

                if (player != null && Multiplayer.player != player)
                {
                    menu.player = player
                    menu.show()
                }
                return true
            }

            if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10)
            {
                moved = true
                alpha = 0.15f
                return true
            }

            return false
        }
    }
}
