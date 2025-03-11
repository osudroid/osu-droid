package com.reco1l.andengine.texture

import android.graphics.*
import android.media.*
import android.opengl.*
import android.view.*
import org.anddev.andengine.opengl.texture.*
import java.io.*
import javax.microedition.khronos.opengles.*
import javax.microedition.khronos.opengles.GL10.*

class VideoTexture(val source: String) : Texture(
    PixelFormat.UNDEFINED,
    TextureOptions(
        GL_NEAREST,
        GL_LINEAR,
        GL_CLAMP_TO_EDGE,
        GL_CLAMP_TO_EDGE,
        false
    ),
    null
) {

    val player = MediaPlayer().apply {

        setDataSource(source)
        setVolume(0f, 0f)
        isLooping = false
        prepare()
    }


    private var surfaceTexture: SurfaceTexture? = null


    override fun writeTextureToHardware(pGL: GL10) {
        // Nothing to write, the texture is handled externally by the SurfaceTexture.
    }

    override fun bindTextureOnHardware(pGL: GL10) {

        pGL.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)

        surfaceTexture = SurfaceTexture(mHardwareTextureID)

        val surface = Surface(surfaceTexture)
        player.setSurface(surface)
        surface.release()
    }

    override fun deleteTextureOnHardware(pGL: GL10?) {

        surfaceTexture?.release()
        surfaceTexture = null

        super.deleteTextureOnHardware(pGL)
    }


    override fun bind(pGL: GL10) {
        if (isLoadedToHardware) {
            surfaceTexture!!.updateTexImage()
        }
    }


    override fun getWidth(): Int {
        return player.videoWidth
    }

    override fun getHeight(): Int {
        return player.videoHeight
    }


    companion object {

        /**
         * See [MediaPlayer documentation](https://developer.android.com/guide/topics/media/platform/supported-formats)
         */
        private val SUPPORTED_VIDEO_FORMATS = arrayOf("3gp", "mp4", "mkv", "webm")


        /**
         * Checks if the file is a supported video format.
         */
        fun isSupportedVideo(file: File): Boolean {
            return file.extension.lowercase() in SUPPORTED_VIDEO_FORMATS
        }

    }
}