package com.reco1l.osu.ui.entity

import com.reco1l.ibancho.RoomAPI
import com.reco1l.ibancho.data.PlayerStatus.READY
import com.reco1l.ibancho.data.RoomBeatmap
import com.reco1l.osu.async
import com.reco1l.osu.multiplayer.Multiplayer
import com.reco1l.osu.beatmaplisting.BeatmapDownloader
import com.reco1l.osu.beatmaplisting.BeatmapListing
import com.reco1l.osu.multiplayer.RoomScene
import org.anddev.andengine.entity.sprite.Sprite
import org.anddev.andengine.entity.text.ChangeableText
import org.anddev.andengine.input.touch.TouchEvent
import org.anddev.andengine.util.MathUtils
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.RGBColor
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.menu.BeatmapItem
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.osu.GlobalManager.getInstance as getGlobal
import ru.nsu.ccfit.zuev.osu.ResourceManager.getInstance as getResources

/**
 * Simplified version of [BeatmapItem]
 */
class BeatmapButton : Sprite(0f, 0f, getResources().getTexture("menu-button-background"))
{

    private val trackTitle = ChangeableText(32f, 20f, getResources().getFont("smallFont"), "", 100)

    private val creatorInfo = ChangeableText(32f, trackTitle.height + 20, getResources().getFont("smallFont"), "", 200)

    private val stars = Array(10) { i ->

        Sprite(0f, 0f, getResources().getTexture("star")).also {

            it.setScale(0.5f)
            it.setPosition(20f + it.widthScaled * i, creatorInfo.y + 20f)
        }
    }

    private var moved = false
    private var initialX: Float? = null
    private var initialY: Float? = null


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
        if (event.isActionDown) {
            moved = false
            initialX = localX
            initialY = localY
        }

        if (event.isActionOutside || initialX == null || initialY == null ||
            event.isActionMove && MathUtils.distance(initialX!!, initialY!!, localX, localY) > 30) {
            moved = true
        }

        if (moved || !event.isActionUp || Multiplayer.player!!.status == READY || RoomScene.awaitBeatmapChange || RoomScene.awaitStatusChange)
            return true

        getResources().getSound("menuclick")?.play()

        initialX = null
        initialY = null

        if (Multiplayer.isRoomHost)
        {
            if (LibraryManager.getLibrary().isEmpty())
            {
                getGlobal().songService.pause()
                BeatmapListing().show()
                return true
            }

            getGlobal().songMenu.reload()
            getGlobal().songMenu.show()
            getGlobal().songMenu.select()

            // We notify all clients that the host is changing beatmap
            RoomAPI.changeBeatmap()
            return true
        }


        // If the room beatmap has set a 'parentSetID' it means that the beatmap can be downloaded
        if (getGlobal().selectedBeatmap == null) Multiplayer.room!!.beatmap?.apply {

            // If it's null the beatmap isn't available on Chimu servers.
            if (parentSetID == null)
                return true

            val url = BeatmapListing.mirror.downloadEndpoint(parentSetID!!)

            async {
                try {
                    BeatmapDownloader.download(url, "$parentSetID $artist - $title")
                } catch (e: Exception) {
                    ToastLogger.showText("Unable to download beatmap: ${e.message}", true)
                    e.printStackTrace()
                }
            }
        }
        return true
    }


    fun updateBeatmap(beatmap: RoomBeatmap?)
    {
        stars.forEach { it.isVisible = false }

        if (beatmap == null)
        {
            trackTitle.text = if (Multiplayer.isRoomHost) "Tap to select a beatmap." else "Room is changing beatmap..."
            creatorInfo.text = ""
            return
        }

        trackTitle.text = "${beatmap.artist} - ${beatmap.title}"
        creatorInfo.text = "Mapped by ${beatmap.creator} // ${beatmap.version}"

        if (getGlobal().selectedBeatmap == null)
        {
            creatorInfo.text += "\n${

                if (beatmap.parentSetID == null)
                    "Beatmap not found on Chimu."
                else
                    "Tap to download."
            }"
            return
        }

        val difficulty = getGlobal().selectedBeatmap!!.getStarRating()

        stars.forEachIndexed { i, it ->
            it.isVisible = difficulty >= i
            it.setScale(0.5f * (difficulty - i).coerceIn(0f, 1f))
        }
    }


    companion object
    {
        private val DEFAULT_COLOR = RGBColor(25 / 255f, 25 / 255f, 240 / 255f)
        val DEFAULT_TEXT_COLOR = RGBColor(1f, 1f, 1f)
    }
}
