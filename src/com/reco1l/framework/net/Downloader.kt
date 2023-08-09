package com.reco1l.framework.net

import com.reco1l.framework.extensions.className
import com.reco1l.framework.extensions.logE
import com.reco1l.framework.lang.async
import com.reco1l.framework.net.SizeMeasure.*
import okhttp3.ResponseBody
import java.io.File
import java.io.IOException

class Downloader(val file: File, private val url: String)
{

    var observer: IDownloaderObserver? = null

    /**
     * The exception got in case the download failed.
     */
    var exception: Exception? = null
        private set

    /**
     * `true` if this downloader has already been used and cannot be used anymore.
     */
    var isValid = true
        private set

    /**
     * Define if the downloader should execute in asynchronous, keep in mind executing on main thread will throw an exception.
     */
    var runOnAsync = true

    /**
     * `true` if currently downloading.
     */
    var isDownloading = false
        private set

    /**
     * `true` if the download has been completed successfully.
     */
    val isCompleted: Boolean
        get() = progress == 100.0

    /**
     * The current download speed in B/s.
     */
    var speed = 0.0
        private set

    /**
     * The current download progress, it'll be `-1` if the download hasn't been started.
     */
    var progress = -1.0
        private set

    /**
     * Set the download buffer size, by default [DEFAULT_BUFFER_SIZE].
     */
    var bufferSize = DEFAULT_BUFFER_SIZE



    constructor(path: String, url: String) : this(File(path), url)



    /**
     * Returns the current download speed in the desired measure
     *
     * @param measure The measurement for the return value, setting this to null will use Byte/s.
     * @see SizeMeasure
     */
    fun getSpeed(measure: SizeMeasure?): Double
    {
        return if (measure != null)
        {
            when (measure)
            {
                KBPS -> speed / 1024 * 1e9
                MBPS -> speed / (1024 * 1024) * 1e9
            }
        }
        else speed
    }



    private fun handleDownload()
    {
        try
        {
            Requester(url).use { onRequestSuccess(it.executeAndGetBody()) }
        }
        catch (e: Exception)
        {
            clear()
            exception = e

            observer?.onDownloadFail(this)
        }
    }

    private fun onRequestSuccess(body: ResponseBody)
    {
        val length = body.contentLength() * 1.0
        val buffer = ByteArray(bufferSize)
        val stream = body.byteStream()

        var total = 0L
        var bytes: Int

        try
        {
            stream.buffered(bufferSize).use {

                file.outputStream().use { out ->

                    isDownloading = true
                    observer?.onDownloadStart(this)

                    val startTime = System.nanoTime()

                    while (it.read(buffer).also { bytes = it } != -1 && isDownloading)
                    {
                        out.write(buffer, 0, bytes)
                        total += bytes.toLong()

                        val elapsedTime = (System.nanoTime() - startTime).toDouble()

                        progress = total / length * 100
                        speed = total / elapsedTime

                        observer?.onDownloadUpdate(this)
                    }
                    onLoopExit()
                }
            }
        }
        catch (e: IOException)
        {
            // We don't call clear() here to leave the last progress and the last speed when the download failed.
            isDownloading = false
            exception = e

            observer?.onDownloadFail(this)
        }
    }

    private fun onLoopExit()
    {
        if (isDownloading || isCompleted)
        {
            isDownloading = false
            observer?.onDownloadEnd(this)
            return
        }

        // This mean the file wasn't fully downloaded.
        if (file.exists())
        {
            file.delete()
        }
        observer?.onDownloadCancel(this)
    }



    fun download()
    {
        if (!isValid)
        {
            "This Downloader isn't more valid, it has already been used.".logE(className)
            return
        }
        isValid = false

        if (runOnAsync)
        {
            async { handleDownload() }
        }
        else
        {
            handleDownload()
        }
    }

    /**
     * Keep in mind if the file completed download the onCancel callback will not run.
     */
    fun cancel()
    {
        isDownloading = false
    }

    private fun clear()
    {
        isDownloading = false
        progress = -1.0
        speed = 0.0
    }
}
