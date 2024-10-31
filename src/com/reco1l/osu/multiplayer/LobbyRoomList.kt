package com.reco1l.osu.multiplayer

import android.view.inputmethod.EditorInfo
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.Room
import com.reco1l.ibancho.data.RoomStatus.*
import com.reco1l.ibancho.data.TeamMode.HeadToHead
import com.reco1l.ibancho.data.TeamMode.TeamVersus
import com.reco1l.ibancho.data.WinCondition.*
import com.reco1l.osu.mainThread
import com.reco1l.osu.ui.PromptDialog
import com.reco1l.osu.ui.entity.ScrollableList
import com.reco1l.toolkt.kotlin.async
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.Text
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager

class LobbyRoomList : ScrollableList() {


    fun setList(rooms: List<Room>) {

        for (i in 0 until childCount) {
            LobbyScene.unregisterTouchArea(getChild(i) as Sprite)
        }

        detachChildren()
        rooms.iterator().forEach {
            addItem(it)
        }
    }


    private fun showPasswordPrompt(room: Room) = mainThread {

        PromptDialog().apply {
            setTitle(room.name)
            setMessage("Please enter the room password:")
            setAllowDismiss(false)

            setOnTextInputBind {
                it.inputType = EditorInfo.TYPE_TEXT_VARIATION_PASSWORD
            }

            addButton("Join") {
                val password = (it as PromptDialog).input
                it.dismiss()
                connectToRoom(room, password)
            }

            addButton("Cancel") {
                it.dismiss()
            }

        }.show()
    }

    private fun connectToRoom(room: Room, password: String? = null) {

        Multiplayer.log("Trying to connect socket...")

        LobbyScene.search.dismiss()
        LoadingScreen().show()

        async {

            try {
                RoomAPI.connectToRoom(
                    roomId = room.id,
                    userId = OnlineManager.getInstance().userId,
                    username = OnlineManager.getInstance().username,
                    roomPassword = password
                )
            } catch (e: Exception) {

                ToastLogger.showText("Failed to connect to the room: ${e.javaClass} - ${e.message}", true)
                Multiplayer.log(e)

                LobbyScene.show()
            }
        }
    }

    private fun addItem(room: Room) {

        val texture = ResourceManager.getInstance().getTexture("menu-button-background")

        camY = -146f

        val sprite = object : Sprite(Config.getRES_WIDTH() - texture.width - 20f, 0f, texture) {

            private var moved = false
            private var dx = 0f
            private var dy = 0f

            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

                handleScrolling(event)

                if (event.isActionDown) {
                    moved = false
                    dx = localX
                    dy = localY

                    alpha = 0.6f
                    return true
                }

                if (event.isActionUp) {
                    velocityY = 0f
                    alpha = 0.3f

                    if (moved || isScroll) return false

                    ResourceManager.getInstance().getSound("menuclick")?.play()

                    if (room.isLocked) {
                        showPasswordPrompt(room)
                    } else {
                        connectToRoom(room)
                    }

                    return true
                }

                if (event.isActionOutside || event.isActionMove && MathUtils.distance(dx, dy, localX, localY) > 10) {
                    alpha = 0.3f
                    moved = true
                }
                return false
            }
        }

        sprite.setColor(0f, 0f, 0f, 0.3f)

        val texName = when (room.teamMode) {
            HeadToHead -> "head_head"
            TeamVersus -> "team_vs"
        }

        val icon = Sprite(10f, 0f, ResourceManager.getInstance().getTexture(texName)).also {

            it.setScale(0.5f)
            it.setPosition(10f, (sprite.height - it.height) / 2f)
            sprite.attachChild(it)
        }

        val name = Text(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), room.name).also {

            it.setPosition(icon.x + icon.width, 24f)
            sprite.attachChild(it)
        }

        val status = when (room.status) {
            ChangingBeatmap -> "Changing beatmap"
            Playing -> "Playing a match"
            else -> "Idle"
        }

        val winCondition = when (room.winCondition) {
            ScoreV1 -> "Score V1"
            HighestAccuracy -> "Accuracy"
            MaximumCombo -> "Combo"
            ScoreV2 -> "Score V2"
        }

        val infoText = """
            ${room.playerCount} / ${room.maxPlayers} - ${room.playerNames}
            $status - $winCondition - ${room.modsToReadableString()}
        """.trimIndent()

        Text(0f, 0f, ResourceManager.getInstance().getFont("smallFont"), infoText).also {

            it.setPosition(icon.x + icon.width, name.y + name.height)
            it.setColor(0.8f, 0.8f, 0.8f)
            sprite.attachChild(it)
        }

        if (room.isLocked) {
            Sprite(0f, 0f, ResourceManager.getInstance().getTexture("lock")).also {

                it.setPosition(sprite.width - it.width - 5f, sprite.height - it.height - 5f)
                sprite.attachChild(it)
            }
        }

        attachChild(sprite)
        LobbyScene.registerTouchArea(sprite)

        itemHeight = sprite.height
    }


    override fun detachChildren() {

        for (i in 0 until childCount) {
            LobbyScene.unregisterTouchArea(getChild(i) as Sprite)
        }

        super.detachChildren()
    }
}
