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
import com.reco1l.andengine.ui.*
import com.reco1l.framework.*
import com.reco1l.framework.math.*
import com.reco1l.toolkt.*
import org.anddev.andengine.input.touch.*
import ru.nsu.ccfit.zuev.osu.*

@Suppress("LeakingThis")
open class BeatmapPanel(private val beatmapSetPanel: BeatmapSetPanel, val beatmapInfo: BeatmapInfo) : UILinearContainer() {


    init {
        orientation = Orientation.Horizontal
        width = FillParent
        anchor = Anchor.TopRight
        origin = Anchor.TopRight
        padding = Vec4(12f, 24f)
        spacing = 10f

        background = UIBox().apply {
            cornerRadius = 14f
            applyTheme = {
                color = it.accentColor * 0.1f
                alpha = 0.75f
            }
            buffer = sharedBackgroundVBO
        }

        foreground = UIBox().apply {
            paintStyle = PaintStyle.Outline
            cornerRadius = 14f
            lineWidth = 2f
            applyTheme = {
                color = it.accentColor * 0.2f
            }
            buffer = sharedForegroundVBO
        }

        badge {
            val starRating = beatmapInfo.getStarRating()

            text = starRating.roundBy(2).toString()
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            leadingIcon = UISprite(ResourceManager.getInstance().getTexture("star-xs")).apply {
                buffer = sharedSpriteVBO
            }
            sizeVariant = SizeVariant.Small
            applyTheme = {}
            color = if (starRating >= 6.5) Color4(0xFFFFD966) else Color4.Black.copy(alpha = 0.75f)

            textEntity.buffer = sharedTextCB

            (background as UIBox).apply {
                color = OsuColors.getStarRatingColor(starRating.toDouble())
                buffer = sharedBadgeBackgroundVBO
            }
        }

        text {
            font = ResourceManager.getInstance().getFont("smallFont")
            text = beatmapInfo.version
            anchor = Anchor.CenterLeft
            origin = Anchor.CenterLeft
            applyTheme = { color = it.accentColor }
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
            background?.apply {
                clearModifiers(ModifierType.Alpha)
                fadeIn(0.2f).eased(Easing.Out)
            }
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
            background?.apply {
                clearModifiers(ModifierType.Alpha)
                fadeTo(0.75f, 0.1f)
            }
        }
        return true
    }


    companion object {
        private val sharedBackgroundVBO = UIBox.BoxVBO(14f, UICircle.approximateSegments(14f, 14f, 90f), PaintStyle.Fill).asSharedStatically()
        private val sharedForegroundVBO = UIBox.BoxVBO(14f, UICircle.approximateSegments(14f, 14f, 90f), PaintStyle.Outline).asSharedStatically()
        private val sharedBadgeBackgroundVBO = UIBox.BoxVBO(6f, UICircle.approximateSegments(6f, 6f, 90f), PaintStyle.Fill).asSharedStatically()
        private val sharedTextCB = CompoundBuffer(UIText.TextTextureBuffer(256), UIText.TextVertexBuffer(256)).asSharedDynamically()
        private val sharedSpriteVBO = UISprite.SpriteVBO().asSharedDynamically()
    }

}