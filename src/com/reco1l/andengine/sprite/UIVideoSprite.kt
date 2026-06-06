package com.reco1l.andengine.sprite

import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.os.Handler
import android.os.Looper
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.util.UnstableApi
import androidx.annotation.OptIn
import com.osudroid.utils.updateThread
import com.reco1l.andengine.texture.*
import org.anddev.andengine.engine.Engine
import org.anddev.andengine.engine.camera.Camera
import org.anddev.andengine.entity.sprite.*
import org.anddev.andengine.opengl.texture.region.*
import org.anddev.andengine.opengl.util.GLHelper
import javax.microedition.khronos.opengles.GL10

@OptIn(UnstableApi::class)
class UIVideoSprite(source: String, private val engine: Engine) : Sprite(0f, 0f, VideoTexture(source).let {
    TextureRegion(it, 0, 0, it.width, it.height)
}) {

    private val texture = textureRegion.texture as VideoTexture


    private var isMaliGPU: Boolean? = null


    init {
        engine.textureManager.loadTexture(texture)
    }


    override fun onInitDraw(pGL: GL10) {

        if (isMaliGPU == null) {
            isMaliGPU = pGL.glGetString(GL10.GL_RENDERER).contains("Mali", true)
        }

        super.onInitDraw(pGL)

        // Apparently there is either a bug or unintended behavior in Mali GPUs' OpenGL ES implementation.
        // Causes the wrong texture to be displayed when GL_TEXTURE_2D is enabled before enabling GL_TEXTURE_EXTERNAL_OES.
        if (isMaliGPU!!) {
            GLHelper.disableTextures(pGL)
        }

        pGL.glEnable(GL_TEXTURE_EXTERNAL_OES)
    }

    override fun drawVertices(gl: GL10, camera: Camera) {
        super.drawVertices(gl, camera)

        gl.glDisable(GL_TEXTURE_EXTERNAL_OES)
    }


    fun setOnReady(callback: Runnable) {
        texture.onReady = Runnable {
            updateThread {
                // Update the TextureRegion UV extents and Sprite vertex dimensions now that the actual video resolution is
                // known.
                // Both were constructed as 0×0 because ExoPlayer reports dimensions asynchronously via onVideoSizeChanged.
                val w = texture.videoWidth.toFloat()
                val h = texture.videoHeight.toFloat()
                textureRegion.setWidth(texture.videoWidth)
                textureRegion.setHeight(texture.videoHeight)
                setSize(w, h)

                // RectangularShape initializes mScaleCenterX/Y from constructor dimensions (0×0 here).
                // setSize() updates mWidth/mHeight but not the scale center, so we must fix it manually.
                // Without this, applyBackground's centering formula — which assumes scale is applied around
                // the entity center — places the video at the wrong screen position.
                setScaleCenter(w / 2f, h / 2f)

                mainHandler.post(callback)
            }
        }
    }

    fun release() {
        // Unloading the texture triggers deleteTextureOnHardware, which posts a single ordered block
        // to the main handler: setVideoSurface(null) --> surface teardown --> player.release().
        engine.textureManager.unloadTexture(texture)
    }

    fun play() {
        mainHandler.post { texture.player.play() }
    }

    fun pause() {
        mainHandler.post { texture.player.pause() }
    }

    fun seekTo(ms: Int) {
        mainHandler.post { texture.player.seekTo(ms.toLong()) }
    }

    fun setPlaybackSpeed(speed: Float) {
        mainHandler.post { texture.player.playbackParameters = PlaybackParameters(speed) }
    }


    override fun finalize() {
        // Guard against a partially-constructed object: if VideoTexture's init threw,
        // texture.player is null at the bytecode level despite Kotlin's non-null type.
        try {
            release()
        } catch (_: Throwable) {}

        super.finalize()
    }

    companion object {
        private val mainHandler = Handler(Looper.getMainLooper())
    }
}
