package com.reco1l.osu.ui.modmenu

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
import com.reco1l.ibancho.RoomAPI.setPlayerMods
import com.reco1l.ibancho.RoomAPI.setRoomMods
import com.reco1l.osu.multiplayer.*
import com.reco1l.toolkt.kotlin.*
import com.rian.osu.*
import com.rian.osu.beatmap.parser.*
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.rian.osu.mods.*
import com.rian.osu.utils.*
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.*
import ru.nsu.ccfit.zuev.osu.helper.StringTable
import java.util.concurrent.CancellationException

object ModMenuV2 : ExtendedScene() {


    /**
     * List of currently enabled mods.
     */
    val enabledMods = ModHashMap()

    /**
     * The job used to calculate the score multiplier.
     */
    var calculationJob: Job? = null
        private set


    private val modButtons = mutableListOf<ModButton>()

    private val rankedBadge: Badge

    private val scoreMultiplierBadge: StatisticBadge

    private val customizeButton: Button

    private val customizationMenu: ModCustomizationMenu


    init {
        isBackgroundEnabled = false

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
                alpha = 0.95f
            }

            attachChild(LinearContainer().apply {
                orientation = Orientation.Horizontal
                width = FitContent
                height = FitParent
                spacing = 16f
                padding = Vec4(60f, 20f)

                val mods = ModUtils.allModsInstances

                ModType.entries.forEach { type ->
                    val sectionName = StringTable.get(type.stringId)
                    val sectionMods = mods.filter { it.type == type }
                    attachChild(Section(sectionName, sectionMods))
                }
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
                    onActionUp = {
                        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                        calculateStarRating()
                        back()
                    }
                    onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                })

                customizeButton = Button().apply {
                    text = "Customize"
                    isEnabled = false
                    leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("tune"))
                    onActionUp = {
                        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                        if (customizationMenu.isVisible) {
                            customizationMenu.hide()
                        } else {
                            customizationMenu.show()
                        }
                    }
                    onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
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
                        ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                        clear()
                    }
                    onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
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


    //region Calculation

    fun cancelCalculationJob() {
        calculationJob?.cancel(CancellationException("Difficulty calculation has been cancelled."))
        calculationJob = null
    }

    private fun calculateStarRating() {
        cancelCalculationJob()

        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap

        calculationJob = async scope@{

            if (selectedBeatmap == null) {
                return@scope
            }

            val difficultyAlgorithm = Config.getDifficultyAlgorithm()

            BeatmapParser(selectedBeatmap.path, this@scope).use { parser ->

                val beatmap = parser.parse(
                    withHitObjects = true,
                    mode = if (difficultyAlgorithm == DifficultyAlgorithm.droid) GameMode.Droid else GameMode.Standard
                )

                if (beatmap == null) {
                    GlobalManager.getInstance().songMenu.setStarsDisplay(0f)
                    return@scope
                }

                // Copy the mods to avoid concurrent modification
                val mods = enabledMods.deepCopy().values

                when (difficultyAlgorithm) {

                    DifficultyAlgorithm.droid -> {
                        val attributes = calculateDroidDifficulty(beatmap, mods, this@scope)
                        GlobalManager.getInstance().songMenu.setStarsDisplay(GameHelper.Round(attributes.starRating, 2))
                    }

                    DifficultyAlgorithm.standard -> {
                        val attributes = calculateStandardDifficulty(beatmap, mods, this@scope)
                        GlobalManager.getInstance().songMenu.setStarsDisplay(GameHelper.Round(attributes.starRating, 2))
                    }
                }
            }
        }
    }

    //endregion

    //region Visibility

    fun show() {
        GlobalManager.getInstance().engine.scene.setChildScene(
            this,
            false,
            true,
            true
        )
    }

    override fun back() {
        back(true)
    }

    fun back(updatePlayerMods: Boolean) {

        if (Multiplayer.isConnected) {
            RoomScene.isWaitingForModsChange = true

            val string = enabledMods.toString()

            // The room mods are the same as the host mods
            if (Multiplayer.isRoomHost) {
                setRoomMods(string)
            } else if (updatePlayerMods) {
                setPlayerMods(string)
            } else {
                RoomScene.isWaitingForModsChange = false
            }
        }

        super.back()
    }

    //endregion

    //region Mods

    fun clear() {
        cancelCalculationJob()
        enabledMods.toList().fastForEach { removeMod(it.second) }
    }

    fun onModsChanged(lastChangedMod: Mod) {

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

        val difficulty = GlobalManager.getInstance().selectedBeatmap?.getBeatmapDifficulty()

        if (difficulty != null) {
            scoreMultiplierBadge.value = "%.2fx".format(
                enabledMods.values.fold(1f) { acc, mod ->
                    acc * mod.calculateScoreMultiplier(difficulty)
                }
            )
        } else {
            scoreMultiplierBadge.value = "1.00x"
        }

        customizeButton.isEnabled = !customizationMenu.isSelectorEmpty()

        if (lastChangedMod is ModRateAdjust) {
            GlobalManager.getInstance().songMenu.updateMusicEffects()
        }
    }

    private fun addMod(mod: Mod) {

        if (mod in enabledMods) {
            return
        }
        enabledMods.put(mod)

        modButtons.fastForEach { button ->

            val wasSelected = button.isSelected
            button.isSelected = button.mod in enabledMods

            // Handle incompatible mods with the selected mod.
            if (wasSelected && !button.isSelected) {
                customizationMenu.onModRemoved(button.mod)
            }
        }

        customizationMenu.onModAdded(mod)
        onModsChanged(mod)
    }

    private fun removeMod(mod: Mod) {

        if (mod !in enabledMods) {
            return
        }
        enabledMods.remove(mod)
        modButtons.find { it.mod == mod }?.isSelected = false

        customizationMenu.onModRemoved(mod)
        onModsChanged(mod)
    }

    //endregion

    //region Components

    private class Section(name: String, mods: List<Mod>) : ScrollableContainer() {

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

                mods.fastForEach { mod ->
                    val button = ModButton(mod)
                    modButtons.add(button)
                    attachChild(button)
                }
            })
        }
    }

    private class ModButton(val mod: Mod): Button() {

        init {
            width = FitParent
            theme = ButtonTheme(
                iconSize = 40f,
                backgroundColor = 0xFF1E1E2E
            )
            text = mod.name
            leadingIcon = ModIcon(mod)
            padding = Vec4(20f, 8f)

            onActionUp = {
                if (isSelected) {
                    removeMod(mod)
                    ResourceManager.getInstance().getSound("check-off")?.play()
                } else {
                    addMod(mod)
                    ResourceManager.getInstance().getSound("check-on")?.play()
                }
            }
        }
    }

    //endregion

}


