package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.mainmenu.MainScene
import com.osudroid.ui.v2.modmenu.*
import com.osudroid.utils.*
import com.reco1l.andengine.*
import com.reco1l.andengine.component.scaleCenter
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import com.rian.osu.beatmap.parser.*
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.rian.osu.mods.*
import com.rian.osu.utils.*
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import java.io.*
import kotlin.coroutines.cancellation.CancellationException


object SongSelect : UIScene() {

    /**
     * The currently selected beatmap.
     */
    var selectedBeatmap: BeatmapInfo? = null
        private set


    private val beatmapLoadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val filterBar = SongSelectFilterBar()
    private val leaderboard = SongSelectLeaderboard()
    private val beatmapCarrousel = BeatmapCarrousel()
    private val beatmapInfoWedge = BeatmapInfoWedge()
    private val backgroundSprite: UISprite

    private lateinit var modsButton: UITextButton

    private var currentSongPath: String? = null
    private var currentBackgroundPath: String? = null
    private var currentBeatmapLoadingJob: Job? = null
    private var currentCalculationJob: Job? = null


    init {
        ResourceManager.getInstance().loadHighQualityAsset("mods", "mods.png")
        ResourceManager.getInstance().loadHighQualityAsset("random", "random.png")

        backgroundSprite = sprite {
            width = Size.Full
            height = Size.Full
            textureRegion = ResourceManager.getInstance().getTexture("menu-background")
            scaleType = ScaleType.Crop
            color *= 0.4f
        }

        fillContainer {
            width = Size.Full
            height = Size.Full

            // Left side
            linearContainer {
                width = Size.Full
                height = Size.Full
                orientation = Orientation.Vertical
                style = {
                    spacing = 2f.srem
                }

                +beatmapInfoWedge
                +leaderboard
            }

            // Right side
            container {
                width = Size.Full
                height = Size.Full

                +beatmapCarrousel.apply {
                    setScale(0.8f)
                    scaleCenter = Anchor.Center
                    borderWidth = 4f
                    borderColor = Colors.White
                }
                +filterBar
            }
        }

        linearContainer {
            orientation = Orientation.Horizontal
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            style = {
                padding = Vec4(UIEngine.current.safeArea.x + 2f.srem, 4f.srem)
                spacing = 2f.srem
            }

            textButton {
                text = "Back"
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                leadingIcon = FontAwesomeIcon(Icon.ArrowLeft)

                onActionUp = {
                    UIEngine.current.scene = MainScene
                }
            }

            modsButton = textButton {
                text = "Mods"
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("mods"))
                onActionUp = { ModMenu.show() }
            }

            textButton {
                text = "Random"
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("random"))
                onActionUp = {
                    beatmapCarrousel.applyComponent(beatmapCarrousel.data.random()) { selectRandom() }
                }
            }
        }
    }


    @JvmOverloads
    fun loadBeatmaps(query: String? = null, sort: String = "title") {
        beatmapCarrousel.data = DatabaseManager.beatmapInfoTable.getBeatmapSetList().map { BeatmapSetModel(it) }
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        selectedBeatmap = beatmapInfo

        currentCalculationJob?.cancel(CancellationException("Calculation cancelled due to new selection."))
        currentBeatmapLoadingJob?.cancel(CancellationException("Beatmap loading cancelled due to new selection."))

        currentBeatmapLoadingJob = beatmapLoadingScope.launch {

            ensureActive()
            beatmapInfoWedge.onBeatmapSelected(beatmapInfo)

            ensureActive()
            leaderboard.onBeatmapSelected(beatmapInfo)

            GlobalManager.getInstance().selectedBeatmap = beatmapInfo

            ensureActive()
            if (beatmapInfo == null) {
                currentSongPath = null
                currentBackgroundPath = null

                ensureActive()
                backgroundSprite.textureRegion = ResourceManager.getInstance().getTexture("menu-background")

                ensureActive()
                GlobalManager.getInstance().songService.stop()

                return@launch
            }

            if (!Config.isSafeBeatmapBg() && currentBackgroundPath != beatmapInfo.backgroundPath) {
                currentBackgroundPath = beatmapInfo.backgroundPath

                val backgroundFile = File(beatmapInfo.backgroundPath)

                ensureActive()
                val textureRegion = ResourceManager.getInstance().loadHighQualityFile("::background", backgroundFile)

                ensureActive()
                backgroundSprite.textureRegion = textureRegion ?: ResourceManager.getInstance().getTexture("menu-background")
            }

            if (currentSongPath != beatmapInfo.audioPath) {
                currentSongPath = beatmapInfo.audioPath

                ensureActive()
                GlobalManager.getInstance().songService.preLoad(beatmapInfo.audioPath)

                ensureActive()
                updateMusicEffects()

                val previewTime = if (beatmapInfo.previewTime >= 0)
                    beatmapInfo.previewTime
                else
                    (GlobalManager.getInstance().songService.length * 0.4f).toInt()

                ensureActive()
                GlobalManager.getInstance().songService.play()

                ensureActive()
                GlobalManager.getInstance().songService.seekTo(previewTime)
            }

            ensureActive()
            currentCalculationJob = beatmapLoadingScope.launch calculationJob@{

                ensureActive()
                BeatmapParser(beatmapInfo.path, this@calculationJob).use { parser ->

                    ensureActive()
                    val data = parser.parse(true)

                    if (data == null) {
                        ensureActive()
                        beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)
                        return@calculationJob
                    }

                    ensureActive()
                    val newBeatmapInfo = BeatmapInfo(data, beatmapInfo.dateImported, true, this@calculationJob)
                    beatmapInfo.apply(newBeatmapInfo)

                    ensureActive()
                    beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)

                    val mods = ModMenu.enabledMods.deepCopy().values

                    ensureActive()
                    val attributes = when (Config.getDifficultyAlgorithm()) {
                        DifficultyAlgorithm.droid -> calculateDroidDifficulty(data, mods, this@calculationJob)
                        DifficultyAlgorithm.standard -> calculateStandardDifficulty(data, mods, this@calculationJob)
                    }

                    ensureActive()
                    beatmapInfoWedge.setStarRatingDisplay(attributes.starRating)
                }
            }
        }
    }

    fun onModsChanged() {
        modsButton.apply {
            if (ModMenu.enabledMods.isEmpty()) {
                trailingIcon = null
            } else {
                trailingIcon = ModsIndicator().apply {
                    iconSize = 28f
                    mods = ModMenu.enabledMods.values
                }
            }
        }
    }

    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) {
        beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)
    }

    fun setStarRatingDisplay(starRating: Double) {
        beatmapInfoWedge.setStarRatingDisplay(starRating)
    }

    fun updateMusicEffects() {
        val speed = ModUtils.calculateRateWithMods(ModMenu.enabledMods.values, Double.POSITIVE_INFINITY)
        val adjustPitch = ModMenu.enabledMods.contains(ModNightCore::class.java)

        GlobalManager.getInstance().songService.setSpeed(speed)
        GlobalManager.getInstance().songService.setAdjustPitch(adjustPitch)
    }

    override fun onAttached() {
        super.onAttached()

        updateThread {
            val currentBeatmapInfo = GlobalManager.getInstance().selectedBeatmap

            currentSongPath = currentBeatmapInfo?.audioPath
            currentBackgroundPath = null

            val model = beatmapCarrousel.data.find { it.beatmapSetInfo.directory == currentBeatmapInfo?.setDirectory } ?: return@updateThread

            beatmapCarrousel.applyComponent(model) {
                val beatmapInfoIndex = model.beatmapSetInfo.beatmaps.indexOfFirst { it.path == currentBeatmapInfo?.path }
                select(beatmapInfoIndex)
            }
        }
    }


}


