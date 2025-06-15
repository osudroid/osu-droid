package com.osudroid.ui.v2.hud

import com.osudroid.ui.v2.hud.elements.HUDAccuracyCounter
import com.osudroid.ui.v2.hud.elements.HUDAverageOffsetCounter
import com.osudroid.ui.v2.hud.elements.HUDBPMCounter
import com.osudroid.ui.v2.hud.elements.HUDBackButton
import com.osudroid.ui.v2.hud.elements.HUDComboCounter
import com.osudroid.ui.v2.hud.elements.HUDGreatCounter
import com.osudroid.ui.v2.hud.elements.HUDHealthBar
import com.osudroid.ui.v2.hud.elements.HUDHitErrorMeter
import com.osudroid.ui.v2.hud.elements.HUDLinearSongProgress
import com.osudroid.ui.v2.hud.elements.HUDMehCounter
import com.osudroid.ui.v2.hud.elements.HUDMissCounter
import com.osudroid.ui.v2.hud.elements.HUDNotesPerSecondCounter
import com.osudroid.ui.v2.hud.elements.HUDOkCounter
import com.osudroid.ui.v2.hud.elements.HUDPPCounter
import com.osudroid.ui.v2.hud.elements.HUDPieSongProgress
import com.osudroid.ui.v2.hud.elements.HUDScoreCounter
import com.osudroid.ui.v2.hud.elements.HUDTapsPerSecondCounter
import com.osudroid.ui.v2.hud.elements.HUDUnstableRateCounter
import com.reco1l.andengine.*
import com.reco1l.andengine.container.UIContainer
import com.reco1l.andengine.shape.*
import com.reco1l.framework.Color4
import com.reco1l.framework.math.Vec2
import com.osudroid.ui.v2.hud.editor.HUDElementOverlay
import com.osudroid.ui.v2.hud.elements.HUDLeaderboard
import com.reco1l.andengine.component.*
import com.reco1l.toolkt.kotlin.capitalize
import com.rian.osu.beatmap.hitobject.HitObject
import org.anddev.andengine.input.touch.TouchEvent
import kotlin.math.abs
import kotlin.reflect.KClass
import ru.nsu.ccfit.zuev.osu.Config
import ru.nsu.ccfit.zuev.osu.game.GameScene
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2


/**
 * List all the elements that can be added to the HUD.
 */
@Suppress("EnumEntryName")
enum class HUDElements(val type: KClass<out HUDElement>) {

    accuracy_counter(HUDAccuracyCounter::class),
    combo_counter(HUDComboCounter::class),
    health_bar(HUDHealthBar::class),
    pie_song_progress(HUDPieSongProgress::class),
    pp_counter(HUDPPCounter::class),
    score_counter(HUDScoreCounter::class),
    ur_counter(HUDUnstableRateCounter::class),
    avg_offset_counter(HUDAverageOffsetCounter::class),
    hit_error_meter(HUDHitErrorMeter::class),
    linear_song_progress(HUDLinearSongProgress::class),
    great_counter(HUDGreatCounter::class),
    ok_counter(HUDOkCounter::class),
    meh_counter(HUDMehCounter::class),
    miss_counter(HUDMissCounter::class),
    bpm_counter(HUDBPMCounter::class),
    notes_per_second_counter(HUDNotesPerSecondCounter::class),
    taps_per_second_counter(HUDTapsPerSecondCounter::class),
    back_button(HUDBackButton::class),
    leaderboard(HUDLeaderboard::class);

    companion object {
        operator fun get(type: KClass<out HUDElement>) = entries.first { it.type == type }
        operator fun get(name: String) = entries.find { it.name == name }
    }
}


abstract class HUDElement : UIContainer(), IGameplayEvents {

    /**
     * Returns the name of this element.
     */
    open val name
        get() = HUDElements[this::class].name.replace('_', ' ').capitalize()

    /**
     * Indicates whether the element is selected.
     */
    val isSelected
        get() = (parent as? GameplayHUD)?.selected == this

    /**
     * Whether this [HUDElement] should be shown.
     */
    open val shouldBeShown = !Config.isHideInGameUI()

    /**
     * The overlay for this element to be used in edit mode.
     */
    var editorOverlay: HUDElementOverlay? = null

    /**
     * The restore data of the element.
     */
    var restoreData: HUDElementSkinData? = null


    protected var isInEditMode = false
        private set


    private var connectionLine: UILine? = null


    //region Skinning

    /**
     * Sets the skin data of the element.
     */
    open fun setSkinData(data: HUDElementSkinData?) {
        if (data == null) {
            return
        }

        anchor = data.anchor
        origin = data.origin
        setScale(data.scale)
        setPosition(data.position.x, data.position.y)

        // When the element is restored it's usually selected so we need to update the connection line.
        if (isSelected) {
            editorOverlay?.updateOutline()
            updateConnectionLine()
        }
    }

    /**
     * Creates a [HUDElementSkinData] object with the current element's skin data.
     */
    open fun getSkinData() = HUDElementSkinData(
        type = this::class,
        anchor = anchor,
        origin = origin,
        scale = (mScaleX + mScaleY) / 2f,
        position = Vec2(x, y)
    )

    /**
     * Restores the element to its original state before any changes were made.
     */
    fun restore() {
        setSkinData(restoreData)
    }

    //endregion

    //region Element events

    /**
     * Sets the edit mode of the element.
     */
    open fun setEditMode(value: Boolean) {
        isInEditMode = value

        if (value) {
            background = UIBox().apply {
                color = Color4(0x29F27272)
                alpha = 0.15f
            }
            editorOverlay = HUDElementOverlay(this)

            parent!!.attachChild(editorOverlay!!)
        } else {
            connectionLine?.detachSelf()
            editorOverlay?.detachSelf()

            connectionLine = null
            background = null
            editorOverlay = null
        }
    }

    /**
     * Called when the selection state of the element changes.
     */
    open fun onSelectionStateChange(isSelected: Boolean) {

        background?.clearEntityModifiers()
        background?.fadeTo(if (isSelected) 0.5f else 0.15f, 0.1f)

        editorOverlay?.clearEntityModifiers()
        editorOverlay?.fadeTo(if (isSelected) 1f else 0f, 0.1f)

        if (isSelected) {
            updateConnectionLine()
        } else {
            wasSelected = false
        }

        connectionLine?.clearEntityModifiers()
        connectionLine?.fadeTo(if (isSelected) 1f else 0f, 0.1f)
    }

    //endregion

    //region Gameplay events

    override fun onGameplayUpdate(gameScene: GameScene, secondsElapsed: Float) {}

    override fun onGameplayTouchDown(time: Float) {}

    override fun onHitObjectLifetimeStart(obj: HitObject) {}

    override fun onNoteHit(statistics: StatisticV2) {}

    override fun onBreakStateChange(isBreak: Boolean) {}

    override fun onAccuracyRegister(accuracy: Float) {}

    //endregion

    //region Entity events

    private var initialX = 0f
    private var initialY = 0f

    private var wasSelected = false
    private var wasMoved = false


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            val hud = parent as GameplayHUD

            val parentLocalX = absoluteX + localX
            val parentLocalY = absoluteY + localY

            val deltaX = parentLocalX - initialX
            val deltaY = parentLocalY - initialY

            if (event.isActionDown) {
                initialX = parentLocalX
                initialY = parentLocalY

                wasMoved = false
                wasSelected = hud.selected == this

                hud.selected = this
                return true
            }

            if (event.isActionMove) {
                // Preventing from moving the element if it's not selected.
                if (isSelected) {
                    move(deltaX, deltaY)
                    wasMoved = true

                    initialX = parentLocalX
                    initialY = parentLocalY
                    return true
                }
            }

            if (event.isActionUp) {
                if (wasSelected && isSelected && !wasMoved) {
                    hud.forEachElement { element ->

                        if (element != this && element.contains(parentLocalX, parentLocalY)) {
                            hud.selected = element
                            return@forEachElement
                        }
                    }
                }
                return true
            }
        }

        return false
    }

    override fun onInvalidateTransformations() {
        super.onInvalidateTransformations()
        editorOverlay?.onInvalidateTransformations()
    }

    //endregion

    //region Edit mode

    private fun applyClosestAnchorOrigin() {

        val drawSize = size * scale
        val drawPosition = anchorPosition + position - drawSize * origin
        val parentDrawSize = (parent as UIComponent).size

        val relativeTopLeft = drawPosition / parentDrawSize
        val relativeTopRight = (drawPosition + Vec2(drawSize.x, 0f)) / parentDrawSize
        val relativeBottomRight = (drawPosition + drawSize) / parentDrawSize
        val relativeBottomLeft = (drawPosition + Vec2(0f, drawSize.y)) / parentDrawSize

        val closest = Anchor.all.minBy {
            minOf(
                abs(relativeTopLeft.distance(it)),
                abs(relativeTopRight.distance(it)),
                abs(relativeBottomRight.distance(it)),
                abs(relativeBottomLeft.distance(it))
            )
        }

        if (anchor != closest) {
            val previousAnchorOffset = anchorPosition
            anchor = closest
            position -= anchorPosition - previousAnchorOffset
        }

        if (origin != closest) {
            val previousOriginOffset = -(size * scale * origin)
            val originOffset = -(size * scale * closest)
            origin = closest
            position -= originOffset - previousOriginOffset
        }
    }

    private fun updateConnectionLine() {

        if (connectionLine == null) {
            connectionLine = UILine().apply {
                color = Color4(0xFFF27272)
                lineWidth = 10f
                alpha = 0f
            }
            parent!!.attachChild(connectionLine!!)
        }

        connectionLine!!.fromPoint = anchorPosition
        connectionLine!!.toPoint = (editorOverlay?.outlinePosition ?: Vec2.Zero) + size * scale * origin
    }

    override fun setScaleX(pScaleX: Float) {
        super.setScaleX(pScaleX)
        updateConnectionLine()
    }

    override fun setScaleY(pScaleY: Float) {
        super.setScaleY(pScaleY)
        updateConnectionLine()
    }

    override fun setScale(pScale: Float) {
        super.setScale(pScale)
        updateConnectionLine()
    }

    override fun setScale(pScaleX: Float, pScaleY: Float) {
        super.setScale(pScaleX, pScaleY)
        updateConnectionLine()
    }

    override fun detachSelf(): Boolean {
        editorOverlay?.detachSelf()
        connectionLine?.detachSelf()

        return super.detachSelf()
    }

    /**
     * Moves the element by the specified delta.
     */
    fun move(deltaX: Float, deltaY: Float) {
        setPosition(x + deltaX, y + deltaY)
        applyClosestAnchorOrigin()
        updateConnectionLine()
    }

    /**
     * Removes the element from the HUD.
     */
    fun remove() {
        setEditMode(false)
        detachSelf()
    }

    //endregion
}
