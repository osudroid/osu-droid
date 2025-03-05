package com.reco1l.osu.hud

import com.reco1l.andengine.*
import com.reco1l.andengine.container.Container
import com.reco1l.andengine.shape.*
import com.reco1l.framework.ColorARGB
import com.reco1l.framework.math.Vec2
import com.reco1l.osu.hud.editor.HUDElementOverlay
import com.reco1l.osu.hud.elements.HUDAccuracyCounter
import com.reco1l.osu.hud.elements.HUDAverageOffsetCounter
import com.reco1l.osu.hud.elements.HUDComboCounter
import com.reco1l.osu.hud.elements.HUDHealthBar
import com.reco1l.osu.hud.elements.HUDHitErrorMeter
import com.reco1l.osu.hud.elements.HUDLinearSongProgress
import com.reco1l.osu.hud.elements.HUDPPCounter
import com.reco1l.osu.hud.elements.HUDPieSongProgress
import com.reco1l.osu.hud.elements.HUDScoreCounter
import com.reco1l.osu.hud.elements.HUDUnstableRateCounter
import com.reco1l.osu.ui.entity.GameplayLeaderboard
import com.reco1l.toolkt.kotlin.capitalize
import org.anddev.andengine.input.touch.TouchEvent
import kotlin.math.abs
import kotlin.reflect.KClass


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
    leaderboard(GameplayLeaderboard::class);

    companion object {
        operator fun get(type: KClass<out HUDElement>) = entries.first { it.type == type }
        operator fun get(name: String) = entries.find { it.name == name }
    }
}


abstract class HUDElement : Container(), IGameplayEvents {

    /**
     * Returns the name of this element.
     */
    open val name = HUDElements[this::class].name.replace('_', ' ').capitalize()

    /**
     * Indicates whether the element is selected.
     */
    val isSelected
        get() = (parent as? GameplayHUD)?.selected == this


    private var editorOverlay: HUDElementOverlay? = null

    private var connectionLine: Line? = null

    private var isInEditMode = false


    //region Skinning

    /**
     * Sets the skin data of the element.
     */
    open fun setSkinData(data: HUDElementSkinData?) {
        if (data != null) {
            anchor = data.anchor
            origin = data.origin
            setScale(data.scale.x, data.scale.y)
            setPosition(data.position.x, data.position.y)
        }
    }

    /**
     * Creates a [HUDElementSkinData] object with the current element's skin data.
     */
    open fun getSkinData() = HUDElementSkinData(
        type = this::class,
        anchor = anchor,
        origin = origin,
        scale = scale,
        position = Vec2(x, y)
    )

    //endregion

    //region Element events

    /**
     * Sets the edit mode of the element.
     */
    open fun setEditMode(value: Boolean) {
        isInEditMode = value

        if (value) {
            background = Box().apply {
                color = ColorARGB(0x29F27272)
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

    //region Entity events

    private var initialX = 0f
    private var initialY = 0f

    private var wasSelected = false
    private var wasMoved = false


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (isInEditMode) {
            val hud = parent as GameplayHUD

            val parentLocalX = drawX + localX
            val parentLocalY = drawY + localY

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

    override fun invalidateTransformations() {
        super.invalidateTransformations()
        editorOverlay?.invalidateTransformations()
    }

    //endregion

    //region Edit mode

    private fun applyClosestAnchorOrigin() {

        val drawSize = drawSize * scale
        val drawPosition = anchorOffset + position - drawSize * origin
        val parentDrawSize = (parent as ExtendedEntity).drawSize

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
            val previousAnchorOffset = anchorOffset
            anchor = closest
            position -= anchorOffset - previousAnchorOffset
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
            connectionLine = Line().apply {
                color = ColorARGB(0xFFF27272)
                lineWidth = 10f
                alpha = 0f
            }
            parent!!.attachChild(connectionLine!!)
        }

        connectionLine!!.fromPoint = anchorOffset
        connectionLine!!.toPoint = (editorOverlay?.outline?.drawPosition ?: Vec2.Zero) + drawSize * scale.absolute() * origin
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
