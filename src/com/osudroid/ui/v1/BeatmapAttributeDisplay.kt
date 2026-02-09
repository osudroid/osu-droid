package com.osudroid.ui.v1

import com.osudroid.ui.OsuColors
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.UIScene
import com.reco1l.andengine.container.Orientation
import com.reco1l.andengine.container.UILinearContainer
import com.reco1l.andengine.container.UIScrollableContainer
import com.reco1l.andengine.text.UIText
import com.reco1l.andengine.theme.FontSize
import com.reco1l.andengine.theme.pct
import com.reco1l.andengine.ui.UIModal
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec4
import com.reco1l.toolkt.kotlin.fastForEach
import com.reco1l.toolkt.roundBy
import com.rian.osu.GameMode
import com.rian.osu.beatmap.DroidHitWindow
import com.rian.osu.beatmap.HitWindow
import com.rian.osu.beatmap.PreciseDroidHitWindow
import com.rian.osu.beatmap.hitobject.HitObject
import com.rian.osu.beatmap.sections.BeatmapDifficulty
import com.rian.osu.mods.Mod
import com.rian.osu.mods.ModPrecise
import com.rian.osu.utils.CircleSizeCalculator
import com.rian.osu.utils.ModUtils
import kotlin.math.roundToInt
import ru.nsu.ccfit.zuev.osu.GlobalManager

/**
 * A [UIScene] containing a [UIModal] that shows the attributes of a beatmap, adjusted for the given mods.
 *
 * @param difficulty The base [BeatmapDifficulty] of the beatmap.
 * @param mods The [Mod]s to adjust the [BeatmapDifficulty] with.
 */
open class BeatmapAttributeDisplay(difficulty: BeatmapDifficulty, mods: Iterable<Mod>) : UIScene() {
    private val modal = BeatmapAttributeModal()
    private val modalCard: UILinearContainer = modal.card[0]!!

    init {
        isBackgroundEnabled = false

        attachChild(modal)

        val nonRateAdjustedDifficulty = difficulty.clone()
        val rateAdjustedDifficulty = difficulty.clone()

        ModUtils.applyModsToBeatmapDifficulty(nonRateAdjustedDifficulty, GameMode.Droid, mods, false)
        ModUtils.applyModsToBeatmapDifficulty(rateAdjustedDifficulty, GameMode.Droid, mods, true)

        // CS

        addAttribute(
            "CS (Circle Size)",
            "Affects the size of hit circles and sliders.",
            difficulty.difficultyCS,
            rateAdjustedDifficulty.difficultyCS,
            arrayOf(
                Metric(
                    "Hit circle radius",
                    (HitObject.OBJECT_RADIUS * CircleSizeCalculator.droidCSToDroidScale(rateAdjustedDifficulty.difficultyCS))
                        .roundBy(1)
                        .toString()
                )
            )
        )

        // AR

        val approachTime = BeatmapDifficulty.difficultyRangeInt(
            rateAdjustedDifficulty.ar.toDouble(),
            HitObject.PREEMPT_MAX,
            HitObject.PREEMPT_MID,
            HitObject.PREEMPT_MIN
        )

        addAttribute(
            "AR (Approach Rate)",
            "Affects how early objects appear on screen relative to their hit time.",
            difficulty.ar,
            rateAdjustedDifficulty.ar,
            arrayOf(Metric("Approach time", "${approachTime}ms"))
        )

        // OD
        // For OD, we want to display rate-affected hit window values in a different way, because they are not
        // accurately represented by the OD value itself.
        val isPrecise = mods.any { it is ModPrecise }
        val rate = ModUtils.calculateRateWithMods(mods, Double.POSITIVE_INFINITY)

        val hitWindow =
            if (isPrecise) PreciseDroidHitWindow(nonRateAdjustedDifficulty.od)
            else DroidHitWindow(nonRateAdjustedDifficulty.od)

        addAttribute(
            "OD (Overall Difficulty)",
            "Affects timing requirements for hit circles and spin speed requirements for spinners.",
            difficulty.od,
            rateAdjustedDifficulty.od,
            arrayOf(
                Metric("GREAT hit window", "±${(hitWindow.greatWindow / rate).roundBy(2)}ms", OsuColors.blue),
                Metric("OK hit window", "±${(hitWindow.okWindow / rate).roundBy(2)}ms", OsuColors.green),
                Metric("MEH hit window", "±${(hitWindow.mehWindow / rate).roundBy(2)}ms", OsuColors.yellow),
                Metric("MISS hit window", "±${(HitWindow.MISS_WINDOW / rate).roundBy(2)}ms", OsuColors.red),
                Metric("RPM required to clear spinners", (2 + 12 * nonRateAdjustedDifficulty.od).roundToInt().toString())
            ),
            if (isPrecise) arrayOf("Hit windows are being adjusted by the Precise mod.") else emptyArray()
        )

        // HP

        addAttribute(
            "HP (Health Drain)",
            "Affects the harshness of health drain and the health penalties for missing.",
            difficulty.hp,
            rateAdjustedDifficulty.hp
        )
    }

    override fun show() {
        GlobalManager.getInstance().engine.scene.setChildScene(this, false, false, true)

        modal.show()
    }

    private fun addAttribute(name: String, description: String, originalValue: Float, adjustedValue: Float,
                             metrics: Array<Metric> = emptyArray(), additionalInfo: Array<String> = emptyArray()) {
        modalCard += UILinearContainer().apply {
            orientation = Orientation.Vertical
            spacing = 10f

            +UILinearContainer().apply {
                orientation = Orientation.Vertical
                spacing = 5f

                +UIText().apply {
                    
                    text = name
                    style = { color = it.accentColor }
                }

                +UIText().apply {
                    fontSize = FontSize.SM
                    text = description
                    style = { color = it.accentColor }
                }
            }

            +UILinearContainer().apply {
                orientation = Orientation.Vertical

                metrics.fastForEach { metric ->
                    +UIText().apply {
                        fontSize = FontSize.SM
                        text = "${metric.name}: ${metric.value}"
                        style = { color = metric.color ?: (it.accentColor * 0.8f) }
                    }
                }
            }

            if (originalValue != adjustedValue || additionalInfo.isNotEmpty()) {
                +UILinearContainer().apply {
                    orientation = Orientation.Vertical

                    if (originalValue != adjustedValue) {
                        +UIText().apply {
                            fontSize = FontSize.SM
                            text = "This value is being adjusted by mods (${originalValue.roundBy(2)} ➜ ${adjustedValue.roundBy(2)})."
                            style = { color = it.accentColor * 0.6f }
                        }
                    }

                    additionalInfo.fastForEach { info ->
                        +UIText().apply {
                            fontSize = FontSize.SM
                            text = info
                            style = { color = it.accentColor * 0.6f }
                        }
                    }
                }
            }
        }
    }

    private inner class BeatmapAttributeModal : UIModal(
        UIScrollableContainer().apply {
            scrollAxes = Axes.Y
            width = 0.8f.pct
            height = 0.75f.pct
            anchor = Anchor.Center
            origin = Anchor.Center
            clipToBounds = true

            +UILinearContainer().apply {
                orientation = Orientation.Vertical
                padding = Vec4(20f)
                spacing = 25f
            }
        }
    ) {
        override fun onHidden() {
            super.onHidden()
            back()
        }
    }

    private data class Metric(val name: String, val value: String, val color: Color4? = null)
}