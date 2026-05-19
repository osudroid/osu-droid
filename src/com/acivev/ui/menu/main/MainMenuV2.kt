package com.acivev.ui.menu.main

import android.util.Log
import android.content.Intent
import android.net.Uri
import com.edlplan.framework.easing.Easing
import com.osudroid.beatmaplisting.BeatmapListing
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.beatmaps.timings.EffectControlPoint
import com.osudroid.beatmaps.timings.TimingControlPoint
import com.osudroid.resources.R
import com.osudroid.ui.BannerManager
import com.osudroid.ui.v1.SettingsFragment
import com.osudroid.ui.v2.multi.LobbyScene
import com.reco1l.andengine.ui.UIConfirmDialog
import com.reco1l.osu.ui.HorizontalMessageDialog
import ru.nsu.ccfit.zuev.osuplus.BuildConfig
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.utils.async
import com.osudroid.utils.mainThread
import com.osudroid.utils.updateThread
import com.osudroid.data.BeatmapInfo
import com.reco1l.andengine.*
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.UIBox
import com.reco1l.andengine.shape.UICircle
import com.reco1l.andengine.sprite.ScaleType.Crop
import com.reco1l.andengine.sprite.UISprite
import com.reco1l.andengine.text.UIText
import com.reco1l.framework.Color4
import org.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.MainScene
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring
import ru.nsu.ccfit.zuev.osu.scoring.Replay
import java.io.IOException
import java.util.LinkedList
import android.os.Handler
import android.os.Looper

class MainMenuV2 : UIScene() {

    private val width get() = Config.getRES_WIDTH().toFloat()
    private val height get() = Config.getRES_HEIGHT().toFloat()

    // Overlays
    private var background: UISprite
    private val dimBox: UIBox
    private val sideFlashes: SideFlashes
    private val fadeInOverlay: UIBox

    // Logo
    private val logo: Logo

    // Menu buttons
    private val buttonClipContainer: UIContainer
    private val btnFirst: UISprite
    private val btnSecond: UISprite
    private val btnThird: UISprite
    private val buttons get() = listOf(btnFirst, btnSecond, btnThird)

    // Textures
    private val playTex by lazy { ResourceManager.getInstance().getTexture("play") }
    private val soloTex by lazy { ResourceManager.getInstance().getTexture("solo") }
    private val optionsTex by lazy { ResourceManager.getInstance().getTexture("options") }
    private val multiTex by lazy { ResourceManager.getInstance().getTexture("multi") }
    private val exitTex by lazy { ResourceManager.getInstance().getTexture("exit") }
    private val backTex by lazy { ResourceManager.getInstance().getTexture("back") }
    private val playSound
            by lazy { ResourceManager.getInstance().loadSound("menuhit", "sfx/menuhit.ogg", false) }

    // Timing
    private var timingControlPoints = LinkedList<TimingControlPoint>()
    private var effectControlPoints = LinkedList<EffectControlPoint>()
    private var currentTimingPoint: TimingControlPoint? = null
    private var currentEffectPoint: EffectControlPoint? = null
    private var bpmLength = 1000f
    private var kiaiActive = false
    private var musicStarted = false

    // Flash
    /**
     * Wall-clock beat accumulator, mirrors old MainScene's beatPassTime.
     * Advances every frame and fires an idle beat when no music is playing,
     * so the logo keeps pulsing at the current BPM (default 60 BPM / 1000 ms).
     */
    private var beatPassTime = 0f

    /**
     * state only needs the previous beat index now; animation handled by entity modifiers.
     */
    private var prevBeat = -1

    private val starFountain: StarFountain

    // State
    private var menuOpen = false
    private var isSecondMenu = false

    /**
     * tracks when beatmap changes for bg crossfade
     */
    private var lastBgAudioPath: String? = null

    /**
     * seconds the menu has been open without interaction
     */
    private var menuOpenTime = 0f

    var isOnExitAnim = false

    // Guards initOnlinePanel() so it only runs once per process lifetime,
    // preventing duplicate panels on scene re-attach (onLoadComplete fires each time).
    private var onlinePanelInitialised = false

    /**
     * set to true after a skin reload so the background texture is refreshed
     * on the next frame instead of doing two HashMap lookups every frame unconditionally.
     */
    var bgNeedsRefresh = false

    /**
     * Current beatmap (owned by MainMenuV2, replaces MainScene.beatmapInfo)
     */
    var beatmapInfo: BeatmapInfo? = null

    // Music player panel
    private val musicPlayerPanel = MusicPlayerPanel()

    init {
        val w = width
        val h = height

        // Background
        background = UISprite().also {
            it.width = FillParent
            it.height = FillParent
            it.scaleType = Crop
            it.textureRegion = ResourceManager.getInstance().getTexture("menu-background")
                ?: ResourceManager.getInstance().getTexture("::background")

            attachChild(it)
        }

        // Dark dim
        dimBox = UIBox().also {
            it.width = FillParent
            it.height = FillParent
            it.color = Color4.Black
            it.alpha = 0.45f

            attachChild(it)
        }

        // Kiai side flashes
        sideFlashes = SideFlashes(w).also { it.attachTo(this) }

        // Clip container: its left edge = logo's right edge, so anything to the left
        // of the logo is scissored clipped and never rendered no animation tricks needed.
        // Attached BEFORE the logo so logo remains on top for touch priority.
        buttonClipContainer = UIContainer().also { c ->
            c.clipToBounds = true
            c.x = w / 2f  // will be repositioned in openMenu()
            c.y = 0f
            c.width = FillParent
            c.height = FillParent

            attachChild(c)
        }

        btnFirst = object : UISprite() {
            init {
                textureRegion = playTex
                alpha = 0f
            }
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (alpha < 0.05f) return false
                if (event.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }
                if (event.isActionUp) {
                    setColor(1f, 1f, 1f)
                    playSound?.play()
                    onFirstButton()
                    return true
                }
                return false
            }
        }.also { buttonClipContainer.attachChild(it) }
        btnSecond = object : UISprite() {
            init {
                textureRegion = optionsTex
                alpha = 0f
            }
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (alpha < 0.05f) return false
                if (event.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }
                if (event.isActionUp) {
                    setColor(1f, 1f, 1f)
                    playSound?.play()
                    onSecondButton()
                    return true
                }

                return false
            }
        }.also { buttonClipContainer.attachChild(it) }
        btnThird = object : UISprite() {
            init {
                textureRegion = exitTex
                alpha = 0f
            }
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (alpha < 0.05f) return false
                if (event.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }
                if (event.isActionUp) {
                    setColor(1f, 1f, 1f)
                    playSound?.play()
                    onThirdButton()
                    return true
                }

                return false
            }
        }.also { buttonClipContainer.attachChild(it) }

        // Logo
        logo = Logo(onTap = ::onLogoTap, onKiaiBurst = ::onKiaiBurst).also {
            it.anchor = Anchor.TopLeft
            it.origin = Anchor.Center
            it.x = w / 2f
            it.y = h / 2f

            attachChild(it)
        }

        val versionText = UIText().apply {
            font = ResourceManager.getInstance().getFont("smallFont")
            text = "osu!droid ${BuildConfig.VERSION_NAME}"
            color = Color4.White
            inheritAncestorsColor = false
            x = 20f
            y = h - 30f
        }

        val versionBg = object : UIBox() {
            private var sized = false

            override fun onManagedUpdate(deltaTimeSec: Float) {
                super.onManagedUpdate(deltaTimeSec)
                if (sized) return

                val tw = versionText.width
                val th = versionText.height

                if (tw > 0f && th > 0f) {
                    sized = true
                    width = tw + 25f
                    height = th + 12f
                    x = 10f
                    y = h - height - 10f
                    // keep text centred (with padding) inside the box
                    versionText.x = x + 10f
                    versionText.y = y + (height - th) / 2f
                }
            }
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (event.isActionUp) {
                    mainThread {
                        HorizontalMessageDialog()
                            .setTitle("About")
                            .setMessage(
                                "<h1>osu!droid</h1>\n" +
                                "<h5>Version ${BuildConfig.VERSION_NAME}</h5>\n" +
                                "<p>Made by osu!droid team<br>osu! is © peppy 2007-2026</p>\n" +
                                "<br>\n" +
                                "<a href=\"https://osu.ppy.sh\">Visit official osu! website ↗</a>\n" +
                                "<br><br>\n" +
                                "<a href=\"https://osudroid.moe\">Visit official osu!droid website ↗</a>\n" +
                                "<br><br>\n" +
                                "<a href=\"https://discord.gg/nyD92cE\">Join the official Discord server ↗</a>\n",
                                true
                            )
                            .addButton("Changelog") { dialog ->
                                dialog.dismiss()
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://osudroid.moe/changelog/latest"))
                                    GlobalManager.getInstance().mainActivity.startActivity(intent)
                                } catch (e: Exception) {
                                    Log.e("MainMenuV2", "Failed to open changelog", e)
                                }
                            }
                            .addButton("Close") { it.dismiss() }
                            .show()
                    }
                }
                return true
            }
        }.apply {
            color = Color4(0f, 0f, 0f, 0.5f)
            cornerRadius = 12f
        }

        attachChild(versionBg)
        attachChild(versionText)

        // Beatmap downloader
        object : UISprite() {
            init {
                textureRegion = ResourceManager.getInstance().getTexture("beatmap_downloader")
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
            }
            override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
                if (event.isActionDown) {
                    setColor(0.7f, 0.7f, 0.7f)
                    return true
                }

                if (event.isActionUp) {
                    setColor(1f, 1f, 1f)
                    mainThread { BeatmapListing().show() }
                    return true
                }

                return false
            }
        }.also { attachChild(it) }

        // Music player panel
        attachChild(musicPlayerPanel)
        musicPlayerPanel.onTrackChanged = {
            musicStarted = false
            reloadTimingPoints()
        }
        musicPlayerPanel.onSeek = {
            prevBeat = -1
            beatPassTime = 0f
            reloadTimingPoints()
            // Flush any visual state that stacked up while the seekbar was held:
            // reset the dim box, silence the side flashes, and stop the fountain.
            dimBox.clearEntityModifiers()
            dimBox.alpha = 0.45f
            sideFlashes.fadeOut(0f) // instant no leftover flash
            starFountain.stop()
            kiaiActive = false
        }

        // Star fountains
        starFountain = StarFountain(w, h).also { it.attachTo(this) }

        // Fade-in black overlay
        fadeInOverlay = UIBox().also {
            it.width = FillParent
            it.height = FillParent
            it.color = Color4.Black
            it.alpha = 1f
            attachChild(it)
        }

        // Debug build banner
        if (BuildConfig.DEBUG) {
            ResourceManager.getInstance().loadHighQualityAsset("dev-build-overlay", "dev-build-overlay.png")

            val overlayTex = ResourceManager.getInstance().getTexture("dev-build-overlay")
            val overlayH = overlayTex?.height ?: 0f
            val cx = w / 2f

            attachChild(UISprite().also {
                it.textureRegion = overlayTex
                it.origin = Anchor.BottomCenter
                it.x = cx
                it.y = h
            })
            attachChild(UIText().also {
                it.font = ResourceManager.getInstance().getFont("smallFont")
                it.text = "DEVELOPMENT BUILD"
                it.color = Color4(0f, 0f, 0f, 0.5f)
                it.origin = Anchor.BottomCenter
                it.x = cx + 2f
                it.y = h - overlayH - 1f + 2f
            })
            attachChild(UIText().also {
                it.font = ResourceManager.getInstance().getFont("smallFont")
                it.text = "DEVELOPMENT BUILD"
                it.color = Color4(1f, 237f / 255f, 0f, 1f)
                it.origin = Anchor.BottomCenter
                it.x = cx
                it.y = h - overlayH - 1f
            })
        }
    }

    override fun onLoadComplete() {
        // Reset overlay to opaque so it covers the background reload on every scene entry.
        fadeInOverlay.clearEntityModifiers()
        fadeInOverlay.alpha = 1f
        fadeInOverlay.fadeTo(0f, 0.5f, Easing.OutCubic)

        // Force background to always reload so stale/detached sprites are replaced.
        lastBgAudioPath = null

        reloadTimingPoints()
        val songService = GlobalManager.getInstance().songService

        if (songService != null && songService.status == Status.STOPPED)
            musicStarted = false

        if (!onlinePanelInitialised) {
            onlinePanelInitialised = true
            initOnlinePanel()
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        super.onManagedUpdate(deltaTimeSec)

        // During the exit animation let entity modifiers (position/alpha) keep running
        // via super, but skip all game/music logic to prevent music from restarting,
        // kiai from cancelling animations, background crossfades, etc.
        if (isOnExitAnim) return

        val songService = GlobalManager.getInstance().songService ?: return
        if (!musicStarted && songService.status == Status.STOPPED) {
            songService.play()
            songService.volume = Config.getBgmVolume()
            musicStarted = true
        }

        // Kiai cannot be active when there is no playback, clear it so CatJam
        // and other kiai visuals don't persist beyond the end of a track.
        if (songService.status == Status.STOPPED && kiaiActive) {
            kiaiActive = false
            onKiaiEnd()
        }

        val isSeeking = musicPlayerPanel.isDraggingSeek
        val songPos = songService.position

        if (!isSeeking) {
            while (timingControlPoints.isNotEmpty() &&
                songPos.toDouble() > (timingControlPoints.peek()?.time ?: Double.MAX_VALUE))
                currentTimingPoint = timingControlPoints.poll()

            currentTimingPoint?.let { bpmLength = it.msPerBeat.toFloat() }

            while (effectControlPoints.isNotEmpty() &&
                songPos.toDouble() > (effectControlPoints.peek()?.time ?: Double.MAX_VALUE))

                currentEffectPoint = effectControlPoints.poll()

            val nowKiai = currentEffectPoint?.isKiai == true
            if (!kiaiActive && nowKiai) onKiaiStart()
            else if (kiaiActive && !nowKiai) onKiaiEnd()

            kiaiActive = nowKiai
            starFountain.update(songPos)

            if (musicStarted && songService.status != Status.PLAYING && songPos >= songService.getLength()) {
                musicStarted = false   // allow the new preloaded track to start
                musicControl(MusicOption.NEXT)
            }

            if (bpmLength > 0f) {
                if (songService.status == Status.PLAYING) {
                    val timingPointStart = (currentTimingPoint?.time ?: 0.0).toFloat()
                    val timingPointSongPos = (songPos - timingPointStart).coerceAtLeast(0f)
                    beatPassTime = timingPointSongPos % bpmLength
                    val beat = (timingPointSongPos / bpmLength).toInt()
                    if (beat != prevBeat) {
                        prevBeat = beat
                        sideFlashes.onNewBeat(
                            beat = beat,
                            kiaiActive = kiaiActive,
                            bpmMs = bpmLength,
                            fft = songService.spectrum,
                            timeSignature = currentTimingPoint?.timeSignature ?: 4
                        )
                        logo.onBeat()
                    }
                } else {
                    // No music / paused, advance wall-clock accumulator at the current BPM
                    // (defaults to 1000 ms = 60 BPM), exactly as old MainScene did.
                    beatPassTime += deltaTimeSec * 1000f
                    if (beatPassTime >= bpmLength) {
                        beatPassTime %= bpmLength
                        // pulseBeat() sets an explicit scale kick so the logo visually
                        // bounces even though FFT energy is zero (paused/stopped).
                        logo.pulseBeat()
                        // Side flashes are intentionally not fired when idle
                        // (no music = no amplitude data to drive them).
                    }
                }
            }
        }

        logo.kiaiActive = kiaiActive
        logo.update(
            dt = deltaTimeSec,
            songPos = songPos,
            bpmMs = bpmLength,
            timingOffset = currentTimingPoint?.time?.toInt() ?: 0,
            fft = songService.spectrum,
            isPlaying = songService.status == Status.PLAYING
        )

        // Refresh background texture only when flagged (e.g. after a skin reload).
        // Avoids two HashMap lookups every frame for a value that almost never changes.
        if (bgNeedsRefresh) {
            bgNeedsRefresh = false

            val resourceManager = ResourceManager.getInstance()
            val texture = resourceManager.getTexture("::background")
                ?: resourceManager.getTexture("menu-background")

            if (texture != null)
                background.textureRegion = texture
        }

        if (menuOpen) {
            menuOpenTime += deltaTimeSec
            if (menuOpenTime >= 5f)
                closeMenu()
        }

        musicPlayerPanel.update()
    }


    // Kiai
    private fun onKiaiStart() {
        dimBox.clearEntityModifiers()
        dimBox.fadeTo(0.25f, 0.15f, Easing.Out)
        dimBox.fadeTo(0.45f, 0.8f, Easing.OutSine)

        // Fire star fountains with a random direction (-1 / 0 / 1)
        val songPos = GlobalManager.getInstance().songService?.position ?: 0
        starFountain.shoot(songPos)
    }
    private fun onKiaiEnd() {
        dimBox.clearEntityModifiers()
        dimBox.fadeTo(0.45f, 0.3f, Easing.OutSine)
    }
    private fun onKiaiBurst(cx: Float, cy: Float, radius: Float) {
        attachChild(ExpandingRing(cx, cy, radius))
    }

    // Background crossfade
    private fun crossfadeBackground(info: BeatmapInfo) {
        async {
            val newTex = (if (info.backgroundFilename != null && !Config.isSafeBeatmapBg()) {
                try {
                    ResourceManager.getInstance().loadBackground(info.backgroundPath)
                }
                catch (_: Exception) { null }
            } else null)
                ?: ResourceManager.getInstance().getTexture("menu-background")
                ?: ResourceManager.getInstance().getTexture("::background")
                ?: return@async

            updateThread {
                val oldBg = background
                val newBg = UISprite().apply {
                    width = FillParent
                    height = FillParent
                    scaleType = Crop
                    textureRegion = newTex
                    alpha = 0f
                }
                background = newBg
                attachChild(newBg, 0)
                newBg.fadeTo(1f, 1.5f, Easing.OutCubic)
                oldBg.fadeTo(0f, 1.5f, Easing.OutCubic).after {
                    updateThread { oldBg.detachSelf() }
                }
            }
        }
    }

    // Logo tap
    private fun onLogoTap() {
        if (isOnExitAnim) return  // ignore taps during the exit animation

        playSound?.play()

        if (menuOpen) closeMenu()
        else openMenu()
    }

    private fun openMenu() {
        menuOpen = true
        menuOpenTime = 0f

        val nativeH = btnSecond.height.takeIf { it > 0f } ?: 80f
        val scale = (Logo.LOGO_SIZE * 0.9f / (3f * nativeH + 80f)).coerceIn(0.2f, 2f)
        btnFirst.setScale(scale); btnSecond.setScale(scale); btnThird.setScale(scale)

        // Center the whole composition (logo disc + buttons)
        val overlapPx = Logo.LOGO_SIZE * MENU_OVERLAP
        val btnW = btnFirst.width.takeIf { it > 0f }?.times(scale) ?: (Logo.LOGO_SIZE * 2.5f)
        val totalW = Logo.LOGO_SIZE + btnW - overlapPx
        val compLeft = width / 2f - totalW / 2f
        val logoX = compLeft + Logo.LOGO_SIZE / 2f
        val menuX = compLeft + Logo.LOGO_SIZE - overlapPx

        // Clip container starts at menuX.
        // Buttons themselves are at x=0 relative to container.
        // The logo (rendered on top) covers the overlap region,
        // preserving the layered look while guaranteeing nothing renders left of menuX.
        buttonClipContainer.x = menuX

        // Use RENDERED heights (native × scale) for gap/spacing.
        val secondH = btnSecond.height * scale
        val firstH = btnFirst.height  * scale
        val gap = 40f * scale

        val secondY = (height - secondH) / 2f
        val firstY = secondY - firstH - gap
        val thirdY = secondY + secondH + gap

        // Buttons at x=0 inside the container.
        // Slide in from slightly off-left, then settle at 0.
        btnFirst .setPosition(-100f, firstY)
        btnSecond.setPosition(-100f, secondY)
        btnThird .setPosition(-100f, thirdY)

        for (btn in buttons) {
            btn.clearEntityModifiers(); btn.alpha = 0f
            btn.moveToX(0f, 0.5f, Easing.OutElastic)
            btn.fadeTo(0.9f, 0.5f, Easing.OutCubic)
        }

        // Slide logo to its computed centred position.
        logo.clearEntityModifiers()
        logo.moveToX(logoX, 0.3f, Easing.OutExpo)
    }

    private fun closeMenu() {
        menuOpen = false
        menuToFirst()
        isSecondMenu = false

        // Fade buttons before logo moves far enough to uncover the clip boundary.
        for (btn in buttons) {
            btn.clearEntityModifiers()
            btn.fadeTo(0f, 0.15f, Easing.OutQuad)
        }
        logo.clearEntityModifiers()

        logo.moveTo(width / 2f, height / 2f, 0.3f, Easing.OutCubic)
    }

    // Button actions
    private fun onFirstButton() {
        if (isSecondMenu) {
            closeMenu()
            GlobalManager.getInstance().songService?.isGaming = true
            async {
                LoadingScreen().show()
                GlobalManager.getInstance().mainActivity.checkNewSkins()
                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()
                if (LibraryManager.getLibrary().isEmpty()) {
                    updateThread {
                        GlobalManager.getInstance().songService?.isGaming = false
                        GlobalManager.getInstance().engine.scene = this@MainMenuV2
                        BeatmapListing().show()
                    }
                } else {
                    updateThread {
                        musicControl(MusicOption.PLAY)
                        GlobalManager.getInstance().songMenu.reload()
                        GlobalManager.getInstance().songMenu.show()
                        GlobalManager.getInstance().songMenu.select()
                    }
                }
            }
        } else {
            menuToSecond()
        }
    }

    private fun onSecondButton() {
        if (isSecondMenu) {
            closeMenu()
            if (!OnlineManager.getInstance().isStayOnline) {
                ToastLogger.showText(StringTable.format(R.string.multiplayer_not_online), true)
                return
            }
            GlobalManager.getInstance().songService?.isGaming = true
            Multiplayer.isMultiplayer = true
            async {
                LoadingScreen().show()
                GlobalManager.getInstance().mainActivity.checkNewSkins()
                GlobalManager.getInstance().mainActivity.loadBeatmapLibrary()
                GlobalManager.getInstance().songMenu.reload()
                updateThread { GlobalManager.getInstance().engine.scene = LobbyScene() }
            }
        } else {
            closeMenu()
            GlobalManager.getInstance().songService?.isGaming = true
            mainThread { SettingsFragment().show() }
        }
    }

    private fun onThirdButton() {
        if (isSecondMenu) menuToFirst()
        else showExitDialog()
    }

    private fun menuToFirst() {
        if (!isSecondMenu) return

        isSecondMenu = false

        btnFirst.textureRegion = playTex
        btnSecond.textureRegion = optionsTex
        btnThird.textureRegion = exitTex
    }
    private fun menuToSecond() {
        if (isSecondMenu) return

        isSecondMenu = true
        menuOpenTime = 0f

        btnFirst.textureRegion = soloTex
        btnSecond.textureRegion = multiTex
        btnThird.textureRegion = backTex
    }

    // Timing
    fun reloadTimingPoints() {
        val info = beatmapInfo ?: return
        try {
            val beatmap = BeatmapCache.getBeatmap(info, false)
            val allTiming = LinkedList(beatmap.controlPoints.timing.controlPoints)
            val allEffect = LinkedList(beatmap.controlPoints.effect.controlPoints)
            val pos = GlobalManager.getInstance().songService?.position ?: 0

            var tp: TimingControlPoint? = null
            while (allTiming.isNotEmpty() && pos.toDouble() > (allTiming.peek()?.time ?: Double.MAX_VALUE))
                tp = allTiming.poll()

            var ep: EffectControlPoint? = null
            while (allEffect.isNotEmpty() && pos.toDouble() > (allEffect.peek()?.time ?: Double.MAX_VALUE))
                ep = allEffect.poll()

            if (tp == null) tp = beatmap.controlPoints.timing.defaultControlPoint
            if (ep == null) ep = beatmap.controlPoints.effect.defaultControlPoint

            timingControlPoints = allTiming; effectControlPoints = allEffect
            currentTimingPoint = tp
            currentEffectPoint  = ep
            bpmLength  = tp?.msPerBeat?.toFloat() ?: 1000f

            val nowKiai = ep?.isKiai == true
            if (!kiaiActive && nowKiai) onKiaiStart()
            else if (kiaiActive && !nowKiai) onKiaiEnd()
            kiaiActive = nowKiai

            // Update background for the new beatmap
            if (info.audioPath != lastBgAudioPath) {
                lastBgAudioPath = info.audioPath
                crossfadeBackground(info)
            }
        } catch (e: IOException) {
            Log.w("MainMenuV2", "Failed to load timing points", e)
        } catch (e: IllegalArgumentException) {
            Log.w("MainMenuV2", "Failed to parse timing points for beatmap", e)
        }
    }

    /** Load a random beatmap from the library and start playing it. */
    fun loadBeatmap() {
        LibraryManager.shuffleLibrary()
        if (LibraryManager.getSizeOfBeatmaps() == 0) return

        beatmapInfo = LibraryManager.getCurrentBeatmapSet()[0]
        GlobalManager.getInstance().selectedBeatmap = beatmapInfo
        reloadTimingPoints()

        val svc = GlobalManager.getInstance().songService ?: return
        svc.preLoad(beatmapInfo!!.audioPath)

        musicStarted = false
    }

    /** Switch the active beatmap (e.g. after track change or replay launch). */
    fun setBeatmap(info: BeatmapInfo) {
        LibraryManager.findBeatmapSetIndex(info)
        beatmapInfo = info
        GlobalManager.getInstance().selectedBeatmap = info
        reloadTimingPoints()
        musicControl(MusicOption.SYNC)
    }

    /** Handle PREV / PLAY / PAUSE / STOP / NEXT / SYNC music actions. */
    fun musicControl(option: MusicOption) {
        val songService = GlobalManager.getInstance().songService ?: return
        if (beatmapInfo == null) return

        when (option) {
            MusicOption.PREV -> {
                if (songService.status == Status.PLAYING || songService.status == Status.PAUSED)
                    songService.stop()

                LibraryManager.selectPreviousBeatmapSet()
                beatmapInfo = LibraryManager.getCurrentBeatmapSet()[0]
                GlobalManager.getInstance().selectedBeatmap = beatmapInfo
                reloadTimingPoints()
                songService.preLoad(beatmapInfo!!.audioPath)

                musicStarted = false
            }
            MusicOption.NEXT -> {
                if (songService.status == Status.PLAYING || songService.status == Status.PAUSED)
                    songService.stop()

                LibraryManager.selectNextBeatmapSet()
                beatmapInfo = LibraryManager.getCurrentBeatmapSet()[0]
                GlobalManager.getInstance().selectedBeatmap = beatmapInfo
                reloadTimingPoints()
                songService.preLoad(beatmapInfo!!.audioPath)

                musicStarted = false
            }
            MusicOption.PLAY -> {
                if (songService.status == Status.PAUSED || songService.status == Status.STOPPED) {
                    if (songService.status == Status.STOPPED) {
                        songService.preLoad(beatmapInfo!!.audioPath)
                    }
                    songService.play()
                }
            }
            MusicOption.PAUSE -> {
                if (songService.status == Status.PLAYING) songService.pause()
            }
            MusicOption.STOP -> {
                if (songService.status == Status.PLAYING || songService.status == Status.PAUSED)
                    songService.stop()
            }
            MusicOption.SYNC -> { /* timing already handled by reloadTimingPoints */ }
        }
    }

    /** Attach the online banner sprite to this scene. */
    fun loadBannerSprite() {
        if (!Config.isStayOnline()) return

        val sprite = BannerManager.loadBannerSprite() ?: return
        sprite.setPosition(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
        sprite.origin = Anchor.BottomRight

        attachChild(sprite)
    }

    /**
     * One-time online setup: loads the online config, initialises OnlineManager,
     * creates the panel and kicks off login. Called once from MainActivity after
     * the scene is created (mirrors old MainScene.createOnlinePanel).
     */
    fun initOnlinePanel() {
        Config.loadOnlineConfig(GlobalManager.getInstance().mainActivity)
        OnlineManager.getInstance().init()

        if (OnlineManager.getInstance().isStayOnline) {
            OnlineScoring.getInstance().createPanel()

            val panel = OnlineScoring.getInstance().panel
            panel.setPosition(5f, 5f)
            updateThread {
                attachChild(panel)
                registerTouchArea(panel.rect)
            }
        }

        OnlineScoring.getInstance().login()
    }

    /** Detach and recreate the online panel (called after settings change). */
    fun reloadOnlinePanel() {
        updateThread {
            val currentPanel = OnlineScoring.getInstance().panel
            if (currentPanel != null) {
                unregisterTouchArea(currentPanel.rect)
                detachChild(currentPanel)
            }
            Config.loadOnlineConfig(GlobalManager.getInstance().mainActivity)
            OnlineManager.getInstance().init()

            if (OnlineManager.getInstance().isStayOnline) {
                OnlineScoring.getInstance().createPanel()

                val panel = OnlineScoring.getInstance().panel
                panel.setPosition(5f, 5f)

                attachChild(panel)
                registerTouchArea(panel.rect)
            }

            OnlineScoring.getInstance().login()
        }
    }

    /** Show the exit confirmation dialog. */
    fun showExitDialog() {
        if (isOnExitAnim) return

        val dialog = UIConfirmDialog()
        dialog.title = "Exit"
        dialog.text = GlobalManager.getInstance().mainActivity.getString(R.string.dialog_exit_message)
        dialog.onConfirm = { exit() }
        dialog.show()
    }

    /** Play exit animation and finish the activity. */
    fun exit() {
        if (isOnExitAnim) return

        isOnExitAnim = true

        menuOpen = false
        isSecondMenu = false

        // Buttons fade out
        for (btn in buttons) {
            btn.clearEntityModifiers()
            btn.fadeTo(0f, 0.3f, Easing.OutQuad)
        }

        // Logo glides back to centre
        logo.clearEntityModifiers()
        logo.moveTo(width / 2f, height / 2f, 0.8f, Easing.OutExpo)

        // Music player panel fades out
        musicPlayerPanel.clearEntityModifiers()
        musicPlayerPanel.fadeTo(0f, 0.5f, Easing.OutQuint)

        // Dim overlay fades out background brightens
        dimBox.clearEntityModifiers()
        dimBox.fadeTo(0f, 0.5f, Easing.OutSine)

        // Side flashes and star fountain disappear
        sideFlashes.fadeOut()
        starFountain.stop()

        // Re-raise the black overlay to the top of the draw stack so it covers
        // any elements that were dynamically attached after it (e.g. online panel,
        // banner), then fade it in over 3 seconds
        updateThread {
            fadeInOverlay.detachSelf()
            attachChild(fadeInOverlay)
            fadeInOverlay.clearEntityModifiers()
            fadeInOverlay.alpha = 0f
            fadeInOverlay.fadeTo(1f, 3.0f, Easing.None)
        }

        // Audio
        val exitSound = ResourceManager.getInstance().getSound("seeya")
        exitSound?.play()

        GlobalManager.getInstance().songService?.stop()

        // Finish after the 3-second fade completes.
        // killOnDestroy = true ensures onDestroy() calls Process.killProcess(), fully
        // clearing all static singletons (ResourceManager, GlobalManager, UIEngine, etc.).
        // Without this, the process stays alive and on the next launch from the task switcher
        // the new Engine's FontManager is unaware of fonts still held in ResourceManager
        // (registered against the old Engine), causing black fonts and black icon squares.
        Handler(Looper.getMainLooper()).postDelayed({
            val activity = GlobalManager.getInstance().mainActivity
            activity.killOnDestroy = true
            activity.finish()
        }, 3000)
    }

    /** Load a replay file and jump straight to the scoring scene. */
    fun watchReplay(replayFile: String) {
        val replay = Replay()
        if (!replay.load(replayFile, false) || replay.replayVersion < 3) return

        val beatmap = LibraryManager.findBeatmapByMD5(replay.md5) ?: return
        setBeatmap(beatmap)

        val stat = replay.getStat()
        stat.migrateLegacyMods(beatmap.getBeatmapDifficulty())

        GlobalManager.getInstance().songMenu.select()
        ResourceManager.getInstance().loadBackground(beatmap.backgroundPath)

        val songService = GlobalManager.getInstance().songService!!
        songService.preLoad(beatmap.audioPath)
        songService.play()

        val scoring = GlobalManager.getInstance().scoring
        scoring.load(stat, null, songService, replayFile, null, beatmap)

        GlobalManager.getInstance().engine.setScene(scoring.scene)
    }

    /** Return to this scene (called from SongMenu / LobbyScene on back). */
    override fun show() {
        GlobalManager.getInstance().songService?.isGaming = false
        GlobalManager.getInstance().engine.scene = this

        val selected = GlobalManager.getInstance().selectedBeatmap
        if (selected != null)
            setBeatmap(selected)
    }

    companion object {
        /** Fraction of LOGO_SIZE that buttons overlap behind the logo edge. Tune this value. */
        private const val MENU_OVERLAP = 0.58f
    }

    // Expanding kiai ring
    private class ExpandingRing(cx: Float, cy: Float, r: Float) : UICircle() {
        private var progress = 0f

        init {
            width = r * 2f; height = r * 2f
            origin = Anchor.Center
            x = cx
            y = cy
            alpha = 0.5f
            color = Color4.White
        }
        override fun onManagedUpdate(deltaTimeSec: Float) {
            super.onManagedUpdate(deltaTimeSec)
            progress += deltaTimeSec / 0.8f

            if (progress >= 1f) {
                detachSelf()
                return
            }
            val s = 1f + 1.5f * progress
            scaleX = s; scaleY = s; alpha = (1f - progress) * 0.5f
        }
    }
}
