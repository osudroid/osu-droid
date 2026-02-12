package com.osudroid.ui.v2.multi

import com.edlplan.ui.fragment.WebViewFragment
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.api.RoomAPI
import com.osudroid.multiplayer.api.data.*
import com.osudroid.multiplayer.api.data.PlayerStatus.*
import com.osudroid.multiplayer.api.data.RoomTeam.Blue
import com.osudroid.multiplayer.api.data.RoomTeam.Red
import com.osudroid.ui.OsuColors
import com.osudroid.ui.v2.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text.CompoundText
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R

class RoomPlayerCard : UILinearContainer() {
    private val teamColorBar: UIBox
    private val playerButton: RoomPlayerButton

    init {
        width = FillParent
        orientation = Orientation.Horizontal
        padding = Vec4(12f)
        spacing = 12f

        teamColorBar = UIBox().apply {
            relativeSizeAxes = Axes.X
            width = 0.025f
            height = FillParent
            cornerRadius = 12f
        }

        playerButton = RoomPlayerButton()
        +playerButton
    }

    fun updateState(room: Room, player: RoomPlayer) {
        playerButton.updateState(room, player)

        if (room.isTeamVersus) {
            if (!teamColorBar.hasParent()) {
                attachChild(teamColorBar, 0)
            }

            teamColorBar.color = when (player.team) {
                Blue -> Color4("#1E62E8") * 0.8f
                Red -> Color4("#E34444") * 0.8f
                null -> Theme.current.accentColor * 0.6f
            }
        } else if (teamColorBar.hasParent()) {
            detachChild(teamColorBar)
        }
    }

    private class RoomPlayerButton : UIButton() {

        private lateinit var nameText: CompoundText
        private lateinit var missingIndicator: UISprite

        private val innerContainer: UILinearContainer
        private var modDisplay: UIComponent? = null

        private val hostIcon = FontAwesomeIcon(Icon.Crown).apply {
            applyTheme = { color = it.accentColor }
        }

        private val mutedIcon = FontAwesomeIcon(Icon.MicrophoneSlash).apply {
            applyTheme = { color = OsuColors.redLight }
        }

        override var applyTheme: UIComponent.(Theme) -> Unit = { theme ->
            color = theme.accentColor
            alpha = if (isEnabled) 1f else 0.5f
        }


        init {
            width = FillParent
            orientation = Orientation.Horizontal
            spacing = 6f

            background = UIBox().apply {
                cornerRadius = 12f
                color = Theme.current.accentColor * 0.15f
                alpha = 0.5f
            }

            foreground = UIBox().apply {
                cornerRadius = 12f
                paintStyle = PaintStyle.Outline
            }

            innerContainer = linearContainer {
                orientation = Orientation.Vertical
                inheritAncestorsColor = false

                linearContainer {
                    orientation = Orientation.Horizontal
                    spacing = 4f

                    nameText = compoundText {
                        applyTheme = { color = it.accentColor }
                    }

                    missingIndicator = sprite {
                        textureRegion = ResourceManager.getInstance().getTexture("missing")
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        size = Vec2(18f)
                    }
                }
            }

        }


        fun updateState(room: Room, player: RoomPlayer) {
            foreground!!.color = when (player.status) {
                Playing -> Theme.current.accentColor
                Ready -> Color4("#A0FFA0")
                NotReady, MissingBeatmap -> Color4("#FFA0A0")
            }

            nameText.text = player.name
            nameText.spacing = 6f
            nameText.trailingIcon = when {
                player.id == room.host && player.isMuted -> UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    spacing = 6f

                    +hostIcon
                    +mutedIcon
                }

                player.id == room.host -> hostIcon
                player.isMuted -> mutedIcon

                else -> null
            }

            missingIndicator.isVisible = player.status == MissingBeatmap

            if (Config.isPreferModAcronymInMultiplayer()) {
                if (modDisplay !is UIText) {
                    modDisplay?.detachSelf()

                    modDisplay = UIText().apply {
                        minHeight = 18f // Force to take space even if no mods are enabled
                        font = ResourceManager.getInstance().getFont("xs")
                        applyTheme = { color = it.accentColor * 0.8f }
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as UIText).text = player.mods.toDisplayModString()
            } else {
                if (modDisplay !is ModsIndicator) {
                    modDisplay?.detachSelf()

                    modDisplay = ModsIndicator().apply {
                        minHeight = 18f // Force to take space even if no mods are enabled
                        iconSize = 18f
                    }

                    innerContainer += modDisplay!!
                }

                (modDisplay as ModsIndicator).mods = if (room.gameplaySettings.isFreeMod) player.mods.values else null
            }

            onActionLongPress = {
                UIDropdown(this@RoomPlayerButton).apply dropdown@{
                    maxWidth = 260f

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
                                applyTheme = {
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
}