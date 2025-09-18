package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.osudroid.online.*
import com.osudroid.online.model.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.songselect.LeaderboardType.*
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.*
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.online.*
import java.text.*
import java.util.*
import kotlin.coroutines.cancellation.CancellationException

class SongSelectLeaderboard : UILinearContainer() {


    /**
     * The current leaderboard type.
     */
    var leaderboardType: LeaderboardType = Local
        set(value) {
            if (field != value) {
                field = value
                scoresContainer.onCreateComponent = null
                onBeatmapSelected(SongSelect.selectedBeatmap)
                scoresContainer.onCreateComponent = { LeaderboardItem(value == Global) }
            }
        }


    private val scoreLoadingScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    private val scoresContainer = UIRecyclerContainer<BeatmapScoreModel, LeaderboardItem>(18)

    private val overlayContainer = UILinearContainer()


    private var currentLoadingJob: Job? = null


    init {
        ResourceManager.getInstance().loadHighQualityAsset("info", "info.png")

        height = FillParent
        width = FillParent
        orientation = Orientation.Vertical
        padding = Vec4(80f, 0f, 80f + 16f, 0f)
        spacing = 8f

        +UITabSelector().apply {
            width = FillParent
            addButton("Local") { leaderboardType = Local }
            addButton("Global") { leaderboardType = Global }
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

                // Without avatars because local is chosen by default
                onCreateComponent = { LeaderboardItem(false) }
            }
        }

    }


    private fun switchContainers(adverseContainer: UIComponent, showingContainer: UIComponent) {

        adverseContainer.apply {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            fadeOut(0.2f).then {
                isVisible = false
            }
        }

        showingContainer.apply {
            clearModifiers(ModifierType.Alpha, ModifierType.ScaleXY)
            isVisible = true
            scaleX = 0.95f
            scaleY = 0.95f
            alpha = 0f

            scaleTo(1f, 0.1f)
            fadeIn(0.1f)
        }
    }


    private fun showMessage(message: String) {
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
                font = ResourceManager.getInstance().getFont("smallFont")
                applyTheme = { color = it.accentColor }
                text = message
            }
        }
    }

    private fun showLoading() {
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
                font = ResourceManager.getInstance().getFont("smallFont")
                applyTheme = { color = it.accentColor }
                text = "Loading scores..."
            }
        }
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {

        currentLoadingJob?.cancel(CancellationException())
        currentLoadingJob = scoreLoadingScope.launch {

            ensureActive()
            showLoading()

            ensureActive()
            scoresContainer.data = listOf()

            if (beatmapInfo != null) {
                var itemModels = listOf<BeatmapScoreModel>()

                try {
                    when (leaderboardType) {

                        Local -> {
                            ensureActive()
                            itemModels = DatabaseManager.scoreInfoTable.getBeatmapScores(beatmapInfo.md5).map { scoreInfo ->
                                ensureActive()
                                BeatmapScore(scoreInfo)
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
                }

                if (itemModels.isNotEmpty()) {
                    ensureActive()
                    scoresContainer.data = itemModels

                    ensureActive()
                    switchContainers(overlayContainer, scoresContainer)
                } else {
                    ensureActive()
                    showMessage("No scores found for this beatmap.")
                }
            }

        }

    }


    class LeaderboardItem(withAvatars: Boolean) : RecyclableComponent<BeatmapScoreModel>() {

        override val isRecyclable: Boolean
            get() = !isPressed


        private val nameText = UIText()
        private val rankText = UIText()
        private val modsIndicator = ModsIndicator()
        private val avatarSprite = if (withAvatars) UIShapedSprite() else null

        private lateinit var scoreText: UIText
        private lateinit var maxComboText: UIText
        private lateinit var accuracyText: UIText


        init {
            width = FillParent
            orientation = Orientation.Horizontal
            padding = Vec4(8f)
            spacing = 8f

            background = UIBox().apply {
                cornerRadius = 12f
                applyTheme = {
                    color = it.accentColor * 0.1f
                    alpha = 0.75f
                }
                buffer = sharedBackgroundVBO
            }

            foreground = UIBox().apply {
                paintStyle = PaintStyle.Outline
                cornerRadius = 12f
                lineWidth = 2f
                applyTheme = {
                    color = it.accentColor * 0.2f
                }
                buffer = sharedForegroundVBO
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
                        font = ResourceManager.getInstance().getFont("xs")
                        applyTheme = { color = it.accentColor }
                        buffer = sharedTextCB
                        text = ""
                    }

                    if (withAvatars) {
                        +avatarSprite!!.apply {
                            width = 32f
                            height = 32f
                            anchor = Anchor.CenterLeft
                            origin = Anchor.CenterLeft
                            shape = UIBox().apply { cornerRadius = 12f - 4f }
                            textureRegion = ResourceManager.getInstance().getTexture("offline-avatar")
                            buffer = sharedSpriteVBO
                        }
                    }

                    linearContainer {
                        orientation = Orientation.Vertical
                        spacing = 2f

                        +nameText.apply {
                            text = "Unknown Player"
                            applyTheme = { color = it.accentColor }
                            font = ResourceManager.getInstance().getFont("xs")
                            buffer = sharedTextCB
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

                    fun MetricText(name: String, placeholder: String): UIText {

                        val metricText = UIText().apply {
                            text = placeholder
                            font = ResourceManager.getInstance().getFont("xs")
                            applyTheme = { color = it.accentColor }
                            buffer = sharedTextCB
                        }

                        linearContainer {
                            orientation = Orientation.Vertical
                            spacing = -2f

                            text {
                                text = name.uppercase()
                                font = ResourceManager.getInstance().getFont("xxs")
                                applyTheme = { color = it.accentColor * 0.8f }
                                buffer = sharedTextCB
                            }
                            +metricText
                        }

                        return metricText
                    }

                    scoreText = MetricText("Score", "0,000,000,000")
                    maxComboText = MetricText("Max combo", "00000x")
                    accuracyText = MetricText("Accuracy", "000.00%")
                }

            }

        }


        override fun onBind(data: BeatmapScoreModel) {
            nameText.text = data.playerName
            modsIndicator.mods = data.mods

            if (avatarSprite != null) {
                avatarSprite.textureRegion = ResourceManager.getInstance().getTextureIfLoaded(data.avatarUrl ?: "offline-avatar")
            }

            scoreText.text = NumberFormat.getNumberInstance(Locale.US).format(data.score)
            maxComboText.text = "${data.maxCombo}x"
            accuracyText.text = "%.2f%%".format(data.accuracy)

            rankText.text = if (data.rank > 0) data.rank.toString() else ""
        }


        companion object {
            private val sharedTextCB = CompoundBuffer(UIText.TextTextureBuffer(128), UIText.TextVertexBuffer(128)).asSharedDynamically()
            private val sharedBackgroundVBO = UIBox.BoxVBO(12f, UICircle.approximateSegments(12f, 12f, 90f), PaintStyle.Fill).asSharedStatically()
            private val sharedForegroundVBO = UIBox.BoxVBO(12f, UICircle.approximateSegments(12f, 12f, 90f), PaintStyle.Outline).asSharedStatically()
            private val sharedSpriteVBO = UISprite.SpriteVBO().asSharedDynamically()
        }

    }

}

enum class LeaderboardType {
    Global,
    Local
}