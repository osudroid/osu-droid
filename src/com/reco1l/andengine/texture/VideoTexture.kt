package com.reco1l.andengine.texture

import android.graphics.*
import android.media.*
import android.opengl.*
import android.opengl.GLES32.GL_CLAMP_TO_EDGE
import android.opengl.GLES32.GL_LINEAR
import android.opengl.GLES20.GL_NEAREST
import android.util.Log
import android.view.*
import org.andengine.opengl.texture.*
import org.andengine.opengl.texture.PixelFormat
import org.andengine.opengl.util.GLState
import java.io.File

class VideoTexture(val source: String) : Texture(
    null,
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
        setOnErrorListener { _, what, extra ->
            Log.e("VideoTexture", "MediaPlayer error: what=$what extra=$extra")
            true
        }
        prepare()
    }

    // Cache video dimensions after prepare(); some decoders report 0×0 until
    // the first frame is decoded. UIVideoSprite registers setOnVideoSizeChangedListener
    // to fix up the TextureRegion lazily when this happens.
    private var cachedWidth  = player.videoWidth
    private var cachedHeight = player.videoHeight


    private var surfaceTexture: SurfaceTexture? = null

    // ST transform matrix provided by SurfaceTexture — applied in the vertex shader to
    // correctly orient the UV coordinates (hardware decoders often flip/rotate them).
    private val stMatrix = FloatArray(16).also {
        android.opengl.Matrix.setIdentityM(it, 0)
    }

    /**
     * Latches the latest decoded frame into the OES texture and updates the ST transform matrix.
     * Must be called once per frame, BEFORE [getTransformMatrix] and BEFORE [bind].
     */
    fun latch() {
        if (isLoadedToHardware) {
            surfaceTexture?.updateTexImage()
            surfaceTexture?.getTransformMatrix(stMatrix)
        }
    }

    /** Returns the ST transform matrix updated by the most recent [latch] call. */
    fun getTransformMatrix(): FloatArray = stMatrix

    override fun writeTextureToHardware(pGLState: GLState) {
        // Nothing – the SurfaceTexture pushes frames itself.
    }

    /**
     * Called by [TextureManager.onReload] after context loss.
     * We must release the old [SurfaceTexture] here because [loadToHardware] will create
     * a fresh one; leaving the old one alive leaks its native resources.
     */
    override fun setNotLoadedToHardware() {
        surfaceTexture?.release()
        surfaceTexture = null
        super.setNotLoadedToHardware()
    }

    override fun loadToHardware(pGLState: GLState) {
        // Release any lingering SurfaceTexture (safety net — normally cleared by
        // setNotLoadedToHardware, but guard against direct loadToHardware calls too).
        surfaceTexture?.release()
        surfaceTexture = null

        val textures = IntArray(1)
        GLES32.glGenTextures(1, textures, 0)
        mHardwareTextureID = textures[0]

        // OES external textures must be bound to the OES target.
        GLES32.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)

        // Required filter settings for GL_TEXTURE_EXTERNAL_OES.
        GLES32.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MIN_FILTER,
            GL_LINEAR
        )
        GLES32.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_MAG_FILTER,
            GL_LINEAR
        )
        GLES32.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_S,
            GL_CLAMP_TO_EDGE
        )
        GLES32.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES32.GL_TEXTURE_WRAP_T,
            GL_CLAMP_TO_EDGE
        )

        surfaceTexture = SurfaceTexture(mHardwareTextureID)

        val surface = Surface(surfaceTexture)
        player.setSurface(surface)
        surface.release()

        mUpdateOnHardwareNeeded = false
    }

    override fun unloadFromHardware(pGLState: GLState) {
        surfaceTexture?.release()
        surfaceTexture = null
        super.unloadFromHardware(pGLState)
    }

    /** Binds the OES texture. Call [latch] first to update the frame and ST matrix. */
    override fun bind(pGLState: GLState) {
        if (isLoadedToHardware) {
            GLES32.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mHardwareTextureID)
        }
    }

    override fun getWidth(): Int  = if (cachedWidth  > 0) cachedWidth  else player.videoWidth.also  { if (it > 0) cachedWidth  = it }
    override fun getHeight(): Int = if (cachedHeight > 0) cachedHeight else player.videoHeight.also { if (it > 0) cachedHeight = it }

    /** Update the cached dimensions. Called by UIVideoSprite via OnVideoSizeChangedListener. */
    fun updateCachedSize(width: Int, height: Int) {
        if (width > 0)  cachedWidth  = width
        if (height > 0) cachedHeight = height
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