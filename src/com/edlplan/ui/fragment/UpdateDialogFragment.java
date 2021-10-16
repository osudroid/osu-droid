package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osuplus.R;

public class UpdateDialogFragment extends BaseFragment {

    private String changelogMsg, downloadUrl;

    public UpdateDialogFragment() {
        setDismissOnBackgroundClick(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_update_dialog;
    }

    @Override
    protected void onLoadView() {
        findViewById(R.id.updateButton).setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl));
            getActivity().startActivity(browserIntent);
            dismiss();
        });

        findViewById(R.id.changelogButton).setOnClickListener(v -> {
            new MarkdownFragment()
                .setTitle(R.string.changelog_title)
                .setMarkdown(changelogMsg)
                .show();
        });

        findViewById(R.id.laterButton).setOnClickListener(v -> dismiss());
        playOnLoadAnim();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(super::dismiss);
    }

    public UpdateDialogFragment setChangelogMessage(String changelogMsg) {
        this.changelogMsg = changelogMsg;
        return this;
    }

    public UpdateDialogFragment setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
        return this;
    }

    protected void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        body.setTranslationY(100);
        body.animate().cancel();
        body.animate()
                .translationY(0)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .start();
        playBackgroundHideInAnim(200);
    }

    protected void playOnDismissAnim(Runnable runnable) {
        View body = findViewById(R.id.frg_body);
        body.animate().cancel();
        body.animate()
                .translationY(100)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
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
