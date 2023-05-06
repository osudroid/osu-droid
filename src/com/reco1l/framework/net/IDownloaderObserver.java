package com.reco1l.framework.net;

/**
 * @author Reco1l
 */
public interface IDownloaderObserver
{

    default void onDownloadStart(Downloader downloader)
    {
    }

    default void onDownloadEnd(Downloader downloader)
    {
    }

    default void onDownloadCancel(Downloader downloader)
    {
    }

    default void onDownloadUpdate(Downloader downloader)
    {
    }

    default void onDownloadFail(Downloader downloader)
    {
    }
}
