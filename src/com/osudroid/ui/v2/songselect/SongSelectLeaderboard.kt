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
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
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

        height = Size.Full
        width = Size.Full
        orientation = Orientation.Vertical
        style = {
            padding = Vec4(
                UIEngine.current.safeArea.x + 4f.srem,
                0f,
                8f.srem + 2f.srem, // The shear size
                0f,
            )
        }

        +UITabSelector().apply {
            width = Size.Full
            addButton("Local") { leaderboardType = Local }
            addButton("Global") { leaderboardType = Global }
            selectedTab = 0
        }

        container {
            width = Size.Full
            height = Size.Full

            +overlayContainer.apply {
                width = Size.Full
                height = Size.Full
                orientation = Orientation.Vertical
                spacing = 8f
                padding = Vec4(24f)
                scaleCenter = Anchor.Center
            }

            +scoresContainer.apply {
                width = Size.Full
                height = Size.Full
                scrollAxes = Axes.Y
                clipToBounds = true
                alpha = 0f
                isVisible = false
                scaleCenter = Anchor.Center

                componentWrapper.apply {
                    orientation = Orientation.Vertical
                    width = Size.Full
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
            fadeOut(0.2f).after {
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
                style = { color = it.accentColor }
            }

            text {
                anchor = Anchor.TopCenter
                origin = Anchor.TopCenter
                style = { color = it.accentColor }
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
                style = { color = it.accentColor }
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
            get() = !button.isPressed


        private val button: UIButton
        private val nameText = UIText()
        private val rankText = UIText()
        private val modsIndicator = ModsIndicator()

        private val avatarSprite = if (withAvatars) UISprite() else null

        private lateinit var scoreText: UIText
        private lateinit var maxComboText: UIText
        private lateinit var accuracyText: UIText


        init {
            width = Size.Full

            +UIButton().apply {
                width = Size.Full
                button = this

                linearContainer {
                    width = Size.Full

                    orientation = Orientation.Horizontal
                    padding = Vec4(8f)
                    spacing = 8f

                    style = {
                        backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.75f)
                        radius = 12f
                        borderWidth = 2f
                        borderColor = it.accentColor * 0.2f
                    }

                    container {
                        width = Size.Full

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
                                style = { color = it.accentColor }
                                //buffer = sharedTextCB
                                text = ""
                            }

                            if (withAvatars) {
                                +avatarSprite!!.apply {
                                    width = 32f
                                    height = 32f
                                    anchor = Anchor.CenterLeft
                                    origin = Anchor.CenterLeft
                                    radius = 12f - 4f
                                    textureRegion = ResourceManager.getInstance().getTexture("offline-avatar")
                                    //buffer = sharedSpriteVBO
                                }
                            }

                            linearContainer {
                                orientation = Orientation.Vertical
                                spacing = 2f

                                +nameText.apply {
                                    text = "Unknown Player"
                                    style = { color = it.accentColor }
                                    //buffer = sharedTextCB
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
                                    style = { color = it.accentColor }
                                    //buffer = sharedTextCB
                                }

                                linearContainer {
                                    orientation = Orientation.Vertical
                                    spacing = -2f

                                    text {
                                        text = name.uppercase()
                                        style = { color = it.accentColor * 0.8f }
                                        //buffer = sharedTextCB
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
            }
        }


        override fun onBind(data: BeatmapScoreModel) {
            nameText.text = data.playerName
            //modsIndicator.mods = data.mods

            if (avatarSprite != null) {
                avatarSprite.textureRegion = ResourceManager.getInstance().getTextureIfLoaded(data.avatarUrl ?: "offline-avatar")
            }

            scoreText.text = NumberFormat.getNumberInstance(Locale.US).format(data.score)
            maxComboText.text = "${data.maxCombo}x"
            accuracyText.text = "%.2f%%".format(data.accuracy)

            rankText.text = if (data.rank > 0) data.rank.toString() else ""
        }


    }

}

enum class LeaderboardType {
    Global,
    Local
}