package com.osudroid.ui.v2.modmenu

import com.reco1l.andengine.*
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
import com.osudroid.ui.v2.ModsIndicator
import com.osudroid.ui.v2.StarRatingBadge
import com.osudroid.utils.updateThread
import com.reco1l.andengine.text.FontAwesomeIcon
import com.reco1l.andengine.theme.Icon
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.theme.rem
import com.reco1l.andengine.theme.srem
import com.reco1l.andengine.ui.UITextButton
import com.reco1l.toolkt.kotlin.*
import com.reco1l.toolkt.kotlin.async
import com.rian.osu.*
import com.rian.osu.beatmap.Beatmap
import com.rian.osu.beatmap.parser.*
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateDroidDifficulty
import com.rian.osu.difficulty.BeatmapDifficultyCalculator.calculateStandardDifficulty
import com.rian.osu.mods.*
import com.rian.osu.utils.*
import com.rian.osu.utils.ModUtils
import kotlinx.coroutines.*
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm.*
import ru.nsu.ccfit.zuev.osu.helper.*
import java.util.LinkedList
import java.util.concurrent.CancellationException
import kotlin.math.*
import kotlin.reflect.KClass

object ModMenu : UIScene() {


    /**
     * List of currently enabled mods.
     */
    val enabledMods = ModHashMap()

    /**
     * List of all mod sections.
     */
    val modSections = mutableListOf<ModMenuSection>()

    /**
     * List of all mod toggles.
     */
    val modToggles = mutableListOf<ModMenuToggle>()


    private val modChangeQueue = LinkedList<Mod>()
    private val modPresetsSection: ModMenuPresetsSection

    private val customizeButton: UITextButton
    private val customizationMenu: ModCustomizationMenu
    private val selectedModsIndicator: ModsIndicator
    private val searchInput: ModMenuSearchInput

    private val rankedBadge: UIBadge
    private val arBadge: UILabeledBadge
    private val odBadge: UILabeledBadge
    private val csBadge: UILabeledBadge
    private val hpBadge: UILabeledBadge
    private val bpmBadge: UILabeledBadge
    private val starRatingBadge: StarRatingBadge
    private val scoreMultiplierBadge: UILabeledBadge

    private var parsedBeatmap: Beatmap? = null
    private var calculationJob: Job? = null


    init {
        isBackgroundEnabled = false

        customizationMenu = ModCustomizationMenu()

        attachChild(UIFillContainer().apply {
            width = Size.Full
            height = Size.Full
            orientation = Orientation.Vertical
            style = {
                backgroundColor = it.accentColor * 0.1f / 0.9f
            }

            +UIContainer().apply {
                width = Size.Full
                height = Size.Auto
                style = {
                    padding = UIEngine.current.safeArea.copy(y = 2f.srem, w = 2f.srem)
                }

                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = {
                        spacing = 2f.srem
                    }

                    +UITextButton().apply {
                        text = "Back"
                        leadingIcon = FontAwesomeIcon(Icon.ArrowLeft)
                        onActionUp = {
                            ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                            back()
                        }
                        onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    }

                    customizeButton = UITextButton().apply {
                        text = "Customize"
                        isEnabled = false
                        leadingIcon = FontAwesomeIcon(Icon.Sliders)
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

                    +customizeButton

                    +UITextButton().apply {
                        text = "Clear"
                        style += {
                            color = Color4(0xFFFFBFBF)
                            backgroundColor = Color4(0xFF342121)
                        }
                        leadingIcon = FontAwesomeIcon(Icon.DeleteLeft)
                        onActionUp = {
                            ResourceManager.getInstance().getSound("click-short-confirm")?.play()
                            clear()
                        }
                        onActionCancel = { ResourceManager.getInstance().getSound("click-short")?.play() }
                    }

                    +UIScrollableContainer().apply {
                        width = 340f
                        height = Size.Auto
                        anchor = Anchor.CenterLeft
                        origin = Anchor.CenterLeft
                        scrollAxes = Axes.X
                        clipToBounds = true

                        selectedModsIndicator = ModsIndicator()
                        +selectedModsIndicator
                    }
                }

                +UIContainer().apply {
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight

                    searchInput = ModMenuSearchInput().apply {
                        width = 18f.rem
                        onSearchTermUpdate = { searchTerm ->
                            modSections.fastForEach { it.onSearchTermUpdate(searchTerm) }

                            if (modPresetsSection.isVisible) {
                                modPresetsSection.onSearchTermUpdate(searchTerm)
                            }
                        }
                    }

                    +searchInput

                    container {
                        anchor = Anchor.CenterRight
                        origin = Anchor.CenterRight
                        style = {
                            padding = Vec4(2f.srem, 0f)
                        }

                        +FontAwesomeIcon(Icon.MagnifyingGlass).apply {
                            style = {
                                color = it.accentColor
                            }
                        }
                    }
                }
            }

            +UIScrollableContainer().apply {
                width = Size.Full
                height = Size.Full
                scrollAxes = Axes.X

                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    width = Size.Auto
                    height = Size.Full
                    style = {
                        spacing = 2f.srem
                        padding = UIEngine.current.safeArea
                    }

                    modPresetsSection = ModMenuPresetsSection()
                    +modPresetsSection

                    val mods = ModUtils.allModsInstances

                    ModType.entries.forEach { type ->
                        val sectionName = StringTable.get(type.stringId)
                        val sectionToggles = mods.filter { it !is IMigratableMod && it.isUserPlayable && it.type == type }.map { ModMenuToggle(it) }

                        if (sectionToggles.isEmpty()) {
                            return@forEach
                        }

                        modToggles.addAll(sectionToggles)

                        val section = ModMenuSection(sectionName, sectionToggles)

                        +section
                        modSections.add(section)
                    }
                }
            }

            +UIContainer().apply {
                width = Size.Full
                height = Size.Auto
                style = {
                    padding = UIEngine.current.safeArea.copy(
                        y = 2f.srem,
                        w = 2f.srem + (Multiplayer.roomScene?.chat?.buttonHeight ?: 0f)
                    )
                }

                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterLeft
                    origin = Anchor.CenterLeft
                    style = {
                        spacing = 2f.srem
                    }

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

                +UILinearContainer().apply {
                    orientation = Orientation.Horizontal
                    anchor = Anchor.CenterRight
                    origin = Anchor.CenterRight
                    style = {
                        spacing = 2f.srem
                    }

                    starRatingBadge = StarRatingBadge()
                    +starRatingBadge

                    rankedBadge = badge {
                        text = "Ranked"
                        backgroundColor = Color4(0xFF342121)
                        color = Color4(0xFF161622)
                        style = {}
                    }

                    scoreMultiplierBadge = labeledBadge {
                        label = "Score"
                        value = "1.00x"
                    }
                }
            }
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

        val selectedBeatmap = GlobalManager.getInstance().selectedBeatmap ?: return

        calculationJob = async scope@{

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

            enabledMods.values.filterIsInstance<IModRequiresOriginalBeatmap>().fastForEach { mod ->
                ensureActive()
                mod.applyFromBeatmap(beatmap)
            }
            customizationMenu.updateComponents()

            // Copy the mods to avoid concurrent modification
            val mods = enabledMods.deepCopy().values
            val difficulty = beatmap.difficulty.clone()
            val rate = ModUtils.calculateRateWithMods(mods, Double.POSITIVE_INFINITY)

            ModUtils.applyModsToBeatmapDifficulty(difficulty, gameMode, mods, true, this@scope)

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

                scoreMultiplierBadge.updateStatisticBadge(
                    "1.00x",
                    "%.2fx".format(ModUtils.calculateScoreMultiplier(enabledMods))
                )
            }

            ensureActive()

            val attributes = when (difficultyAlgorithm) {
                droid -> calculateDroidDifficulty(beatmap, mods, this@scope)
                standard -> calculateStandardDifficulty(beatmap, mods, this@scope)
            }

            ensureActive()

            starRatingBadge.rating = attributes.starRating
            songMenu.changeDimensionInfo(selectedBeatmap)
            songMenu.setStarsDisplay(attributes.starRating.toFloat())
        }
    }

    //endregion

    //region Visibility

    override fun show() {
        GlobalManager.getInstance().engine.scene.setChildScene(
            this,
            false,
            true,
            true
        )

        // Do not show mod presets in multiplayer.
        modPresetsSection.isVisible = !Multiplayer.isMultiplayer

        if (modPresetsSection.isVisible) {
            modPresetsSection.onSearchTermUpdate(searchInput.value)
        }

        // Ensure mods and customizations that can be enabled by the user are displayed and enabled.
        updateModButtonVisibility()
        updateCustomizationMenuEnabledStates()

        // Only parsing to update mod's specific settings defaults, specially those which rely on the original beatmap data.
        parseBeatmap()
    }

    override fun back() {
        back(true)
    }

    fun back(updatePlayerMods: Boolean) {

        if (Multiplayer.isConnected) {
            Multiplayer.roomScene?.chat?.show()
            Multiplayer.roomScene?.isWaitingForModsChange = true

            // The room mods are the same as the host mods
            if (Multiplayer.isRoomHost) {
                setRoomMods(enabledMods.serializeMods())
            } else if (updatePlayerMods) {
                setPlayerMods(enabledMods.serializeMods())
            } else {
                Multiplayer.roomScene?.isWaitingForModsChange = false
            }
        }

        super.back()
    }

    //endregion

    //region Mods

    fun setMods(mods: RoomMods, isFreeMod: Boolean) {
        if (isFreeMod) {
            for ((_, mod) in enabledMods.toList()) {
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

            if (!isFreeMod || !mod.isValidForMultiplayerAsFreeMod) {
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

        updateModButtonVisibility()
    }

    fun updateModButtonVisibility() {
        modToggles.fastForEach {
            it.updateVisibility(searchInput.value)
            it.applyCompatibilityState()
        }

        // Update section visibility after toggles have been updated since it may not need to be visible anymore.
        modSections.fastForEach { it.updateVisibility() }
    }

    fun updateCustomizationMenuEnabledStates() {
        customizationMenu.updateComponentEnabledStates()
    }

    fun clear() {
        cancelCalculationJob()

        val room = Multiplayer.room
        val isHost = Multiplayer.isRoomHost

        enabledMods.toList().fastForEach {
            val mod = it.second

            if (room != null) {
                // For non-host in multiplayer, we want to keep mods that are not allowed to be selected by the player
                // since only the host can change those mods.
                if (!isHost && room.gameplaySettings.isFreeMod && !mod.isValidForMultiplayerAsFreeMod) {
                    return@fastForEach
                }

                // Special handling for the ScoreV2 mod, which is used by the ScoreV2 win condition.
                if (mod is ModScoreV2) {
                    return@fastForEach
                }
            }

            removeMod(mod::class)
        }
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
        val enabledMods = enabledMods.values.toList()
        val isRanked = enabledMods.isEmpty() || enabledMods.none { !it.isRanked }

        rankedBadge.apply {
            text = if (isRanked) "Ranked" else "Unranked"

            clearEntityModifiers()
            colorTo(if (isRanked) Color4(0xFF161622) else Theme.current.accentColor, 0.1f)

            backgroundColor = if (isRanked) Color4(0xFF83DF6B) else Theme.current.accentColor * 0.15f
        }

        modToggles.fastForEach {
            it.hasIncompatibility =
                if (!it.isSelected) enabledMods.any { m -> !it.mod.isCompatibleWith(m) } else false
        }

        scoreMultiplierBadge.updateStatisticBadge(
            "1.00x",
            "%.2fx".format(ModUtils.calculateScoreMultiplier(enabledMods))
        )

        customizeButton.isEnabled = !customizationMenu.isEmpty()

        if (modChangeQueue.any { it is IModApplicableToTrackRate }) {
            GlobalManager.getInstance().songMenu.updateMusicEffects()
        }
        modChangeQueue.clear()

        parseBeatmap()

        selectedModsIndicator.mods = enabledMods
        modPresetsSection.onModsChanged()
    }

    fun addMod(mod: Mod) {

        if (mod in enabledMods) {
            return
        }
        enabledMods.put(mod)

        modToggles.fastForEach { button ->

            val wasSelected = button.isSelected
            button.isSelected = button.mod::class in enabledMods

            if (button.mod::class == mod::class) {
                button.mod = mod
            }

            // Handle incompatible mods with the selected mod.
            if (wasSelected && !button.isSelected) {
                customizationMenu.onModRemoved(button.mod)
                button.mod = button.mod::class.createInstance()
            }
        }

        customizationMenu.onModAdded(mod)
        queueModChange(mod)
    }

    fun removeMod(mod: Mod) = removeMod(mod::class)

    fun removeMod(modClass: KClass<out Mod>) {

        if (modClass !in enabledMods) {
            return
        }

        val toggle = modToggles.find { it.mod::class == modClass } ?: return

        enabledMods.remove(modClass)

        toggle.isSelected = false

        customizationMenu.onModRemoved(toggle.mod)
        queueModChange(toggle.mod)

        toggle.mod = modClass.createInstance()
    }

    //endregion

    //region Components

    private fun <T : Comparable<T>> UILabeledBadge.updateStatisticBadge(initialValue: T, finalValue: T) {

        val newText = if (finalValue is Float || finalValue is Double) "%.2f".format(finalValue) else finalValue.toString()

        if (valueComponent.text == newText) {
            return
        }
        valueComponent.text = newText

        valueComponent.clearEntityModifiers()
        valueComponent.colorTo(Color4(when {
            initialValue < finalValue -> 0xFFF78383
            initialValue > finalValue -> 0xFF40CF5D
            else -> 0xFFFFFFFF
        }), 0.1f)
    }

    //endregion

}


