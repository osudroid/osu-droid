package com.reco1l.legacy.ui.entity

import com.reco1l.api.chimu.CheesegullAPI
import com.reco1l.api.ibancho.RoomAPI
import com.reco1l.api.ibancho.data.PlayerStatus.READY
import com.reco1l.api.ibancho.data.RoomBeatmap
import com.reco1l.framework.extensions.orAsyncCatch
import com.reco1l.legacy.ui.ChimuWebView.FILE_EXTENSION
import com.reco1l.legacy.ui.multiplayer.Multiplayer
import com.reco1l.legacy.ui.multiplayer.RoomScene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.MenuItemTrack
import ru.nsu.ccfit.zuev.skins.OsuSkin
import com.reco1l.legacy.ui.ChimuWebView.INSTANCE as chimuFragment
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.LibraryManager.INSTANCE as libraryManager
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * Simplified version of [MenuItemTrack]
 */
open class BeatmapButton : Sprite(0f, 0f, getResources().getTexture("menu-button-background"))
{

    private val trackTitle = ChangeableText(32f, 20f, getResources().getFont("smallFont"), "", 100)

    private val creatorInfo = ChangeableText(32f, trackTitle.height + 20, getResources().getFont("smallFont"), "", 200)

    private val stars = Array(10) { i ->

        Sprite(0f, 0f, getResources().getTexture("star")).also {

            it.setScale(0.5f)
            it.setPosition(20f + it.widthScaled * i, creatorInfo.y + 20f)
        }
    }

    private var pressed = false
    private var moved = false
    private var initialX = 0f
    private var initialY = 0f


    init
    {
        OsuSkin.get().getColor("MenuItemVersionsDefaultColor", DEFAULT_COLOR).apply(this)
        OsuSkin.get().getColor("MenuItemDefaultTextColor", DEFAULT_TEXT_COLOR).applyAll(trackTitle, creatorInfo)

        alpha = 0.8f
        creatorInfo.apply { setColor(red * 0.8f, green * 0.8f, blue * 0.8f) }

        this.attachChild(trackTitle)
        this.attachChild(creatorInfo)
        stars.iterator().forEach { attachChild(it) }
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean
    {
        if (event.isActionDown) {
            pressed = true
            moved = false
            initialX = localX
            initialY = localY
        }

        if (event.isActionOutside || event.isActionMove && MathUtils.distance(initialX, initialY, localX, localY) > 30) {
            moved = true
        }

        if (!pressed || moved || !event.isActionUp || Multiplayer.player!!.status == READY || RoomScene.awaitBeatmapChange || RoomScene.awaitStatusChange)
            return true

        getResources().getSound("menuclick")?.play()

        pressed = false
        moved = false

        if (Multiplayer.isRoomHost)
        {
            if (libraryManager.library.isEmpty())
            {
                getGlobal().songService.pause()
                chimuFragment.show()
                return true
            }

            getGlobal().songMenu.reload()
            getGlobal().songMenu.show()
            getGlobal().songMenu.select()

            // We notify all clients that the host is changing beatmap
            RoomAPI.changeBeatmap()
            return true
        }


        // If the room beatmap has set a 'parentSetID' it means that the beatmap can be downloaded trough Chimu.moe
        if (getGlobal().selectedTrack == null) Multiplayer.room!!.beatmap?.apply {

            // If it's null the beatmap isn't available on Chimu servers.
            if (parentSetID == null)
                return true

            val url = "${CheesegullAPI.DOWNLOAD}/$parentSetID"

            { chimuFragment.startDownload(url, "$parentSetID $artist - $title$FILE_EXTENSION") }.orAsyncCatch {

                ToastLogger.showText("Unable to download beatmap: ${it.message}", true)
                it.printStackTrace()
            }
        }
        return true
    }


    fun updateBeatmap(beatmap: RoomBeatmap?)
    {
        stars.iterator().forEach { it.isVisible = false }

        if (beatmap == null)
        {
            trackTitle.text = if (Multiplayer.isRoomHost) "Tap to select a beatmap." else "Room is changing beatmap..."
            creatorInfo.text = ""
            return
        }

        trackTitle.text = "${beatmap.artist} - ${beatmap.title}"
        creatorInfo.text = "Mapped by ${beatmap.creator} // ${beatmap.version}"

        if (getGlobal().selectedTrack == null)
        {
            creatorInfo.text += "\n${

                if (beatmap.parentSetID == null)
                    "Beatmap not found on Chimu."
                else
                    "Tap to download."
            }"
            return
        }

        stars.forEachIndexed { i, it -> it.isVisible = getGlobal().selectedTrack.difficulty >= i }
    }


    companion object
    {
        private val DEFAULT_COLOR = RGBColor(25 / 255f, 25 / 255f, 240 / 255f)
        val DEFAULT_TEXT_COLOR = RGBColor(1f, 1f, 1f)
    }
}
