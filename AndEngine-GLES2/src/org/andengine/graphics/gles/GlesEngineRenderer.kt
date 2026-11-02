package org.andengine.graphics.gles

import org.andengine.BuildConfig
import org.andengine.engine.Engine
import org.andengine.graphics.DrawContext
import org.andengine.graphics.view.FrameRenderer
import org.andengine.graphics.view.IRendererListener
import org.andengine.util.debug.Debug

/**
 * Draws the frames of an [Engine] on a GLES context whose config was picked by [configChooser].
 *
 * @param glFactory Creates the GL entry points for every new context. Tests pass a fake.
 */
class GlesEngineRenderer @JvmOverloads constructor(
    private val engine: Engine,
    private val configChooser: GlesConfigChooser,
    private val rendererListener: IRendererListener?,
    private val glFactory: () -> GL32 = { AndroidGL32() }
) : FrameRenderer {

    private val drawContext: DrawContext = engine.drawContext
    private val multiSampling: Boolean = engine.engineOptions.renderOptions.isMultiSampling

    override fun onContextCreated() {
        synchronized(DRAW_LOCK) {
            val renderOptions = engine.engineOptions.renderOptions
            // New context: detect capabilities, start with an empty state cache and replay the
            // resource registry (shader programs, AndEngine managers and the app's restorers).
            // Must run before GlesBackend.resetDrawState(), which sets its defaults through that new cache.
            // This also happens for the very first context, so the managers reload and the engine
            // reports one lagging frame (Engine.mIsLagging) right after startup; harmless.
            GlesBackend.onContextCreated(glFactory())
            if (BuildConfig.DEBUG) {
                Debug.d(
                    "EGLCONFIG: EGLConfig(Red=" + configChooser.redSize + ", Green=" + configChooser.greenSize +
                        ", Blue=" + configChooser.blueSize + ", Alpha=" + configChooser.alphaSize +
                        ", Depth=" + configChooser.depthSize + ", Stencil=" + configChooser.stencilSize + ")"
                )
                Debug.d("MAX_TEXTURE_SIZE: " + GlesBackend.caps.maxTextureSize)
            }
            drawContext.resetModelViewGLMatrixStack()
            drawContext.resetProjectionGLMatrixStack()
            GlesBackend.resetDrawState(renderOptions.isDithering)

            rendererListener?.onSurfaceCreated(drawContext)
        }
    }

    override fun onSurfaceChanged(width: Int, height: Int) {
        engine.setSurfaceSize(width, height)
        GlesBackend.surfaceWidth = width
        GlesBackend.surfaceHeight = height
        GlesBackend.state.viewport(0, 0, width, height)
        drawContext.loadProjectionGLMatrixIdentity()

        rendererListener?.onSurfaceChanged(drawContext, width, height)
    }

    override fun onDrawFrame() {
        synchronized(DRAW_LOCK) {
            GlesBackend.beginFrame(multiSampling && configChooser.isCoverageMultiSampling)

            try {
                engine.onDrawFrame(drawContext)
                GlesDebugOutput.poll()
            } catch (e: InterruptedException) {
                Debug.e("Render thread interrupted!", e)
            }
        }
    }

    override fun onFrameSwapped() {
        engine.onFrameSwapped()
    }

    private companion object {
        /** Held while a new context is set up and while a frame is drawn, by every renderer. */
        val DRAW_LOCK = Any()
    }
}
