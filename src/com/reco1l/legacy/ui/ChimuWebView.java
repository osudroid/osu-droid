package com.reco1l.legacy.ui;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.View;
import com.edlplan.ui.fragment.LoadingFragment;
import com.edlplan.ui.fragment.WebViewFragment;
import com.reco1l.framework.net.Downloader;
import com.reco1l.framework.net.IDownloaderObserver;
import com.reco1l.framework.net.SizeMeasure;
import com.reco1l.framework.util.FileUtil;
import im.delight.android.webview.AdvancedWebView;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FilenameUtils;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

import java.io.File;
import java.io.IOException;

import static android.content.Intent.ACTION_VIEW;
import static android.os.Environment.DIRECTORY_DOWNLOADS;

@SuppressWarnings("DataFlowIssue")
public class ChimuWebView extends WebViewFragment implements IDownloaderObserver
{

    public static final Uri MIRROR = Uri.parse("https://chimu.moe/en/beatmaps?mode=0");

    public static final String FILE_EXTENSION = ".osz";

    //----------------------------------------------------------------------------------------------------------------//

    private DownloadingFragment mFragment;

    private AdvancedWebView mWebView;
    private String mCurrentFilename;

    //----------------------------------------------------------------------------------------------------------------//

    public ChimuWebView()
    {
        super.setURL(MIRROR.toString());
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutID()
    {
        return R.layout.fragment_chimu;
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    protected void onLoadView()
    {
        super.onLoadView();

        mWebView = findViewById(R.id.web);
        assert mWebView != null;

        mWebView.addPermittedHostname(MIRROR.getHost());
        mWebView.setListener(getActivity(), new AdvancedWebView.Listener()
        {
            final LoadingFragment fragment = new LoadingFragment();

            @Override
            public void onPageStarted(String url, Bitmap favicon)
            {
                fragment.show();
            }

            @Override
            public void onPageFinished(String url)
            {
                fragment.dismiss();
            }

            @Override
            public void onPageError(int errorCode, String description, String failingUrl)
            {

            }

            @Override
            public void onDownloadRequested(String url, String filename, String mimeType, long contentLength, String contentDisposition, String userAgent)
            {
                String name = FileUtil.decodeUTF8(filename);
                filename = FileUtil.validateChars(name);

                if (!filename.endsWith(FILE_EXTENSION))
                {
                    ToastLogger.showText("Failed to start download, invalid file extension.", true);
                    return;
                }

                mCurrentFilename = FilenameUtils.removeExtension(name);

                File directory = getActivity().getExternalFilesDir(DIRECTORY_DOWNLOADS);
                File file = new File(directory, filename + FILE_EXTENSION);

                Downloader downloader = new Downloader(file, url);

                mFragment = new DownloadingFragment(downloader, () -> {

                    mFragment.getText().setVisibility(View.VISIBLE);
                    mFragment.getText().setText(R.string.chimu_connecting);
                    mFragment.getButton().setVisibility(View.VISIBLE);
                    mFragment.getButton().setText(R.string.chimu_cancel);

                    downloader.setObserver(ChimuWebView.this);
                    downloader.download();

                    mFragment.getButton().setOnClickListener(v -> downloader.cancel());
                });

                mFragment.show();
            }

            @Override
            public void onExternalPageRequest(String url)
            {
                Intent intent = new Intent(ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    public void dismiss()
    {
        mWebView.destroy();
        super.dismiss();
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    public void onDownloadStart(Downloader downloader)
    {
        getActivity().runOnUiThread(() -> {
            mFragment.getText().setText(StringTable.format(R.string.chimu_downloading, mCurrentFilename));
        });
    }

    @Override
    public void onDownloadEnd(Downloader downloader)
    {
        getActivity().runOnUiThread(() -> {
            mFragment.getProgressBar().setVisibility(View.GONE);
            mFragment.getProgressBar().setIndeterminate(true);
            mFragment.getProgressBar().setVisibility(View.VISIBLE);

            mFragment.getText().setText(StringTable.format(R.string.chimu_importing, mCurrentFilename));
            mFragment.getButton().setVisibility(View.GONE);
        });

        File file = downloader.getFile();

        try (ZipFile zip = new ZipFile(file))
        {
            if (!zip.isValidZipFile())
            {
                getActivity().runOnUiThread(mFragment::dismiss);
                ToastLogger.showText("Import failed, invalid ZIP file.", true);
                return;
            }

            if (!FileUtils.extractZip(file.getPath(), Config.getBeatmapPath()))
            {
                getActivity().runOnUiThread(mFragment::dismiss);
                ToastLogger.showText("Import failed, unable to extract ZIP file.", true);
                return;
            }

            LibraryManager.getInstance().updateLibrary(true);
        }
        catch (IOException e)
        {
            ToastLogger.showText("Import failed: " + e.getMessage(), true);
        }
        getActivity().runOnUiThread(mFragment::dismiss);
    }

    @Override
    public void onDownloadCancel(Downloader downloader)
    {
        ToastLogger.showText("Download canceled.", true);
        getActivity().runOnUiThread(mFragment::dismiss);
    }

    @Override
    public void onDownloadUpdate(Downloader downloader)
    {
        String info = String.format("\n%.3f kb/s (%d%%)", downloader.getSpeed(SizeMeasure.MBPS), downloader.getProgress());

        getActivity().runOnUiThread(() -> {
            mFragment.getText().setText(StringTable.format(R.string.chimu_downloading, mCurrentFilename) + info);
            mFragment.getProgressBar().setIndeterminate(false);
            mFragment.getProgressBar().setProgress(downloader.getProgress());
        });
    }

    @Override
    public void onDownloadFail(Downloader downloader)
    {
        getActivity().runOnUiThread(mFragment::dismiss);

        if (downloader.getException() != null)
        {
            ToastLogger.showText("Download failed: " + downloader.getException().getMessage(), true);
            return;
        }
        ToastLogger.showText("Download failed.", true);
    }
}
