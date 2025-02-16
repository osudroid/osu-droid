package com.reco1l.osu.hud

import android.util.Log
import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.Container
import com.reco1l.osu.hud.editor.HUDElementSelector
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.osu.updateThread
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.engine.camera.hud.*
import org.anddev.andengine.entity.IEntity
import org.json.JSONObject
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.ResourceManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.skins.OsuSkin
import java.io.File
import kotlin.reflect.full.primaryConstructor

class GameplayHUD(
    private val statistics: StatisticV2,
    private val gameScene: GameScene
) : Container() {

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
        // The engine we expect the HUD to be a effectively an instance of the AndEngine's HUD class.
        // Since we need Container features we set a HUD instance as the parent and we just need to
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


    //region Skinning

    fun saveToSkinJSON() {

        val data = getSkinData()

        val jsonFile = File(GlobalManager.getInstance().skinNow, "skin.json")
        val json: JSONObject

        if (jsonFile.exists()) {
            json = JSONObject(jsonFile.reader().readText())
        } else {
            jsonFile.createNewFile()
            json = JSONObject()
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

        Log.i("GameplayHUD", "Setting skin data: $layoutData<")

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

        if (value) {
            ResourceManager.getInstance().loadHighQualityAsset("delete", "delete.png")
            ResourceManager.getInstance().loadHighQualityAsset("expand", "expand.png")
            ResourceManager.getInstance().loadHighQualityAsset("rotate_left", "rotate_left.png")
            ResourceManager.getInstance().loadHighQualityAsset("rotate_right", "rotate_right.png")

            elementSelector = HUDElementSelector(this)

            parent!!.attachChild(elementSelector)
            parent!!.registerTouchArea(elementSelector)

        } else {
            updateThread {
                parent!!.detachChild(elementSelector)
                parent!!.unregisterTouchArea(elementSelector)

                elementSelector = null
            }
        }

        updateThread {
            mChildren?.filterIsInstance<HUDElement>()?.forEach { it.setEditMode(value) }
        }
    }
    //endregion

    //region Entity events
    override fun onManagedUpdate(pSecondsElapsed: Float) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onGameplayUpdate(gameScene, statistics, pSecondsElapsed)
        }
        elementSelector?.onGameplayUpdate(gameScene, statistics, pSecondsElapsed)
        super.onManagedUpdate(pSecondsElapsed)
    }
    //endregion

    //region Gameplay Events
    fun onNoteHit(statistics: StatisticV2) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onNoteHit(statistics)
        }
        elementSelector?.onNoteHit(statistics)
    }

    fun onBreakStateChange(isBreak: Boolean) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onBreakStateChange(isBreak)
        }
        elementSelector?.onBreakStateChange(isBreak)
    }
    //endregion


    override fun getParent(): HUD? {
        // Nullable because during initialization the parent is not set yet.
        return super.getParent() as? HUD
    }
}


