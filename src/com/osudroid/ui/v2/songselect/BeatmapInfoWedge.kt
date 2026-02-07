package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.osudroid.ui.*
import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.*
import com.rian.osu.*
import com.rian.osu.beatmap.*
import com.rian.osu.beatmap.DroidHitWindow.Companion.hitWindow300ToOverallDifficulty
import com.rian.osu.mods.*
import com.rian.osu.utils.ModUtils.applyModsToBeatmapDifficulty
import com.rian.osu.utils.ModUtils.calculateRateWithMods
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.*
import java.text.*
import java.util.*

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

    private lateinit var starsText: CompoundText
    private lateinit var versionText: UIText


    init {
        width = Size.Full
        translationX = -2f
        translationY = -2f

        +UIWedge().apply {
            style = {
                color = it.accentColor * 0.1f
                alpha = 0.75f
            }
        }

        linearContainer {
            width = Size.Full
            orientation = Orientation.Vertical
            padding = Vec4(60f, 18f, 50f, 18f)
            spacing = 6f

            fillContainer {
                orientation = Orientation.Horizontal
                spacing = 10f
                width = Size.Full

                linearContainer {
                    width = Size.Full
                    orientation = Orientation.Vertical

                    artistText = text {
                        text = "Unknown"
                        style = { color = it.accentColor * 0.9f}
                    }

                    titleText = text {
                        width = Size.Full
                        text = "No selected beatmap"
                        style = { color = it.accentColor }
                        clipToBounds = true
                    }

                    versionText = text {
                        text = "Unknown"
                        style = { color = it.accentColor * 0.8f}
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
                    style = { color = it.accentColor }
                }

                bpmText = compoundText {
                    leadingIcon = UISprite(ResourceManager.getInstance().getTexture("bpm"))
                    text = "0"
                    style = { color = it.accentColor }
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

        +UIWedge().apply {
            paintStyle = PaintStyle.Outline
            lineWidth = 4f
            style = {
                color = it.accentColor * 0.2f
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
            starsText.text = "0.00"
            bpmText.text = "0"
            lengthText.text = "00:00"
            return
        }

        val mods = ModMenu.enabledMods
        val isPreciseMod = ModPrecise::class in mods
        val totalSpeedMultiplier = calculateRateWithMods(mods.values, Double.POSITIVE_INFINITY)

        val difficulty = beatmapInfo.getBeatmapDifficulty()

        applyModsToBeatmapDifficulty(difficulty, GameMode.Droid, mods.values, true)

        if (isPreciseMod) {
            // Special case for OD. The Precise mod changes the hit window and not the OD itself, but we must
            // map the hit window back to the original hit window for the user to understand the difficulty
            // increase of the mod.
            val greatWindow = PreciseDroidHitWindow(difficulty.od).greatWindow
            difficulty.od = hitWindow300ToOverallDifficulty(greatWindow)
        }

        // Round to 2 decimal places.
        // Using difficulty circle size is quite inaccurate here as the real circle size changes
        // depending on the height of the running device, but for the sake of comparison across
        // players, we assume the height of the device to be fixed.
        difficulty.difficultyCS = GameHelper.Round(difficulty.difficultyCS.toDouble(), 2)
        difficulty.ar = GameHelper.Round(difficulty.ar.toDouble(), 2)
        difficulty.od = GameHelper.Round(difficulty.od.toDouble(), 2)
        difficulty.hp = GameHelper.Round(difficulty.hp.toDouble(), 2)

        val minBpm = Math.round(beatmapInfo.bpmMin * totalSpeedMultiplier)
        val maxBpm = Math.round(beatmapInfo.bpmMax * totalSpeedMultiplier)
        val commonBpm = Math.round(beatmapInfo.mostCommonBPM * totalSpeedMultiplier)
        val length = (beatmapInfo.length / totalSpeedMultiplier).toLong()

        val sdf = SimpleDateFormat(if (length > 3600 * 1000) "HH:mm:ss" else "mm:ss")
        sdf.timeZone = TimeZone.getTimeZone("GMT+0")
        lengthText.text = sdf.format(length)

        bpmText.text = (if (minBpm == maxBpm) commonBpm.toString() else "$minBpm-$maxBpm ($commonBpm)")

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
        starsText.apply {
            text = value.roundBy(2).toString()
            color = if (value >= 6.5) Color4(0xFFFFD966) else Color4.Black.copy(alpha = 0.75f)
            backgroundColor = OsuColors.getStarRatingColor(value)
        }
    }


    companion object {
        init {
            ResourceManager.getInstance().loadHighQualityAsset("clock", "clock.png")
            ResourceManager.getInstance().loadHighQualityAsset("bpm", "bpm.png")
            ResourceManager.getInstance().loadHighQualityAsset("star", "star.png")
        }
    }
}

