package com.osudroid.ui.v2.songselect

import com.osudroid.data.*
import com.osudroid.ui.v2.*
import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.theme.*
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

private val textBufferRef = MutableReference<CompoundBuffer?>(null)
private val backgroundBufferRef = MutableReference<UIBox.BoxVBO?>(null)



@Suppress("LeakingThis")
open class BeatmapPanel(private val beatmapSetPanel: BeatmapSetPanel, val beatmapInfo: BeatmapInfo) : UIContainer() {

    private var isExpanded = false


    init {
        width = Size.Full
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        style = {
            radius = Radius.XL
            backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.6f)
            borderColor = it.accentColor.copy(alpha = 0.3f)
            borderWidth = 0.25f.srem
        }
        background?.apply {
            bufferSharingMode = BufferSharingMode.Dynamic
            bufferReference = backgroundBufferRef
        }

        linearContainer {
            orientation = Orientation.Horizontal
            style = {
                padding = Vec4(4f.srem)
                spacing = 2f.srem
            }

            +StarRatingBadge().apply {
                rating = beatmapInfo.getStarRating().toDouble()
                sizeVariant = SizeVariant.Small
                applyColorsInstantly()
            }

            text {
                text = beatmapInfo.version
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                bufferSharingMode = BufferSharingMode.Dynamic
                bufferReference = textBufferRef
                style = {
                    color = it.accentColor
                    fontFamily = Fonts.TorusBold
                }
            }

        }

    }

    override fun onManagedUpdate(deltaTimeSec: Float) {

        val targetBorderColor = if (isExpanded) Theme.current.accentColor else Theme.current.accentColor.copy(alpha = 0.3f)
        val targetBorderWidth = if (isExpanded) 0.5f.srem else 0.25f.srem
        val targetWidth = if (isExpanded) parent.innerWidth + 2f.rem else parent.innerWidth

        borderColor = Interpolation.colorLerp(deltaTimeSec, 0.1f, borderColor, targetBorderColor)
        borderWidth = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, borderWidth, targetBorderWidth, 0.5f)
        width = Interpolation.floatLerpWithSnap(deltaTimeSec, 0.1f, width, targetWidth, 0.5f)

        super.onManagedUpdate(deltaTimeSec)
    }


    fun expand() {
        isExpanded = true
    }

    fun collapse() {
        isExpanded = false
    }


    override fun onAreaTouched(event: TouchEvent, localX: Float, localY: Float): Boolean {

        if (event.isActionDown) {
            /*background?.apply {
                clearModifiers(ModifierType.Alpha)
                fadeIn(0.2f).eased(Easing.Out)
            }*/
        }

        if (event.isActionUp) {

            if (SongSelect.selectedBeatmap == beatmapInfo && beatmapSetPanel.selectedPanel == this) {
                ResourceManager.getInstance().getSound("menuhit").play()

                GlobalManager.getInstance().gameScene.setOldScene(SongSelect)
                GlobalManager.getInstance().gameScene.startGame(beatmapInfo, null, ModMenu.enabledMods)
            } else {
                ResourceManager.getInstance().getSound("menuclick").play()
            }

            beatmapSetPanel.selectedPanel = this
        }

        if (event.isActionUp || event.isActionOutside || event.isActionCancel) {
            /*background?.apply {
                clearModifiers(ModifierType.Alpha)
                fadeTo(0.75f, 0.1f)
            }*/
        }
        return true
    }

}