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
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*
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
    private val body = UILinearContainer()

    private val messageTimestampBuffer = UITextCompoundBuffer(8).asSharedDynamically()
    private val messagePlayerTagBuffer = UITextCompoundBuffer(32).asSharedDynamically()
    private val messageTextBuffer = UITextCompoundBuffer(128).asSharedDynamically()
    private val messageBackgroundBuffer = UIBox.BoxVBO(0f, 0, PaintStyle.Fill).asSharedDynamically()

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
        width = FillParent
        orientation = Orientation.Vertical
        anchor = Anchor.BottomCenter
        origin = Anchor.BottomCenter
        background = UIBox().apply {
            applyTheme = {
                color = it.accentColor * 0.1f
                alpha = 0.9f
            }
        }

        +button
        +body.apply {
            width = FillParent
            height = 0f
            orientation = Orientation.Vertical

            scrollableContainer {
                width = FillParent
                height = body_height - 84f // Input height based of button height plus padding
                scrollAxes = Axes.Y
                clipToBounds = true

                messageContainer = linearContainer {
                    width = FillParent
                    orientation = Orientation.Vertical

                    repeat(max_messages) {
                        attachChild(MessageComponent())
                    }
                }
            }

            flexContainer {
                width = FillParent
                padding = Vec4(80f, 12f)
                gap = 8f

                +UITextInput("").apply {
                    height = FillParent
                    placeholder = "Type a message..."
                    onConfirm = { sendMessage() }
                    flexRules {
                        grow = 1f
                    }

                    input = this
                }

                iconButton {
                    icon = ResourceManager.getInstance().getTexture("send")
                    isSelected = true
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
        input.blur()

        async {
            try {
                RoomAPI.sendMessage(text)
            } catch (e: Exception) {
                onSystemChatMessage("Error to send message: ${e.message}", "#FFBFBF")
                e.printStackTrace()
            }
        }
    }

    private fun appendMessage(message: Message) {

        if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
            ResourceManager.getInstance().getSound("heartbeat")?.play(0.75f)
        }

        messages.add(message)
        if (messages.size > max_messages) {
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
            body.apply {
                clearModifiers(ModifierType.SizeY)
                sizeToY(body_height, 0.4f).eased(Easing.OutExpo)
            }
        }
    }

    fun collapse() {
        if (isExpanded) {
            isExpanded = false
            body.apply {
                clearModifiers(ModifierType.SizeY)
                sizeToY(0f, 0.4f).eased(Easing.OutExpo)
            }
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

            for (i in max_messages - 1 downTo 0) {
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


    inner class ChatButton : UILinearContainer() {

        private lateinit var tagText: UIText
        private lateinit var messageText: UIText


        init {
            width = FillParent
            orientation = Orientation.Horizontal
            padding = Vec4(80f, 18f)
            spacing = 12f
            background = UIBox().apply {
                applyTheme = {
                    color = it.accentColor * 0.15f
                    alpha = 0.5f
                }
            }

            sprite {
                textureRegion = ResourceManager.getInstance().getTexture("chat")
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                applyTheme = { color = it.accentColor }
                size = Vec2(28f)
            }

            linearContainer {
                width = FillParent
                orientation = Orientation.Horizontal

                tagText = text {
                    text = "Chat"
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    color = Theme.current.accentColor
                }

                messageText = text {
                    width = FillParent
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    applyTheme = { color = it.accentColor }
                }
            }
        }


        fun updateState() {
            val lastMessage = messages.peekLast()

            if (isExpanded || lastMessage == null) {
                tagText.apply {
                    text = "Chat"
                    color = Theme.current.accentColor
                }
                messageText.text = ""
            } else {
                tagText.apply {
                    text = "${if (lastMessage is PlayerMessage) lastMessage.player.name else "System"}: "
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
            width = FillParent
            padding = Vec4(80f, 0f)
            orientation = Orientation.Horizontal
            cullingMode = CullingMode.ParentBounds
            background = UIBox().apply {
                buffer = messageBackgroundBuffer
                applyTheme = {
                    color = it.accentColor * 0.09f
                    alpha = 0f
                }
            }
        }


        override fun onManagedUpdate(deltaTimeSec: Float) {

            if (messageChanged) {
                messageChanged = false

                detachChildren()

                val message = message ?: return
                val messageIndex = messages.indexOf(message)

                background!!.alpha = if (messageIndex % 2 == 0) 0.9f else 0f

                if (message is SystemMessage) {
                    text {
                        buffer = messageTextBuffer
                        width = FillParent
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

                    flexContainer {
                        relativeSizeAxes = Axes.X
                        width = 0.2f
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        direction = FlexDirection.Row
                        justifyContent = JustifyContent.SpaceBetween

                        text {
                            buffer = messageTimestampBuffer
                            applyTheme = { color = it.accentColor * 0.5f }
                            text = timestampFormat.format(message.time)
                        }

                        if (showSender) {
                            text {
                                buffer = messagePlayerTagBuffer
                                color = getPlayerTagColor(message.player)
                                text = "${message.player.name}: "
                            }
                        }
                    }

                    text {
                        buffer = messageTextBuffer
                        width = FillParent
                        applyTheme = { color = it.accentColor }
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
        private const val max_messages = 50
        private const val body_height = 420f
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