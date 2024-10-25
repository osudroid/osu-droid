package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import club.andnext.markdown.MarkdownWebView;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MarkdownFragment extends BaseFragment {

    private String markdown;

    @StringRes
    private int title;

    public MarkdownFragment() {
        setDismissOnBackgroundClick(false);
        setDismissOnBackPress(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.markdown_fragment;
    }

    @Override
    protected void onLoadView() {
        if (markdown != null) {
            ((MarkdownWebView) findViewById(R.id.markdown_view)).setText(markdown);
        }
        if (title != 0) {
            ((TextView) findViewById(R.id.title)).setText(title);
        }
        findViewById(R.id.frg_close).setOnClickListener(v -> this.dismiss());
        playOnLoadAnim();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(super::dismiss);
    }

    public MarkdownFragment setMarkdown(String markdown) {
        this.markdown = markdown;
        if (findViewById(R.id.markdown_view) != null) {
            ((MarkdownWebView) findViewById(R.id.markdown_view)).setText(markdown);
        }
        return this;
    }

    public MarkdownFragment setTitle(@StringRes int title) {
        this.title = title;
        if (findViewById(R.id.title) != null) {
            ((TextView) findViewById(R.id.title)).setText(title);
        }
        return this;
    }

    protected void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        body.setTranslationY(500);
        body.animate().cancel();
        body.animate()
                .translationY(0)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.OutQuad))
                .start();
        playBackgroundHideInAnim(200);
    }

    protected void playOnDismissAnim(Runnable runnable) {
        View body = findViewById(R.id.frg_body);
        body.animate().cancel();
        body.animate()
                .translationY(500)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InQuad))
                .setListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (runnable != null) {
                            runnable.run();
                        }
                    }
                })
                .start();
        playBackgroundHideOutAnim(200);
    }
}
