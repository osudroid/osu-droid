package com.reco1l.osu.hud

import com.reco1l.andengine.Anchor
import com.reco1l.andengine.Axes
import com.reco1l.andengine.attachTo
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.shape.Line
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.data.HUDSkinData
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDElement
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.osu.hud.elements.create
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.engine.camera.hud.*
import org.anddev.andengine.entity.IEntity
import org.anddev.andengine.input.touch.TouchEvent
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.GlobalManager
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*

class GameplayHUD(private val stat: StatisticV2, private val game: GameScene) : Container() {


    /**
     * The layout data for the HUD.
     */
    var skinData: HUDSkinData = HUDSkinData.Default
        set(value) {
            if (field != value) {
                onSkinDataChange(value)
                field = value
            }
        }

    /**
     * Whether the HUD is in edit mode or not.
     */
    var isInEditMode = true
        set(value) {
            if (field != value) {
                onEditModeChange(value)
                field = value
            }
        }

    var selected: HUDElement? = null
        set(value) {
            if (field != value) {
                field = value
                mChildren?.fastForEach {
                    (it as? HUDElement)?.isSelected = it == value
                }
            }
        }


    private var elementSelector: HUDElementSelector? = null

    private var elementProperties: HUDElementProperties? = null


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

        onSkinDataChange(skinData)
        onEditModeChange(isInEditMode)

        elementSelector = HUDElementSelector(this) attachTo parent
        parent.registerTouchArea(elementSelector)

        elementProperties = HUDElementProperties(this) attachTo parent
        parent.registerTouchArea(elementProperties)
    }


    private fun onEditModeChange(value: Boolean) {
        mChildren?.forEach { (it as? HUDElement)?.isInEditMode = value }
    }

    private fun onSkinDataChange(layoutData: HUDSkinData) {

        mChildren?.filterIsInstance<HUDElement>()?.forEach(IEntity::detachSelf)

        // First pass: We attach everything so that elements can reference between them when
        // applying default layout.
        layoutData.elements.forEach { data ->
            val element = data.type.create()
            element.elementData = data
            attachChild(element)
        }

        applyDefaultLayout()

        // Second pass: Apply custom element data that will override the default layout if any.
        mChildren?.forEach { (it as? HUDElement)?.onApplyElementSkinData() }

        addAnchorNodes()
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

    private fun addAnchorNodes() {

        fun addAnchorNodeLine(element: HUDElement) {

            val pointOnParent = Vec2(
                drawWidth * element.anchor.x,
                drawHeight * element.anchor.y
            )

            val pointFromChild = Vec2(
                element.drawX + element.drawWidth * element.origin.x,
                element.drawY + element.drawHeight * element.origin.y
            )

            element.nodeLine = Line().apply {
                fromPoint = pointFromChild
                toPoint = pointOnParent
                color = ColorARGB(0xFFF27272)
                lineWidth = 10f
                isVisible = false
            }

            attachChild(element.nodeLine!!)
        }

        mChildren?.filterIsInstance<Line>()?.forEach(IEntity::detachSelf)
        mChildren?.toList()?.forEach {
            if (it is HUDElement) {
                addAnchorNodeLine(it)
            }
        }
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onGameplayUpdate(game, stat, pSecondsElapsed)
        }
        elementSelector?.onGameplayUpdate(game, stat, pSecondsElapsed)
        super.onManagedUpdate(pSecondsElapsed)
    }

    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {
        if (super.onAreaTouched(event, localX, localY)) {
            return true
        }
        selected = null
        return false
    }


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


    private inline fun <reified T : HUDElement>getFirstOf() : T? {
        return mChildren?.firstOrNull { it is T } as? T
    }


    override fun getParent(): HUD? {
        // Nullable because during initialization the parent is not set yet.
        return super.getParent() as? HUD
    }
}


