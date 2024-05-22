package com.reco1l.osu.multiplayer

import android.app.AlertDialog
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomStatus.*
import com.reco1l.ibancho.data.TeamMode.HEAD_TO_HEAD
import com.reco1l.ibancho.data.TeamMode.TEAM_VS_TEAM
import com.reco1l.ibancho.data.WinCondition.*
import com.reco1l.osu.mainThread
import com.reco1l.osu.ui.entity.ScrollableList
import com.reco1l.toolkt.kotlin.async
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

class LobbyRoomList : ScrollableList()
{

    fun setList(rooms: List<Room>)
    {
        for (i in 0 until childCount)
        {
            val item = getChild(i) as Sprite
            LobbyScene.unregisterTouchArea(item)
        }
        detachChildren()
        rooms.iterator().forEach { addItem(it) }
    }

    private fun showPasswordPrompt(room: Room) = mainThread {

        val input = EditText(getGlobal().mainActivity)
        input.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD

        AlertDialog.Builder(getGlobal().mainActivity).apply {

            setTitle(room.name)
            setMessage("Please enter the room password:")
            setView(input)
            setCancelable(false)
            setPositiveButton("Join") { dialog, _ ->

                val password = input.text.toString()
                dialog.dismiss()
                connectToRoom(room, password)
            }

            setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        }.show()
    }

    private fun connectToRoom(room: Room, password: String? = null)
    {
        Multiplayer.log("Trying to connect socket...")

        LobbyScene.search.dismiss()
        LoadingScreen().show()

        async {

            try {
                RoomAPI.connectToRoom(room.id, getOnline().userId, getOnline().username, password)
            } catch (e: Exception) {

                ToastLogger.showText("Failed to connect to the room: ${e.javaClass} - ${e.message}", true)
                Multiplayer.log(e)

                LobbyScene.show()
            }
        }
    }

    private fun addItem(room: Room)
    {
        val texture = getResources().getTexture("menu-button-background")

        camY = -146f

        val sprite = object : Sprite(Config.getRES_WIDTH() - texture.width - 20f, 0f, texture)
        {
            private var moved = false
            private var dx = 0f
            private var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
            {
                handleScrolling(event)

                if (event.isActionDown)
                {
                    moved = false
                    dx = localX
                    dy = localY

                    alpha = 0.6f
                    return true
                }

                if (event.isActionUp)
                {
                    velocityY = 0f
                    alpha = 0.3f

                    if (moved || isScroll)
                        return false

                    getResources().getSound("menuclick")?.play()

                    if (room.isLocked)
                        showPasswordPrompt(room)
                    else
                        connectToRoom(room)

                    return true
                }

                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10)
                {
                    alpha = 0.3f
                    moved = true
                }
                return false
            }
        }

        sprite.setColor(0f, 0f, 0f, 0.3f)

        // Icon
        val texName = when (room.teamMode)
        {
            HEAD_TO_HEAD -> "head_head"
            TEAM_VS_TEAM -> "team_vs"
        }

        val icon = Sprite(10f, 0f, getResources().getTexture(texName)).also {

            it.setScale(0.5f)
            it.setPosition(10f, (sprite.height - it.height) / 2f)
            sprite.attachChild(it)
        }

        // Title
        val name = Text(0f, 0f, getResources().getFont("smallFont"), room.name).also {

            it.setPosition(icon.x + icon.width, 24f)
            sprite.attachChild(it)
        }

        // Info

        val status = when (room.status)
        {
            CHANGING_BEATMAP -> "Changing beatmap"
            PLAYING -> "Playing a match"
            else -> "Idle"
        }

        val winCondition = when (room.winCondition)
        {
            SCORE_V1 -> "Score V1"
            ACCURACY -> "Accuracy"
            MAX_COMBO -> "Combo"
            SCORE_V2 -> "Score V2"
        }

        val infoText = """
            ${room.playerCount} / ${room.maxPlayers} - ${room.playerNames}
            $status - $winCondition - ${room.modsToReadableString()}
        """.trimIndent()

        Text(0f, 0f, getResources().getFont("smallFont"), infoText).also {

            it.setPosition(icon.x + icon.width, name.y + name.height)
            it.setColor(0.8f, 0.8f, 0.8f)
            sprite.attachChild(it)
        }

        // Lock indicator
        if (room.isLocked)
        {
            Sprite(0f, 0f, getResources().getTexture("lock")).also {

                it.setPosition(sprite.width - it.width - 5f, sprite.height - it.height - 5f)
                sprite.attachChild(it)
            }
        }

        attachChild(sprite)
        LobbyScene.registerTouchArea(sprite)

        itemHeight = sprite.height
    }

    override fun detachChildren()
    {
        for (i in 0 until childCount)
        {
            val item = getChild(i) as Sprite
            LobbyScene.unregisterTouchArea(item)
        }
        super.detachChildren()
    }
}
