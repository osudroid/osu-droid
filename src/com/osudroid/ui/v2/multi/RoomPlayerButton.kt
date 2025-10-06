package com.osudroid.ui.v2.multi

import com.edlplan.ui.fragment.WebViewFragment
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*

class RoomPlayerButton(room: Room, player: RoomPlayer) : UIButton() {

    override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
        color = theme.accentColor
        alpha = if (isEnabled) 1f else 0.5f
    }


    init {
        width = FillParent
        orientation = Orientation.Horizontal
        padding = Vec4(12f)
        spacing = 6f

        background = UIBox().apply {
            cornerRadius = 12f
            color = when {
                room.isTeamVersus -> when (player.team) {
                    RoomTeam.Blue -> Color4("#A0C0FF")
                    RoomTeam.Red -> Color4("#FFA0A0")
                    null -> Theme.current.accentColor
                } * 0.1f

                else -> Theme.current.accentColor
            } * 0.1f
            alpha = 0.5f
        }

        foreground = UIBox().apply {
            cornerRadius = 12f
            paintStyle = PaintStyle.Outline
            color = when (player.status) {
                Playing -> Theme.current.accentColor
                Ready -> Color4("#A0FFA0")
                NotReady, MissingBeatmap -> Color4("#FFA0A0")
            }
        }

        linearContainer {
            orientation = Orientation.Vertical
            inheritAncestorsColor = false

            text {
                text = player.name
                applyTheme = { color = it.accentColor }
            }

            if (room.gameplaySettings.isFreeMod) {
                +ModsIndicator().apply {
                    minHeight = 24f // Force to take space even if no mods are enabled
                    iconSize = 18f
                    mods = player.mods.json
                }
            }
        }

        onActionLongPress = {
            UIDropdown(this@RoomPlayerButton).apply dropdown@{
                maxWidth = 260f

                addButton {
                    text = "View profile"
                    onActionUp = {
                        WebViewFragment()
                            .setURL(WebViewFragment.PROFILE_URL + player.id)
                            .show()
                        this@dropdown.hide()
                    }
                }

                if (player.id != Multiplayer.player!!.id) {
                    addButton {
                        text = if (player.isMuted) "Unmute" else "Mute"
                        onActionUp = {
                            player.isMuted = !player.isMuted
                            this@dropdown.hide()
                        }
                    }

                    if (Multiplayer.isRoomHost) {
                        addButton {
                            text = "Transfer host"
                            onActionUp = {
                                UIConfirmDialog().apply {
                                    title = "Transfer room host to ${player.name}"
                                    text = "Are you sure?"
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
                            text = "Kick player"
                            applyTheme = {
                                color = Color4("#FFBFBF")
                            }
                            onActionUp = {
                                UIConfirmDialog().apply {
                                    title = "Kick ${player.name}"
                                    text = "Are you sure?"
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