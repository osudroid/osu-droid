package org.andengine.opengl.texture.atlas.bitmap

import org.andengine.graphics.texture.Pixels
import org.andengine.opengl.texture.atlas.bitmap.source.IBitmapTextureAtlasSource
import org.andengine.opengl.texture.atlas.bitmap.source.IPixelTextureAtlasSource
import org.andengine.util.debug.Debug

/**
 * The pixels of all sources of a [BitmapTextureAtlas], loaded on a background thread so the GL thread
 * only has to upload them. Created on the GL thread with a snapshot of the atlas' sources and their
 * positions; [run] loads them on the worker. The GL thread then [take]s the result once, or the
 * preparation is [cancel]led and its pixels dropped.
 *
 * Each source is asked for the atlas' variant ([premultiplied] or straight bytes). A source whose
 * [IPixelTextureAtlasSource.onLoadPixels] returned `null` keeps a `null` entry: the upload uses its
 * bitmap path, as it would without a preparation; so does an entry of the other variant. If any source throws, the
 * preparation [isFailed] and the atlas loads on the synchronous path instead.
 *
 * Thread-safe: the state and the pixels are guarded by this object's monitor; the snapshot is never
 * modified after construction.
 */
class PixelPreparation @JvmOverloads constructor(
    sources: List<IPixelTextureAtlasSource>,
    width: Int,
    height: Int,
    val premultiplied: Boolean = false
) : Runnable {

    private val sources: Array<IPixelTextureAtlasSource> = sources.toTypedArray()
    private val xs = IntArray(sources.size) { sources[it].textureX }
    private val ys = IntArray(sources.size) { sources[it].textureY }

    /** Upper bound of the memory the pixels take: the whole atlas as RGBA bytes. */
    val estimatedBytes: Long = estimateBytes(width, height)

    private var state = PENDING
    private var finished = false
    private var pixels: Array<Pixels?>? = null
    private var preparedBytes = 0L

    /**
     * Loads the pixels of every source; runs on the worker thread. Never throws: any failure (also an
     * [Error]) is logged and marks the preparation failed, so the atlas loads on the synchronous path and
     * the worker keeps running.
     */
    override fun run() {
        var result: Array<Pixels?>? = null
        var bytes = 0L
        var failure: Throwable? = null
        try {
            val loadedPixels = arrayOfNulls<Pixels>(sources.size)
            for (i in sources.indices) {
                if (isCancelled) break
                val loaded = sources[i].onLoadPixels(premultiplied)
                loadedPixels[i] = loaded
                if (loaded != null) bytes += loaded.data.remaining()
            }
            result = loadedPixels
        } catch (t: Throwable) {
            failure = t
        } finally {
            synchronized(this) {
                if (state == PENDING) {
                    if (result == null) {
                        state = FAILED
                    } else {
                        pixels = result
                        preparedBytes = bytes
                        state = READY
                    }
                }
                finished = true
            }
        }

        if (failure != null) {
            try {
                Debug.e("Preparing texture pixels failed, loading them on the GL thread instead", failure)
            } catch (t: Throwable) {
                // Logging must not end the worker either.
            }
        }
    }

    /** Marks a preparation that could not be started as failed, so the atlas loads synchronously. */
    fun fail() = synchronized(this) {
        if (state == PENDING) state = FAILED
        finished = true
    }

    /** Drops the result (or the result still being loaded). */
    fun cancel() = synchronized(this) {
        state = CANCELLED
        pixels = null
    }

    val isReady: Boolean get() = synchronized(this) { state == READY }
    val isFailed: Boolean get() = synchronized(this) { state == FAILED }
    val isCancelled: Boolean get() = synchronized(this) { state == CANCELLED }

    /** Whether [run] has returned (or will never run); a cancelled preparation holds memory until then. */
    val isFinished: Boolean get() = synchronized(this) { finished }

    /** Bytes of the loaded pixels, once [isReady]. */
    val bytes: Long get() = synchronized(this) { preparedBytes }

    /**
     * Hands the pixels over for the upload and forgets them; `null` unless [isReady]. Also `null`
     * when [current] (the atlas' sources at upload time) no longer matches the snapshot, e.g. a source
     * was added or moved meanwhile; the atlas then loads on the synchronous path.
     */
    fun take(current: List<IBitmapTextureAtlasSource>): Array<Pixels?>? = synchronized(this) {
        if (state != READY) return null
        val result = pixels
        pixels = null
        state = CANCELLED
        if (!matches(current)) null else result
    }

    /** Whether [current] (an atlas' sources) are still the snapshot: the same sources at the same positions. */
    fun matches(current: List<IBitmapTextureAtlasSource>): Boolean {
        if (current.size != sources.size) return false
        for (i in sources.indices) {
            val source = current[i]
            if (source !== sources[i] || source.textureX != xs[i] || source.textureY != ys[i]) return false
        }
        return true
    }

    companion object {
        /** Memory of a [width] x [height] atlas as RGBA bytes. */
        @JvmStatic
        fun estimateBytes(width: Int, height: Int): Long = width.toLong() * height * 4

        private const val PENDING = 0
        private const val READY = 1
        private const val FAILED = 2
        private const val CANCELLED = 3
    }
}
