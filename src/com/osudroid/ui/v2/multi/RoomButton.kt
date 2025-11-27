package com.osudroid.ui.v2.multi

import com.osudroid.resources.R.string
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
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
        width = FillParent
        background?.apply {
            color = Color4.Black
            alpha = 0.25f
        }

        // Override the default background
        applyTheme = {}

        container {
            width = FillParent

            linearContainer {
                orientation = Orientation.Vertical
                spacing = 8f
                inheritAncestorsColor = false

                linearContainer {
                    spacing = 4f

                    sprite {
                        origin = Anchor.CenterLeft
                        anchor = Anchor.CenterLeft
                        textureRegion = ResourceManager.getInstance().getTexture(if (room.isLocked) "lock" else "unlock")
                        size = Vec2(20f)
                        applyTheme = { color = it.accentColor }
                    }

                    text {
                        origin = Anchor.CenterLeft
                        anchor = Anchor.CenterLeft
                        font = ResourceManager.getInstance().getFont("smallFont")
                        text = room.name
                        applyTheme = { color = it.accentColor }
                    }
                }

                text {
                    font = ResourceManager.getInstance().getFont("xs")
                    text = room.playerNames.takeUnless { it.isEmpty() } ?: StringTable.get(string.multiplayer_room_no_players)
                    applyTheme = {
                        color = it.accentColor
                        alpha = 0.95f
                    }
                }

                linearContainer {
                    spacing = 8f

                    badge {
                        sizeVariant = SizeVariant.Small
                        setText(when (room.teamMode) {
                            TeamMode.HeadToHead -> string.multiplayer_room_head_to_head
                            TeamMode.TeamVersus -> string.multiplayer_room_team_versus
                        })
                    }

                    badge {
                        sizeVariant = SizeVariant.Small
                        setText(when (room.winCondition) {
                            WinCondition.ScoreV1 -> string.multiplayer_room_score_v1
                            WinCondition.ScoreV2 -> string.multiplayer_room_score_v2
                            WinCondition.HighestAccuracy -> string.multiplayer_room_highest_accuracy
                            WinCondition.MaximumCombo -> string.multiplayer_room_maximum_combo
                        })
                    }

                    labeledBadge {
                        sizeVariant = SizeVariant.Small
                        label = StringTable.get(string.multiplayer_room_players)
                        value = "${room.playerCount}/${room.maxPlayers}"
                    }

                    if (room.gameplaySettings.isFreeMod) {
                        badge {
                            sizeVariant = SizeVariant.Small
                            applyTheme = {
                                color = it.accentColor * 0.1f
                                background?.color = it.accentColor
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

            text {
                origin = Anchor.TopRight
                anchor = Anchor.TopRight
                font = ResourceManager.getInstance().getFont("smallFont")
                setScale(0.85f)
                setText(when (room.status) {
                    RoomStatus.ChangingBeatmap -> string.multiplayer_room_status_changing_beatmap
                    RoomStatus.Playing -> string.multiplayer_room_status_playing
                    else -> string.multiplayer_room_status_idle
                })
                applyTheme = { color = it.accentColor }
            }
        }

        onActionUp = {
            if (room.isLocked) {

                val form: FormContainer

                object : UIDialog<FormContainer>(FormContainer().apply {
                    width = FillParent
                    form = this

                    +FormInput().apply {
                        key = "password"
                        width = FillParent
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

                UIEngine.current.scene = lobbyScene
            }
        }

    }

}