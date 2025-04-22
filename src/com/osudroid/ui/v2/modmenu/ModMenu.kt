package com.osudroid.ui.v2.modmenu

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
import com.osudroid.multiplayer.api.RoomAPI.setPlayerMods
import com.osudroid.multiplayer.api.RoomAPI.setRoomMods
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.RoomScene
import com.osudroid.ui.OsuColors
import com.reco1l.osu.*
import com.osudroid.ui.v1.SettingsFragment
import com.reco1l.toolkt.kotlin.*
import com.reco1l.toolkt.kotlin.async
import com.rian.osu.*
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.parser.*
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.rian.osu.difficulty.attributes.*
import com.rian.osu.mods.*
import com.rian.osu.utils.*
import com.rian.osu.utils.ModUtils
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.*
import ru.nsu.ccfit.zuev.osu.game.*
import ru.nsu.ccfit.zuev.osu.helper.*
import java.util.concurrent.CancellationException
import kotlin.math.*

object ModMenu : ExtendedScene() {


    /**
     * List of currently enabled mods.
     */
    val enabledMods = ModHashMap()

    private var parsedBeatmap: Beatmap? = null

    private val modButtons = mutableListOf<ModButton>()

    private val customizeButton: Button
    private val customizationMenu: ModCustomizationMenu

    private val rankedBadge: Badge
    private val arBadge: LabeledBadge
    private val odBadge: LabeledBadge
    private val csBadge: LabeledBadge
    private val hpBadge: LabeledBadge
    private val bpmBadge: LabeledBadge
    private val starRatingBadge: LabeledBadge
    private val scoreMultiplierBadge: LabeledBadge


    private var calculationJob: Job? = null


    init {
        isBackgroundEnabled = false

        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")
        ResourceManager.getInstance().loadHighQualityAsset("tune", "tune.png")
        ResourceManager.getInstance().loadHighQualityAsset("backspace", "backspace.png")
        ResourceManager.getInstance().loadHighQualityAsset("search", "search.png")
        ResourceManager.getInstance().loadHighQualityAsset("settings", "settings.png")

        customizationMenu = ModCustomizationMenu()

        attachChild(LinearContainer().apply {
            width = FitParent
            height = FitParent
            orientation = Orientation.Vertical
            background = Box().apply {
                color = ColorARGB(0xFF161622)
                alpha = 0.95f
            }

            attachChild(Container().apply {
                width = FitParent
                height = FitContent
                padding = Vec4(60f, 20f)

                attachChild(LinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    height = FitContent
                    spacing = 10f

                    attachChild(Button().apply {
                        text = "Back"
                        leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("back-arrow"))
                        onActionUp = {
                            ResourceManager.getInstance().getSound("click-short-confirm")?.play()
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
                        text = "Clear"
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

                    +Button().apply {
                        leadingIcon = ExtendedSprite(ResourceManager.getInstance().getTexture("settings"))
                        spacing = 0f
                        onActionUp = {
                            ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                            mainThread { SettingsFragment().show() }
                        }
                        onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    }
                })

                attachChild(LinearContainer().apply {
                    orientation = Orientation.Vertical
                    spacing = 10f
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight

                    +LinearContainer().apply {
                        orientation = Orientation.Horizontal
                        anchor = Anchor.TopRight
                        origin = Anchor.TopRight
                        spacing = 10f

                        +LabeledBadge("Score multiplier", "1.00x").apply { scoreMultiplierBadge = this }
                        +LabeledBadge("Star rating", "0.0").apply { starRatingBadge = this }

                        +Badge("Ranked").apply {
                            background!!.color = ColorARGB(0xFF83DF6B)
                            color = ColorARGB(0xFF161622)
                            rankedBadge = this
                        }
                    }

                    +LinearContainer().apply {
                        orientation = Orientation.Horizontal
                        origin = Anchor.TopRight
                        anchor = Anchor.TopRight
                        spacing = 10f

                        +LabeledBadge("AR", "0.00").apply { arBadge = this }
                        +LabeledBadge("OD", "0.00").apply { odBadge = this }
                        +LabeledBadge("CS", "0.00").apply { csBadge = this }
                        +LabeledBadge("HP", "0.00").apply { hpBadge = this }
                        +LabeledBadge("BPM", "0.0").apply { bpmBadge = this }
                    }
                })
            })

            attachChild(ScrollableContainer().apply {
                width = FitParent
                height = FitParent
                scrollAxes = Axes.X

                attachChild(LinearContainer().apply {
                    orientation = Orientation.Horizontal
                    width = FitContent
                    height = FitParent
                    spacing = 16f
                    padding = Vec4(60f, 0f)

                    val mods = ModUtils.allModsInstances

                    ModType.entries.forEach { type ->
                        val sectionName = StringTable.get(type.stringId)
                        val sectionMods = mods.filter { it !is IMigratableMod && it.type == type }

                        if (sectionMods.isNotEmpty()) {
                            +Section(sectionName, sectionMods)
                        }
                    }
                })
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

    private fun parseBeatmap() {
        cancelCalculationJob()

        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap

        calculationJob = async scope@{

            if (selectedBeatmap == null) {
                return@scope
            }

            val difficultyAlgorithm = Config.getDifficultyAlgorithm()
            val gameMode = if (difficultyAlgorithm == droid) GameMode.Droid else GameMode.Standard
            val beatmap: Beatmap?

            if (parsedBeatmap?.md5 != selectedBeatmap.md5 || parsedBeatmap?.mode != gameMode) {
                 BeatmapParser(selectedBeatmap.path, this@scope).use { parser ->
                     beatmap = parser.parse(withHitObjects = true, mode = gameMode)
                     parsedBeatmap = beatmap
                 }
            } else {
                beatmap = parsedBeatmap
            }

            val songMenu = GlobalManager.getInstance().songMenu

            if (beatmap == null) {
                songMenu.setStarsDisplay(0f)
                return@scope
            }

            modButtons.map { it.mod }.filterIsInstance<IModRequiresOriginalBeatmap>().fastForEach { mod ->
                ensureActive()
                mod.applyFromBeatmap(beatmap)
            }
            customizationMenu.updateComponents()

            // Copy the mods to avoid concurrent modification
            val mods = enabledMods.deepCopy().values
            val difficulty = beatmap.difficulty.clone()

            ModUtils.applyModsToBeatmapDifficulty(difficulty, gameMode, mods, true)

            ensureActive()
            arBadge.value = "%.2f".format(difficulty.ar)
            odBadge.value = "%.2f".format(difficulty.od)
            csBadge.value = "%.2f".format(difficulty.difficultyCS)
            hpBadge.value = "%.2f".format(difficulty.hp)
            bpmBadge.value = (selectedBeatmap.mostCommonBPM * ModUtils.calculateRateWithMods(mods, Double.POSITIVE_INFINITY)).roundToInt().toString()

            val attributes: DifficultyAttributes = when (difficultyAlgorithm) {
                droid -> calculateDroidDifficulty(beatmap, mods, this@scope)
                standard -> calculateStandardDifficulty(beatmap, mods, this@scope)
            }

            ensureActive()
            starRatingBadge.clearEntityModifiers()
            ensureActive()
            starRatingBadge.background!!.clearEntityModifiers()
            ensureActive()

            starRatingBadge.value = "%.2f".format(attributes.starRating)
            starRatingBadge.background!!.colorTo(OsuColors.getStarRatingColor(attributes.starRating), 0.1f)

            if (attributes.starRating >= 6.5) {
                starRatingBadge.colorTo(ColorARGB(0xFFFFD966), 0.1f)
                starRatingBadge.fadeTo(1f, 0.1f)
            } else {
                starRatingBadge.colorTo(ColorARGB.Black, 0.1f)
                starRatingBadge.fadeTo(0.75f, 0.1f)
            }

            songMenu.changeDimensionInfo(selectedBeatmap)
            songMenu.setStarsDisplay(GameHelper.Round(attributes.starRating, 2))
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

        // Only parsing to update mod's specific settings defaults, specially those which rely on the original beatmap data.
        parseBeatmap()
    }

    override fun back() {
        back(true)
    }

    fun back(updatePlayerMods: Boolean) {

        if (Multiplayer.isConnected) {
            RoomScene.isWaitingForModsChange = true

            // The room mods are the same as the host mods
            if (Multiplayer.isRoomHost) {
                setRoomMods(enabledMods.serializeMods())
            } else if (updatePlayerMods) {
                setPlayerMods(enabledMods.serializeMods())
            } else {
                RoomScene.isWaitingForModsChange = false
            }
        }

        super.back()
    }

    //endregion

    //region Mods

    fun setMods(mods: RoomMods, isFreeMod: Boolean) {
        if (isFreeMod) {
            for (mod in enabledMods.values) {
                if (mod.isValidForMultiplayerAsFreeMod) {
                    continue
                }

                if (mod !in mods) {
                    removeMod(mod)
                }
            }
        } else {
            clear()
        }

        for (mod in mods.values) {
            if (!mod.isValidForMultiplayer) {
                continue
            }

            if (!isFreeMod || mod.isValidForMultiplayerAsFreeMod) {
                addMod(mod)
            }
        }

        if (!Multiplayer.isRoomHost) {
            val doubleTime = enabledMods.ofType<ModDoubleTime>()
            val nightCore = enabledMods.ofType<ModNightCore>()

            if (Config.isUseNightcoreOnMultiplayer() && doubleTime != null) {
                removeMod(doubleTime)
                addMod(ModNightCore())
            } else if (!Config.isUseNightcoreOnMultiplayer() && nightCore != null) {
                removeMod(nightCore)
                addMod(ModDoubleTime())
            }
        }

        updateModButtonEnabledState()
    }

    fun updateModButtonEnabledState() {
        modButtons.fastForEach { it.updateEnabledState() }
    }

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

        val beatmap = GlobalManager.getInstance().selectedBeatmap
        val difficulty = beatmap?.getBeatmapDifficulty()

        if (difficulty != null) {
            scoreMultiplierBadge.value = "%.2fx".format(
                enabledMods.values.fold(1f) { acc, mod ->
                    acc * mod.calculateScoreMultiplier(difficulty)
                }
            )
        } else {
            scoreMultiplierBadge.value = "1.00x"
        }

        customizeButton.isEnabled = !customizationMenu.isEmpty()

        if (lastChangedMod is IModApplicableToTrackRate) {
            GlobalManager.getInstance().songMenu.updateMusicEffects()
        }

        parseBeatmap()
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

        modButtons.find { it.mod::class == mod::class }?.apply {
            isSelected = false
            mod.settings.fastForEach { it.value = it.defaultValue }
        }

        customizationMenu.onModRemoved(mod)
        onModsChanged(mod)
    }

    //endregion

    //region Components

    private class Section(name: String, mods: List<Mod>) : LinearContainer() {

        init {
            orientation = Orientation.Vertical
            width = 340f
            height = FitParent

            background = Box().apply {
                color = ColorARGB(0xFF13131E)
                cornerRadius = 16f
            }

            +ExtendedText().apply {
                width = FitParent
                text = name
                alignment = Anchor.Center
                font = ResourceManager.getInstance().getFont("smallFont")
                padding = Vec4(12f)
                color = ColorARGB(0xFF8282A8)
            }

            +ScrollableContainer().apply {
                scrollAxes = Axes.Y
                width = FitParent
                height = FitParent
                clipChildren = true

                +LinearContainer().apply {
                    width = FitParent
                    orientation = Orientation.Vertical
                    padding = Vec4(12f, 0f, 12f, 12f)
                    spacing = 16f

                    mods.fastForEach { mod ->
                        val button = ModButton(mod)
                        modButtons.add(button)
                        attachChild(button)
                    }
                }
            }
        }
    }

    private class ModButton(val mod: Mod): Button() {

        private val titleText = firstOf<ExtendedText>()!!
        private val descriptionText = ExtendedText()


        init {
            titleText.detachSelf()

            +LinearContainer().apply {
                width = FitParent
                padding = Vec4(0f, 6f)
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                orientation = Orientation.Vertical

                +titleText.apply {
                    height = FitContent
                    font = ResourceManager.getInstance().getFont("smallFont")
                    alignment = Anchor.TopLeft
                    anchor = Anchor.TopLeft
                    origin = Anchor.TopLeft
                }

                +descriptionText.apply {
                    width = FitParent
                    font = ResourceManager.getInstance().getFont("xs")
                    text = mod.description
                    clipChildren = true
                    alpha = 0.75f
                }
            }

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

            updateEnabledState()
        }

        fun updateEnabledState() {
            // TODO: the button should be hidden when it is disabled after Container can observe child visibility.
            isEnabled = if (Multiplayer.isMultiplayer && Multiplayer.room != null) {
                mod.isValidForMultiplayer && (Multiplayer.isRoomHost ||
                    (Multiplayer.room!!.gameplaySettings.isFreeMod && mod.isValidForMultiplayerAsFreeMod))
            } else {
                true
            }
        }

        override fun onManagedUpdate(deltaTimeSec: Float) {

            // Match the description text color with the title text color during animations.
            if (!descriptionText.color.colorEquals(titleText.color)) {
                descriptionText.color = titleText.color.copy(alpha = descriptionText.alpha)
            }

            super.onManagedUpdate(deltaTimeSec)
        }
    }

    //endregion

}


