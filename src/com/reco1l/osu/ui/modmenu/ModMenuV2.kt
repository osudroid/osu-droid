package com.reco1l.osu.ui.modmenu

import com.edlplan.framework.easing.*
import com.reco1l.andengine.*
import com.reco1l.andengine.ExtendedEntity.Companion.FitContent
import com.reco1l.andengine.ExtendedEntity.Companion.FitParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.mods.*
import com.rian.osu.utils.ModHashMap
import ru.nsu.ccfit.zuev.osu.ResourceManager

class ModMenuV2 : ExtendedScene() {


    /**
     * List of currently enabled mods.
     */
    val enabledMods = ModHashMap()


    private val modButtons = mutableListOf<ModButton>()

    private val rankedBadge: Badge
    private val scoreMultiplierBadge: StatisticBadge

    private val customizeButton: Button

    private val customizationMenu: ModCustomizationMenu


    init {
        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")
        ResourceManager.getInstance().loadHighQualityAsset("tune", "tune.png")
        ResourceManager.getInstance().loadHighQualityAsset("backspace", "backspace.png")
        ResourceManager.getInstance().loadHighQualityAsset("search", "search.png")

        customizationMenu = ModCustomizationMenu()

        attachChild(ScrollableContainer().apply {
            width = FitParent
            height = FitParent
            scrollAxes = Axes.X
            padding = Vec4(0f, 80f, 0f, 0f)
            background = Box().apply {
                color = ColorARGB(0xFF161622)
                alpha = 0.5f
            }

            attachChild(LinearContainer().apply {
                orientation = Orientation.Horizontal
                width = FitContent
                height = FitParent
                spacing = 16f
                padding = Vec4(60f, 20f)

                attachChild(Section("Difficulty Reduction", arrayOf(
                    ModEasy(),
                    ModNoFail(),
                    ModHalfTime(),
                    ModReallyEasy()
                )))

                attachChild(Section("Difficulty Increase", arrayOf(
                    ModHardRock(),
                    ModDoubleTime(),
                    ModNightCore(),
                    ModHidden(),
                    ModTraceable(),
                    ModFlashlight(),
                    ModSuddenDeath(),
                    ModPerfect(),
                    ModPrecise()
                )))

                attachChild(Section("Automation", arrayOf(
                    ModRelax(),
                    ModAutopilot(),
                    ModAuto()
                )))

                attachChild(Section("Conversion", arrayOf(
                    ModCustomSpeed(),
                    ModDifficultyAdjust(),
                    ModScoreV2()
                )))
            })
        })


        attachChild(Container().apply {
            width = FitParent
            height = 60f
            padding = Vec4(60f, 20f, 60f, 0f)

            attachChild(LinearContainer().apply {
                orientation = Orientation.Horizontal
                height = FitParent
                spacing = 10f

                attachChild(Button().apply {
                    text = "Back"
                    leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("back-arrow"))
                })

                customizeButton = Button().apply {
                    text = "Customize"
                    isEnabled = false
                    leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("tune"))
                    onActionUp = {
                        if (customizationMenu.isVisible) {
                            customizationMenu.hide()
                        } else {
                            customizationMenu.show()
                        }
                    }
                }
                attachChild(customizeButton)

                attachChild(Button().apply {
                    text = "Clear all mods"
                    leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("backspace"))
                    theme = ButtonTheme(
                        backgroundColor = 0xFF342121,
                        textColor = 0xFFFFBFBF,
                    )

                    onActionUp = {
                        enabledMods.toList().fastForEach { removeMod(it.second) }
                    }
                })
            })

            attachChild(LinearContainer().apply {
                orientation = Orientation.Horizontal
                height = FitParent
                spacing = 10f
                anchor = Anchor.CenterRight
                origin = Anchor.CenterRight
                padding = Vec4(0f, 6f)

                rankedBadge = Badge("Ranked").apply {
                    background!!.color = ColorARGB(0xFF83DF6B)
                    color = ColorARGB(0xFF161622)
                }
                attachChild(rankedBadge)

                scoreMultiplierBadge = StatisticBadge("Score multiplier", "1.00x")
                attachChild(scoreMultiplierBadge)
            })
        })

        // Customizations menu
        attachChild(customizationMenu)
    }


    private fun onModsChanged() {

        rankedBadge.clearEntityModifiers()
        rankedBadge.background!!.clearEntityModifiers()

        if (enabledMods.isEmpty() || enabledMods.none { !it.value.isRanked }) {
            rankedBadge.text = "Ranked"
            rankedBadge.colorTo(0xFF161622, 0.1f)
            rankedBadge.background!!.colorTo(0xFF83DF6B, 0.1f)
        } else {
            rankedBadge.text = "Unranked"
            rankedBadge.colorTo(0xFFFFFFFF, 0.1f)
            rankedBadge.background!!.colorTo(0xFF1E1E2E, 0.1f)
        }

        customizeButton.isEnabled = !customizationMenu.isSelectorEmpty()
    }

    private fun addMod(mod: Mod) {

        if (mod in enabledMods) {
            return
        }
        enabledMods.put(mod)

        modButtons.fastForEach { button ->

            if (button.mod == mod) {
                button.isSelected = true
            } else if (button.mod::class in mod.incompatibleMods) {
                button.isSelected = false
                enabledMods.remove(button.mod)
                customizationMenu.onModRemoved(button.mod)
            }
        }

        customizationMenu.onModAdded(mod)
        onModsChanged()
    }

    private fun removeMod(mod: Mod) {

        if (mod !in enabledMods) {
            return
        }
        enabledMods.remove(mod)
        modButtons.find { it.mod == mod }?.isSelected = false

        customizationMenu.onModRemoved(mod)

        onModsChanged()
    }


    //region Components

    private inner class Section(name: String, mods: Array<Mod>) : ScrollableContainer() {
        init {
            width = 340f
            height = FitParent
            scrollAxes = Axes.Y
            clipChildren = true

            background = RoundedBox().apply {
                color = ColorARGB(0xFF13131E)
                cornerRadius = 16f
            }

            attachChild(LinearContainer().apply {
                width = FitParent
                height = FitContent
                orientation = Orientation.Vertical
                padding = Vec4(12f)
                spacing = 16f

                attachChild(ExtendedText().apply {
                    width = FitParent
                    text = name.uppercase()
                    alignment = Anchor.CenterLeft
                    font = ResourceManager.getInstance().getFont("smallFont")
                    padding = Vec4(0f, 20f, 0f, 10f)
                    color = ColorARGB(0xFF8282A8)
                })

                mods.fastForEachIndexed { i, mod ->
                    attachChild(ModButton(mod).apply {

                        beginSequence {
                            translateToY(50f * (i + 1))
                            delay(0.1f + 0.05f * i)
                            translateToY(0f, 0.5f, Easing.OutExpo)
                        }

                        modButtons.add(this)
                    })
                }
            })
        }
    }

    private inner class ModButton(val mod: Mod): Button() {

        init {
            width = FitParent
            theme = MOD_BUTTON_THEME
            text = mod.name
            leadingIcon = ModIcon(mod)
            padding = Vec4(20f, 8f)

            onActionUp = {
                if (isSelected) {
                    removeMod(mod)
                } else {
                    addMod(mod)
                }
            }
        }
    }

    companion object {
        private val MOD_BUTTON_THEME = ButtonTheme(
            iconSize = 40f,
            backgroundColor = 0xFF1E1E2E
        )
    }

    //endregion

}


