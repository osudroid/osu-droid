package com.osudroid.ui.v2.songselect

import android.util.Log
import android.view.RoundedCorner
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.osudroid.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.osudroid.mods.ModNightCore
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.utils.DisplayCorners
import com.osudroid.utils.calculateRate
import com.osudroid.utils.updateThread
import com.edlplan.framework.easing.Easing
import com.rian.andengine.modifier.ModifierType
import org.andengine.opengl.texture.region.TextureRegion
import com.reco1l.andengine.*
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sqrt


/**
 * The song selection scene. Replaces the legacy [ru.nsu.ccfit.zuev.osu.menu.SongMenu] for singleplayer for now.
 */
object SongSelect : UIScene() {

    /**
     * The currently selected beatmap.
     */
    var selectedBeatmap: BeatmapInfo? = null
        private set


    /**
     * The app's accent red, `colorAccent` in colors.xml. Declared before the views, which use it while being created.
     */
    private val BACK_BUTTON_COLOR = Color4(243, 115, 115)

    private val beatmapLoadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val leaderboard = SongSelectLeaderboard()
    private val filterBar = SongSelectFilterBar(leaderboard)
    private val beatmapCarrousel = BeatmapCarrousel()
    private val beatmapInfoWedge = BeatmapInfoWedge()
    private val backgroundSprite: UISprite

    private lateinit var modsButton: UITextButton
    private lateinit var randomButton: UITextButton

    /**
     * The previously selected beatmaps, one per beatmap set change, newest last. Holding the random button
     * walks back through them like the legacy song menu did.
     */
    private val previousSelections = ArrayDeque<BeatmapInfo>()

    /**
     * Whether the current selection change comes from walking back through [previousSelections].
     */
    private var isRestoringPreviousSelection = false

    /**
     * Whether a previous selection was restored during the current random button press, which then
     * must not also select a random beatmap on release.
     */
    private var previousSelectionPerformed = false

    private var randomHoldTime = 0f

    /**
     * All beatmap sets from the database, independent of the current filter.
     */
    private var allBeatmapSets: List<BeatmapSetModel> = emptyList()

    private var currentSongPath: String? = null
    private var currentBackgroundPath: String? = null
    private var currentBeatmapLoadingJob: Job? = null
    private var currentCalculationJob: Job? = null
    private var currentLibraryLoadingJob: Job? = null


    init {
        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")
        ResourceManager.getInstance().loadHighQualityAsset("mods", "mods.png")
        ResourceManager.getInstance().loadHighQualityAsset("random", "random.png")

        backgroundSprite = sprite {
            width = FillParent
            height = FillParent
            textureRegion = ResourceManager.getInstance().getTexture("menu-background")
            scaleType = ScaleType.Crop
            setColor(BACKGROUND_BRIGHTNESS, BACKGROUND_BRIGHTNESS, BACKGROUND_BRIGHTNESS)
        }

        flexContainer {
            width = FillParent
            height = FillParent
            direction = FlexDirection.Row

            // Left side
            linearContainer {
                height = FillParent
                orientation = Orientation.Vertical
                spacing = 2f.srem
                val cornerMargin = topLeftCornerMargin()
                padding = Vec4(cornerMargin, cornerMargin, 4f.srem, 0f)
                flexRules {
                    grow = 1f
                    basis = 0f
                }

                +beatmapInfoWedge
                +leaderboard
            }

            // Right side
            container {
                height = FillParent
                flexRules {
                    grow = 1f
                    basis = 0f
                }

                +beatmapCarrousel
                +filterBar
            }
        }

        linearContainer {
            orientation = Orientation.Horizontal
            anchor = Anchor.BottomLeft
            origin = Anchor.BottomLeft
            padding = Vec4(2f.srem, 4f.srem)
            spacing = 2f.srem

            // Unlike Mods and Random it leaves the song select, so it stands out in the app's accent red.
            textButton {
                text = "Back"
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                // Same arrow as in the mod menu.
                leadingIcon = UISprite().apply {
                    textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
                    width = 28f
                    height = 28f
                }
                applyTheme = {
                    background?.color = BACK_BUTTON_COLOR
                    color = Colors.White
                }

                onActionUp = {
                    GlobalManager.getInstance().mainMenuV2.show()
                }
            }

            modsButton = textButton {
                text = "Mods"
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("mods"))
                onActionUp = { ModMenu.show() }
            }

            // Tap: random beatmap. Hold: go back to the previous selection, one step per second.
            randomButton = textButton {
                text = "Random"
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("random"))
                onActionDown = {
                    randomHoldTime = 0f
                    previousSelectionPerformed = false
                }
                onActionUp = {
                    if (!previousSelectionPerformed) {
                        selectRandomBeatmap()
                    }
                    previousSelectionPerformed = false
                }
                onActionCancel = {
                    previousSelectionPerformed = false
                }
            }
        }
    }


    /**
     * Shows this scene, loading the beatmap library if it has not been loaded yet.
     */
    override fun show() {
        GlobalManager.getInstance().songService?.isGaming = true

        if (allBeatmapSets.isEmpty()) {
            loadBeatmaps()
        }

        UIEngine.current.scene = this
    }


    /**
     * (Re)loads the beatmap sets from the database and applies the optional [query] filter to the carrousel.
     */
    @JvmOverloads
    fun loadBeatmaps(query: String? = null) {
        currentLibraryLoadingJob?.cancel(CancellationException("Library loading cancelled due to a new request."))
        currentLibraryLoadingJob = beatmapLoadingScope.launch {

            if (allBeatmapSets.isEmpty()) {
                ensureActive()
                allBeatmapSets = DatabaseManager.beatmapInfoTable.getBeatmapSetList().map { BeatmapSetModel(it) }
            }

            // Every word of the query must match at least one field of at least one beatmap of the set.
            val queryWords = query?.lowercase()?.split(' ')?.filter { it.isNotBlank() }.orEmpty()

            val filtered = if (queryWords.isEmpty()) allBeatmapSets else allBeatmapSets.filter { model ->
                queryWords.all { word ->
                    model.beatmapSetInfo.beatmaps.any { beatmap ->
                        beatmap.titleText.lowercase().contains(word)
                            || beatmap.artistText.lowercase().contains(word)
                            || beatmap.creator.lowercase().contains(word)
                            || beatmap.version.lowercase().contains(word)
                    }
                }
            }

            ensureActive()
            updateThread {
                beatmapCarrousel.data = filtered
                selectCurrentBeatmap()
            }
        }
    }

    /**
     * Forces the beatmap sets to be reloaded from the database the next time [loadBeatmaps] or [show] is called.
     */
    fun invalidateLibrary() {
        allBeatmapSets = emptyList()
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        val previous = selectedBeatmap

        // Remember the previous beatmap whenever the set changes, unless we are walking back through the history.
        if (!isRestoringPreviousSelection && previous != null && beatmapInfo != null && previous.setDirectory != beatmapInfo.setDirectory) {
            while (previousSelections.size >= PREVIOUS_SELECTION_LIMIT) {
                previousSelections.removeFirst()
            }
            previousSelections.addLast(previous)
        }

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
                GlobalManager.getInstance().songService?.stop()

                return@launch
            }

            if (currentBackgroundPath != beatmapInfo.backgroundPath) {
                currentBackgroundPath = beatmapInfo.backgroundPath

                ensureActive()
                val textureRegion = if (Config.isSafeBeatmapBg()) null else ResourceManager.getInstance().loadBackground(beatmapInfo.backgroundPath)

                ensureActive()
                showBackground(textureRegion ?: ResourceManager.getInstance().getTexture("menu-background"))
            }

            if (currentSongPath != beatmapInfo.audioPath) {
                currentSongPath = beatmapInfo.audioPath

                ensureActive()
                playMusic(beatmapInfo)
            }

            ensureActive()
            currentCalculationJob = beatmapLoadingScope.launch calculationJob@{

                val gameMode = if (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid) GameMode.Droid else GameMode.Standard

                ensureActive()
                val beatmap = try {
                    BeatmapCache.getBeatmap(beatmapInfo, true, gameMode, this)
                } catch (e: Exception) {
                    when (e) {
                        is CancellationException -> throw e
                        is IOException, is IllegalArgumentException -> null
                        else -> {
                            Log.e("SongSelect", "Failed to parse beatmap ${beatmapInfo.path}", e)
                            null
                        }
                    }
                }

                if (beatmap == null) {
                    ensureActive()
                    beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)
                    return@calculationJob
                }

                if (beatmapInfo.needsDifficultyCalculation) {
                    ensureActive()
                    val newBeatmapInfo = BeatmapInfo(beatmap, beatmapInfo.dateImported, true, this@calculationJob)
                    beatmapInfo.apply(newBeatmapInfo)
                    DatabaseManager.beatmapInfoTable.update(newBeatmapInfo)
                }

                ensureActive()
                beatmapInfoWedge.setDifficultyStatistics(beatmapInfo)

                // Copy the mods to avoid concurrent modification.
                val mods = ModMenu.enabledMods.deepCopy().values

                ensureActive()
                val attributes = when (Config.getDifficultyAlgorithm()) {
                    DifficultyAlgorithm.droid -> calculateDroidDifficulty(beatmap, mods, this@calculationJob)
                    DifficultyAlgorithm.standard -> calculateStandardDifficulty(beatmap, mods, this@calculationJob)
                    else -> calculateDroidDifficulty(beatmap, mods, this@calculationJob)
                }

                ensureActive()
                beatmapInfoWedge.setStarRatingDisplay(attributes.starRating)
            }
        }
    }


    private fun playMusic(beatmapInfo: BeatmapInfo) {
        val songService = GlobalManager.getInstance().songService ?: return

        songService.preLoad(beatmapInfo.audioPath)
        updateMusicEffects()

        val previewTime = if (beatmapInfo.previewTime >= 0)
            beatmapInfo.previewTime
        else
            (songService.length * 0.4f).toInt()

        songService.play()
        songService.seekTo(previewTime)
    }

    /**
     * Restarts the music of the currently selected beatmap from its preview time.
     */
    fun playCurrentMusic() {
        val beatmapInfo = selectedBeatmap ?: return
        currentSongPath = beatmapInfo.audioPath
        playMusic(beatmapInfo)
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
        val songService = GlobalManager.getInstance().songService ?: return
        val mods = ModMenu.enabledMods

        val speed = mods.values.calculateRate(Double.POSITIVE_INFINITY)
        val adjustPitch = ModNightCore::class in mods

        songService.setSpeed(speed)
        songService.setAdjustPitch(adjustPitch)
    }


    private fun selectRandomBeatmap() {
        val data = beatmapCarrousel.data
        if (data.size <= 1) {
            return
        }

        val currentDirectory = selectedBeatmap?.setDirectory
        val candidates = data.filter { it.beatmapSetInfo.directory != currentDirectory }

        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
        beatmapCarrousel.applyComponent(candidates.random()) { selectRandom() }
    }

    /**
     * Selects the most recent entry of [previousSelections] that is still part of the carrousel data.
     *
     * @return `false` if there was nothing to go back to.
     */
    private fun selectPreviousBeatmap(): Boolean {
        while (previousSelections.isNotEmpty()) {
            val beatmapInfo = previousSelections.removeLast()
            val model = beatmapCarrousel.data.find { it.beatmapSetInfo.directory == beatmapInfo.setDirectory } ?: continue

            ResourceManager.getInstance().getSound("click-short-confirm")?.play()

            isRestoringPreviousSelection = true
            try {
                beatmapCarrousel.applyComponent(model) { select(beatmapInfo) }
            } finally {
                isRestoringPreviousSelection = false
            }
            return true
        }
        return false
    }

    /**
     * Shows a new background, fading it in from black like the legacy song menu, but faster and eased.
     */
    private fun showBackground(textureRegion: TextureRegion?) = updateThread {
        backgroundSprite.textureRegion = textureRegion
        backgroundSprite.clearModifiers(ModifierType.Color)
        backgroundSprite.setColor(0f, 0f, 0f)
        backgroundSprite.colorTo(BACKGROUND_BRIGHTNESS, BACKGROUND_BRIGHTNESS, BACKGROUND_BRIGHTNESS, BACKGROUND_FADE_DURATION, Easing.OutQuint)
    }

    override fun onManagedUpdate(pSecondsElapsed: Float) {
        // Holding the random button walks back through the previous selections, once per second.
        if (::randomButton.isInitialized && randomButton.isPressed) {
            randomHoldTime += pSecondsElapsed

            if (randomHoldTime >= PREVIOUS_SELECTION_INTERVAL) {
                randomHoldTime -= PREVIOUS_SELECTION_INTERVAL

                if (selectPreviousBeatmap()) {
                    previousSelectionPerformed = true
                }
            }
        }

        super.onManagedUpdate(pSecondsElapsed)
    }

    /**
     * Selects the panel of the globally selected beatmap, if it is part of the carrousel data.
     */
    private fun selectCurrentBeatmap() {
        val currentBeatmapInfo = GlobalManager.getInstance().selectedBeatmap ?: return
        val model = beatmapCarrousel.data.find { it.beatmapSetInfo.directory == currentBeatmapInfo.setDirectory } ?: return

        beatmapCarrousel.applyComponent(model) {
            select(currentBeatmapInfo)
        }

        // The beatmap may already be selected (e.g. when coming back from gameplay), which does not trigger the
        // scroll to it, while the carousel may have been scrolled away in the meantime.
        beatmapCarrousel.autoScrollToSelectedPanel = true
    }

    override fun onAttached() {
        super.onAttached()

        updateThread {
            currentSongPath = GlobalManager.getInstance().selectedBeatmap?.audioPath
            currentBackgroundPath = null
            selectCurrentBeatmap()
        }
    }


    /**
     * The margin of the info card from the top left screen edges. Larger on displays with a large rounded
     * corner, so the card and its text are not cut off by it.
     */
    private fun topLeftCornerMargin(): Float {
        val radius = DisplayCorners.getRadius(RoundedCorner.POSITION_TOP_LEFT)

        // The card's rounded corner stays inside the display's rounded corner when the distance between
        // both corner centers plus the card's radius does not exceed the display's radius.
        val cardRadius = BeatmapInfoWedge.CORNER_RADIUS
        return maxOf(4f.srem, (radius - cardRadius) * (1f - 1f / sqrt(2f)))
    }


    private const val PREVIOUS_SELECTION_LIMIT = 10
    private const val PREVIOUS_SELECTION_INTERVAL = 1f

    private const val BACKGROUND_BRIGHTNESS = 0.4f

    /**
     * Duration of the background fade-in in seconds.
     */
    private const val BACKGROUND_FADE_DURATION = 0.4f

}
