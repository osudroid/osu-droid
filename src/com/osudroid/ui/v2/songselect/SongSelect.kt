package com.osudroid.ui.v2.songselect

import android.util.Log
import android.view.RoundedCorner
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.BeatmapSetInfo
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
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Colors
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Color4
import com.reco1l.framework.interpolate
import com.reco1l.framework.math.Vec4
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.audio.Status
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.LibraryManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.sqrt


/**
 * The app's accent red, `colorAccent` in colors.xml.
 */
internal val ACCENT_RED = Color4(243, 115, 115)

/**
 * The song selection scene. Replaces the legacy [ru.nsu.ccfit.zuev.osu.menu.SongMenu] for singleplayer for now.
 */
object SongSelect : UIScene() {

    /**
     * The currently selected beatmap.
     */
    var selectedBeatmap: BeatmapInfo? = null
        private set


    private val beatmapLoadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val leaderboard = SongSelectLeaderboard()
    private val filterBar = SongSelectFilterBar(leaderboard)
    private val beatmapCarrousel = BeatmapCarrousel()
    private val beatmapInfoWedge = BeatmapInfoWedge()
    private val backgroundSprite: UISprite

    private lateinit var modsButton: UITextButton
    private lateinit var randomButton: UITextButton

    /**
     * Scrolls back to the selected beatmap, only shown while it is out of sight.
     */
    private val jumpButton = UIButton().apply {
        anchor = Anchor.BottomRight
        origin = Anchor.BottomRight
        x = -8f.srem
        y = -8f.srem
        padding = Vec4(16f)
        isVisible = false
        onActionUp = { selectCurrentBeatmap() }

        +FontAwesomeIcon(Icon.LocationCrosshairs).apply {
            anchor = Anchor.Center
            origin = Anchor.Center
            iconSize = 22f
        }
    }

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

    /**
     * The text of the search input, kept so that changing the other filters does not reset it.
     */
    private var searchQuery = ""

    /**
     * The order of the beatmap sets, applied on the next [loadBeatmaps].
     */
    var sortOrder = BeatmapSortOrder.entries[Config.getInt(SORT_ORDER_PREFERENCE, 0).mod(BeatmapSortOrder.entries.size)]
        set(value) {
            field = value
            Config.setInt(SORT_ORDER_PREFERENCE, value.ordinal)
        }

    /**
     * Whether only favorite beatmap sets are listed, applied on the next [loadBeatmaps].
     */
    var isFavoritesOnly = false

    /**
     * The collection whose beatmap sets are listed, `null` for all. Applied on the next [loadBeatmaps].
     */
    var collection: String? = null

    /**
     * The number of beatmap sets in the library.
     */
    val setCount
        get() = allBeatmapSets.size

    /**
     * The number of beatmap sets the carrousel lists, less than [setCount] while the search or a filter hides some.
     */
    var listedSetCount = 0
        private set

    /**
     * Whether beatmap sets are hidden by something other than the search text.
     */
    val isFilterActive
        get() = isFavoritesOnly || collection != null

    private var currentSongPath: String? = null
    private var currentBackgroundPath: String? = null
    private var currentBeatmapLoadingJob: Job? = null
    private var currentCalculationJob: Job? = null
    private var currentLibraryLoadingJob: Job? = null

    /**
     * The seconds since the music started to fade in, negative when it does not.
     */
    @Volatile
    private var musicFadeTime = -1f

    /**
     * Whether the music is started again when it ended. Not while it is being loaded.
     */
    @Volatile
    private var isMusicRestartAllowed = false


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
                +jumpButton
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
                    background?.color = ACCENT_RED
                    color = Colors.White
                }

                onActionUp = { onBackPressed() }
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
     * (Re)loads the beatmap sets from the database and lists the ones matching the search [query] and the
     * other filters in the carrousel, in the current [sortOrder].
     */
    @JvmOverloads
    fun loadBeatmaps(query: String = searchQuery) {
        searchQuery = query

        // Read here, they may change while the library loads.
        val sortOrder = sortOrder
        val isFavoritesOnly = isFavoritesOnly
        val collection = collection

        currentLibraryLoadingJob?.cancel(CancellationException("Library loading cancelled due to a new request."))
        currentLibraryLoadingJob = beatmapLoadingScope.launch {

            if (allBeatmapSets.isEmpty()) {
                ensureActive()
                val favorites = DatabaseManager.beatmapOptionsTable.getFavorites().toHashSet()

                allBeatmapSets = DatabaseManager.beatmapInfoTable.getBeatmapSetList().map {
                    BeatmapSetModel(it).apply { isFavorite = it.directory in favorites }
                }
            }

            val search = BeatmapSearchQuery(query)
            val entries = if (sortOrder.isPerDifficulty) allBeatmapSets.flatMap { it.difficulties } else allBeatmapSets

            val collectionSets = collection?.let { DatabaseManager.beatmapCollectionsTable.getBeatmaps(it)?.toHashSet() }

            val filtered = entries.filter { model ->
                (!isFavoritesOnly || model.isFavorite)
                    && (collectionSets == null || model.beatmapSetInfo.directory in collectionSets)
                    && search.matches(model.beatmaps)
            }

            val sorted = filtered.sortedWith(sortOrder.comparator)
            // Sets can share a directory.
            val listedSets = if (sortOrder.isPerDifficulty)
                filtered.distinctBy { it.beatmapSetInfo.directory to it.beatmapSetInfo.id }.size
            else
                filtered.size

            ensureActive()
            updateThread {
                beatmapCarrousel.data = sorted
                listedSetCount = listedSets
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

    /**
     * Shows the star ratings of the current difficulty algorithm.
     */
    fun onDifficultyAlgorithmChanged() {
        beatmapCarrousel.selectedPanel?.updateStarRatings()
        onBeatmapSelected(selectedBeatmap)
        loadBeatmaps()
    }

    /**
     * Leaves the mod menu if it is shown, the song select otherwise.
     */
    fun onBackPressed() {
        if (childScene === ModMenu) {
            ModMenu.back()
        } else {
            GlobalManager.getInstance().mainMenuV2.show()
        }
    }

    /**
     * Deletes a beatmap set from the storage and the library.
     */
    fun deleteBeatmapSet(beatmapSetInfo: BeatmapSetInfo) {
        if (selectedBeatmap?.setDirectory == beatmapSetInfo.directory) {
            beatmapCarrousel.selectedPanel = null
            onBeatmapSelected(null)
        }

        beatmapLoadingScope.launch {
            LibraryManager.deleteBeatmapSet(beatmapSetInfo)
            invalidateLibrary()
            loadBeatmaps()
        }
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
        if (!Config.isPlayMusicPreview()) {
            return
        }

        val songService = GlobalManager.getInstance().songService ?: return

        isMusicRestartAllowed = false
        musicFadeTime = -1f

        songService.preLoad(beatmapInfo.audioPath)
        updateMusicEffects()

        val previewTime = if (beatmapInfo.previewTime >= 0)
            beatmapInfo.previewTime
        else
            (songService.length * 0.4f).toInt()

        songService.play()
        songService.setVolume(0f)
        songService.seekTo(previewTime)

        musicFadeTime = 0f
        isMusicRestartAllowed = true
    }

    /**
     * Fades the music in after it started, and starts it again once it ended.
     */
    private fun updateMusic(deltaTimeSec: Float) {
        val songService = GlobalManager.getInstance().songService ?: return

        if (musicFadeTime >= 0f) {
            musicFadeTime += deltaTimeSec

            val progress = (musicFadeTime / MUSIC_FADE_DURATION).coerceAtMost(1f)
            songService.setVolume(Easing.Out.interpolate(progress) * Config.getBgmVolume())

            if (progress >= 1f || songService.status == Status.STOPPED) {
                musicFadeTime = -1f
            }
        }

        // The length of a stream that finished playing is 0.
        if (isMusicRestartAllowed && Config.isPlayMusicPreview() && songService.length == 0) {
            val beatmapInfo = selectedBeatmap ?: return

            isMusicRestartAllowed = false
            beatmapLoadingScope.launch { playMusic(beatmapInfo) }
        }
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
            val model = findModel(beatmapInfo) ?: continue

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

        updateMusic(pSecondsElapsed)

        jumpButton.isVisible = !beatmapCarrousel.isSelectedEntryInSight

        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun onDetached() {
        super.onDetached()

        // The fade is not continued while the scene is not shown.
        if (musicFadeTime >= 0f) {
            musicFadeTime = -1f
            GlobalManager.getInstance().songService?.setVolume(Config.getBgmVolume())
        }
    }

    /**
     * The entry of the carrousel that lists a beatmap, or another one of its set if the beatmap itself is hidden.
     */
    private fun findModel(beatmapInfo: BeatmapInfo): BeatmapSetModel? {
        val models = beatmapCarrousel.data.filter { it.beatmapSetInfo.directory == beatmapInfo.setDirectory }

        return models.find { model -> model.beatmaps.any { it.path == beatmapInfo.path } } ?: models.firstOrNull()
    }

    /**
     * Selects the panel of the globally selected beatmap, if it is part of the carrousel data.
     */
    private fun selectCurrentBeatmap() {
        val currentBeatmapInfo = GlobalManager.getInstance().selectedBeatmap ?: return
        val model = findModel(currentBeatmapInfo) ?: return

        beatmapCarrousel.applyComponent(model) {
            select(currentBeatmapInfo)
        }

        // The beatmap may already be selected (e.g. when coming back from gameplay), which does not trigger the
        // scroll to it, while the carousel may have been scrolled away in the meantime.
        beatmapCarrousel.autoScrollToSelectedPanel = true
    }

    override fun onAttached() {
        super.onAttached()

        // A score may have been set while the scene was not shown, e.g. when coming back from gameplay.
        leaderboard.onBeatmapSelected(selectedBeatmap)

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


    /**
     * Shared with the legacy song menu.
     */
    private const val SORT_ORDER_PREFERENCE = "sortorder"

    private const val PREVIOUS_SELECTION_LIMIT = 10
    private const val PREVIOUS_SELECTION_INTERVAL = 1f

    private const val BACKGROUND_BRIGHTNESS = 0.4f

    /**
     * Duration of the background fade-in in seconds.
     */
    private const val BACKGROUND_FADE_DURATION = 0.4f

    /**
     * Duration of the music fade-in in seconds.
     */
    private const val MUSIC_FADE_DURATION = 0.8f

}
