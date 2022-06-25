package com.reco1l.ui.platform;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;

// Created by Reco1l on 22/6/22 02:26

public abstract class BaseLayout extends Fragment {

    protected final MainActivity mActivity = GlobalManager.getInstance().getMainActivity();

    protected View rootView;
    protected boolean
            isDismissOnBackgroundPress = false,
            isDismissOnBackPress = true;

    public boolean isShowing = false;

    //--------------------------------------------------------------------------------------------//
    protected abstract void onLoad();

    // This simplified the way views are called with the method find(), every layout XML file have an
    // undefined prefix (you have to define it on every view ID declaration) and this method gets that prefix.
    protected abstract String getPrefix();

    protected abstract @LayoutRes int getLayout();
    //--------------------------------------------------------------------------------------------//

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle bundle) {

        rootView = inflater.inflate(getLayout(), container, false);

        // Is important to set "background" as ID for the back view on your layout if you
        // want to enable dismiss on background press.
        View background = find("background");
        if (isDismissOnBackgroundPress && background != null)
            background.setOnTouchListener((view, event) -> {
                if (event.getAction() == MotionEvent.ACTION_UP)
                    close();
                return true;
            });

        onLoad();
        return rootView;
    }

    //---------------------------------------Management-------------------------------------------//

    public void close() {
        // If you override this method always compare if 'isShowing' is true, otherwise any action
        // with a View that is not showing can generate a NullPointerException.
        if (!isShowing)
            return;
        FragmentPlatform.getInstance().removeFragment(BaseLayout.this);
        isShowing = false;
        // Also don't forget to call 'super.close()' otherwise the layout will not dismiss.
    }

    public void show() {
        if (isShowing)
            return;
        String tag = this.getClass().getName() + "@" + this.hashCode();
        FragmentPlatform.getInstance().addFragment(this, tag);
        isShowing = true;
    }

    //Alternates between show and close the fragment according if it's showing or not.
    public void altShow() {
        if (isShowing) close();
        else show();
    }

    protected void setDismissMode(boolean onBackgroundPress, boolean onBackPress) {
        isDismissOnBackgroundPress = onBackgroundPress;
        isDismissOnBackPress = onBackPress;
    }

    //-----------------------------------------Tools----------------------------------------------//
    @SuppressWarnings("unchecked")
    public <T extends View> T find(@IdRes int id) {
        if (rootView == null || id == 0)
            return null;
        Object object = rootView.findViewById(id);

        return object != null ? (T) object : null;
    }

    public <T extends View> T find(String view) {
        return find(getPrefix(), view);
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T find(String prefix, String view) {
        if (rootView == null || view == null || prefix == null)
            return null;

        @IdRes int id = mActivity.getResources()
                .getIdentifier(prefix + "_" + view, "id", mActivity.getPackageName());

        Object object = rootView.findViewById(id);

        return object != null ? (T) object : null;
    }

    public void setVisible(View... views) {
        setVisible(true, views);
    }

    public void setVisible(boolean bool, View... views) {
        for (View view: views) {
            if (view == null)
                return;
            if (bool) view.setVisibility(View.VISIBLE);
            else view.setVisibility(View.GONE);
        }
    }

    // Simple method to check in multiple views if one of them is null, in fact that should never
    // happen making this method unnecessary but it's a workaround to avoid stupid warnings.
    public boolean isNull(View... views) {
        boolean returnValue = false;
        for (View view: views) {
            returnValue = view == null;
        }
        return returnValue;
    }

    public DisplayMetrics displayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    public Resources res(){
        return mActivity.getResources();
    }
}
