package com.reco1l.andengine.texture

import android.graphics.*
import android.media.*
import android.opengl.*
import android.os.*
import android.view.*
import org.anddev.andengine.opengl.texture.*
import org.anddev.andengine.opengl.texture.region.*
import java.io.*
import javax.microedition.khronos.opengles.*

class VideoTexture(val source: String)

    : Texture(
    PixelFormat.UNDEFINED,
    TextureOptions(
        GL10.GL_NEAREST,
        GL10.GL_LINEAR,
        GL10.GL_CLAMP_TO_EDGE,
        GL10.GL_CLAMP_TO_EDGE,
        false
    ), null)
{

    private val player = MediaPlayer().apply {

        setDataSource(source)
        setVolume(0f, 0f)
        isLooping = false
        prepare()
    }


    private var surfaceTexture: SurfaceTexture? = null


    override fun getWidth() = player.videoWidth

    override fun getHeight() = player.videoHeight


    override fun writeTextureToHardware(pGL: GL10) = Unit

    override fun bindTextureOnHardware(pGL: GL10) = pGL.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)

    override fun deleteTextureOnHardware(pGL: GL10?)
    {
        surfaceTexture?.release()
        surfaceTexture = null
        super.deleteTextureOnHardware(pGL)
    }


    override fun bind(pGL: GL10)
    {
        if (!isLoadedToHardware)
            return

        if (surfaceTexture == null)
        {
            surfaceTexture = SurfaceTexture(mHardwareTextureID)

            val surface = Surface(surfaceTexture)
            player.setSurface(surface)
            surface.release()
        }

        try {
            surfaceTexture?.updateTexImage()
        } catch (_: Exception) {
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


    fun toRegion() = TextureRegion(this, 0, 0, width, height)


    companion object
    {
        /**
         * See [MediaPlayer documentation](https://developer.android.com/guide/topics/media/platform/supported-formats)
         */
        private val SUPPORTED_VIDEO_FORMATS = arrayOf("3gp", "mp4", "mkv", "webm")

        fun isSupportedVideo(file: File): Boolean = file.extension.lowercase() in SUPPORTED_VIDEO_FORMATS
    }
}