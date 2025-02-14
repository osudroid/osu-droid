package com.reco1l.osu.hud

import android.util.Log
import androidx.annotation.*
import com.reco1l.andengine.Axes
import com.reco1l.andengine.container.ConstraintContainer
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDHealthBar
import com.reco1l.osu.hud.elements.HUDPPCounter
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.osu.hud.ProgressIndicatorType.Companion.PIE
import com.reco1l.osu.hud.ProgressIndicatorType.Companion.BAR
import com.reco1l.osu.hud.data.HUDLayoutData
import com.reco1l.osu.hud.elements.HUDElement
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.toolkt.kotlin.fastForEach
import org.anddev.andengine.engine.camera.hud.*
import org.anddev.andengine.entity.IEntity
import ru.nsu.ccfit.zuev.osu.*
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.*
import ru.nsu.ccfit.zuev.osuplus.BuildConfig

class GameplayHUD(private val stat: StatisticV2, private val game: GameScene) : ConstraintContainer() {


    /**
     * The layout data for the HUD.
     */
    var layoutData: HUDLayoutData = HUDLayoutData.Default
        set(value) {
            if (field != value) {
                onLayoutDataChange(value)
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


    init {
        // The engine we expect the HUD to be a effectively an instance of the AndEngine's HUD class.
        // Since we need ConstraintContainer features we set a HUD instance as the parent and we just
        // need to reference the parent of this container to set the engine's HUD.
        val parent = HUD()
        parent.attachChild(this)
        parent.registerTouchArea(this)

        relativeSizeAxes = Axes.Both
        setSize(1f, 1f)

        onLayoutDataChange(layoutData)
        onEditModeChange(isInEditMode)
    }


    override fun getParent(): HUD? {
        // Nullable because during initialization the parent is not set yet.
        return super.getParent() as? HUD
    }


    private fun onEditModeChange(value: Boolean) {
        mChildren?.forEach { (it as? HUDElement)?.isInEditMode = value }
    }

    private fun onLayoutDataChange(layoutData: HUDLayoutData) {
        mChildren?.filterIsInstance<HUDElement>()?.forEach(IEntity::detachSelf)

        // First pass: We attach everything so that elements can reference between them in the second
        // pass for constraints.
        layoutData.elements.forEach { (tag, data) ->

            val element = createElementFromTag(tag)
            attachChild(element)

            if (BuildConfig.DEBUG) {
                Log.i("GameplayHUD", "Attached element: $tag with data: $data")
            }
        }

        // Second pass: We apply the constraints.
        layoutData.elements.forEach { (tag, data) ->
            getElementByTag(tag)!!.onElementDataChange(data)
        }
    }


    override fun onManagedUpdate(pSecondsElapsed: Float) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onGameplayUpdate(game, stat, pSecondsElapsed)
        }
        super.onManagedUpdate(pSecondsElapsed)
    }

    fun onNoteHit(statistics: StatisticV2) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onNoteHit(statistics)
        }
    }

    fun onBreakStateChange(isBreak: Boolean) {
        mChildren?.fastForEach {
            (it as? HUDElement)?.onBreakStateChange(isBreak)
        }
    }


    fun getElementByTag(tag: String): HUDElement? {
        return mChildren?.firstOrNull { it is HUDElement && it.tag == tag } as? HUDElement
    }

    companion object {

        /**
         * Creates a new HUD element from its tag.
         *
         * This is a factory method that creates a new instance of a HUD element based on the tag.
         * If new HUD elements are added, they should be added here too.
         */
        fun createElementFromTag(tag: String): HUDElement {
            return when (tag) {
                "healthBar" -> HUDHealthBar()
                "ppCounter" -> HUDPPCounter()
                "scoreCounter" -> HUDScoreCounter()
                "comboCounter" -> HUDComboCounter()
                "accuracyCounter" -> HUDAccuracyCounter()
                "pieSongProgress" -> HUDPieSongProgress()
                else -> throw IllegalArgumentException("Unknown tag: $tag")
            }
        }

    }

}


@IntDef(PIE, BAR)
annotation class ProgressIndicatorType {
    companion object {
        const val PIE = 0
        const val BAR = 1
    }
}


