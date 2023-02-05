package com.reco1l.ui.fragments;

import android.graphics.Bitmap;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.reco1l.ui.BaseFragment;

import ru.nsu.ccfit.zuev.osuplus.R;

public class WebViewPanel extends BaseFragment {

    private WebView webview;
    private String url;

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

    @Override
    protected void onLoad() {
        webview = find("web");

        WebSettings webSettings = webview.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUserAgentString("rimu");

        webview.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                // load
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                // load off
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view,  WebResourceRequest request) {
                return false;
            }
        });
        webview.loadUrl(url);
    }


    public boolean show(String url) {
        this.url = url;
        return super.show();
    }

    @Override
    public boolean show() {
        throw new RuntimeException("Call show(url) instead of this, URL cannot be null");
    }
}
