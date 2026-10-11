package com.osudroid.ui.v2.songselect

import com.osudroid.GameMode
import com.osudroid.beatmaps.DroidHitWindow.Companion.hitWindow300ToOverallDifficulty
import com.osudroid.beatmaps.PreciseDroidHitWindow
import com.osudroid.data.BeatmapInfo
import com.osudroid.math.Precision
import com.osudroid.mods.ModDifficultyAdjust
import com.osudroid.mods.ModPrecise
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.ui.v2.modmenu.ModMenu
import com.osudroid.utils.applyMods
import com.osudroid.utils.calculateRate
import com.reco1l.andengine.*
import com.reco1l.andengine.component.UIComponent
import com.reco1l.framework.Color4
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

    private lateinit var circlesText: UIText
    private lateinit var slidersText: UIText
    private lateinit var spinnersText: UIText

    private lateinit var starsText: StarRatingBadge

    /**
     * Colors of statistics changed by mods, like the legacy song menu, red when harder, green when easier,
     * orange when set by Difficulty Adjust. Components without an entry use the theme's accent color.
     */
    private val statisticColors = mutableMapOf<UIComponent, Color4>()
    private lateinit var versionText: UIText


    init {
        width = FillParent

        // The shapes are background/foreground so they take the container's size without
        // being part of the content measurement (a FillParent child would keep the container from shrinking).
        background = UIBox().apply {
            alpha = 0.75f
            cornerRadius = 0.75f.rem
            applyTheme = { color = it.accentColor * 0.1f }
        }

        foreground = UIBox().apply {
            paintStyle = PaintStyle.Outline
            lineWidth = 2f
            cornerRadius = 0.75f.rem
            applyTheme = { color = it.accentColor * 0.2f }
        }

        linearContainer {
            width = FillParent
            orientation = Orientation.Vertical
            padding = Vec4(3f.srem)
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

                // Star rating at the top right, next to the title block.
                starsText = StarRatingBadge()
                +starsText
            }

            // Length and BPM right below the title, the object counts set apart from them by a wider gap.
            // The difficulty values have a row of their own.
            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 8f.srem

                linearContainer {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    spacing = 4f.srem

                    lengthText = compoundText {
                        leadingIcon = FontAwesomeIcon(Icon.Clock).apply { iconSize = STAT_ICON_SIZE }
                        autoSizeLeadingIcon = Axes.None
                        text = "00:00"
                        font = ResourceManager.getInstance().getFont("torusBoldSm")
                        spacing = 1f.srem
                    }

                    bpmText = compoundText {
                        leadingIcon = FontAwesomeIcon(Icon.HeartPulse).apply { iconSize = STAT_ICON_SIZE }
                        autoSizeLeadingIcon = Axes.None
                        text = "0"
                        font = ResourceManager.getInstance().getFont("torusBoldSm")
                        spacing = 1f.srem
                    }
                }

                linearContainer {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    spacing = 3f.srem

                    circlesText = objectCount("Circles")
                    slidersText = objectCount("Sliders")
                    spinnersText = objectCount("Spinners")
                }
            }

            linearContainer {
                orientation = Orientation.Horizontal
                spacing = 1f.srem

                arText = difficultyBadge("AR")
                odText = difficultyBadge("OD")
                csText = difficultyBadge("CS")
                hpText = difficultyBadge("HP")
            }
        }

        lengthText.useStatisticColor()
        bpmText.useStatisticColor()
        for (badge in listOf(arText, odText, csText, hpText)) {
            badge.valueEntity.useStatisticColor()
        }
    }


    private fun UILinearContainer.difficultyBadge(label: String) = labeledBadge {
        this.label = label
        value = "00.0"
        sizeVariant = SizeVariant.Small
    }

    /**
     * Adds an object count as a plain label and value, without the badge background of the difficulty values.
     */
    private fun UILinearContainer.objectCount(label: String): UIText {
        lateinit var valueText: UIText

        linearContainer {
            orientation = Orientation.Horizontal
            spacing = 2f.srem

            text {
                text = label
                font = ResourceManager.getInstance().getFont("torusBoldSm")
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                applyTheme = { color = it.accentColor * 0.7f }
            }

            valueText = text {
                text = "0"
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                font = ResourceManager.getInstance().getFont("torusBoldSm")
                applyTheme = { color = it.accentColor }
            }
        }

        return valueText
    }


    fun onBeatmapSelected(beatmapInfo: BeatmapInfo?) {
        titleText.text = beatmapInfo?.titleText ?: "No selected beatmap"
        artistText.text = beatmapInfo?.artistText ?: "Unknown"
        versionText.text = beatmapInfo?.version ?: "Unknown"
        circlesText.text = beatmapInfo?.hitCircleCount?.toString() ?: "0"
        slidersText.text = beatmapInfo?.sliderCount?.toString() ?: "0"
        spinnersText.text = beatmapInfo?.spinnerCount?.toString() ?: "0"

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

        val isDifficultyAdjusted = ModDifficultyAdjust::class in mods

        setStatisticColor(arText.valueEntity, compareColor(beatmapInfo.approachRate, difficulty.ar, isDifficultyAdjusted, isPreciseMod))
        setStatisticColor(odText.valueEntity, compareColor(beatmapInfo.overallDifficulty, difficulty.od, isDifficultyAdjusted, isPreciseMod))
        setStatisticColor(csText.valueEntity, compareColor(beatmapInfo.circleSize, difficulty.difficultyCS, isDifficultyAdjusted, isPreciseMod))
        setStatisticColor(hpText.valueEntity, compareColor(beatmapInfo.hpDrainRate, difficulty.hp, isDifficultyAdjusted, isPreciseMod))

        // A faster rate makes the map harder: shorter length, higher BPM.
        val rateColor = compareColor(1f, totalSpeedMultiplier, false, false)
        setStatisticColor(lengthText, rateColor)
        setStatisticColor(bpmText, rateColor)

        setStarRatingDisplay(beatmapInfo.getStarRating().toDouble())
    }

    /**
     * Precise uses its own, stronger color variants, like the legacy song menu.
     */
    private fun compareColor(original: Float, modified: Float, isDifficultyAdjusted: Boolean, isPrecise: Boolean): Color4? = when {
        Precision.almostEquals(original, modified) -> null
        isDifficultyAdjusted -> if (isPrecise) PRECISE_DIFFICULTY_ADJUSTED_COLOR else DIFFICULTY_ADJUSTED_COLOR
        modified > original -> if (isPrecise) PRECISE_HARDER_COLOR else HARDER_COLOR
        else -> if (isPrecise) PRECISE_EASIER_COLOR else EASIER_COLOR
    }

    private fun setStatisticColor(component: UIComponent, color: Color4?) {
        if (color == null) {
            statisticColors.remove(component)
        } else {
            statisticColors[component] = color
        }
        component.onThemeChanged(Theme.current)
    }

    /**
     * Makes the component take its color from [statisticColors], falling back to the theme's accent color.
     * Used for the values of the difficulty badges and for the length and BPM.
     */
    private fun UIComponent.useStatisticColor() {
        inheritAncestorsColor = false
        applyTheme = { theme -> color = statisticColors[this] ?: theme.accentColor }
    }

    /**
     * Change the displayed star rating.
     */
    fun setStarRatingDisplay(value: Double) {
        starsText.rating = value
    }


    companion object {
        /**
         * Smaller than the text, so the filled icons do not outweigh the values next to them.
         */
        private const val STAT_ICON_SIZE = 15f

        private val HARDER_COLOR = Color4(205, 85, 85)
        private val EASIER_COLOR = Color4(46, 139, 87)
        private val DIFFICULTY_ADJUSTED_COLOR = Color4(255, 180, 0)
        private val PRECISE_HARDER_COLOR = Color4(214, 45, 45)
        private val PRECISE_EASIER_COLOR = Color4(158, 108, 65)
        private val PRECISE_DIFFICULTY_ADJUSTED_COLOR = Color4(255, 120, 0)
    }
}
