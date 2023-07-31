package com.reco1l.legacy.engine

import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.media.MediaPlayer
import android.os.Build
import android.view.Surface
import org.anddev.andengine.opengl.texture.Texture
import org.anddev.andengine.opengl.texture.TextureOptions
import org.anddev.andengine.opengl.texture.region.TextureRegion
import javax.microedition.khronos.opengles.GL10

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


    val isPlaying
        get() = player.isPlaying


    private val player = MediaPlayer().also {

        it.setDataSource(source)
        it.setVolume(0f, 0f)
        it.isLooping = false
        it.prepare()
    }


    private var surfaceTexture: SurfaceTexture? = null

    private var width = 0

    private var height = 0


    init
    {
        resolveFrameSize()
    }


    private fun resolveFrameSize()
    {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(source)

        width = retriever.extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
        height = retriever.extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
    }


    override fun getWidth() = width

    override fun getHeight() = height


    override fun writeTextureToHardware(pGL: GL10) = Unit

    override fun bindTextureOnHardware(pGL: GL10) = pGL.glBindTexture(GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)

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

        bindTextureOnHardware(pGL)

        if (surfaceTexture == null)
        {
            surfaceTexture = SurfaceTexture(mHardwareTextureID)

            val surface = Surface(surfaceTexture)
            player.setSurface(surface)
            surface.release()
        }

        try
        {
            surfaceTexture?.updateTexImage()
        }
        catch (_: Exception)
        {
            isUpdateOnHardwareNeeded = true
        }
    }


    fun play() = player.start()

    fun pause() = player.pause()

    fun stop() = player.stop()

    fun release() = player.release()

    fun seekTo(ms: Int) = player.seekTo(ms)


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
        const val GL_TEXTURE_EXTERNAL_OES = 0x8D65
    }
}