package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ConfirmDialogFragment extends BaseFragment {

    private OnResult onResult;

    @StringRes
    private int text;

    public ConfirmDialogFragment() {
        setDismissOnBackgroundClick(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.frgdialog_confirm;
    }

    @Override
    protected void onLoadView() {
        findViewById(R.id.okButton).setOnClickListener(v -> {
            if (onResult != null) {
                onResult.onAccept(true);
                dismiss();
            }
        });
        if (text != 0) {
            ((TextView) findViewById(R.id.confirm_message)).setText(text);
        }
        playOnLoadAnim();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(super::dismiss);
    }

    public ConfirmDialogFragment setMessage(@StringRes int text) {
        this.text = text;
        if (findViewById(R.id.confirm_message) != null) {
            ((TextView) findViewById(R.id.confirm_message)).setText(text);
        }
        return this;
    }

    public void showForResult(OnResult result) {
        this.onResult = result;
        show();
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


    public interface OnResult {
        void onAccept(boolean isAccepted);
    }

}
