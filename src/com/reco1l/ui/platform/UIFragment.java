package com.reco1l.ui.platform;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.reco1l.enums.Screens;
import com.reco1l.utils.listeners.TouchListener;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.utils.Resources;
import com.reco1l.interfaces.IReferences;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26

public abstract class UIFragment extends Fragment implements IReferences {

    public boolean isShowing = false;

    protected View rootView, rootBackground;

    protected boolean
            isDismissOnBackgroundPress = false,
            isDismissOnBackPress = true,
            isLoaded = false;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    protected final Map<View, ViewTouchHandler> registeredViews;

    private final String tag;
    private final Runnable close = this::close;

    //--------------------------------------------------------------------------------------------//

    public UIFragment() {
        tag = this.getClass().getSimpleName() + "@" + this.hashCode();
        registeredViews = new HashMap<>();
        if (getParent() != null) {
            platform.assignToScene(getParent(), this);
        }
        if (getParents() != null) {
            for (Screens scene : getParents()) {
                platform.assignToScene(scene, this);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//
    // Please use this instead of directly referring to the variable.
    public final boolean isShowing() {
        return isShowing;
    }

    // To override
    //--------------------------------------------------------------------------------------------//
    /**
     * Runs once the layout XML is inflated.
     */
    protected abstract void onLoad();

    protected void onScreenChange(Screens lastScreen, Screens newScreen) { }

    /**
     * Simplifies the way views are got with the method {@link #find(String)}, every layout XML file have an
     * undefined prefix (you have to define it on every view ID declaration).
     */
    protected abstract String getPrefix();
    protected abstract @LayoutRes int getLayout();

    /**
     * Defines which scene the fragment belongs to.
     */
    protected Screens getParent() { return null; }

    /**
     * Does the same that {@link #getParent()} but in this case this is intended to those fragments
     * that belongs from multiple scenes.
     * <p>Note: in case the fragment is intended to show on every scene (like overlays) you can
     * return <code>Scenes.values()</code></p>
     */
    protected Screens[] getParents() { return null; }


    /**
     * Override and set this to true if the fragment is a overlay (they are attached to a different layer.
     */
    protected boolean isOverlay() { return false; }

    /**
     * Sets the time of inactivity that need to be reached to close the fragment.
     * <p>Note: Use this only on extras dialogs.</p>
     */
    protected long getDismissTime() { return 0; }

    protected void onUpdate(float secondsElapsed) {}
    //--------------------------------------------------------------------------------------------//

    @Nullable @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        rootView = inflater.inflate(getLayout(), container, false);

        // Don't forget to create a View matching root bounds and set its ID to "background" for this feature.
        // You can also set the root view ID as "background".
        rootBackground = rootView.findViewById(R.id.background);
        onLoad();
        isLoaded = true;
        if (isDismissOnBackgroundPress && rootBackground != null) {
            rootBackground.setClickable(true);

            if (!rootBackground.hasOnClickListeners()) {
                bindTouchListener(rootBackground, new TouchListener() {
                    public boolean hasTouchEffect() { return false; }
                    public boolean isOnlyOnce() { return true; }

                    public void onPressUp() {
                        unbindTouchListeners();
                        close();
                    }
                });
            }
        }
        if (getDismissTime() > 0) {
            rootView.postDelayed(close, getDismissTime());
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
        if (!isShowing) {
            Log.i("FragmentPlatform", "Fragment " + tag + " isn't showing.");
            return;
        }
        rootView.removeCallbacks(close);
        if (platform.removeFragment(this)) {
            Log.i("FragmentPlatform", "Removed fragment " + tag);
        } else {
            Log.i("FragmentPlatform", "Unable to remove fragment " + tag);
        }
        isShowing = false;
        isLoaded = false;
        unbindTouchListeners();
        registeredViews.clear();
        System.gc();
    }

    public void show() {
        if (isShowing) {
            Log.i("FragmentPlatform", "Fragment " + tag + " is already showing.");
            return;
        }
        isLoaded = false;
        if (platform.addFragment(this, tag, isOverlay())) {
            Log.i("FragmentPlatform", "Added fragment " + tag);
        } else {
            Log.i("FragmentPlatform", "Unable to add fragment " + tag);
        }
        isShowing = true;
        System.gc();
    }

    public final void altShow() {
        if (isShowing) {
            close();
        } else {
            show();
        }
    }

    /**
     * @param onBackgroundPress allows the user dismiss the fragment when the background is pressed.
     *                          <p> default value is: <code>false</code>.
     * <p>
     * @param onBackPress allows the user dismiss the fragment when the back button is pressed.
     *                    <p> default value is: <code>true</code>.
     */
    protected final void setDismissMode(boolean onBackgroundPress, boolean onBackPress) {
        isDismissOnBackgroundPress = onBackgroundPress;
        isDismissOnBackPress = onBackPress;
    }

    //--------------------------------------------------------------------------------------------//

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

        int identifier;
        if (getPrefix() == null || id.startsWith(getPrefix() + "_")) {
            identifier = Resources.id(id, "id");
        } else {
            identifier = Resources.id(getPrefix() + "_" + id, "id");
        }

        Object view = rootView.findViewById(identifier);

        if (view != null && !registeredViews.containsKey((T) view)) {
            registeredViews.put((T) view, null);
            // Log.i("UIFragment", "Found view: " + getPrefix() + "_" + id + " (registered views: " + registeredViews.size() + ")");
        }
        return (T) view;
    }

    /**
     * Simple method to check nullability of multiples views at once.
     */
    protected final boolean isNull(View... views) {
        for (View view: views) {
            if (view == null)
                return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public void onTouchEventNotified(int action) {
        if (getDismissTime() > 0) {
            if (action == MotionEvent.ACTION_DOWN) {
                rootView.removeCallbacks(close);
            }
            if (action == MotionEvent.ACTION_UP) {
                rootView.postDelayed(close, getDismissTime());
            }
        }
    }

    public final void bindTouchListener(View view, Runnable onSingleTapUp) {
        bindTouchListener(view, new TouchListener() {
            public void onPressUp() {
                if (onSingleTapUp != null) {
                    onSingleTapUp.run();
                }
            }
        });
    }

    public final void bindTouchListener(View view, TouchListener listener) {
        ViewTouchHandler vth = registeredViews.get(view);
        if (vth == null) {
            vth = new ViewTouchHandler(listener);
            registeredViews.put(view, vth);
        } else {
            vth.listener = listener;
        }
        vth.linkToFragment(this);
        vth.apply(view);
    }

    protected final void unbindTouchListener(View view) {
        registeredViews.remove(view);
    }

    protected final void unbindTouchListeners() {
        for (View view : registeredViews.keySet()) {
            view.setOnTouchListener(null);
        }
    }

    protected final void rebindTouchListeners() {
        for (View view : registeredViews.keySet()) {
            ViewTouchHandler touchHandler = registeredViews.get(view);
            if (touchHandler != null) {
                touchHandler.apply(view);
            }
        }
    }
}
