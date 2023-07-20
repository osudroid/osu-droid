package com.reco1l.legacy.ui.multiplayer

import com.reco1l.api.ibancho.data.*
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

    var menu: RoomPlayerMenu? = null

    init
    {
        for (i in 0 until room.maxPlayers)
        {
            camY = -146f

            val item = PlayerItem()
            attachChild(item)

            itemHeight = item.height
        }
    }

    fun updateItems() = room.players.forEachIndexed { i, it -> setItem(i, it) }

    private fun setItem(index: Int, player: RoomPlayer?)
    {
        val item = getChild(index) as PlayerItem

        item.room = room
        item.player = player
        item.isHost = player?.let { it.id == room.host } ?: false
        item.isVisible = true

        glThread { item.load() }
        RoomScene.registerTouchArea(item)
    }

    override fun detachSelf(): Boolean
    {
        for (i in 0 until childCount)
        {
            val item = getChild(i) as PlayerItem
            RoomScene.unregisterTouchArea(item)
        }
        detachChildren()
        return super.detachSelf()
    }

    inner class PlayerItem : Rectangle(40f, 0f, Config.getRES_WIDTH() * 0.4f, 80f)
    {

        var room: Room? = null

        var player: RoomPlayer? = null

        var isHost: Boolean = false

        private val stateIndicator = Rectangle(0f, 0f, 5f, height)

        private val nameText = ChangeableText(20f, 16f, getResources().getFont("smallFont"), "", 64)

        private val infoText = ChangeableText(20f, nameText.y + nameText.height, getResources().getFont("smallFont"), "", 64)

        private var hostIcon: Sprite? = null

        private var missingIcon: Sprite? = null

        private var moved = false
        private var dx = 0f
        private var dy = 0f

        init
        {
            attachChild(stateIndicator)
            attachChild(nameText)
            attachChild(infoText)
        }

        fun load()
        {
            hostIcon?.detachSelf()
            missingIcon?.detachSelf()
            hostIcon = null
            missingIcon = null

            if (room == null)
                return

            if (player == null)
            {
                stateIndicator.setColor(1f, 1f, 1f)
                setColor(1f, 1f, 1f, 0.15f)

                nameText.text = "empty"
                infoText.text = ""

                return
            }

            if (room!!.teamMode == TeamMode.TEAM_VS_TEAM)
            {
                when (player!!.team)
                {
                    RoomTeam.RED -> setColor(1f, 0.2f, 0.2f, 0.15f)
                    RoomTeam.BLUE -> setColor(0.2f, 0.2f, 1f, 0.15f)
                    else -> setColor(1f, 1f, 1f, 0.15f)
                }
            }
            else setColor(1f, 1f, 1f, 0.15f)

            nameText.text = player!!.name
            infoText.text = modsToReadable(player!!.mods)

            when (player!!.status)
            {
                PlayerStatus.MISSING_BEATMAP ->
                {
                    val icon = getResources().getTexture("missing")

                    missingIcon = Sprite(nameText.x + nameText.width + 5f, nameText.y + (nameText.height - icon.height) / 2f, icon)
                    attachChild(missingIcon)

                    stateIndicator.setColor(1f, 0.1f, 0.1f)
                }

                PlayerStatus.NOT_READY -> stateIndicator.setColor(1f, 0.1f, 0.1f)
                PlayerStatus.READY -> stateIndicator.setColor(0.1f, 1f, 0.1f)
                PlayerStatus.PLAYING -> stateIndicator.setColor(0.1f, 0.1f, 1f)
            }

            if (isHost)
            {
                val icon = getResources().getTexture("crown")

                hostIcon = Sprite(width - icon.width - 10f, (height - icon.height) / 2f, icon)
                attachChild(hostIcon)
            }
        }

        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
        {
            handleScrolling(event)

            return when
            {
                event.isActionDown ->
                {
                    moved = false
                    dx = localX
                    dy = localY

                    alpha = 0.25f
                    true
                }

                event.isActionUp ->
                {
                    alpha = 0.15f

                    if (moved || isScroll)
                        return false

                    if (RoomScene.player != player && player != null)
                    {
                        menu = RoomPlayerMenu()
                        menu!!.show(player!!)
                    }
                    true
                }

                event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10 ->
                {
                    moved = true
                    alpha = 0.15f
                    false
                }

                else -> false
            }
        }
    }
}
