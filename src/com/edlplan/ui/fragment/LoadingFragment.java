package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.view.View;

import androidx.annotation.StringRes;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osuplus.R;

public class LoadingFragment extends BaseFragment {

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_loading;
    }

    @Override
    protected void onLoadView() {
        playOnLoadAnim();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(super::dismiss);
    }

    protected void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        if(body == null) return;
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
        if(body == null) return;
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