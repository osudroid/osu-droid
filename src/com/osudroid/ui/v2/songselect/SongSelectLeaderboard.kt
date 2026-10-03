package com.osudroid.ui.v2.songselect

import com.osudroid.data.BeatmapInfo
import com.osudroid.data.DatabaseManager
import com.osudroid.online.IBanchoAPI
import com.osudroid.online.model.BeatmapScore
import com.osudroid.online.model.BeatmapScoreModel
import com.osudroid.ui.v2.ModsIndicator
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
import com.reco1l.framework.math.Vec4
import com.rian.andengine.modifier.ModifierType
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator
import ru.nsu.ccfit.zuev.osu.online.OnlineManager
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode
import kotlin.math.roundToInt
import java.text.NumberFormat
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

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

    /**
     * Shows the active global ranking type (PP or score) inside the global tab.
     */
    private val scoringModeText = UIText().apply {
        font = ResourceManager.getInstance().getFont("xs")
    }

    private lateinit var globalButton: UITextButton


    private var currentLoadingJob: Job? = null


    init {
        ResourceManager.getInstance().loadHighQualityAsset("info", "info.png")

        height = FillParent
        width = FillParent
        orientation = Orientation.Vertical
        padding = Vec4(
            4f.srem,
            0f,
            8f.srem + 2f.srem, // The shear size
            0f,
        )

        +UITabSelector().apply {
            width = FillParent
            addButton("Local") {
                leaderboardType = Local
                updateScoringModeLabel()
            }

            // Tapping the global tab while it is already active toggles between PP and score ranking.
            globalButton = addButton("Global") {
                if (leaderboardType == Global) {
                    val newMode = if (Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP)
                        BeatmapLeaderboardScoringMode.SCORE
                    else
                        BeatmapLeaderboardScoringMode.PP

                    Config.setBeatmapLeaderboardScoringMode(newMode)
                    updateScoringModeLabel()
                    onBeatmapSelected(SongSelect.selectedBeatmap)
                } else {
                    leaderboardType = Global
                    updateScoringModeLabel()
                }
            }

            // The mode label lives in the button's foreground: it is not part of the centered button
            // content, but it scales with the press feedback and sits at a fixed spot on the right.
            globalButton.foreground = UIContainer().apply {
                +scoringModeText.apply {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    x = -3f.srem
                }
            }
            updateScoringModeLabel()

            selectedTab = 0
        }

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
                    spacing = 4f
                    padding = Vec4(0f, 0f, 0f, 90f)
                }

                // One pool for both leaderboard types: recreating it on every tab switch caused a visible hitch.
                // Local scores have no avatar URL and show the offline avatar.
                onCreateComponent = { LeaderboardItem(true) }
            }
        }

    }


    private fun updateScoringModeLabel() {
        scoringModeText.text = if (Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP) "PP" else "Score"
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {
        // Follow the (animated) text color of the tab so the label always matches its selection state.
        scoringModeText.color = globalButton.color
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

    private fun showScores(itemModels: List<BeatmapScoreModel>) = updateThread {
        scoresContainer.data = itemModels
        scoresContainer.scrollY = 0f
        switchContainers(overlayContainer, scoresContainer)
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
                        if (!OnlineManager.getInstance().isStayOnline) {
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

            ensureActive()
            if (itemModels.isNotEmpty()) {
                showScores(itemModels)

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
        private val nameText = UIText()
        private val rankText = UIText()
        private val modsIndicator = ModsIndicator()

        private val avatarSprite = if (withAvatars) UISprite() else null

        private lateinit var scoreLabel: UIText
        private lateinit var scoreText: UIText
        private lateinit var maxComboText: UIText
        private lateinit var accuracyText: UIText


        init {
            width = FillParent

            +UIButton().apply {
                width = FillParent
                button = this

                linearContainer {
                    width = FillParent

                    orientation = Orientation.Horizontal
                    padding = Vec4(8f)
                    spacing = 8f

                    val backgroundBox = UIBox().apply { cornerRadius = 12f }
                    val borderBox = UIBox().apply {
                        cornerRadius = 12f
                        paintStyle = PaintStyle.Outline
                        lineWidth = 2f
                    }

                    background = backgroundBox
                    foreground = borderBox

                    applyTheme = { theme ->
                        backgroundBox.color = (theme.accentColor * 0.1f).copy(alpha = 0.75f)
                        borderBox.color = theme.accentColor * 0.2f
                    }

                    container {
                        width = FillParent

                        linearContainer {
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            orientation = Orientation.Horizontal
                            spacing = 8f

                            +rankText.apply {
                                width = 18f
                                anchor = Anchor.CenterLeft
                                origin = Anchor.CenterLeft
                                alignment = Anchor.Center
                                applyTheme = { color = it.accentColor }
                                text = ""
                            }

                            if (avatarSprite != null) {
                                +avatarSprite.apply {
                                    width = 32f
                                    height = 32f
                                    anchor = Anchor.CenterLeft
                                    origin = Anchor.CenterLeft
                                    textureRegion = ResourceManager.getInstance().getTextureIfLoaded("offline-avatar")
                                }
                            }

                            linearContainer {
                                orientation = Orientation.Vertical
                                spacing = 2f

                                +nameText.apply {
                                    text = "Unknown Player"
                                    applyTheme = { color = it.accentColor }
                                }

                                +modsIndicator.apply {
                                    iconSize = 16f
                                }
                            }

                        }

                        linearContainer {
                            orientation = Orientation.Horizontal
                            spacing = 8f
                            anchor = Anchor.CenterRight
                            origin = Anchor.CenterRight

                            fun metricText(name: String, placeholder: String): Pair<UIText, UIText> {

                                val metricText = UIText().apply {
                                    text = placeholder
                                    applyTheme = { color = it.accentColor }
                                }

                                val label = UIText().apply {
                                    text = name.uppercase()
                                    applyTheme = { color = it.accentColor * 0.8f }
                                }

                                linearContainer {
                                    orientation = Orientation.Vertical
                                    spacing = -2f

                                    +label
                                    +metricText
                                }

                                return label to metricText
                            }

                            metricText("Score", "0,000,000,000").let { (label, value) ->
                                scoreLabel = label
                                scoreText = value
                            }
                            maxComboText = metricText("Max combo", "00000x").second
                            accuracyText = metricText("Accuracy", "000.00%").second
                        }

                    }
                }
            }
        }


        override fun onBind(data: BeatmapScoreModel) {
            nameText.text = data.playerName

            modsIndicator.mods = try {
                deserializeMods(data.mods).values
            } catch (e: Exception) {
                null
            }

            refreshAvatar()

            val showPP = Config.getBeatmapLeaderboardScoringMode() == BeatmapLeaderboardScoringMode.PP && data.pp > 0f

            scoreLabel.text = if (showPP) "PP" else "SCORE"
            scoreText.text = if (showPP)
                data.pp.roundToInt().toString() + "pp"
            else
                NumberFormat.getNumberInstance(Locale.US).format(data.score)
            maxComboText.text = "${data.maxCombo}x"
            accuracyText.text = "%.2f%%".format(data.accuracy * 100f)

            rankText.text = if (data.rank > 0) data.rank.toString() else ""
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


    }

}

enum class LeaderboardType {
    Global,
    Local
}
