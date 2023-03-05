package com.reco1l.ui.fragments;

import android.graphics.Bitmap;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.base.BaseFragment;

import im.delight.android.webview.AdvancedWebView;
import ru.nsu.ccfit.zuev.osuplus.R;

public class WebViewFragment extends BaseFragment implements AdvancedWebView.Listener {

    protected AdvancedWebView mWebView;
    protected String mUrl;

    private boolean mLockHostname = false;

    //--------------------------------------------------------------------------------------------//

    public WebViewFragment(String url) {
        mUrl = url;
    }

    public WebViewFragment(String url, BaseScene... scenes) {
        super(scenes);
        mUrl = url;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.extra_webview;
    }

    @Override
    protected String getPrefix() {
        return "wv";
    }

    @Override
    protected boolean isExtra() {
        return true;
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mWebView = find("web");
        mWebView.setMixedContentAllowed(false);

        mWebView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                if (mLockHostname) {
                    String url = request.getUrl().toString();
                    return !mUrl.contains(url);
                }
                return false;
            }
        });

        mWebView.setWebChromeClient(new WebChromeClient() {

        });

        mWebView.setListener(getActivity(), this);
        mWebView.loadUrl(mUrl);
    }

    @Override
    public boolean show() {
        if (mUrl == null) {
            throw new NullPointerException("URL cannot be null!");
        }
        return super.show();
    }

    //--------------------------------------------------------------------------------------------//

    public void setLockToHostname(boolean bool) {
        mLockHostname = bool;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onPageStarted(String url, Bitmap favicon) {

    }

    @Override
    public void onPageFinished(String url) {

    }

    @Override
    public void onPageError(int errorCode, String description, String failingUrl) {

    }

    @Override
    public void onDownloadRequested(String url, String suggestedFilename, String mimeType, long contentLength, String contentDisposition, String userAgent) {

    }

    @Override
    public void onExternalPageRequest(String url) {

    }
}
