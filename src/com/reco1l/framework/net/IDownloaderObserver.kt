package com.reco1l.framework.net

/**
 * @author Reco1l
 */
interface IDownloaderObserver
{
    fun onDownloadStart(downloader: Downloader)
    {
    }

    fun onDownloadEnd(downloader: Downloader)
    {
    }

    fun onDownloadCancel(downloader: Downloader)
    {
    }

    fun onDownloadUpdate(downloader: Downloader)
    {
    }

    fun onDownloadFail(downloader: Downloader)
    {
    }
}
