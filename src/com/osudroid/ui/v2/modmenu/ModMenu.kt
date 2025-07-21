package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
import com.reco1l.andengine.component.UIComponent.Companion.MatchContent
import com.reco1l.andengine.component.UIComponent.Companion.FillParent
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.osudroid.multiplayer.api.RoomAPI.setPlayerMods
import com.osudroid.multiplayer.api.RoomAPI.setRoomMods
import com.osudroid.multiplayer.api.data.RoomMods
import com.osudroid.multiplayer.Multiplayer
import com.osudroid.multiplayer.RoomScene
import com.osudroid.ui.OsuColors
import com.osudroid.utils.updateThread
import com.reco1l.andengine.ui.UITextButton
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
import java.util.LinkedList
import java.util.concurrent.CancellationException
import kotlin.math.*

object ModMenu : UIScene() {


    /**
     * List of currently enabled mods.
     */
    val enabledMods = ModHashMap()

    /**
     * List of all mod toggles.
     */
    val modToggles = mutableListOf<ModMenuToggle>()


    private val modChangeQueue = LinkedList<Mod>()
    private val modPresetsSection: ModMenuPresetsSection

    private val customizeButton: UITextButton
    private val customizationMenu: ModCustomizationMenu

    private val rankedBadge: UIBadge
    private val arBadge: UILabeledBadge
    private val odBadge: UILabeledBadge
    private val csBadge: UILabeledBadge
    private val hpBadge: UILabeledBadge
    private val bpmBadge: UILabeledBadge
    private val starRatingBadge: UILabeledBadge
    private val scoreMultiplierBadge: UILabeledBadge

    private var parsedBeatmap: Beatmap? = null
    private var calculationJob: Job? = null


    init {
        isBackgroundEnabled = false

        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png")
        ResourceManager.getInstance().loadHighQualityAsset("tune", "tune.png")
        ResourceManager.getInstance().loadHighQualityAsset("backspace", "backspace.png")
        ResourceManager.getInstance().loadHighQualityAsset("search", "search.png")
        ResourceManager.getInstance().loadHighQualityAsset("settings", "settings.png")

        customizationMenu = ModCustomizationMenu()

        attachChild(UILinearContainer().apply {
            width = FillParent
            height = FillParent
            orientation = Orientation.Vertical
            background = UIBox().apply {
                applyTheme = {
                    color = it.accentColor * 0.1f
                    alpha = 0.9f
                }
            }

            attachChild(UIContainer().apply {
                width = FillParent
                height = MatchContent
                padding = Vec4(60f, 12f)

                attachChild(UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    height = MatchContent
                    spacing = 10f

                    attachChild(UITextButton().apply {
                        text = "Back"
                        leadingIcon = UISprite().apply {
                            textureRegion = ResourceManager.getInstance().getTexture("back-arrow")
                            width = 28f
                            height = 28f
                        }
                        onActionUp = {
                            ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                            back()
                        }
                        onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    })

                    customizeButton = UITextButton().apply {
                        text = "Customize"
                        isEnabled = false
                        leadingIcon = UISprite().apply {
                            textureRegion = ResourceManager.getInstance().getTexture("tune")
                            width = 28f
                            height = 28f
                        }
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

                    attachChild(UITextButton().apply {
                        text = "Clear"
                        applyTheme = {}
                        color = Color4(0xFFFFBFBF)
                        background?.color = Color4(0xFF342121)
                        leadingIcon = UISprite().apply {
                            textureRegion = ResourceManager.getInstance().getTexture("backspace")
                            width = 28f
                            height = 28f
                        }
                        onActionUp = {
                            ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                            clear()
                        }
                        onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    })
                })

                attachChild(UILinearContainer().apply {
                    orientation = Orientation.Vertical
                    spacing = 10f
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight

                    +UILinearContainer().apply {
                        orientation = Orientation.Horizontal
                        anchor = Anchor.TopRight
                        origin = Anchor.TopRight
                        spacing = 10f

                        scoreMultiplierBadge = labeledBadge {
                            label = "Score multiplier"
                            value = "1.00x"
                        }

                        starRatingBadge = labeledBadge {
                            label = "Star rating"
                            value = "0.00"
                        }

                        rankedBadge = badge {
                            text = "Ranked"
                            background!!.color = Color4(0xFF83DF6B)
                            color = Color4(0xFF161622)
                            applyTheme = {}
                        }
                    }

                    +UILinearContainer().apply {
                        orientation = Orientation.Horizontal
                        origin = Anchor.TopRight
                        anchor = Anchor.TopRight
                        spacing = 10f

                        arBadge = labeledBadge {
                            label = "AR"
                            value = "0.00"
                        }
                        odBadge = labeledBadge {
                            label = "OD"
                            value = "0.00"
                        }
                        csBadge = labeledBadge {
                            label = "CS"
                            value = "0.00"
                        }
                        hpBadge = labeledBadge {
                            label = "HP"
                            value = "0.00"
                        }
                        bpmBadge = labeledBadge {
                            label = "BPM"
                            value = "0.0"
                        }
                    }
                })
            })

            attachChild(UIScrollableContainer().apply {
                width = FillParent
                height = FillParent
                scrollAxes = Axes.X

                attachChild(UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    width = MatchContent
                    height = FillParent
                    spacing = 16f
                    padding = Vec4(60f, 0f)

                    modPresetsSection = ModMenuPresetsSection()
                    +modPresetsSection

                    val mods = ModUtils.allModsInstances

                    ModType.entries.forEach { type ->
                        val sectionName = StringTable.get(type.stringId)
                        val sectionToggles = mods.filter { it !is IMigratableMod && it.isUserPlayable && it.type == type }.map { ModMenuToggle(it) }

                        modToggles.addAll(sectionToggles)

                        if (sectionToggles.isNotEmpty()) {
                            +ModMenuSection(sectionName, sectionToggles)
                        }
                    }
                })
            })

        })


        // Customizations menu
        attachChild(customizationMenu)

        modPresetsSection.loadPresets()
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

            modToggles.map { it.mod }.filterIsInstance<IModRequiresOriginalBeatmap>().fastForEach { mod ->
                ensureActive()
                mod.applyFromBeatmap(beatmap)
            }
            customizationMenu.updateComponents()

            // Copy the mods to avoid concurrent modification
            val mods = enabledMods.deepCopy().values
            val difficulty = beatmap.difficulty.clone()
            val rate = ModUtils.calculateRateWithMods(mods, Double.POSITIVE_INFINITY)

            ModUtils.applyModsToBeatmapDifficulty(difficulty, gameMode, mods, true)

            ensureActive()

            updateThread {
                arBadge.updateStatisticBadge(selectedBeatmap.approachRate, difficulty.ar)
                odBadge.updateStatisticBadge(selectedBeatmap.overallDifficulty, difficulty.od)
                csBadge.updateStatisticBadge(selectedBeatmap.circleSize, difficulty.difficultyCS)
                hpBadge.updateStatisticBadge(selectedBeatmap.hpDrainRate, difficulty.hp)

                bpmBadge.updateStatisticBadge(
                    selectedBeatmap.mostCommonBPM.roundToInt(),
                    (selectedBeatmap.mostCommonBPM * rate).roundToInt()
                )
            }

            ensureActive()

            val attributes: DifficultyAttributes = when (difficultyAlgorithm) {
                droid -> calculateDroidDifficulty(beatmap, mods, this@scope)
                standard -> calculateStandardDifficulty(beatmap, mods, this@scope)
            }

            ensureActive()
            starRatingBadge.clearEntityModifiers()
            ensureActive()
            starRatingBadge.background!!.clearEntityModifiers()
            ensureActive()

            starRatingBadge.valueEntity.text = "%.2f".format(attributes.starRating)
            starRatingBadge.background!!.colorTo(OsuColors.getStarRatingColor(attributes.starRating), 0.1f)

            if (attributes.starRating >= 6.5) {
                starRatingBadge.colorTo(Color4(0xFFFFD966), 0.1f)
                starRatingBadge.fadeTo(1f, 0.1f)
            } else {
                starRatingBadge.colorTo(Color4.Black, 0.1f)
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

        // Do not show mod presets in multiplayer.
        modPresetsSection.isVisible = !Multiplayer.isMultiplayer

        // Ensure mods that can be enabled by the user are displayed.
        updateModButtonEnabledState()

        // Only parsing to update mod's specific settings defaults, specially those which rely on the original beatmap data.
        parseBeatmap()
    }

    override fun back() {
        back(true)
    }

    fun back(updatePlayerMods: Boolean) {

        if (Multiplayer.isConnected) {
            RoomScene.chat.show()
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

        if (ModScoreV2::class in mods) {
            addMod(ModScoreV2())
        } else {
            removeMod(ModScoreV2())
        }

        updateModButtonEnabledState()
    }

    fun updateModButtonEnabledState() {
        modToggles.fastForEach { it.updateEnabledState() }
    }

    fun clear() {
        cancelCalculationJob()
        enabledMods.toList().fastForEach { removeMod(it.second) }
    }

    fun queueModChange(mod: Mod) = synchronized(modChangeQueue) {
        // Adding to first place in case it is already queued.
        if (modChangeQueue.isEmpty() || modChangeQueue.first() != mod) {
            if (modChangeQueue.isNotEmpty()) {
                modChangeQueue.remove(mod)
            }
            modChangeQueue.addFirst(mod)
        }
    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        if (modChangeQueue.isNotEmpty()) {
            onModsChanged()
        }

        super.onManagedUpdate(deltaTimeSec)
    }

    private fun onModsChanged() = synchronized(modChangeQueue) {

        val isRanked = enabledMods.isEmpty() || enabledMods.none { !it.value.isRanked }

        rankedBadge.apply {
            text = if (isRanked) "Ranked" else "Unranked"

            clearEntityModifiers()
            colorTo(if (isRanked) Color4(0xFF161622) else Theme.current.accentColor, 0.1f)

            background!!.clearEntityModifiers()
            background!!.colorTo(if (isRanked) Color4(0xFF83DF6B) else Theme.current.accentColor * 0.15f, 0.1f)
        }

        val beatmap = GlobalManager.getInstance().selectedBeatmap
        val difficulty = beatmap?.getBeatmapDifficulty()

        scoreMultiplierBadge.updateStatisticBadge(
            "1.00x",
            if (difficulty != null) {
                "%.2fx".format(enabledMods.values.fold(1f) { acc, mod ->
                    acc * mod.calculateScoreMultiplier(difficulty)
                })
            } else {
                "1.00x"
            }
        )

        customizeButton.isEnabled = !customizationMenu.isEmpty()

        if (modChangeQueue.any { it is IModApplicableToTrackRate }) {
            GlobalManager.getInstance().songMenu.updateMusicEffects()
        }
        modChangeQueue.clear()

        parseBeatmap()

        modPresetsSection.onModsChanged()
    }

    fun addMod(mod: Mod) {

        if (mod in enabledMods) {
            return
        }
        enabledMods.put(mod)

        modToggles.fastForEach { button ->

            val wasSelected = button.isSelected
            button.isSelected = button.mod in enabledMods

            // Handle incompatible mods with the selected mod.
            if (wasSelected && !button.isSelected) {
                customizationMenu.onModRemoved(button.mod)
            }
        }

        customizationMenu.onModAdded(mod)
        queueModChange(mod)
    }

    fun removeMod(mod: Mod) {

        if (mod !in enabledMods) {
            return
        }
        enabledMods.remove(mod)

        modToggles.find { it.mod::class == mod::class }?.apply {
            isSelected = false
            mod.settings.fastForEach { it.value = it.defaultValue }
        }

        customizationMenu.onModRemoved(mod)
        queueModChange(mod)
    }

    //endregion

    //region Components

    private fun <T : Comparable<T>> UILabeledBadge.updateStatisticBadge(initialValue: T, finalValue: T) {

        val newText = if (finalValue is Float || finalValue is Double) "%.2f".format(finalValue) else finalValue.toString()

        if (valueEntity.text == newText) {
            return
        }
        valueEntity.text = newText

        valueEntity.clearEntityModifiers()
        valueEntity.colorTo(Color4(when {
            initialValue < finalValue -> 0xFFF78383
            initialValue > finalValue -> 0xFF40CF5D
            else -> 0xFFFFFFFF
        }), 0.1f)
    }

    //endregion

}


