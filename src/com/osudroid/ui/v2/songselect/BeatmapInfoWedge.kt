package com.osudroid.ui.v2.songselect

import com.osudroid.GameMode
import com.osudroid.beatmaps.DroidHitWindow.Companion.hitWindow300ToOverallDifficulty
import com.osudroid.beatmaps.PreciseDroidHitWindow
import com.osudroid.data.BeatmapInfo
import com.osudroid.mods.ModPrecise
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.utils.applyMods
import com.osudroid.utils.calculateRate
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.math.Vec4
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameHelper
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

/**
 * Shows the metadata and difficulty statistics of the selected beatmap.
 */
class BeatmapInfoWedge : UIContainer() {


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

    private lateinit var starsText: StarRatingBadge
    private lateinit var versionText: UIText


    init {
        width = FillParent
        translationX = -2f
        translationY = -2f

        // The wedge shapes are background/foreground so they take the container's size without
        // being part of the content measurement (a FillParent child would keep the container from shrinking).
        background = UIWedge().apply {
            alpha = 0.75f
            cornerRadius = 0.75f.rem
            applyTheme = { color = it.accentColor * 0.1f }
        }

        foreground = UIWedge().apply {
            paintStyle = PaintStyle.Outline
            lineWidth = 2f
            cornerRadius = 0.75f.rem
            applyTheme = { color = it.accentColor * 0.2f }
        }

        linearContainer {
            width = FillParent
            orientation = Orientation.Vertical
            // Extra right padding keeps the badges clear of the wedge's sheared edge.
            padding = Vec4(4f.srem, 3f.srem, 6f.srem, 3f.srem)
            spacing = 1f.srem

            flexContainer {
                direction = FlexDirection.Row
                gap = 10f
                width = FillParent

                linearContainer {
                    orientation = Orientation.Vertical
                    flexRules {
                        grow = 1f
                        basis = 0f
                    }

                    artistText = text {
                        text = "Unknown"
                        font = ResourceManager.getInstance().getFont("torusBold")
                        applyTheme = { color = it.accentColor * 0.8f }
                    }

                    titleText = text {
                        width = FillParent
                        text = "No selected beatmap"
                        font = ResourceManager.getInstance().getFont("torusBoldLarge")
                        applyTheme = { color = it.accentColor }
                        clipToBounds = true
                    }

                    versionText = text {
                        text = "Unknown"
                        applyTheme = { color = it.accentColor * 0.8f }
                    }
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 2f.srem

                starsText = StarRatingBadge().apply {
                    sizeVariant = SizeVariant.Small
                }
                +starsText

                lengthText = badge {
                    leadingIcon = FontAwesomeIcon(Icon.Clock)
                    text = "00:00"
                    sizeVariant = SizeVariant.Small
                }

                bpmText = badge {
                    leadingIcon = FontAwesomeIcon(Icon.HeartPulse)
                    text = "0"
                    sizeVariant = SizeVariant.Small
                }
            }

            linearContainer {
                orientation = Orientation.Vertical
                spacing = 1f.srem

                linearContainer {
                    orientation = Orientation.Horizontal
                    spacing = 1f.srem

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
                    spacing = 1f.srem

                    arText = labeledBadge {
                        label = "AR"
                        value = "00.0"
                        sizeVariant = SizeVariant.Small
                    }
                    odText = labeledBadge {
                        label = "OD"
                        value = "00.0"
                        sizeVariant = SizeVariant.Small
                    }
                    csText = labeledBadge {
                        label = "CS"
                        value = "00.0"
                        sizeVariant = SizeVariant.Small
                    }
                    hpText = labeledBadge {
                        label = "HP"
                        value = "00.0"
                        sizeVariant = SizeVariant.Small
                    }
                }
            }
        }

    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        titleText.text = beatmapInfo?.titleText ?: "No selected beatmap"
        artistText.text = beatmapInfo?.artistText ?: "Unknown"
        versionText.text = beatmapInfo?.version ?: "Unknown"
        circlesBadge.value = beatmapInfo?.hitCircleCount?.toString() ?: "0"
        slidersBadge.value = beatmapInfo?.sliderCount?.toString() ?: "0"
        spinnersBadge.value = beatmapInfo?.spinnerCount?.toString() ?: "0"

        setDifficultyStatistics(beatmapInfo)
    }

    /**
     * Change the displayed difficulty statistics.
     */
    fun setDifficultyStatistics(beatmapInfo: BeatmapInfo?) {

        if (beatmapInfo == null) {
            arText.value = "00.0"
            odText.value = "00.0"
            csText.value = "00.0"
            hpText.value = "00.0"
            bpmText.text = "0"
            lengthText.text = "00:00"
            starsText.rating = 0.0
            return
        }

        // Copy the mods to avoid concurrent modification.
        val mods = ModMenu.enabledMods.deepCopy()
        val isPreciseMod = ModPrecise::class in mods
        val totalSpeedMultiplier = mods.values.calculateRate(Double.POSITIVE_INFINITY)

        val difficulty = beatmapInfo.getBeatmapDifficulty()

        difficulty.applyMods(GameMode.Droid, mods.values, true)

        if (isPreciseMod) {
            // Special case for OD. The Precise mod changes the hit window and not the OD itself, but we must
            // map the hit window back to the original hit window for the user to understand the difficulty
            // increase of the mod.
            val greatWindow = PreciseDroidHitWindow(difficulty.od.toDouble()).greatWindow
            difficulty.od = hitWindow300ToOverallDifficulty(greatWindow).toFloat()
        }

        // Round to 2 decimal places.
        // Using difficulty circle size is quite inaccurate here as the real circle size changes
        // depending on the height of the running device, but for the sake of comparison across
        // players, we assume the height of the device to be fixed.
        difficulty.difficultyCS = GameHelper.Round(difficulty.difficultyCS.toDouble(), 2)
        difficulty.ar = GameHelper.Round(difficulty.ar.toDouble(), 2)
        difficulty.od = GameHelper.Round(difficulty.od.toDouble(), 2)
        difficulty.hp = GameHelper.Round(difficulty.hp.toDouble(), 2)

        val minBpm = (beatmapInfo.bpmMin * totalSpeedMultiplier).roundToInt()
        val maxBpm = (beatmapInfo.bpmMax * totalSpeedMultiplier).roundToInt()
        val commonBpm = (beatmapInfo.mostCommonBPM * totalSpeedMultiplier).roundToInt()
        val length = (beatmapInfo.length / totalSpeedMultiplier).toLong()

        val sdf = SimpleDateFormat(if (length > 3600 * 1000) "HH:mm:ss" else "mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT+0")
        lengthText.text = sdf.format(length)

        bpmText.text = if (minBpm == maxBpm) commonBpm.toString() else "$minBpm-$maxBpm ($commonBpm)"

        arText.value = difficulty.ar.toString()
        odText.value = difficulty.od.toString()
        csText.value = difficulty.difficultyCS.toString()
        hpText.value = difficulty.hp.toString()

        setStarRatingDisplay(beatmapInfo.getStarRating().toDouble())
    }

    /**
     * Change the displayed star rating.
     */
    fun setStarRatingDisplay(value: Double) {
        starsText.rating = value
    }

}
