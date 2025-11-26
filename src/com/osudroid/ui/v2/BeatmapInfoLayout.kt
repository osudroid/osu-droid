package com.osudroid.ui.v2

import com.osudroid.data.*
import com.osudroid.multiplayer.api.data.*
import com.osudroid.ui.*
import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.toolkt.*
import com.rian.osu.*
import com.rian.osu.utils.ModUtils.applyModsToBeatmapDifficulty
import com.rian.osu.utils.ModUtils.calculateRateWithMods
import ru.nsu.ccfit.zuev.osu.*
import java.text.*
import java.util.*
import kotlin.math.roundToInt

class BeatmapInfoLayout : UILinearContainer() {


    private lateinit var titleText: UIText
    private lateinit var artistText: UIText

    private lateinit var lengthText: CompoundText
    private lateinit var bpmText: CompoundText

    private lateinit var arText: UILabeledBadge
    private lateinit var csText: UILabeledBadge
    private lateinit var odText: UILabeledBadge
    private lateinit var hpText: UILabeledBadge

    private lateinit var circlesBadge: UILabeledBadge
    private lateinit var slidersBadge: UILabeledBadge
    private lateinit var spinnersBadge: UILabeledBadge

    private lateinit var starsText: CompoundText
    private lateinit var versionText: UIText


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

                artistText = text {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Unknown"
                    applyTheme = { color = it.accentColor * 0.9f}
                }

                titleText = text {
                    width = FillParent
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "No selected beatmap"
                    applyTheme = { color = it.accentColor }
                    clipToBounds = true
                }

                versionText = text {
                    font = ResourceManager.getInstance().getFont("smallFont")
                    text = "Unknown"
                    applyTheme = { color = it.accentColor * 0.8f}
                }
            }

            starsText = badge {
                text = "0.00"
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("star-xs"))
            }
        }

        linearContainer {
            orientation = Orientation.Horizontal
            spacing = 8f

            lengthText = compoundText {
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("clock"))
                text = "00:00"
                applyTheme = { color = it.accentColor }
                font = ResourceManager.getInstance().getFont("xs")
            }

            bpmText = compoundText {
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("bpm"))
                text = "0"
                applyTheme = { color = it.accentColor }
                font = ResourceManager.getInstance().getFont("xs")
            }
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
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 2f

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
            }
        }
    }


    fun setBeatmapInfo(beatmapInfo: BeatmapInfo?) {
        titleText.text = beatmapInfo?.titleText ?: "No selected beatmap"
        artistText.text = beatmapInfo?.artistText ?: "Unknown"
        versionText.text = beatmapInfo?.version ?: "Unknown"
        setDifficultyStatistics(beatmapInfo)
    }

    fun setBeatmapInfo(roomBeatmap: RoomBeatmap?) {
        titleText.text = roomBeatmap?.title ?: "No selected beatmap"
        artistText.text = roomBeatmap?.artist ?: "Unknown"
        versionText.text = roomBeatmap?.version ?: "Unknown"
        setDifficultyStatistics(null)
    }

    /**
     * Change the displayed difficulty statistics.
     */
    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) {

        circlesBadge.value = beatmapInfo?.hitCircleCount?.toString() ?: "0"
        slidersBadge.value = beatmapInfo?.sliderCount?.toString() ?: "0"
        spinnersBadge.value = beatmapInfo?.spinnerCount?.toString() ?: "0"

        if (beatmapInfo == null) {
            arText.value = "0.00"
            odText.value = "0.00"
            csText.value = "0.00"
            hpText.value = "0.00"
            starsText.text = "0.00"
            bpmText.text = "0"
            lengthText.text = "00:00"
            return
        }

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
        lengthText.text = sdf.format(length)

        bpmText.text = if (minBpm == maxBpm) commonBpm.toString() else "$minBpm-$maxBpm ($commonBpm)"

        // Round to 2 decimal places.
        arText.value = difficulty.ar.roundBy(2).toString()
        odText.value = difficulty.od.roundBy(2).toString()
        csText.value = difficulty.difficultyCS.roundBy(2).toString()
        hpText.value = difficulty.hp.roundBy(2).toString()

        setStarRatingDisplay(beatmapInfo.getStarRating().toDouble())
    }

    /**
     * Change the displayed star rating.
     */
    fun setStarRatingDisplay(value: Double) {
        starsText.apply {
            text = value.roundBy(2).toString()
            color = if (value >= 6.5) Color4(0xFFFFD966) else Color4.Black.copy(alpha = 0.75f)
            background?.color = OsuColors.getStarRatingColor(value)
        }
    }


    companion object {
        init {
            ResourceManager.getInstance().loadHighQualityAsset("clock", "clock.png")
            ResourceManager.getInstance().loadHighQualityAsset("bpm", "bpm.png")
            ResourceManager.getInstance().loadHighQualityAsset("star-xs", "star.png")
        }
    }
}

