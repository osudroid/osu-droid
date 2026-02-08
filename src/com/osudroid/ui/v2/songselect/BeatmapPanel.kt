package com.osudroid.ui.v2.songselect

import com.edlplan.framework.easing.*
import com.osudroid.data.*
import com.osudroid.ui.*
import com.osudroid.ui.v2.modmenu.*
import com.reco1l.andengine.*
import com.reco1l.andengine.buffered.*
import com.reco1l.andengine.component.*
import com.reco1l.andengine.container.*
import com.reco1l.andengine.modifier.*
import com.reco1l.andengine.shape.*
import com.reco1l.andengine.sprite.*
import com.reco1l.andengine.text.*
import com.reco1l.andengine.theme.Size
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class BeatmapPanel(private val beatmapSetPanel: BeatmapSetPanel, val beatmapInfo: BeatmapInfo) : UIContainer() {

    private val foreground: UIBox

    init {
        width = Size.Full
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        style = {
            radius = 14f
            backgroundColor = (it.accentColor * 0.1f).copy(alpha = 0.75f)
        }

        linearContainer {
            orientation = Orientation.Horizontal
            padding = Vec4(12f, 24f)
            spacing = 10f

            badge {
                val starRating = beatmapInfo.getStarRating()

                text = starRating.roundBy(2).toString()
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                leadingIcon = UISprite(ResourceManager.getInstance().getTexture("star-xs"))
                sizeVariant = SizeVariant.Small
                color = if (starRating >= 6.5) Color4(0xFFFFD966) else Color4.Black.copy(alpha = 0.75f)
                style = {}
                backgroundColor = OsuColors.getStarRatingColor(starRating.toDouble())
            }

            text {
                text = beatmapInfo.version
                anchor = Anchor.CenterLeft
                origin = Anchor.CenterLeft
                style = { color = it.accentColor }
            }

        }

        foreground = box {
            paintStyle = PaintStyle.Outline
            cornerRadius = 14f
            lineWidth = 2f
            style = {
                color = it.accentColor * 0.2f
            }
        }

    }


    fun expand() {
        clearModifiers(ModifierType.SizeX)
        sizeToX(parent.innerWidth + 130f, 0.1f)

        (foreground as UIBox).apply {
            lineWidth = 4f
            clearModifiers(ModifierType.Color)
            colorTo(Theme.current.accentColor, 0.1f)
        }
    }

    fun collapse() {
        clearModifiers(ModifierType.SizeX)
        sizeToX(parent.innerWidth, 0.1f)

        (foreground as UIBox).apply {
            lineWidth = 2f
            clearModifiers(ModifierType.Color)
            colorTo(Theme.current.accentColor * 0.3f, 0.1f)
        }
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