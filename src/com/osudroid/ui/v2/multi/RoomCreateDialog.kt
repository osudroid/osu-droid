package com.osudroid.ui.v2.multi

import com.osudroid.resources.R.string
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.toolkt.kotlin.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.menu.*
import ru.nsu.ccfit.zuev.osu.online.*

class RoomCreateDialog(lobbyScene: LobbyScene) : UIDialog<UIScrollableContainer>(UIScrollableContainer().apply {
    scrollAxes = Axes.Y
    width = FillParent
    height = 360f
    clipToBounds = true
}) {
    init {

        val form = FormContainer().apply {
            width = FillParent
            orientation = Orientation.Vertical

            onSubmit = { data ->
                async {
                    LoadingScreen().show()

                    val name = data.getString("name") ?: StringTable.format(string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)
                    val password = data.optString("password").takeUnless(String::isBlank)
                    val capacity = data.getDouble("capacity").toInt()

                    val beatmap = GlobalManager.getInstance().selectedBeatmap?.let {

                        RoomBeatmap(
                            md5 = it.md5,
                            title = it.title,
                            artist = it.artist,
                            creator = it.creator,
                            version = it.version
                        )
                    }

                    var signStr = "${name}_${capacity}"
                    if (password != null) {
                        signStr += "_${password}"
                    }
                    signStr += "_${RoomAPI.API_VERSION}"

                    try {

                        val roomId = LobbyAPI.createRoom(
                            name = name,
                            beatmap = beatmap,
                            hostUID = OnlineManager.getInstance().userId,
                            hostUsername = OnlineManager.getInstance().username,
                            sign = SecurityUtils.signRequest(signStr),
                            password = password,
                            maxPlayers = capacity
                        )

                        RoomAPI.connectToRoom(roomId, OnlineManager.getInstance().userId, OnlineManager.getInstance().username, password)

                    } catch (e: Exception) {
                        UIEngine.current.scene = lobbyScene
                        ToastLogger.showText("Failed to create a room: ${e.message}", true)
                        e.printStackTrace()
                    }

                }
            }

            +FormInput(StringTable.format(string.multiplayer_lobby_create_room_name_default, OnlineManager.getInstance().username)).apply {
                key = "name"
                width = FillParent
                label = StringTable.get(string.multiplayer_lobby_room_name)
            }

            +FormInput().apply {
                key = "password"
                width = FillParent
                label = StringTable.get(string.multiplayer_lobby_room_password)
            }

            +FormSlider(8f).apply {
                key = "capacity"
                width = FillParent
                label = StringTable.get(string.multiplayer_lobby_room_capacity)
                control.max = 16f
                control.min = 2f
                control.step = 1f
                valueFormatter = { it.toInt().toString() }
            }

        }

        innerContent.attachChild(form)

        title = StringTable.get(string.multiplayer_lobby_create_room)

        addButton(UITextButton().apply {
            setText(string.multiplayer_lobby_create_room_accept)
            isSelected = true
            onActionUp = {
                form.submit()
            }
        })

        addButton(UITextButton().apply {
            setText(string.multiplayer_lobby_create_room_cancel)
            onActionUp = { hide() }
        })
    }
}
