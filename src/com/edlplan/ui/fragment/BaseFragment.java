package com.edlplan.ui.fragment;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.ActivityOverlay;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osuplus.R;

public abstract class BaseFragment extends Fragment {

    private View root;

    private View background;

    private boolean dismissOnBackgroundClick = false;

    private boolean created = false;

    public boolean isDismissOnBackgroundClick() {
        return dismissOnBackgroundClick;
    }

    public void setDismissOnBackgroundClick(boolean dissmissOnBackgroundClick) {
        this.dismissOnBackgroundClick = dissmissOnBackgroundClick;
    }

    public boolean isCreated() {
        return created;
    }

    public View getRoot() {
        return root;
    }

    public @IdRes
    int getBackgroundId() {
        return R.id.frg_background;
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T findViewById(@IdRes int id) {
        Object o = getRoot() != null ? getRoot().findViewById(id) : null;
        if (o == null) {
            return null;
        } else {
            return (T) o;
        }
    }

    protected abstract @LayoutRes
    int getLayoutID();

    protected abstract void onLoadView();

    protected void playBackgroundHideInAnim(int duration) {
        View background = findViewById(R.id.frg_background);
        if (background != null) {
            background.setAlpha(0);
            background.animate().cancel();
            background.animate()
                    .alpha(1)
                    .setDuration(duration)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .start();
        }
    }

    protected void playBackgroundHideOutAnim(int duration) {
        View background = findViewById(R.id.frg_background);
        if (background != null) {
            background.animate().cancel();
            background.animate()
                    .alpha(0)
                    .setDuration(duration)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .start();
        }
    }


    public void show() {
        ActivityOverlay.addOverlay(this, this.getClass().getName() + "@" + this.hashCode());
    }

    public void dismiss() {
        ActivityOverlay.dismissOverlay(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        created = true;
        root = inflater.inflate(getLayoutID(), container, false);
        if (findViewById(getBackgroundId()) != null) {
            background = findViewById(getBackgroundId());
            background.setOnClickListener(v -> {
                if (isDismissOnBackgroundClick()) {
                    dismiss();
                }
            });
        }
        onLoadView();
        return root;
    }
}
