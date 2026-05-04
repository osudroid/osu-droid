package com.osudroid.ui.v2

import android.util.Log
import com.osudroid.GameMode
import com.osudroid.beatmaps.BeatmapCache
import com.osudroid.data.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.v2.modmenu.*
import com.osudroid.utils.async
import com.osudroid.utils.updateThread
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.Vec4
import com.reco1l.toolkt.*
import com.osudroid.utils.ModUtils.applyModsToBeatmapDifficulty
import com.osudroid.utils.ModUtils.calculateRateWithMods
import kotlinx.coroutines.Job
import ru.nsu.ccfit.zuev.osu.*
import java.text.*
import java.util.*
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive

class BeatmapInfoLayout : UILinearContainer() {


    private lateinit var titleText: UIText
    private lateinit var artistText: UIText
    private lateinit var creatorText: UIText

    private lateinit var lengthBadge: UIBadge
    private lateinit var bpmBadge: UIBadge

    private lateinit var arText: UILabeledBadge
    private lateinit var csText: UILabeledBadge
    private lateinit var odText: UILabeledBadge
    private lateinit var hpText: UILabeledBadge

    private lateinit var circlesBadge: UILabeledBadge
    private lateinit var slidersBadge: UILabeledBadge
    private lateinit var spinnersBadge: UILabeledBadge

    private lateinit var starRatingBadge: StarRatingBadge
    private lateinit var versionText: UIText

    private var calculationJob: Job? = null
    private var displayedBeatmap: BeatmapInfo? = null


    init {
        width = FillParent
        orientation = Orientation.Vertical
        spacing = 6f

        flexContainer {
            width = FillParent

            linearContainer {
                flexRules {
                    grow = 1f
                }
                orientation = Orientation.Vertical
                padding = Vec4(0f, 0f, 12f, 0f)

                artistText = text {
                    width = FillParent
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Unknown"
                    applyTheme = { color = it.accentColor * 0.9f }
                    clipToBounds = true
                }

                titleText = text {
                    width = FillParent
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "No selected beatmap"
                    applyTheme = { color = it.accentColor }
                    clipToBounds = true
                }

                versionText = text {
                    width = FillParent
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Unknown"
                    applyTheme = { color = it.accentColor * 0.8f }
                    clipToBounds = true
                }

                creatorText = text {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Unknown"
                    applyTheme = { color = it.accentColor * 0.8f }
                }
            }

            starRatingBadge = StarRatingBadge()
            +starRatingBadge
        }

        linearContainer {
            orientation = Orientation.Vertical
            spacing = 4f

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 4f

                circlesBadge = labeledBadge {
                    label = "Circles"
                    value = "0"
                    sizeVariant = SizeVariant.Small
                }
                slidersBadge = labeledBadge {
                    label = "Sliders"
                    value = "0"
                    sizeVariant = SizeVariant.Small
                }
                spinnersBadge = labeledBadge {
                    label = "Spinners"
                    value = "0"
                    sizeVariant = SizeVariant.Small
                }
                lengthBadge = badge {
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("clock"))
                    text = "00:00"
                    sizeVariant = SizeVariant.Small
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 4f

                arText = labeledBadge {
                    label = "AR"
                    value = "0.00"
                    sizeVariant = SizeVariant.Small
                }
                odText = labeledBadge {
                    label = "OD"
                    value = "0.00"
                    sizeVariant = SizeVariant.Small
                }
                csText = labeledBadge {
                    label = "CS"
                    value = "0.00"
                    sizeVariant = SizeVariant.Small
                }
                hpText = labeledBadge {
                    label = "HP"
                    value = "0.00"
                    sizeVariant = SizeVariant.Small
                }
                bpmBadge = badge {
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("bpm"))
                    text = "0"
                    font = ResourceManager.getInstance().getFont("xs")
                    sizeVariant = SizeVariant.Small
                }
            }
        }
    }


    fun setBeatmapInfo(beatmapInfo: BeatmapInfo?) {
        titleText.text = beatmapInfo?.titleText ?: "No selected beatmap"
        artistText.text = beatmapInfo?.artistText ?: "Unknown"
        creatorText.text = "Mapped by ${beatmapInfo?.creator ?: "Unknown"}"
        versionText.text = beatmapInfo?.version ?: "Unknown"
        setDifficultyStatistics(beatmapInfo)
    }

    fun setBeatmapInfo(roomBeatmap: RoomBeatmap?) {
        titleText.text = roomBeatmap?.title ?: "No selected beatmap"
        artistText.text = roomBeatmap?.artist ?: "Unknown"
        creatorText.text = "Mapped by ${roomBeatmap?.creator ?: "Unknown"}"
        versionText.text = roomBeatmap?.version ?: "Unknown"
        setDifficultyStatistics(null)
    }

    /**
     * Change the displayed difficulty statistics.
     */
    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) {
        displayedBeatmap = beatmapInfo
        calculationJob?.cancel()
        calculationJob = null

        if (beatmapInfo == null) {
            circlesBadge.value = "0"
            slidersBadge.value = "0"
            spinnersBadge.value = "0"
            arText.value = "0.00"
            odText.value = "0.00"
            csText.value = "0.00"
            hpText.value = "0.00"
            starRatingBadge.rating = 0.0
            bpmBadge.text = "0"
            lengthBadge.text = "00:00"
            return
        }

        updateDisplay(beatmapInfo)

        if (beatmapInfo.needsDifficultyCalculation) {
            calculationJob = async {
                val beatmap = try {
                    BeatmapCache.getBeatmap(
                        beatmapInfo,
                        true,
                        Config.getDifficultyAlgorithm().toGameMode(),
                        this
                    )
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        throw e
                    }

                    Log.e("BeatmapInfoLayout", "Failed to calculate difficulty for ${beatmapInfo.filename}", e)
                    null
                }

                ensureActive()

                if (beatmap != null) {
                    val newInfo = BeatmapInfo(beatmap, beatmapInfo.dateImported, true, this)
                    beatmapInfo.apply(newInfo)
                    DatabaseManager.beatmapInfoTable.update(newInfo)

                    ensureActive()

                    updateThread {
                        if (displayedBeatmap === beatmapInfo) {
                            updateDisplay(beatmapInfo)
                        }
                    }
                }
            }
        }
    }

    fun cancelCalculation() {
        calculationJob?.cancel()
        calculationJob = null
    }

    private fun updateDisplay(beatmapInfo: BeatmapInfo) {
        circlesBadge.value = beatmapInfo.hitCircleCount.toString()
        slidersBadge.value = beatmapInfo.sliderCount.toString()
        spinnersBadge.value = beatmapInfo.spinnerCount.toString()

        val mods = ModMenu.enabledMods
        val totalSpeedMultiplier = calculateRateWithMods(mods.values, Double.POSITIVE_INFINITY)

        val difficulty = beatmapInfo.getBeatmapDifficulty()

        applyModsToBeatmapDifficulty(difficulty, GameMode.Droid, mods.values, true)

        val minBpm = (beatmapInfo.bpmMin * totalSpeedMultiplier).roundToInt()
        val maxBpm = (beatmapInfo.bpmMax * totalSpeedMultiplier).roundToInt()
        val commonBpm = (beatmapInfo.mostCommonBPM * totalSpeedMultiplier).roundToInt()
        val length = (beatmapInfo.length / totalSpeedMultiplier).toLong()

        val sdf = SimpleDateFormat(if (length > 3600 * 1000) "HH:mm:ss" else "mm:ss")
        sdf.timeZone = TimeZone.getTimeZone("GMT+0")
        lengthBadge.text = sdf.format(length)

        bpmBadge.text = if (minBpm == maxBpm) commonBpm.toString() else "$minBpm-$maxBpm ($commonBpm)"

        // Round to 2 decimal places.
        arText.value = difficulty.ar.roundBy(2).toString()
        odText.value = difficulty.od.roundBy(2).toString()
        csText.value = difficulty.difficultyCS.roundBy(2).toString()
        hpText.value = difficulty.hp.roundBy(2).toString()
        starRatingBadge.rating = beatmapInfo.getStarRating().toDouble()
    }


    companion object {
        init {
            ResourceManager.getInstance().loadHighQualityAsset("clock", "clock.png")
            ResourceManager.getInstance().loadHighQualityAsset("bpm", "bpm.png")
        }
    }
}

