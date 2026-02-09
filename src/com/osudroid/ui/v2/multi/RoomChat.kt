package com.osudroid.ui.v2.multi

import android.icu.text.SimpleDateFormat
import com.edlplan.framework.easing.Easing
import com.osudroid.multiplayer.*
import com.osudroid.multiplayer.api.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.pct
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osuplus.R
import java.util.LinkedList

/**
 * Because we're pros we want to highlight us.
 */
private val DEVELOPERS = longArrayOf(
    51076, // Rian8337
    55374, // Acivev
    307054 // Reco1l
)


class RoomChat : UILinearContainer() {

    /**
     * Height of the chat button.
     */
    val buttonHeight
        get() = button.height


    private val messages = LinkedList<Message>()
    private val timestampFormat = SimpleDateFormat("HH:mm:ss")

    private val button = ChatButton()
    private val body: UIFillContainer

    private lateinit var input: UITextInput
    private lateinit var messageContainer: UILinearContainer

    private var messagesChanged = false

    private var isExpanded = false
        set(value) {
            if (field != value) {
                field = value
                button.updateState()
            }
        }


    init {
        width = Size.Full
        orientation = Orientation.Vertical
        anchor = Anchor.BottomCenter
        origin = Anchor.BottomCenter
        style = {
            backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.9f)
            y = if (isExpanded) 0f else BODY_HEIGHT.rem
        }

        +button

        body = fillContainer {
            width = Size.Full
            orientation = Orientation.Vertical
            style = {
                height = BODY_HEIGHT.rem
            }

            scrollableContainer {
                width = Size.Full
                height = Size.Full
                scrollAxes = Axes.Y
                clipToBounds = true

                messageContainer = linearContainer {
                    width = Size.Full
                    orientation = Orientation.Vertical

                    repeat(MAX_MESSAGES) {
                        attachChild(MessageComponent())
                    }
                }
            }

            fillContainer {
                width = Size.Full
                style = {
                    spacing = 2f.srem
                    padding = UIEngine.current.safeArea.copy(y = 2f.srem, w = 3f.srem)
                }

                +UITextInput("").apply {
                    width = Size.Full
                    height = Size.Full
                    placeholder = "Type a message..."
                    onConfirm = { sendMessage() }

                    input = this
                }

                textButton {
                    trailingIcon = FontAwesomeIcon(Icon.PaperPlane)
                    colorVariant = ColorVariant.Primary
                    setText(R.string.multiplayer_room_chat_send)
                    onActionUp = { sendMessage() }
                }
            }
        }

    }


    private fun sendMessage() {

        val text = input.value.trim()
        if (text.isEmpty()) {
            return
        }

        input.value = ""

        async {
            try {
                RoomAPI.sendMessage(text)
            } catch (e: Exception) {
                onSystemChatMessage(StringTable.format(R.string.multiplayer_room_chat_error, e.message), "#FFBFBF")
                e.printStackTrace()
            }
        }
    }

    private fun appendMessage(message: Message) {

        if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
            ResourceManager.getInstance().getSound("heartbeat")?.play(0.75f)
        }

        messages.add(message)
        if (messages.size > MAX_MESSAGES) {
            messages.pollFirst()
        }

        button.updateState()
        messagesChanged = true
    }


    fun show() {
        if (!hasParent()) {
            UIEngine.current.overlay.attachChild(this)
        }
    }

    fun hide() {
        detachSelf()
    }

    fun expand() {
        if (!isExpanded) {
            isExpanded = true
            clearModifiers(ModifierType.SizeY)
            moveToY(0f, 0.4f).eased(Easing.OutExpo)
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            clearModifiers(ModifierType.SizeY)
            moveToY(BODY_HEIGHT.rem, 0.4f).eased(Easing.OutExpo)
        }
    }


    fun onRoomChatMessage(player: RoomPlayer, message: String) = mainThread {
        appendMessage(
            PlayerMessage(
                player = player,
                content = message
            )
        )
    }

    fun onSystemChatMessage(message: String, color: String) = mainThread {
        Multiplayer.log("System message: $message")

        appendMessage(
            SystemMessage(
                color = Color4(color),
                content = message
            )
        )
    }


    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (messagesChanged) {
            messagesChanged = false

            for (i in MAX_MESSAGES - 1 downTo 0) {
                val messageComponent = messageContainer.getChild(i) as MessageComponent
                messageComponent.message = if (i < messages.size) messages[i] else null
            }
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (super.onAreaTouched(event, localX, localY) || isExpanded) {
            return true
        }

        collapse()
        return false
    }


    private fun getPlayerTagColor(player: RoomPlayer): Color4 {
        return Color4(
            when {
                player.id in DEVELOPERS -> "#F280FF"
                player.isHost -> "#00FFEA"
                else -> "#8282A8"
            }
        )
    }


    inner class ChatButton : UIFillContainer() {

        private lateinit var tagText: UIText
        private lateinit var messageText: UIText


        init {
            width = Size.Full
            orientation = Orientation.Horizontal
            style = {
                height = 2.85f.rem
                spacing = 2f.srem
                backgroundColor = (it.accentColor * 0.15f).copy(alpha = 0.5f)
                padding = UIEngine.current.safeArea.copy(y = 0f, w = 0f)
            }

            +FontAwesomeIcon(Icon.Message).apply {
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = { color = it.accentColor }
            }

            linearContainer {
                width = Size.Full
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = Orientation.Horizontal

                tagText = text {
                    text = StringTable.get(R.string.multiplayer_room_chat)
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    color = Theme.current.accentColor
                }

                messageText = text {
                    width = Size.Full
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = { color = it.accentColor }
                }
            }
        }


        fun updateState() {
            val lastMessage = messages.peekLast()

            if (isExpanded || lastMessage == null) {
                tagText.apply {
                    text = StringTable.get(R.string.multiplayer_room_chat)
                    color = Theme.current.accentColor
                }
                messageText.text = ""
            } else {
                tagText.apply {
                    text = "${if (lastMessage is PlayerMessage) lastMessage.player.name else StringTable.get(R.string.multiplayer_room_chat_system)}: "
                    color = if (lastMessage is PlayerMessage) getPlayerTagColor(lastMessage.player) else Theme.current.accentColor
                }
                messageText.text = lastMessage.content
            }
        }


        override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
            if (event.isActionUp) {
                if (!isExpanded) {
                    expand()
                } else {
                    collapse()
                }
            }
            return true
        }

    }


    inner class MessageComponent : UILinearContainer() {

        var message: Message? = null
            set(value) {
                if (field != value) {
                    field = value
                    messageChanged = true
                }
            }


        private var messageChanged = false


        init {
            width = Size.Full
            orientation = Orientation.Horizontal
            cullingMode = CullingMode.ParentBounds
            style = {
                backgroundColor = (it.accentColor * 0.09f).copy(alpha = 0f)
                padding = UIEngine.current.safeArea.copy(y = 0f, w = 0f)
                spacing = 2f.srem
            }
        }


        override fun onManagedUpdate(deltaTimeSec: Float) {

            if (messageChanged) {
                messageChanged = false

                detachChildren()

                val message = message ?: return
                val messageIndex = messages.indexOf(message)

                backgroundColor = backgroundColor.copy(alpha = if (messageIndex % 2 == 0) 0.5f else 0f)

                if (message is SystemMessage) {
                    text {
                        width = Size.Full
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        text = message.content
                        padding = Vec4(12f)
                        color = message.color
                        alignment = Anchor.Center
                    }
                } else if (message is PlayerMessage) {

                    val showSender = messageIndex == 0
                        || messages[messageIndex - 1] !is PlayerMessage
                        || (messages[messageIndex - 1] as PlayerMessage).player.id != message.player.id

                    container {
                        width = 0.25f.pct
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        orientation = Orientation.Horizontal

                        text {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            style = { color = it.accentColor * 0.5f }
                            text = timestampFormat.format(message.time)
                        }

                        if (showSender) {
                            text {
                                anchor = Anchor.CenterRight
                                origin = Anchor.CenterRight
                                color = getPlayerTagColor(message.player)
                                text = message.player.name
                            }
                        }
                    }

                    text {
                        width = Size.Full
                        style = { color = it.accentColor }
                        text = message.content
                    }
                } else {
                    // Technically should never happen.
                    isVisible = false
                }
            }

            super.onManagedUpdate(deltaTimeSec)
        }

    }


    companion object {
        private const val MAX_MESSAGES = 50
        private const val BODY_HEIGHT = 18f
    }
}


//region Messages

interface Message {
    val content: String
    val time: Long
}

data class PlayerMessage(
    val player: RoomPlayer,

    override val content: String,
    override val time: Long = System.currentTimeMillis(),
) : Message

data class SystemMessage(
    val color: Color4,

    override val content: String,
    override val time: Long = System.currentTimeMillis(),
) : Message

//endregion