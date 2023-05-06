package com.reco1l.legacy.ui;

import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.edlplan.ui.fragment.LoadingFragment;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.framework.net.Downloader;
import ru.nsu.ccfit.zuev.osuplus.R;

public class DownloadingFragment extends LoadingFragment
{

    private final Downloader mDownloader;
    private final Runnable mAwaitCall;

    private Button mButton;
    private TextView mText;
    private CircularProgressIndicator mProgressBar;

    //----------------------------------------------------------------------------------------------------------------//

    public DownloadingFragment(Downloader downloader, Runnable await)
    {
        mAwaitCall = await;
        mDownloader = downloader;
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutID()
    {
        return R.layout.fragment_downloading;
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    protected void onLoadView()
    {
        super.onLoadView();

        mText = findViewById(R.id.text);
        mButton = findViewById(R.id.button);
        mProgressBar = findViewById(R.id.progress);

        mAwaitCall.run();
    }

    //----------------------------------------------------------------------------------------------------------------//

    @Override
    public void callDismissOnBackPress()
    {
        if (mDownloader.isDownloading())
        {
            mDownloader.cancel();
            return;
        }
        super.callDismissOnBackPress();
    }

    //----------------------------------------------------------------------------------------------------------------//

    public ProgressBar getProgressBar()
    {
        return mProgressBar;
    }

    public Button getButton()
    {
        return mButton;
    }

    public TextView getText()
    {
        return mText;
    }

    public void setText(String text)
    {
        mText.setVisibility(TextUtils.isEmpty(text) ? View.GONE : View.VISIBLE);
        mText.setText(text);
    }
}