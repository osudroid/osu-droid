package com.reco1l.legacy.graphics.texture

import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.os.Build
import android.view.Surface
import org.andengine.engine.Engine
import org.andengine.opengl.texture.PixelFormat
import org.andengine.opengl.texture.Texture
import org.andengine.opengl.texture.TextureOptions
import org.andengine.opengl.texture.region.TextureRegion
import org.andengine.opengl.util.GLState
import java.io.File

class VideoTexture(val source: String, engine: Engine)
    : Texture(engine.textureManager, PixelFormat.RGBA_8888, TextureOptions.BILINEAR, null)
{

    private val player = MediaPlayer().apply {

        setDataSource(source)
        setVolume(0f, 0f)
        isLooping = false
        prepare()
    }


    private var surfaceTexture: SurfaceTexture? = null


    init
    {
        mTarget = GL_TEXTURE_EXTERNAL_OES
    }


    override fun getWidth() = player.videoWidth
    override fun getHeight() = player.videoHeight


    override fun writeTextureToHardware(pGLState: GLState)
    {
        surfaceTexture?.release()
        surfaceTexture = SurfaceTexture(mHardwareTextureID)

        val surface = Surface(surfaceTexture)
        player.setSurface(surface)
        surface.release()
    }

    override fun unloadFromHardware(pGLState: GLState?)
    {
        surfaceTexture?.release()
        surfaceTexture = null
        super.unloadFromHardware(pGLState)
    }


    override fun bind(pGLState: GLState?)
    {
        if (!isLoadedToHardware)
            return

        try
        {
            surfaceTexture?.updateTexImage()
        }
        catch (e: Exception)
        {
            isUpdateOnHardwareNeeded = true
        }
    }


    fun play() = player.start()
    fun pause() = player.pause()
    fun release() = player.release()


    fun seekTo(ms: Int)
    {
        // Unfortunately in old versions we can't seek at closest frame from the desired position.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            player.seekTo(ms.toLong(), MediaPlayer.SEEK_CLOSEST)
        else
            player.seekTo(ms)
    }

    fun setPlaybackSpeed(speed: Float)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            val newParams = player.playbackParams.setSpeed(speed)
            player.playbackParams = newParams
        }
    }


    fun toRegion() = TextureRegion(this, 0f, 0f, width.toFloat(), height.toFloat())


    companion object
    {
        /**
         * See [MediaPlayer documentation](https://developer.android.com/guide/topics/media/platform/supported-formats)
         */
        val SUPPORTED_VIDEO_FORMATS = arrayOf("3gp", "mp4", "mkv", "webm")

        fun isSupportedVideo(file: File): Boolean = file.extension.lowercase() in SUPPORTED_VIDEO_FORMATS
    }
}