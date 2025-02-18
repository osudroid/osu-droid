package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.osu.hud.editor.HUDElementSelector
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.osu.ui.MessageDialog
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.fastForEach
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
                mChildren?.fastForEach {
                    (it as? HUDElement)?.onSelectionStateChange(it == value)
                }
            }
        }


    private var isInEditMode = false

    private var elementSelector: HUDElementSelector? = null


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

        fun restore() {
            x = 0f
            width = Config.getRES_WIDTH().toFloat()
        }

        MessageDialog()
            .setTitle("HUD Editor")
            .setMessage("Do you want to save the changes?")
            .addButton("Save") {
                it.dismiss()
                updateThread {
                    ToastLogger.showText("Saving changes...", true)
                    restore()
                    setEditMode(false)
                    saveToSkinJSON()
                    ToastLogger.showText("Changes saved!", true)
                }
            }
            .addButton("Discard & Restore") {
                it.dismiss()
                updateThread {
                    restore()
                    setEditMode(false)
                    setSkinData(OsuSkin.get().hudSkinData)
                    ToastLogger.showText("Changes discarded!", true)
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

        OsuSkin.get().hudSkinData = data
    }

    /**
     * Saves the skin data of the HUD.
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
        // Default layout is hardcoded to keep the original layout of the game before the HUD
        // editor was implemented. As well there's no other way since the original layout was
        // using cross references between elements that are not possible to be set in the editor.

        val scoreCounter = getFirstOf<HUDScoreCounter>()
        scoreCounter?.anchor = Anchor.TopRight
        scoreCounter?.origin = Anchor.TopRight
        scoreCounter?.setScale(0.96f)
        scoreCounter?.x = -10f

        val accuracyCounter = getFirstOf<HUDAccuracyCounter>()
        accuracyCounter?.anchor = Anchor.TopRight
        accuracyCounter?.origin = Anchor.TopRight
        accuracyCounter?.setScale(0.6f * 0.96f)
        accuracyCounter?.setPosition(-17f, 9f)

        if (scoreCounter != null && accuracyCounter != null) {
            accuracyCounter.y += scoreCounter.y + scoreCounter.drawHeight
        }

        val pieSongProgress = getFirstOf<HUDPieSongProgress>()
        pieSongProgress?.anchor = Anchor.TopRight
        pieSongProgress?.origin = Anchor.CenterRight

        if (pieSongProgress != null && accuracyCounter != null) {
            pieSongProgress.y = accuracyCounter.y + accuracyCounter.heightScaled / 2f
            pieSongProgress.x = accuracyCounter.x - accuracyCounter.widthScaled - 18f
        }

        val comboCounter = getFirstOf<HUDComboCounter>()
        comboCounter?.anchor = Anchor.BottomLeft
        comboCounter?.origin = Anchor.BottomLeft
        comboCounter?.setPosition(10f, -10f)
        comboCounter?.setScale(1.28f)
    }

    //endregion

    //region Elements events
    fun setEditMode(value: Boolean) {
        isInEditMode = value
        GlobalManager.getInstance().gameScene.isHUDEditorMode = value

        if (value) {
            ResourceManager.getInstance().loadHighQualityAsset("delete", "delete.png")
            ResourceManager.getInstance().loadHighQualityAsset("scale", "scale.png")
            ResourceManager.getInstance().loadHighQualityAsset("rotate", "rotate.png")
            ResourceManager.getInstance().loadHighQualityAsset("rotate_left", "rotate_left.png")
            ResourceManager.getInstance().loadHighQualityAsset("rotate_right", "rotate_right.png")

            elementSelector = HUDElementSelector(this)

            parent!!.attachChild(elementSelector)
            parent!!.registerTouchArea(elementSelector)

        } else {
            parent!!.detachChild(elementSelector)
            parent!!.unregisterTouchArea(elementSelector)

            elementSelector = null
        }

        mChildren?.filterIsInstance<HUDElement>()?.forEach { it.setEditMode(value) }
    }
    //endregion

    //region Gameplay Events

    private fun forEachElement(action: (HUDElement) -> Unit) {
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


