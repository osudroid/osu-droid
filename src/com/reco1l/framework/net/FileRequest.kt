package com.reco1l.framework.net

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Response
import java.io.File

class FileRequest(val file: File, url: HttpUrl): WebRequest(url) {

    constructor(file: File, url: String): this(file, url.toHttpUrl())


    /**
     * The download observer.
     */
    var observer: IDownloaderObserver? = null

    /**
     * Whether the download is in progress, this is set to true once [execute] is called.
     */
    var isDownloading = false
        private set

    /**
     * Whether the download has been completed successfully.
     */
    val isCompleted: Boolean
        get() = progress == 100.0

    /**
     * The current download speed in kilobytes per second.
     */
    var speedKbps = 0f
        private set

    /**
     * The current download progress in 0 to 1 range.
     */
    var progress = -1.0
        private set

    /**
     * The download buffer size, by default [DEFAULT_BUFFER_SIZE].
     */
    var bufferSize = DEFAULT_BUFFER_SIZE



    override fun onResponseSuccess(response: Response) {

        val length = response.body!!.contentLength() * 1.0
        val stream = response.body!!.byteStream()

        val bytes = ByteArray(bufferSize)

        var totalBytes = 0L
        var bytesRead = 0

        stream.buffered(bufferSize).use { buffer ->

            file.outputStream().use { out ->

                isDownloading = true
                val startTime = System.currentTimeMillis()

                while (isDownloading && buffer.read(bytes).also { bytesRead = it } != -1) {

                    out.write(bytes, 0, bytesRead)
                    totalBytes += bytesRead.toLong()

                    val elapsedMilliseconds = (System.currentTimeMillis() - startTime).toFloat()

                    progress = totalBytes / length * 100
                    speedKbps = totalBytes / elapsedMilliseconds / 1024f * 1000f

                    observer?.onDownloadUpdate(this)
                }

                if (isDownloading || isCompleted) {
                    isDownloading = false
                    observer?.onDownloadEnd(this)
                    return
                }

                // This mean the file wasn't fully downloaded.
                if (file.exists()) {
                    file.delete()
                }
                observer?.onDownloadCancel(this)
            }
        }

    }

    override fun onResponseFail(exception: Exception) {

        isDownloading = false
        observer?.onDownloadFail(this, exception) ?: throw exception
    }


    override fun cancel() {
        isDownloading = false
        super.cancel()
    }

    override fun execute(): FileRequest {
        try {
            super.execute()
        } catch (e: Exception) {
            observer?.onDownloadFail(this, e) ?: throw e
        }
        return this
    }

}


interface IDownloaderObserver
{
    fun onDownloadEnd(downloader: FileRequest) = Unit

    fun onDownloadCancel(downloader: FileRequest) = Unit

    fun onDownloadUpdate(downloader: FileRequest) = Unit

    fun onDownloadFail(downloader: FileRequest, exception: Exception) = Unit
}
