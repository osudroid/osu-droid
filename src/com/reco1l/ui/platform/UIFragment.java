package com.reco1l.ui.platform;

import android.content.res.Resources;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;
import com.reco1l.utils.interfaces.UI;
import com.reco1l.utils.interfaces.IMainClasses;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26
// Based on the EdrowsLuo BaseFragment class :)

public abstract class UIFragment extends Fragment implements IMainClasses, UI {

    protected View rootView, rootBackground;
    protected boolean
            isDismissOnBackgroundPress = false,
            isDismissOnBackPress = true;

    public boolean isShowing = false;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    //--------------------------------------------------------------------------------------------//
    /**
     * Runs once the layout XML is inflated.
     */
    protected abstract void onLoad();

    /**
     * Simplifies the way views are got with the method {@link #find(String)}, every layout XML file have an
     * undefined prefix (you have to define it on every view ID declaration).
     */
    protected abstract String getPrefix();

    protected abstract @LayoutRes int getLayout();
    //--------------------------------------------------------------------------------------------//

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        rootView = inflater.inflate(getLayout(), container, false);

        // Don't forget to create a View matching root bounds and set its ID to "background" for this feature.
        // You can also set the root view ID as "background".
        rootBackground = find(R.id.background);
        onLoad();
        if (isDismissOnBackgroundPress && rootBackground != null) {
            rootBackground.setClickable(true);

            if (!rootBackground.hasOnClickListeners())
                new ClickListener(rootBackground).onlyOnce(true).touchEffect(false).simple(this::close);
        }
        return rootView;
    }

    //---------------------------------------Management-------------------------------------------//

    /**
     * Dismiss the layout.
     * <p>
     * If you override this method always compare if {@linkplain #isShowing} is <code>true</code> at
     * the start of the method, otherwise any action with a View that is not showing will throw a
     * {@link NullPointerException}.
     * <p>
     * Also don't forget to call <code>super.close()</code> otherwise the layout will not dismiss, if you add
     * animations call it at the end of the animation, otherwise the animation will broke up.
     */
    public void close() {
        if (!isShowing)
            return;
        FragmentPlatform.getInstance().removeFragment(this);
        isShowing = false;
        System.gc();
    }

    public void show() {
        if (isShowing)
            return;
        String tag = this.getClass().getName() + "@" + this.hashCode();
        FragmentPlatform.getInstance().addFragment(this, tag);
        isShowing = true;
        System.gc();
    }

    /**
     * If the layout is showing then dismiss it, otherwise shows it.
     */
    public void altShow() {
        if (isShowing) close();
        else show();
    }

    /**
     * @param onBackgroundPress allows the user dismiss the fragment when the background is pressed.
     *                          <p> default value is: <code>false</code>.
     * <p>
     * @param onBackPress allows the user dismiss the fragment when the back button is pressed.
     *                    <p> default value is: <code>true</code>.
     */
    protected void setDismissMode(boolean onBackgroundPress, boolean onBackPress) {
        isDismissOnBackgroundPress = onBackgroundPress;
        isDismissOnBackPress = onBackPress;
    }

    //-----------------------------------------Tools----------------------------------------------//
    /**
     * Finds a child View of the parent layout from its resource ID.
     * @return the view itself if it exists as child in the layout, otherwise null.
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T find(@IdRes int id) {
        if (rootView == null || id == 0)
            return null;
        Object object = rootView.findViewById(id);

        return object != null ? (T) object : null;
    }

    /**
     * Finds a child View of the parent layout from its ID name in String format.
     * <p>
     *     Note: if you previously defined the layout prefix with the method {@link #getPrefix()}
     *     you don't need to add the prefix to the ID name.
     * @return the view itself if it exists as child in the layout, otherwise null.
     */
    @SuppressWarnings("unchecked")
    protected <T extends View> T find(String id) {
        if (rootView == null || id == null)
            return null;

        int Id;

        if (getPrefix() == null || id.startsWith(getPrefix())) {
            Id = res().getIdentifier(id, "id", mActivity.getPackageName());
        } else {
            Id = res().getIdentifier(getPrefix() + "_" + id, "id", mActivity.getPackageName());
        }

        Object view = rootView.findViewById(Id);
        return (T) view;
    }

    /**
     * Fast tool to switch the visibility of multiple views to <code>VISIBLE</code>.
     * <p>
     *     To switch visibility of them to <code>GONE</code> use {@link #setVisible(boolean, View...)}
     *     with <code>false</code> as first parameter.
     */
    protected void setVisible(View... views) {
        setVisible(true, views);
    }

    /**
     * Fast tool to switch the visibility of multiple views between <code>GONE</code> and <code>VISIBLE</code>.
     * @param bool true to show the views, false to hide them.
     */
    protected void setVisible(boolean bool, View... views) {
        for (View view: views) {
            if (view == null)
                continue;
            if (bool) view.setVisibility(View.VISIBLE);
            else view.setVisibility(View.GONE);
        }
    }

    protected void setMargin(View view, int left, int top, int right, int bottom) {
        if (view == null)
            return;
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.setMargins(left, top, right, bottom);
        view.requestLayout();
    }

    protected void setVerticalMargin(View view, int top, int bottom) {
        if (view == null)
            return;
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.setMargins(params.leftMargin, top, params.rightMargin, bottom);
        view.requestLayout();
    }

    protected void setHorizontalMargin(View view, int left, int right) {
        if (view == null)
            return;
        MarginLayoutParams params = (MarginLayoutParams) view.getLayoutParams();
        params.setMargins(left, params.topMargin, right, params.bottomMargin);
        view.requestLayout();
    }

    /**
     * Simple method to check nullability of multiples views at once.
     */
    protected boolean isNull(View... views) {
        for (View view: views) {
            if (view == null)
                return true;
        }
        return false;
    }

    protected DisplayMetrics displayMetrics() {
        DisplayMetrics metrics = new DisplayMetrics();
        mActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        return metrics;
    }

    protected Resources res(){
        return mActivity.getResources();
    }
}
