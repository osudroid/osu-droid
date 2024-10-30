package com.reco1l.osu.multiplayer

import android.animation.Animator
import android.app.AlertDialog
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.edlplan.ui.fragment.WebViewFragment
import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.RoomPlayer
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.toolkt.android.cornerRadius
import com.reco1l.toolkt.android.dp
import ru.nsu.ccfit.zuev.osuplus.R

class RoomPlayerMenu : BaseFragment() {


    override val layoutID = R.layout.multiplayer_room_player_menu


    var player: RoomPlayer? = null


    private var webView: WebViewFragment? = null


    init {
        isDismissOnBackgroundClick = true
    }


    override fun onLoadView() {

        if (player == null) {
            dismiss()
            return
        }

        findViewById<View>(R.id.fullLayout)!!.cornerRadius = 14f.dp
        findViewById<TextView>(R.id.room_player)!!.text = player!!.name

        findViewById<View>(R.id.room_profile)!!.setOnClickListener {

            webView = WebViewFragment().apply {
                setURL(WebViewFragment.PROFILE_URL + player!!.id)
                show()
            }
        }

        val mute = findViewById<Button>(R.id.room_mute)!!
        mute.setOnClickListener {
            player!!.isMuted = !player!!.isMuted
            mute.text = if (player!!.isMuted) "Unmute" else "Mute"
        }
        mute.text = if (player!!.isMuted) "Unmute" else "Mute"


        val kick = findViewById<View>(R.id.room_kick)!!
        val host = findViewById<View>(R.id.room_host)!!

        if (!Multiplayer.isRoomHost) {
            kick.visibility = View.GONE
            host.visibility = View.GONE
        }

        kick.setOnClickListener {

            MessageDialog().apply {

                setTitle("Kick ${player!!.name}")
                setMessage("Are you sure?")

                addButton("Yes") {

                    it.dismiss()
                    dismiss()

                    if (Multiplayer.isConnected) RoomAPI.kickPlayer(player!!.id)
                }

                addButton("No") {
                    it.dismiss()
                }

            }.show()
        }

        host.setOnClickListener {

            MessageDialog().apply {

                setTitle("Make ${player!!.name} room host")
                setMessage("Are you sure?")

                addButton("Yes") {

                    it.dismiss()
                    dismiss()

                    if (Multiplayer.isConnected) RoomAPI.setRoomHost(player!!.id)
                }

                addButton("No") {
                    it.dismiss()
                }

            }.show()
        }

        playOnLoadAnim()
    }


    private fun playOnLoadAnim() {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.alpha = 0f
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
        val body = findViewById<View>(R.id.fullLayout) ?: return

        body.animate().cancel()
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
        webView?.dismiss()
        playEndAnim {
            super.dismiss()
        }
    }
}
