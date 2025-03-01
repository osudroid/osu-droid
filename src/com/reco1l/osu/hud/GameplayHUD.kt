package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.osu.hud.editor.HUDElementSelector
import com.reco1l.osu.hud.elements.*
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.*
import org.anddev.andengine.engine.camera.hud.*
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.input.touch.TouchEvent
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.ToastLogger
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import ru.nsu.ccfit.zuev.skins.SkinJsonReader
import java.io.File
import kotlin.reflect.full.primaryConstructor

class GameplayHUD : Container(), IGameplayEvents {

    /**
     * The currently selected element.
     */
    var selected: HUDElement? = null
        set(value) {
            if (field != value) {
                field = value
                forEachElement { it.onSelectionStateChange(it == value) }

                // Move to front
                setChildIndex(value, mChildren.size - 1)
            }
        }

    /**
     * The element selector used in edit mode.
     */
    var elementSelector: HUDElementSelector? = null
        private set


    private var isInEditMode = false


    init {
        // The engine expects the HUD to be an instance of AndEngine's HUD class.
        // Since we need Container features, we set an HUD instance as the parent, and we just need to
        // reference the parent of this container to set the engine's HUD.
        val parent = HUD()
        parent.attachChild(this)
        parent.registerTouchArea(this)
        parent.camera = GlobalManager.getInstance().engine.camera

        autoSizeAxes = Axes.None
        setSize(Config.getRES_WIDTH().toFloat(), Config.getRES_HEIGHT().toFloat())
    }


    /**
     * Adds an element to the HUD.
     */
    fun addElement(data: HUDElementSkinData, inEditMode: Boolean = isInEditMode) {
        val element = data.type.primaryConstructor!!.call()
        attachChild(element)
        element.setSkinData(data)
        element.setEditMode(inEditMode)
    }

    fun onBackPress() {

        if (elementSelector?.isExpanded == true) {
            elementSelector?.collapse()
            return
        }

        MessageDialog()
            .setTitle("HUD Editor")
            .addButton("Save changes") {
                it.dismiss()
                updateThread {
                    ToastLogger.showText("Saving changes...", true)
                    setEditMode(false)
                    saveToSkinJSON()
                    ToastLogger.showText("Changes saved!", true)
                }
            }
            .addButton("Discard changes") {
                it.dismiss()
                updateThread {
                    setSkinData(OsuSkin.get().hudSkinData)
                    ToastLogger.showText("Changes discarded!", true)
                }
            }
            .addButton("Reset to default") {
                it.dismiss()
                updateThread {
                    setSkinData(HUDSkinData.Default)
                    ToastLogger.showText("HUD set to default!", true)
                }
            }
            .addButton("Cancel") { it.dismiss() }
            .show()
    }


    //region Skinning

    fun saveToSkinJSON() {

        val data = getSkinData()

        val jsonFile = File(GlobalManager.getInstance().skinNow, "skin.json")
        val json: JSONObject

        if (jsonFile.exists()) {
            json = JSONObject(jsonFile.reader().readText())
        } else {
            jsonFile.createNewFile()

            // We use the current skin data as a base to avoid losing any other skin data.
            json = SkinJsonReader.getReader().currentData
        }

        json.put("HUD", HUDSkinData.writeToJSON(data))
        jsonFile.writeText(json.toString())

        SkinJsonReader.getReader().currentData = json
        OsuSkin.get().hudSkinData = data
    }

    /**
     * Obtains the skin data of the HUD.
     */
    fun getSkinData(): HUDSkinData {
        return HUDSkinData(
            elements = mChildren?.filterIsInstance<HUDElement>()
                ?.map { it.getSkinData() }
                ?: emptyList()
        )
    }

    /**
     * Sets the skin data of the HUD.
     */
    fun setSkinData(layoutData: HUDSkinData) {
        selected = null
        mChildren?.filterIsInstance<HUDElement>()?.forEach(IEntity::detachSelf)

        // First pass: We attach everything so that elements can reference between them when
        // applying default layout.
        layoutData.elements.forEach { data -> addElement(data) }

        if (layoutData == HUDSkinData.Default) {
            applyDefaultLayout()
        }
    }

    private inline fun <reified T : HUDElement>getFirstOf() : T? {
        return mChildren?.firstOrNull { it is T } as? T
    }

    private fun applyDefaultLayout() {
        // The default layout is hardcoded to keep the original layout before the HUD editor was
        // implemented, as it used cross-references between elements that are not possible to be
        // set in the editor.
        val scoreCounter = getFirstOf<HUDScoreCounter>()!!
        val accuracyCounter = getFirstOf<HUDAccuracyCounter>()!!
        val pieSongProgress = getFirstOf<HUDPieSongProgress>()!!

        accuracyCounter.y += scoreCounter.y + scoreCounter.drawHeight

        pieSongProgress.y = accuracyCounter.y + accuracyCounter.heightScaled / 2f
        pieSongProgress.x = accuracyCounter.x - accuracyCounter.widthScaled - 18f
    }

    //endregion

    //region Elements events
    fun setEditMode(value: Boolean) {
        isInEditMode = value
        GlobalManager.getInstance().gameScene.isHUDEditorMode = value

        if (value) {
            ResourceManager.getInstance().loadHighQualityAsset("delete", "delete.png")
            ResourceManager.getInstance().loadHighQualityAsset("flip", "flip.png")

            elementSelector = HUDElementSelector(this)

            parent!!.attachChild(elementSelector)
            parent!!.registerTouchArea(elementSelector)

        } else {
            parent!!.detachChild(elementSelector)
            parent!!.unregisterTouchArea(elementSelector)

            elementSelector = null
        }


        // Cannot use forEachElement {} because we're modifying the list.
        mChildren?.filterIsInstance<HUDElement>()?.fastForEach { it.setEditMode(value) }
    }
    //endregion

    //region Gameplay Events

    fun forEachElement(action: (HUDElement) -> Unit) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.let(action)
        }
    }

    override fun onGameplayUpdate(gameScene: GameScene, statistics: StatisticV2, secondsElapsed: Float) {
        forEachElement { it.onGameplayUpdate(gameScene, statistics, secondsElapsed) }
        elementSelector?.onGameplayUpdate(gameScene, statistics, secondsElapsed)
    }

    override fun onNoteHit(statistics: StatisticV2) {
        forEachElement { it.onNoteHit(statistics) }
        elementSelector?.onNoteHit(statistics)
    }

    override fun onBreakStateChange(isBreak: Boolean) {
        forEachElement { it.onBreakStateChange(isBreak) }
        elementSelector?.onBreakStateChange(isBreak)
    }

    override fun onAccuracyRegister(accuracy: Float) {
        forEachElement { it.onAccuracyRegister(accuracy) }
        elementSelector?.onAccuracyRegister(accuracy)
    }

    //endregion


    override fun getParent(): HUD? {
        // Nullable because during initialization the parent is not set yet.
        return super.getParent() as? HUD
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (!super.onAreaTouched(event, localX, localY)) {
            if (event.isActionDown) {
                selected = null
            }
            return false
        }
        return true
    }
}


