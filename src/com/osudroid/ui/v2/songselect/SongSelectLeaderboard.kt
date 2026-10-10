package com.osudroid.ui.v2.songselect

import android.opengl.GLES32
import com.osudroid.BuildSettings
import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.online.IBanchoAPI
import com.osudroid.online.model.BeatmapScore
import com.osudroid.online.model.BeatmapScoreModel
import com.osudroid.ui.v2.songselect.LeaderboardType.*
import com.osudroid.utils.deserializeMods
import com.osudroid.utils.updateThread
import com.reco1l.andengine.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import com.rian.andengine.modifier.ModifierType
import kotlinx.coroutines.*
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.roundToInt

/**
 * Lists the local or global scores of the selected beatmap.
 */
class SongSelectLeaderboard : UILinearContainer() {


    /**
     * The current leaderboard type.
     */
    var leaderboardType: LeaderboardType = Local
        set(value) {
            if (field != value) {
                field = value
                // The item pool is shared between both types, switching only reloads the data.
                onBeatmapSelected(SongSelect.selectedBeatmap)
            }
        }


    private val scoreLoadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val scoresContainer = UIRecyclerContainer<BeatmapScoreModel, LeaderboardItem>(18)

    private val overlayContainer = UILinearContainer()

    private lateinit var personalBestContainer: UILinearContainer
    private val personalBestHeader = UIText()
    private val personalBestItem = LeaderboardItem(true)


    private var currentLoadingJob: Job? = null


    init {
        ResourceManager.getInstance().loadHighQualityAsset("info", "info.png")

        height = FillParent
        width = FillParent
        orientation = Orientation.Vertical
        spacing = 2f.srem
        padding = Vec4(
            4f.srem,
            0f,
            8f.srem + 2f.srem, // The shear size
            BOTTOM_BUTTONS_CLEARANCE,
        )

        container {
            width = FillParent
            height = FillParent

            +overlayContainer.apply {
                width = FillParent
                height = FillParent
                orientation = Orientation.Vertical
                spacing = 8f
                padding = Vec4(24f)
                scaleCenter = Anchor.Center
            }

            +scoresContainer.apply {
                width = FillParent
                height = FillParent
                scrollAxes = Axes.Y
                clipToBounds = true
                alpha = 0f
                isVisible = false
                scaleCenter = Anchor.Center

                componentWrapper.apply {
                    orientation = Orientation.Vertical
                    width = FillParent
                    spacing = 1f.srem
                    padding = Vec4(0f, 0f, 0f, 4f.srem)
                }

                // One pool for both leaderboard types, recreating it on every tab switch caused a visible hitch.
                // Local scores have no avatar URL and show the offline avatar.
                onCreateComponent = { LeaderboardItem(true) }
            }

            // Docked at the bottom of the list area, on top of the scrolling scores.
            personalBestContainer = linearContainer {
                width = FillParent
                orientation = Orientation.Vertical
                spacing = 1f.srem
                anchor = Anchor.BottomLeft
                origin = Anchor.BottomLeft
                isVisible = false

                +personalBestHeader.apply {
                    text = "Personal Best"
                    font = ResourceManager.getInstance().getFont("xs")
                    applyTheme = { color = it.accentColor * 0.8f }
                }

                +personalBestItem
            }
        }

    }


    override fun onManagedUpdate(deltaTimeSec: Float) {
        // The score list ends above the docked personal best instead of scrolling underneath it.
        val listArea = scoresContainer.parent as? UIComponent
        if (listArea != null) {
            scoresContainer.height = if (personalBestContainer.isVisible)
                listArea.innerHeight - personalBestContainer.height - 2f.srem
            else
                FillParent
        }

        super.onManagedUpdate(deltaTimeSec)
    }


    private fun switchContainers(adverseContainer: UIComponent, showingContainer: UIComponent) {

        adverseContainer.apply {
            clearModifiers(false, ModifierType.Alpha, ModifierType.ScaleXY)
            fadeOut(0.2f).after {
                isVisible = false
            }
        }

        showingContainer.apply {
            clearModifiers(false, ModifierType.Alpha, ModifierType.ScaleXY)
            isVisible = true
            scaleX = 0.95f
            scaleY = 0.95f
            alpha = 0f

            scaleTo(1f, 0.1f)
            fadeIn(0.1f)
        }
    }


    private fun showMessage(message: String) = updateThread {
        switchContainers(scoresContainer, overlayContainer)
        showPersonalBest(null)

        overlayContainer.apply {
            detachChildren()

            sprite {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                width = 32f
                height = 32f
                textureRegion = ResourceManager.getInstance().getTexture("info")
                applyTheme = { color = it.accentColor }
            }

            text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                applyTheme = { color = it.accentColor }
                text = message
            }
        }
    }

    private fun showLoading() = updateThread {
        switchContainers(scoresContainer, overlayContainer)
        showPersonalBest(null)

        overlayContainer.apply {
            detachChildren()

            +CircularProgressBar().apply {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                width = 32f
                height = 32f
            }

            text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                applyTheme = { color = it.accentColor }
                text = "Loading scores..."
            }
        }
    }

    private fun showScores(itemModels: List<BeatmapScoreModel>, personalBest: BeatmapScoreModel?) = updateThread {
        scoresContainer.data = itemModels
        scoresContainer.scrollY = 0f
        switchContainers(overlayContainer, scoresContainer)
        showPersonalBest(personalBest)
    }

    /**
     * Shows or hides the docked personal best. Must be called from the update thread.
     */
    private fun showPersonalBest(personalBest: BeatmapScoreModel?) {
        if (personalBest == null) {
            personalBestContainer.isVisible = false
            return
        }

        personalBestHeader.text = if (personalBest.rank > 0) "Personal Best (#${personalBest.rank})" else "Personal Best"
        personalBestItem.bindDirectly(personalBest)
        personalBestContainer.isVisible = true
    }

    /**
     * Downloads the avatars of the given scores one by one and refreshes the visible items after each one.
     */
    private suspend fun loadAvatars(itemModels: List<BeatmapScoreModel>) = coroutineScope {
        val urls = itemModels.mapNotNull { it.avatarUrl }.distinct()

        for (url in urls) {
            ensureActive()

            if (ResourceManager.getInstance().getTextureIfLoaded(MD5Calculator.getStringMD5(url)) == null) {
                try {
                    OnlineManager.getInstance().loadAvatarToTextureManager(url)
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    continue
                }
            }

            ensureActive()
            updateThread {
                scoresContainer.boundComponents.values.forEach { it.refreshAvatar() }
                personalBestItem.refreshAvatar()
            }
        }
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {

        currentLoadingJob?.cancel(CancellationException())
        currentLoadingJob = scoreLoadingScope.launch {

            ensureActive()
            showLoading()

            ensureActive()
            updateThread { scoresContainer.data = listOf() }

            if (beatmapInfo == null) {
                return@launch
            }

            var itemModels = listOf<BeatmapScoreModel>()

            try {
                when (leaderboardType) {

                    Local -> {
                        ensureActive()
                        itemModels = DatabaseManager.scoreInfoTable.getBeatmapScores(beatmapInfo.md5).mapIndexed { index, scoreInfo ->
                            ensureActive()
                            BeatmapScore(index + 1, scoreInfo)
                        }
                    }

                    Global -> {
                        if (!OnlineManager.getInstance().isStayOnline && !BuildSettings.MOCK_LEADERBOARD) {
                            ensureActive()
                            showMessage("You must be online to view global leaderboard.")
                            return@launch
                        }

                        ensureActive()
                        itemModels = IBanchoAPI.getBeatmapLeaderboard(beatmapInfo.md5)
                    }
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                ensureActive()
                showMessage("Failed to load global leaderboard.")
                return@launch
            }

            val scores = itemModels.filter { it.isInLeaderboard }
            val personalBest = itemModels.firstOrNull { it.isPersonalBest }

            ensureActive()
            if (scores.isNotEmpty()) {
                showScores(scores, personalBest)

                if (leaderboardType == Global) {
                    loadAvatars(itemModels)
                }
            } else {
                showMessage("No scores found for this beatmap.")
            }
        }

    }


    class LeaderboardItem(withAvatars: Boolean) : RecyclableComponent<BeatmapScoreModel>() {

        override val isRecyclable: Boolean
            get() = !button.isPressed


        private val button: UIButton
        private val rankText = UIText()
        private val nameText = UIText()
        private val comboText = UIText()
        private val accuracyText = UIText()
        private val primaryText = UIText()
        private val modsText = UIText()
        private val markSprite = UISprite()

        private val avatarSprite = if (withAvatars) UIShapedSprite() else null


        init {
            width = FillParent

            button = UIButton().apply {
                width = FillParent
                padding = Vec4(2f.srem, 1f.srem)

                foreground = UIBox().apply {
                    cornerRadius = 12f
                    paintStyle = PaintStyle.Outline
                    lineWidth = 2f
                }

                applyTheme = { theme ->
                    val isPersonalBest = boundData?.isPersonalBest == true
                    background?.color = if (isPersonalBest) theme.accentColor * 0.35f else theme.accentColor * 0.15f
                    foreground?.color = if (isPersonalBest) theme.accentColor else theme.accentColor * 0.2f
                    color = theme.accentColor
                }

                container {
                    width = FillParent
                    // The avatar defines the row height, the other blocks are vertically centered to it.
                    height = AVATAR_SIZE

                    // The columns have fixed widths so they line up between the rows, hence the manual x positions.
                    var cursorX = 0f

                    +rankText.apply {
                        width = RANK_WIDTH
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        alignment = Anchor.Center
                        font = ResourceManager.getInstance().getFont("xs")
                        applyTheme = { color = it.accentColor * 0.8f }
                    }
                    cursorX += RANK_WIDTH + GAP

                    if (avatarSprite != null) {
                        +avatarSprite.apply {
                            width = AVATAR_SIZE
                            height = AVATAR_SIZE
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            x = cursorX
                            clipToBounds = true

                            shape = object : UIBox() {
                                init {
                                    cornerRadius = 8f
                                    color = Color4.Transparent
                                }

                                override fun beginDraw(pGLState: GLState) {
                                    // The shape clears the depth buffer before drawing the mask, but glClear only clears it
                                    // while depth writes are enabled, and a previously drawn entity may have disabled them.
                                    GLES32.glDepthMask(true)
                                    super.beginDraw(pGLState)
                                }
                            }
                        }
                        cursorX += AVATAR_SIZE + GAP
                    }

                    // Name with the mods below it, both clipped to the name column.
                    linearContainer {
                        orientation = Orientation.Vertical
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        x = cursorX
                        width = NAME_WIDTH

                        +nameText.apply {
                            width = FillParent
                            clipToBounds = true
                            text = "Unknown Player"
                            font = ResourceManager.getInstance().getFont("torusBold")
                            applyTheme = { color = it.accentColor }
                        }

                        +modsText.apply {
                            width = FillParent
                            clipToBounds = true
                            font = ResourceManager.getInstance().getFont("xs")
                            applyTheme = { color = it.accentColor * 0.8f }
                        }
                    }
                    cursorX += NAME_WIDTH + GAP * 2

                    // Middle: combo and accuracy as label/value pairs.
                    linearContainer {
                        orientation = Orientation.Horizontal
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        x = cursorX
                        spacing = GAP * 2

                        metric("COMBO", comboText)
                        metric("ACC", accuracyText)
                    }

                    // Right: score or pp, the grade at the edge.
                    linearContainer {
                        orientation = Orientation.Horizontal
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        spacing = GAP

                        +primaryText.apply {
                            width = SCORE_WIDTH
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            alignment = Anchor.CenterRight
                            font = ResourceManager.getInstance().getFont("torusBold")
                            applyTheme = { color = it.accentColor }
                        }

                        +markSprite.apply {
                            height = MARK_HEIGHT
                            width = MARK_HEIGHT * MARK_ASPECT_RATIO
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                        }
                    }
                }
            }
            +button
        }


        /**
         * Adds a small uppercase label with the given value text below it.
         */
        private fun UILinearContainer.metric(label: String, valueText: UIText) {
            linearContainer {
                orientation = Orientation.Vertical
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft

                text {
                    text = label
                    font = ResourceManager.getInstance().getFont("xs")
                    applyTheme = { color = it.accentColor * 0.7f }
                }

                +valueText.apply {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    applyTheme = { color = it.accentColor }
                }
            }
        }


        override fun onBind(data: BeatmapScoreModel) {
            rankText.text = if (data.rank > 0) data.rank.toString() else ""
            nameText.text = data.playerName

            comboText.text = "${data.maxCombo}x"
            accuracyText.text = "%.2f%%".format(data.accuracy * 100f)

            modsText.text = try {
                deserializeMods(data.mods).toDisplayModString(false).takeUnless { it == "-" }.orEmpty()
            } catch (e: Exception) {
                ""
            }

            markSprite.textureRegion = if (data.mark.isNotEmpty()) ResourceManager.getInstance().getTexture("ranking-${data.mark}-small") else null
            markSprite.isVisible = markSprite.textureRegion != null

            refreshAvatar()

            val showPP = Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP && data.pp > 0f

            primaryText.text = if (showPP)
                "${data.pp.roundToInt()}pp"
            else
                NumberFormat.getNumberInstance(Locale.US).format(data.score)

            // Re-apply the theme so the personal best highlight reflects the bound score.
            button.onThemeChanged(Theme.current)
        }

        /**
         * Binds a score outside of a recycler, used for the docked personal best.
         */
        fun bindDirectly(data: BeatmapScoreModel) {
            boundData = data
            onBind(data)
        }

        /**
         * Re-reads the avatar texture of the bound score, used once avatars finished downloading.
         */
        fun refreshAvatar() {
            val avatarSprite = avatarSprite ?: return
            val url = boundData?.avatarUrl

            avatarSprite.textureRegion = (if (url != null) ResourceManager.getInstance().getAvatarTextureIfLoaded(url) else null)
                ?: ResourceManager.getInstance().getTextureIfLoaded("offline-avatar")
        }


        companion object {
            private const val AVATAR_SIZE = 44f
            private const val MARK_HEIGHT = 24f

            /**
             * Width to height ratio of the `ranking-*-small` textures.
             */
            private const val MARK_ASPECT_RATIO = 31f / 38f
            private val RANK_WIDTH get() = 1.75f.rem
            private val NAME_WIDTH get() = 8f.rem
            private val SCORE_WIDTH get() = 8f.rem
            private val GAP get() = 2f.srem
        }

    }


    companion object {
        /**
         * Keeps the list and the docked personal best clear of the Back/Mods/Random buttons.
         */
        private val BOTTOM_BUTTONS_CLEARANCE get() = 4.5f.rem
    }

}

enum class LeaderboardType {
    Global,
    Local
}
