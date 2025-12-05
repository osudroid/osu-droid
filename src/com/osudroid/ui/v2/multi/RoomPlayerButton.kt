package com.osudroid.ui.v2.multi

import com.edlplan.ui.fragment.WebViewFragment
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.multiplayer.api.data.RoomTeam.Blue
import com.osudroid.multiplayer.api.data.RoomTeam.Red
import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class RoomPlayerButton() : UIButton() {

    private lateinit var nameText: UIText
    private lateinit var modsIndicator: ModsIndicator
    private lateinit var missingIndicator: FontAwesomeIcon


    init {
        width = Size.Full
        clipToBounds = false

        style = {
            color = it.accentColor
            alpha = if (isEnabled) 1f else 0.5f
            borderWidth = 2f
            padding = Vec4(2f.srem)
        }

        fillContainer {
            width = Size.Full
            orientation = Orientation.Horizontal
            style = {
                spacing = 2f.srem
            }

            linearContainer {
                orientation = Orientation.Vertical
                inheritAncestorsColor = false

                linearContainer {
                    orientation = Orientation.Horizontal
                    style = {
                        spacing = 3f.srem
                    }

                    nameText = text {
                        style = { color = it.accentColor }
                    }

                    missingIndicator = FontAwesomeIcon(Icon.Music).apply {
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        style = {
                            width = FontSize.SM
                            height = FontSize.SM
                            color = Colors.Red200
                        }
                    }
                    +missingIndicator
                }

                +ModsIndicator().apply {
                    style = {
                        minHeight = FontSize.SM // Force to take space even if no mods are enabled
                        iconSize = FontSize.SM
                    }
                    modsIndicator = this
                }
            }
        }
    }


    fun updateState(room: Room, player: RoomPlayer) {

        backgroundColor = when {
            room.isTeamVersus -> when (player.team) {
                Blue -> Colors.Blue400
                Red -> Colors.Red400
                null -> Theme.current.accentColor * 0.1f
            }
            else -> Theme.current.accentColor * 0.1f
        } / 0.5f

        borderColor = when (player.status) {
            Playing -> Theme.current.accentColor
            Ready -> Colors.Green200
            NotReady, MissingBeatmap -> Colors.Red200
        }

        nameText.text = player.name
        missingIndicator.isVisible = player.status == MissingBeatmap
        modsIndicator.mods = if (room.gameplaySettings.isFreeMod) player.mods.values else null

        onActionLongPress = {
            UIDropdown(this@RoomPlayerButton).apply dropdown@{

                addButton {
                    setText(R.string.multiplayer_room_player_menu_view_profile)
                    onActionUp = {
                        WebViewFragment()
                            .setURL(WebViewFragment.PROFILE_URL + player.id)
                            .show()
                        this@dropdown.hide()
                    }
                }

                if (player.id != Multiplayer.player!!.id) {
                    addButton {
                        setText(if (player.isMuted) R.string.multiplayer_room_player_menu_unmute else R.string.multiplayer_room_player_menu_mute)
                        onActionUp = {
                            player.isMuted = !player.isMuted
                            this@dropdown.hide()
                        }
                    }

                    if (Multiplayer.isRoomHost) {
                        addButton {
                            setText(R.string.multiplayer_room_player_menu_transfer_host)
                            onActionUp = {
                                UIConfirmDialog().apply {
                                    title = StringTable.format(R.string.multiplayer_room_player_transfer_dialog_title, player.name)
                                    text = StringTable.get(R.string.multiplayer_room_player_transfer_dialog_message)
                                    onConfirm = {
                                        if (Multiplayer.isConnected) {
                                            RoomAPI.setRoomHost(player.id)
                                        }
                                    }
                                }.show()
                                this@dropdown.hide()
                            }
                        }

                        addButton {
                            setText(R.string.multiplayer_room_player_menu_kick)
                            style = {
                                color = Color4("#FFBFBF")
                            }
                            onActionUp = {
                                UIConfirmDialog().apply {
                                    title = StringTable.format(R.string.multiplayer_room_player_kick_dialog_title, player.name)
                                    text = StringTable.get(R.string.multiplayer_room_player_kick_dialog_message)
                                    onConfirm = {
                                        if (Multiplayer.isConnected) {
                                            RoomAPI.kickPlayer(player.id)
                                        }
                                    }
                                }.show()
                                this@dropdown.hide()
                            }
                        }
                    }
                }

            }.show()
        }
    }

}