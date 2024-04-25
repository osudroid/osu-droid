package com.reco1l.osu.ui.multiplayer

import android.animation.Animator
import android.app.AlertDialog
import android.view.View
import android.widget.TextView
import com.edlplan.framework.easing.Easing
import com.edlplan.ui.BaseAnimationListener
import com.edlplan.ui.EasingHelper
import com.edlplan.ui.fragment.BaseFragment
import com.edlplan.ui.fragment.WebViewFragment
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.RoomPlayer
import com.reco1l.osu.Multiplayer
import ru.nsu.ccfit.zuev.osuplus.R
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal

class RoomPlayerMenu : BaseFragment()
{

    override val layoutID = R.layout.multiplayer_room_player_menu


    var player: RoomPlayer? = null


    private var webView: WebViewFragment? = null


    init
    {
        isDismissOnBackgroundClick = true
    }


    override fun onLoadView()
    {
        if (player == null)
        {
            dismiss()
            return
        }

        findViewById<TextView>(R.id.room_player)!!.text = player!!.name

        findViewById<View>(R.id.room_profile)!!.setOnClickListener {

            webView = WebViewFragment().apply {
                setURL(WebViewFragment.PROFILE_URL + player!!.id)
                show()
            }
        }

        val muteText = findViewById<TextView>(R.id.mute_text)!!

        fun updateText()
        {
            if (player!!.isMuted)
                muteText.text = "Unmute"
            else
                muteText.text = "Mute"
        }

        updateText()

        findViewById<View>(R.id.room_mute)!!.setOnClickListener {
            player!!.isMuted = !player!!.isMuted
            updateText()
        }

        val kick = findViewById<View>(R.id.room_kick)!!
        val host = findViewById<View>(R.id.room_host)!!

        if (!Multiplayer.isRoomHost)
        {
            kick.visibility = View.INVISIBLE
            host.visibility = View.INVISIBLE
        }

        kick.setOnClickListener {
            AlertDialog.Builder(getGlobal().mainActivity).apply {

                setTitle("Kick ${player!!.name}")
                setMessage("Are you sure?")
                setPositiveButton("Yes") { dialog, _ ->

                    dialog.dismiss()
                    dismiss()

                    if (Multiplayer.isConnected)
                        RoomAPI.kickPlayer(player!!.id)
                }
                setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

            }.show()
        }

        host.setOnClickListener {

            AlertDialog.Builder(getGlobal().mainActivity).apply {

                setTitle("Make ${player!!.name} room host")
                setMessage("Are you sure?")
                setPositiveButton("Yes") { dialog, _ ->

                    dialog.dismiss()
                    dismiss()

                    if (Multiplayer.isConnected)
                        RoomAPI.setRoomHost(player!!.id)
                }
                setNegativeButton("No") { dialog, _ -> dialog.dismiss() }

            }.show()
        }

        playOnLoadAnim()
    }

    // Code extracted from PropsMenuFragment class

    private fun playOnLoadAnim()
    {
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

    private fun playEndAnim(action: Runnable?)
    {
        val body = findViewById<View>(R.id.fullLayout) ?: return
        body.animate().cancel()
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
        webView?.dismiss()
        playEndAnim { super.dismiss() }
    }
}
