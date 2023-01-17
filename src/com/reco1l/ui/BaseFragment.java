package com.reco1l.ui;

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

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.tables.ResourceTable;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.TouchHandler;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26

public abstract class BaseFragment extends Fragment implements ResourceTable {

    final Screens[] parents;

    protected View rootView;

    // The background view of the layout, take care that if the root view doesn't have a child view
    // identified as "background" it'll be null.
    protected View rootBackground;

    private final Map<View, TouchHandler> mRegisteredViews;
    private final Runnable mCloseTask = this::close;

    private boolean
            mIsLoaded = false,
            mCloseOnBackgroundClick = false;

    //--------------------------------------------------------------------------------------------//

    // Use this constructor if the fragment has no parents screens
    public BaseFragment() {
        this((Screens[]) null);
    }

    // This constructor defines the parent screens where the fragment should be added automatically
    public BaseFragment(Screens... pParents) {
        parents = pParents;
        mRegisteredViews = new HashMap<>();

        Game.platform.onFragmentCreated(this);
    }

    //--------------------------------------------------------------------------------------------//

    // Override this to set the layout resource to be inflated and set it as root view
    protected @LayoutRes int getLayout() {
        return 0;
    }

    // Set the prefix which the resources identifiers start of, this is helpful to a proper usage
    // of find() method
    protected String getPrefix() {
        return null;
    }

    // Override this to set the root view directly instead of inflate a layout resource
    protected View getRootView() {
        return null;
    }

    protected abstract void onLoad();

    //--------------------------------------------------------------------------------------------//

    // If the fragment is an overlay it'll be added to the overlay container
    protected boolean isOverlay() {
        return false;
    }

    // If the fragment is an extra means that is not attached to a screen and can be closed by other
    // fragments or by user pressing back button.
    protected boolean isExtra() {
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    protected final void closeOnBackgroundClick(boolean bool) {
        mCloseOnBackgroundClick = bool;
    }

    //--------------------------------------------------------------------------------------------//

    // Override this to define the time to automatically close this fragment.
    protected long getCloseTime() {
        return 0;
    }

    protected boolean getConditionToShow() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onUpdate(float pSecElapsed) {
    }

    protected void onShowAttempt() {
    }

    protected void onCloseAttempt() {
    }

    protected void onScreenChange(Screens pLastScreen, Screens pNewScreen) {
    }

    //--------------------------------------------------------------------------------------------//

    // Return if onLoad() was previously called
    public final boolean isLoaded() {
        return mIsLoaded;
    }

    public int getWidth() {
        return rootView.getWidth();
    }

    public int getHeight() {
        return rootView.getHeight();
    }

    final String generateTag() {
        return getClass().getSimpleName() + "@" + this.hashCode();
    }

    //--------------------------------------------------------------------------------------------//

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater pInflater, ViewGroup pContainer, Bundle pBundle) {
        rootView = getRootView();

        if (rootView != null && getLayout() != 0) {
            throw new RuntimeException("You can't override getRootView() and getLayout() at the same time!");
        } else if (rootView == null && getLayout() == 0) {
            throw new RuntimeException("You've to override getRootView() or getLayout() to create the fragment view!");
        }

        if (getLayout() != 0) {
            rootView = pInflater.inflate(getLayout(), pContainer, false);
        }
        rootBackground = rootView.findViewById(R.id.background);

        onLoad();
        mIsLoaded = true;

        if (rootView != null) {
            if (rootBackground != null) {
                handleBackground();
            }

            if (getCloseTime() > 0) {
                rootView.postDelayed(mCloseTask, getCloseTime());
            }
        }

        return rootView;
    }

    private void handleBackground() {
        if (!mCloseOnBackgroundClick || rootBackground.hasOnClickListeners()) {
            return;
        }
        rootBackground.setClickable(true);

        bindTouch(rootBackground, new TouchListener() {

            public boolean useTouchEffect() {
                return false;
            }

            public void onPressUp() {
                close();
            }
        });
    }

    //---------------------------------------Management-------------------------------------------//

    void onTransaction() {
        final String tag = generateTag();

        if (isAdded()) {
            Log.i("FragmentPlatform", "Added fragment " + tag);
            return;
        }
        Log.i("FragmentPlatform", "Removed fragment " + tag);
        System.gc();
    }

    public void close() {
        if (!isAdded()) {
            onCloseAttempt();
            return;
        }
        mIsLoaded = false;
        rootView.removeCallbacks(mCloseTask);
        unbindTouchHandlers();
        mRegisteredViews.clear();
        Game.platform.removeFragment(this);
    }

    public boolean show() {
        if (isExtra()) {
            Game.platform.closeExtras();
        }

        if (isAdded() || !getConditionToShow()) {
            onShowAttempt();
            return false;
        }
        return Game.platform.addFragment(this);
    }

    // If added it calls show(), otherwise calls close()
    public final void altShow() {
        if (isAdded()) {
            close();
        } else {
            show();
        }
    }

    //--------------------------------------------------------------------------------------------//

    // Find a view by its string format identifier, if you previously defined the prefix you don't
    // have to write the fragment prefix here
    @SuppressWarnings("unchecked")
    protected <T extends View> T find(String pId) {
        if (rootView == null || pId == null) {
            return null;
        }

        int resId;
        if (getPrefix() == null || pId.startsWith(getPrefix() + "_")) {
            resId = id(pId, "id");
        } else {
            resId = id(getPrefix() + "_" + pId, "id");
        }

        Object view = rootView.findViewById(resId);

        if (view != null && !mRegisteredViews.containsKey((T) view)) {
            mRegisteredViews.put((T) view, null);
        }
        return (T) view;
    }

    //--------------------------------------------------------------------------------------------//

    public final void resetCloseTimer() {
        rootView.removeCallbacks(mCloseTask);
        rootView.postDelayed(mCloseTask, getCloseTime());
    }

    //--------------------------------------------------------------------------------------------//

    public void onTouchEventNotified(int pAction) {
        if (getCloseTime() > 0) {
            if (pAction == MotionEvent.ACTION_DOWN) {
                rootView.removeCallbacks(mCloseTask);
            }
            if (pAction == MotionEvent.ACTION_UP) {
                rootView.postDelayed(mCloseTask, getCloseTime());
            }
        }
    }

    // Bind a touch listener to the view and link it to the fragment
    public final void bindTouch(View pView, TouchListener pListener) {
        TouchHandler handler = mRegisteredViews.get(pView);
        if (handler == null) {
            handler = new TouchHandler(pListener);
            mRegisteredViews.put(pView, handler);
        } else {
            handler.listener = pListener;
        }
        handler.linkToFragment(this);
        handler.apply(pView);
    }

    public final void bindTouch(View pView, Runnable pOnActionUp) {
        bindTouch(pView, new TouchListener() {
            public void onPressUp() {
                if (pOnActionUp != null) {
                    pOnActionUp.run();
                }
            }
        });
    }

    protected final void unbindTouch(View pView) {
        mRegisteredViews.remove(pView);
    }

    protected final void unbindTouchHandlers() {
        for (View view : mRegisteredViews.keySet()) {
            view.setOnTouchListener(null);
        }
    }

    protected final void rebindTouchHandlers() {
        for (View view : mRegisteredViews.keySet()) {
            TouchHandler handler = mRegisteredViews.get(view);
            if (handler != null) {
                handler.apply(view);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void init() {
    }
}
