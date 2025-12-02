package com.osudroid.ui.v2.multi

import com.osudroid.resources.R.string
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.andengine.ui.form.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.*
import ru.nsu.ccfit.zuev.osu.menu.*
import ru.nsu.ccfit.zuev.osu.online.*

class RoomButton(val lobbyScene: LobbyScene, val room: Room) : UIButton() {

    init {
        width = Size.Full
        style = {
            backgroundColor = it.accentColor * 0.1f / 0.8f
            padding = Vec4(3f.srem)
        }

        linearContainer {
            orientation = Orientation.Vertical
            width = Size.Full
            inheritAncestorsColor = false
            style = {
                spacing = 1f.srem
            }

            fillContainer {
                width = Size.Full
                style = {
                    spacing = 2f.srem
                }

                +FontAwesomeIcon(Icon.Lock).apply {
                    icon = if (room.isLocked) Icon.Lock else Icon.LockOpen
                    style = { color = it.accentColor }
                }

                text {
                    width = Size.Full
                    text = room.name
                    style = { color = it.accentColor }
                }

                text {
                    setScale(0.85f)
                    style = {
                        fontSize = FontSize.SM
                        color = it.accentColor
                    }
                    setText(when (room.status) {
                        RoomStatus.ChangingBeatmap -> string.multiplayer_room_status_changing_beatmap
                        RoomStatus.Playing -> string.multiplayer_room_status_playing
                        else -> string.multiplayer_room_status_idle
                    })
                }
            }

            text {
                text = room.playerNames.takeUnless { it.isEmpty() } ?: StringTable.get(string.multiplayer_room_no_players)
                style = {
                    fontSize = FontSize.XS
                    color = it.accentColor / 0.95f
                }
            }

            linearContainer {
                style = {
                    spacing = 2f.srem
                }

                badge {
                    sizeVariant = SizeVariant.Small
                    setText(
                        when (room.teamMode) {
                            TeamMode.HeadToHead -> string.multiplayer_room_head_to_head
                            TeamMode.TeamVersus -> string.multiplayer_room_team_versus
                        }
                    )
                }

                badge {
                    sizeVariant = SizeVariant.Small
                    setText(
                        when (room.winCondition) {
                            WinCondition.ScoreV1 -> string.multiplayer_room_score_v1
                            WinCondition.ScoreV2 -> string.multiplayer_room_score_v2
                            WinCondition.HighestAccuracy -> string.multiplayer_room_highest_accuracy
                            WinCondition.MaximumCombo -> string.multiplayer_room_maximum_combo
                        }
                    )
                }

                labeledBadge {
                    sizeVariant = SizeVariant.Small
                    label = StringTable.get(string.multiplayer_room_players)
                    value = "${room.playerCount}/${room.maxPlayers}"
                }

                if (room.gameplaySettings.isFreeMod) {
                    badge {
                        sizeVariant = SizeVariant.Small
                        style = {
                            color = it.accentColor * 0.1f
                            backgroundColor = it.accentColor
                        }
                        setText(string.multiplayer_room_free_mods)
                    }
                }
            }

            if (room.mods.isNotEmpty()) {
                +ModsIndicator().apply {
                    mods = room.mods.values
                    iconSize = 24f
                }
            }
        }

        onActionUp = {
            if (room.isLocked) {

                val form: FormContainer

                object : UIDialog<FormContainer>(FormContainer().apply {
                    width = Size.Full
                    form = this

                    +FormInput().apply {
                        key = "password"
                        width = Size.Full
                        label = StringTable.get(string.multiplayer_lobby_room_password)
                    }

                    onSubmit = {
                        connectToRoom(it.getString("password"))
                    }
                }) {
                    init {
                        title = StringTable.get(string.multiplayer_lobby_join_room)

                        addButton(UITextButton().apply {
                            setText(string.multiplayer_lobby_join_room_accept)
                            isSelected = true
                            onActionUp = { form.submit() }
                        })

                        addButton(UITextButton().apply {
                            setText(string.multiplayer_lobby_join_room_cancel)
                            onActionUp = { hide() }
                        })
                    }
                }.show()
            } else {
                connectToRoom()
            }
        }
    }


    fun connectToRoom(password: String? = null) {

        Multiplayer.log("Trying to connect socket...")
        LoaderScene().show()

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

                UIEngine.current.scene = lobbyScene
            }
        }

    }

}