package com.reco1l.legacy.ui.multiplayer

import android.animation.Animator
import android.view.View
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.dgsrz.bancho.security.SecurityUtils
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.reco1l.api.ibancho.LobbyAPI
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.RoomBeatmap
import com.reco1l.framework.lang.async
import com.reco1l.framework.lang.uiThread
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.getInstance as getOnline

class LobbyCreateRoom : BaseFragment()
{

    override val layoutID = R.layout.multiplayer_create_room

    init
    {
        isDismissOnBackgroundClick = true
    }

    override fun onLoadView()
    {
        val nameField = findViewById<EditText>(R.id.room_name)
        val passwordField = findViewById<EditText>(R.id.room_password)
        val errorText = findViewById<TextView>(R.id.room_error_text)!!
        val maxText = findViewById<TextView>(R.id.room_max_text)!!
        val maxBar = findViewById<SeekBar>(R.id.room_max_bar)!!

        maxBar.setOnSeekBarChangeListener(
                object : OnSeekBarChangeListener
                {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean)
                    {
                        maxText.text = "Max players: $progress"
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
                    override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
                }
        )
        nameField!!.setText("${getOnline().username}'s room")

        findViewById<View>(R.id.room_create)!!.setOnClickListener {

            if (nameField.text.isNullOrEmpty())
            {
                errorText.visibility = View.VISIBLE
                return@setOnClickListener
            }
            else errorText.visibility = View.GONE

            uiThread {
                dismiss()
                LobbyScene.search.dismiss()
            }

            async {
                LoadingScreen().show()

                val name = nameField.text.toString()
                val password = passwordField!!.text.toString().takeUnless { it.isEmpty() }

                val beatmap = getGlobal().selectedTrack?.let {

                    RoomBeatmap(
                            md5 = it.mD5,
                            title = it.beatmap.title,
                            artist = it.beatmap.artist,
                            creator = it.creator,
                            version = it.mode
                    )
                }

                var signStr = "${name}_${maxBar.progress}"
                if (password != null) {
                    signStr += "_${password}"
                }

                try
                {
                    val roomId = LobbyAPI.createRoom(
                            name,
                            beatmap,
                            getOnline().userId,
                            getOnline().username,
                            SecurityUtils.signRequest(signStr),
                            password,
                            maxBar.progress
                    )
                    RoomAPI.connectToRoom(roomId, getOnline().userId, getOnline().username, password)
                }
                catch (e: Exception)
                {
                    ToastLogger.showText("Failed to create room: ${e.message}", true)
                    e.printStackTrace()
                    LobbyScene.show()
                }
            }
        }
        playOnLoadAnim()
    }

    // Code extracted from PropsMenuFragment class

    private fun playOnLoadAnim()
    {
        val body = findViewById<View>(R.id.fullLayout)
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

    private fun playEndAnim(action: Runnable?)
    {
        val body = findViewById<View>(R.id.fullLayout)
        body!!.animate().cancel()
        body.animate()
                .translationY(200f)
                .alpha(0f)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setListener(object : BaseAnimationListener()
                             {
                                 override fun onAnimationEnd(animation: Animator)
                                 {
                                     action?.run()
                                 }
                             })
                .start()
        playBackgroundHideOutAnim(200)
    }

    override fun dismiss()
    {
        playEndAnim { super.dismiss() }
    }
}
