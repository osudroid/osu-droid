package com.reco1l.osu.multiplayer

import android.animation.Animator
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import ru.nsu.ccfit.zuev.osu.SecurityUtils
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.ibancho.LobbyAPI
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.RoomBeatmap
import com.reco1l.osu.async
import com.reco1l.osu.mainThread
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osuplus.R

class LobbyCreateRoom : BaseFragment() {


    override val layoutID = R.layout.multiplayer_create_room


    init {
        isDismissOnBackgroundClick = true
    }


    override fun onLoadView() {

        findViewById<View>(R.id.frg_body)!!.cornerRadius = 14f.dp

        val roomNameField = findViewById<EditText>(R.id.room_name)
        val roomSizeSlider = findViewById<SeekBar>(R.id.room_max_bar)!!
        val roomPasswordField = findViewById<EditText>(R.id.room_password)

        val errorText = findViewById<TextView>(R.id.room_error_text)!!

        roomNameField!!.setText("${OnlineManager.getInstance().username}'s room")

        roomSizeSlider.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                findViewById<TextView>(R.id.room_max_text)!!.text = progress.toString()
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}

        })

        findViewById<View>(R.id.room_create)!!.setOnClickListener {

            if (roomNameField.text.isNullOrEmpty()) {
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            } else {
                errorText.visibility = View.GONE
            }

            mainThread {
                dismiss()
                LobbyScene.search.dismiss()
            }

            async {
                LoadingScreen().show()

                val name = roomNameField.text.toString()
                val password = roomPasswordField!!.text.toString().takeUnless { it.isEmpty() }

                val beatmap = GlobalManager.getInstance().selectedBeatmap?.let {

                    RoomBeatmap(
                        md5 = it.md5,
                        title = it.title,
                        artist = it.artist,
                        creator = it.creator,
                        version = it.version
                    )
                }

                var signStr = "${name}_${roomSizeSlider.progress}"
                if (password != null) {
                    signStr += "_${password}"
                }

                try {

                    val roomId = LobbyAPI.createRoom(
                        name = name,
                        beatmap = beatmap,
                        hostUID = OnlineManager.getInstance().userId,
                        hostUsername = OnlineManager.getInstance().username,
                        sign = SecurityUtils.signRequest(signStr),
                        password = password,
                        maxPlayers = roomSizeSlider.progress
                    )

                    RoomAPI.connectToRoom(roomId, OnlineManager.getInstance().userId, OnlineManager.getInstance().username, password)

                } catch (e: Exception) {
                    ToastLogger.showText("Failed to create a room: ${e.message}", true)
                    e.printStackTrace()
                    LobbyScene.show()
                }

            }
        }

        playOnLoadAnim()
    }


    private fun playOnLoadAnim() {

        val body = findViewById<View>(R.id.frg_body)

        body!!.alpha = 0f
        body.translationY = 200f

        body.animate().cancel()
        body.animate()
            .translationY(0f)
            .alpha(1f)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setDuration(150)
            .start()

        playBackgroundHideInAnim(150)
    }

    private fun playEndAnim(action: Runnable?) {

        val body = findViewById<View>(R.id.frg_body)

        body!!.animate().cancel()
        body.animate()
            .translationY(200f)
            .alpha(0f)
            .setDuration(200)
            .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
            .setListener(object : BaseAnimationListener() {
                override fun onAnimationEnd(animation: Animator) {
                    action?.run()
                }
            })
            .start()

        playBackgroundHideOutAnim(200)
    }


    override fun dismiss() {
        playEndAnim {
            super.dismiss()
        }
    }

}
