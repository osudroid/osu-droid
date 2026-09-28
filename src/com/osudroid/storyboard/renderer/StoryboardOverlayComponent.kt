package com.osudroid.storyboard.renderer

import com.osudroid.storyboard.model.StoryboardLayerType
import com.reco1l.andengine.component.UIComponent
import org.andengine.engine.camera.Camera
import org.andengine.opengl.util.GLState
import ru.nsu.ccfit.zuev.osu.Config

/**
 * Renders the `Overlay` layer of a storyboard, meant to be attached above gameplay elements.
 *
 * The playback state, texture pool and batch are shared with the owning [StoryboardComponent].
 * The background dim is applied by darkening the sprites rather than by covering them, as this
 * layer is displayed in front of gameplay elements.
 */
class StoryboardOverlayComponent internal constructor(
    private val storyboardComponent: StoryboardComponent
) : UIComponent() {

    init {
        width = Config.getRES_WIDTH().toFloat()
        height = Config.getRES_HEIGHT().toFloat()
    }

    override fun doDraw(pGLState: GLState, pCamera: Camera) {
        beginDraw(pGLState)

        val playback = storyboardComponent.playback ?: return
        val batch = storyboardComponent.batch

        batch.begin(pGLState)
        storyboardComponent.drawLayers(playback, OVERLAY_LAYERS, drawAlpha, storyboardComponent.brightness)
        batch.end()
    }

    companion object {
        private val OVERLAY_LAYERS = arrayOf(StoryboardLayerType.Overlay)
    }
}
