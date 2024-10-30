package com.reco1l.osu.multiplayer

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnKeyListener
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.RoomPlayer
import com.reco1l.osu.mainThread
import com.reco1l.toolkt.android.drawableLeft
import com.reco1l.toolkt.android.drawableRight
import com.reco1l.toolkt.android.fontColor
import com.reco1l.toolkt.kotlin.async
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osuplus.R
import kotlin.math.abs


/**
 * Because we're pros we want to highlight us.
 */
private val DEVELOPERS = longArrayOf(
    51076, // Rian8337
    55374, // Acivev
    307054 // Reco1l
)


class RoomChat : BaseFragment(), OnEditorActionListener, OnKeyListener
{

    override val layoutID = R.layout.multiplayer_room_chat


    private lateinit var field: EditText

    private lateinit var recyclerView: RecyclerView


    private val adapter = MessageAdapter()

    private val isExtended: Boolean
        get() = findViewById<View?>(R.id.fullLayout) != null && abs(findViewById<View>(R.id.fullLayout)!!.translationY) < 10


    init {
        isDismissOnBackPress = false
    }


    override fun onLoadView() {
        reload()

        field = findViewById(R.id.chat_field)!!
        field.setOnEditorActionListener(this)
        field.setOnKeyListener(this)

        recyclerView = findViewById(R.id.chat_text)!!
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, true)
        recyclerView.adapter = adapter

        findViewById<Button>(R.id.chat_send)!!.setOnClickListener {
            sendMessage()
        }
     }


    fun clear() {
        val size = adapter.data.size

        adapter.data.clear()
        adapter.notifyItemRangeRemoved(0, size)
    }

    fun onRoomChatMessage(player: RoomPlayer, message: String) = mainThread {

        prependMessage(Message(player.id, message))

        val color = when(player.id)
        {
            Multiplayer.room!!.host -> "#00FFEA"
            in DEVELOPERS -> "#F280FF"
            else -> "#8282A8"
        }

        showPreview(" $message", tag = "${player.name}:", tagColor = color)
    }

    fun onSystemChatMessage(message: String, color: String) = mainThread {

        Multiplayer.log("System message: $message")
        prependMessage(Message(null, message, Color.parseColor(color)))
        showPreview(message, contentColor = color)
    }


    private fun prependMessage(message: Message) {

        if (GlobalManager.getInstance().engine.scene != GlobalManager.getInstance().gameScene.scene) {
            ResourceManager.getInstance().getSound("heartbeat")?.play(0.75f)
        }

        adapter.data.add(0, message)
        adapter.notifyItemRangeChanged(0, adapter.data.size)
    }

    private fun showPreview(content: String, contentColor: String? = null, tag: String? = null, tagColor: String? = null) {

        RGBColor.hex2Rgb(tagColor ?: "#FFFFFF").apply(RoomScene.chatPreviewText.tag)
        RGBColor.hex2Rgb(contentColor ?: "#FFFFFF").apply(RoomScene.chatPreviewText.content)

        RoomScene.chatPreviewText.setTagText(tag ?: "")
        RoomScene.chatPreviewText.setContentText(content)
    }

    private fun hideKeyboard() {
        field.clearFocus()

        val imm = context?.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        imm?.hideSoftInputFromWindow(field.windowToken, 0)
    }

    private fun sendMessage() {

        hideKeyboard()

        val message = field.text.takeUnless { it.isNullOrEmpty() } ?: return
        field.text = null

        async {
            try {
                RoomAPI.sendMessage(message.toString())
            } catch (e: Exception) {
                onSystemChatMessage("Error to send message: ${e.message}", "#FFBFBF")
                e.printStackTrace()
            }

        }

    }

    override fun onEditorAction(v: TextView?, actionId: Int, event: KeyEvent?): Boolean {

        if (actionId == EditorInfo.IME_ACTION_SEND) {
            sendMessage()
            return true
        }
        return false
    }

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == KeyEvent.KEYCODE_ENTER && v is TextView) {
            return onEditorAction(v, EditorInfo.IME_ACTION_SEND, event)
        }
        return false
    }

    private fun reload() {

        findViewById<View>(R.id.showMoreButton)?.setOnTouchListener { view, event ->

            if (event.action == TouchEvent.ACTION_DOWN) {

                view.animate().cancel()
                view.animate().scaleY(0.9f).scaleX(0.9f).translationY(view.height * 0.1f).setDuration(100).start()
                toggleVisibility()

                return@setOnTouchListener true

            } else if (event.action == TouchEvent.ACTION_UP) {

                view.animate().cancel()
                view.animate().scaleY(1f).scaleX(1f).setDuration(100).translationY(0f).start()

                return@setOnTouchListener true
            }
            false
        }

        findViewById<View>(R.id.frg_background)?.isClickable = false
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun toggleVisibility()
    {
        hideKeyboard()

        if (isExtended) {
            playHidePanelAnim()

            findViewById<View>(R.id.frg_background)!!.setOnTouchListener(null)
            findViewById<View>(R.id.frg_background)!!.isClickable = false

            return
        }
        playShowPanelAnim()

        findViewById<View>(R.id.frg_background)!!.setOnTouchListener { _, event ->

            if (event.action == TouchEvent.ACTION_DOWN) {

                if (isExtended) {
                    toggleVisibility()
                }

                return@setOnTouchListener true
            }
            false
        }

        findViewById<View>(R.id.frg_background)!!.isClickable = true
    }

    private fun playShowPanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout) ?: return

        fullLayout.animate().cancel()
        fullLayout.animate()
            .translationY(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    findViewById<View>(R.id.frg_background)!!.isClickable = true
                    findViewById<View>(R.id.frg_background)!!.setOnClickListener { playHidePanelAnim() }
                }

            })
            .start()
    }

    private fun playHidePanelAnim()
    {
        val fullLayout = findViewById<View>(R.id.fullLayout) ?: return
        fullLayout.animate().cancel()
        fullLayout.animate()
            .translationY(findViewById<View>(R.id.optionBody)!!.height.toFloat())
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    findViewById<View>(R.id.frg_background)!!.isClickable = false
                }

            })
            .start()
    }

    override fun callDismissOnBackPress() {

        if (isExtended) {
            mainThread { toggleVisibility() }
            return
        }

        if (GlobalManager.getInstance().engine.scene == GlobalManager.getInstance().gameScene.scene) {
            GlobalManager.getInstance().gameScene.pause()
            return
        }

        mainThread { RoomScene.leaveDialog.show() }
    }

}


data class Message(val sender: Long?, val text: String, val color: Int? = null)


class MessageAdapter : RecyclerView.Adapter<MessageViewHolder>() {


    val data = mutableListOf<Message>()


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.multiplayer_room_chat_item, parent, false) as LinearLayout

        return MessageViewHolder(view)
    }


    override fun getItemCount() = data.size

    override fun getItemId(position: Int) = position.toLong()



    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {

        val msg = data[position]

        // The sender label will be shown if the previous message is not from the same sender
        val showSender = position == data.size - 1 || data[position + 1].sender != msg.sender

        holder.bind(msg, showSender)
    }

}

class MessageViewHolder(private val root: LinearLayout) : RecyclerView.ViewHolder(root) {


    private lateinit var senderText: TextView

    private lateinit var messageText: TextView


    fun bind(msg: Message, showSender: Boolean) {

        senderText = root.findViewById(R.id.sender_text)!!
        messageText = root.findViewById(R.id.message_text)!!

        if (msg.sender == null) {
            messageText.isVisible = false
            senderText.isVisible = true
            senderText.text = msg.text

            root.gravity = Gravity.CENTER

            if (msg.color != null) {
                senderText.fontColor = msg.color
            }
            return
        }

        val isOwnMessage = msg.sender == Multiplayer.player!!.id

        messageText.isVisible = true
        senderText.isVisible = showSender

        if (showSender) {

            val isRoomHost = msg.sender == Multiplayer.room!!.host
            val isDeveloper = msg.sender in DEVELOPERS

            senderText.text = Multiplayer.room!!.playersMap[msg.sender]!!.name

            val color = when {
                isRoomHost -> 0xFF00FFEA.toInt()
                isDeveloper -> 0xFFF280FF.toInt()
                else -> 0xFF8282A8.toInt()
            }

            val drawable = when {
                isRoomHost -> itemView.context.getDrawable(R.drawable.crown_16px)
                isDeveloper -> itemView.context.getDrawable(R.drawable.deployed_code_account_16px)
                else -> null
            }

            if (isOwnMessage) {
                senderText.drawableLeft = drawable
            } else {
                senderText.drawableRight = drawable
            }

            drawable?.setTint(color)

            senderText.fontColor = color
        }

        root.gravity = if (isOwnMessage) Gravity.RIGHT else Gravity.LEFT

        messageText.text = msg.text
    }

}
