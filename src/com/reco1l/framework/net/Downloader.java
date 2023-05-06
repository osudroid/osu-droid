package com.reco1l.framework.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.reco1l.framework.lang.execution.Async;
import com.reco1l.framework.util.Logging;
import okhttp3.ResponseBody;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public final class Downloader
{

    public static int DEFAULT_BUFFER_SIZE = 1024;

    //----------------------------------------------------------------------------------------------------------------//

    private final File mFile;
    private final String mURL;

    private Exception mException;
    private IDownloaderObserver mObserver;

    private boolean
            mIsValid = true,
            mRunOnAsync = true,
            mIsDownloading = false;

    private double
            mSpeed = 0,
            mProgress = -1;

    private int mBufferSize = DEFAULT_BUFFER_SIZE;

    //----------------------------------------------------------------------------------------------------------------//

    public Downloader(@NonNull File file, String url)
    {
        mFile = file;
        mURL = url;
    }

    public Downloader(@NonNull String path, String url)
    {
        this(new File(path), url);
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * Define if it should execute on asynchronous thread, keep in mind executing on main thread will throw an exception.
     *
     * @param bool If {@code true} the downloader will execute on async, if {@code false} it'll not.
     */
    public Downloader setAsync(boolean bool)
    {
        mRunOnAsync = bool;
        return this;
    }

    public Downloader setObserver(IDownloaderObserver observer)
    {
        mObserver = observer;
        return this;
    }

    public Downloader setBufferSize(int size)
    {
        mBufferSize = size;
        return this;
    }

    //----------------------------------------------------------------------------------------------------------------//

    /**
     * @return -1 if there's no download in progress.
     */
    public int getProgress()
    {
        return (int) mProgress;
    }

    /**
     * Returns the current download speed in the desired measure
     *
     * @param measure The measurement for the return value, setting this to null will use Byte/s.
     * @see SizeMeasure
     */
    public double getSpeed(SizeMeasure measure)
    {
        if (measure != null)
        {
            switch (measure)
            {
                case KBPS:
                    return (mSpeed / 1024) * 1e9;

                case MBPS:
                    return (mSpeed / (1024 * 1024)) * 1e9;
            }
        }
        return mSpeed;
    }

    public Exception getException()
    {
        return mException;
    }

    public File getFile()
    {
        return mFile;
    }

    //----------------------------------------------------------------------------------------------------------------//

    public boolean isDownloading()
    {
        return mIsDownloading;
    }

    public boolean isCompleted()
    {
        return mProgress == 100;
    }

    //----------------------------------------------------------------------------------------------------------------//

    private void handleDownload()
    {
        try(var requester = new Requester(mURL))
        {
            onRequestSuccess(requester.executeAndGetBody());
        }
        catch (Exception e)
        {
            clear();
            mException = e;

            if (mObserver != null)
            {
                mObserver.onDownloadFail(this);
            }
        }
    }

    private void onRequestSuccess(ResponseBody body)
    {
        var length = body.contentLength() * 1d;
        var stream = body.byteStream();

        try (var in = new BufferedInputStream(stream); var out = new FileOutputStream(mFile))
        {
            mIsDownloading = true;

            if (mObserver != null)
            {
                mObserver.onDownloadStart(this);
            }

            var startTime = System.nanoTime();
            var buffer = new byte[mBufferSize];

            var total = 0L;
            var bytes = 0;

            while ((bytes = in.read(buffer)) != -1 && mIsDownloading)
            {
                out.write(buffer, 0, bytes);
                total += bytes;

                double elapsedTime = System.nanoTime() - startTime;

                mProgress = total / length * 100;
                mSpeed = total / elapsedTime;

                if (mObserver != null)
                {
                    mObserver.onDownloadUpdate(this);
                }
            }

            onLoopExit();
        }
        catch (IOException e)
        {
            // We don't call clear() here to leave the last progress and the last speed when the download failed.
            mIsDownloading = false;
            mException = e;

            if (mObserver != null)
            {
                mObserver.onDownloadFail(this);
            }
        }
    }

    private void onLoopExit()
    {
        if (mIsDownloading || isCompleted())
        {
            mIsDownloading = false;

            if (mObserver != null)
            {
                mObserver.onDownloadEnd(this);
            }
            return;
        }

        // This mean the file wasn't fully downloaded.
        if (mFile.exists())
        {
            //noinspection ResultOfMethodCallIgnored
            mFile.delete();
        }

        if (mObserver != null)
        {
            mObserver.onDownloadCancel(this);
        }
    }

    //----------------------------------------------------------------------------------------------------------------//

    public void download()
    {
        if (!mIsValid)
        {
            Logging.e(this, "This Downloader isn't more valid, it has already been used.");
            return;
        }
        mIsValid = false;

        if (mRunOnAsync)
        {
            Async.run(this::handleDownload);
        }
        else
        {
            handleDownload();
        }
    }

    /**
     * Keep in mind if the file completed download the onCancel callback will not run.
     */
    public void cancel()
    {
        mIsDownloading = false;
    }

    private void clear()
    {
        mIsDownloading = false;
        mProgress = -1;
        mSpeed = 0;
    }
}
