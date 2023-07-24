package com.reco1l.legacy.ui.entity

import com.reco1l.api.chimu.CheesegullAPI
import com.reco1l.api.ibancho.RoomAPI.changeBeatmap
import com.reco1l.api.ibancho.data.PlayerStatus
import com.reco1l.api.ibancho.data.RoomBeatmap
import com.reco1l.framework.extensions.orAsyncCatch
import com.reco1l.legacy.ui.ChimuWebView
import com.reco1l.legacy.ui.multiplayer.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.TrackInfo
import ru.nsu.ccfit.zuev.osu.menu.MenuItemTrack
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * Simplified version of [MenuItemTrack]
 */
open class BeatmapButton : Sprite(0f, 0f, getResources().getTexture("menu-button-background"))
{

    var beatmap: RoomBeatmap? = null
        set(value)
        {
            field = null
            RoomScene.hasLocalTrack = false

            stars.forEach { it.isVisible = false }

            trackTitle.text = "Room is changing beatmap..."
            creatorInfo.text = ""

            if (value == null)
                return

            trackTitle.text = "${value.version} (${value.creator})"
            creatorInfo.text = "${value.title} by ${value.artist}"

            field = value
        }


    private val trackTitle = ChangeableText(32f, 20f, getResources().getFont("smallFont"), "", 100)

    private val creatorInfo = ChangeableText(32f, trackTitle.height + 20, getResources().getFont("smallFont"), "", 100)

    private val stars = Array(10) { i ->

        val texture = getResources().getTexture("star")
        val sprite = Sprite(20f + texture.width * 0.5f * i, creatorInfo.y + 20f, texture)

        sprite.setScale(0.5f)
        sprite
    }


    init
    {
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this)
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(trackTitle, creatorInfo)

        alpha = 0.8f
        creatorInfo.apply { setColor(red * 0.8f, green * 0.8f, blue * 0.8f) }

        this.attachChild(trackTitle)
        this.attachChild(creatorInfo)
        stars.forEach { attachChild(it) }
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
    {
        if (!event.isActionUp || RoomScene.player!!.status == PlayerStatus.READY || RoomScene.awaitBeatmapChange)
            return true

        getResources().getSound("menuclick")?.play()

        if (Multiplayer.isRoomHost)
        {
            if (LibraryManager.INSTANCE.library.isEmpty())
            {
                getGlobal().songService.pause()
                ChimuWebView.INSTANCE.show()
                return true
            }

            getGlobal().songMenu.reload()
            getGlobal().songMenu.show()
            getGlobal().songMenu.select()

            // We notify all clients that the host is changing beatmap
            changeBeatmap()

            return true
        }

        // Finding the beatmap in Chimu.moe
        if (!RoomScene.hasLocalTrack && beatmap?.parentSetID != null)
        {
            {
                val url = "${CheesegullAPI.DOWNLOAD}/${beatmap!!.parentSetID}"
                val filename = "${beatmap!!.parentSetID} ${beatmap!!.artist} - ${beatmap!!.title}.osz"

                ChimuWebView.INSTANCE.startDownload(url, filename)

            }.orAsyncCatch {

                ToastLogger.showText("Unable to download beatmap: ${it.message}", true)
                it.printStackTrace()
            }
            return true
        }
        return true
    }

    fun loadTrack(track: TrackInfo?)
    {
        if (track != null)
        {
            RoomScene.hasLocalTrack = true
            stars.forEachIndexed { i, it -> it.isVisible = track.difficulty >= i }
            return
        }
        RoomScene.hasLocalTrack = false

        // Updating button text
        trackTitle.text = "${beatmap!!.version} (${beatmap!!.creator})"
        creatorInfo.text = "${beatmap!!.title} by ${beatmap!!.artist}\n${

            if (beatmap!!.parentSetID == null)
                "Beatmap not found on Chimu."
            else
                "Tap to download."
        }"
    }

    companion object
    {
        private val DEFAULT_COLOR = RGBColor(25 / 255f, 25 / 255f, 240 / 255f)
        private val DEFAULT_TEXT_COLOR = RGBColor(1f, 1f, 1f)
    }
}
