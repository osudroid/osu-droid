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

import com.reco1l.global.Game;
import com.reco1l.scenes.BaseScene;
import com.reco1l.tables.ResourceTable;
import com.reco1l.utils.Logging;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.TouchHandler;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26

public abstract class BaseFragment extends Fragment implements ResourceTable {

    final BaseScene[] parents;

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

    public BaseFragment() {
        this((BaseScene[]) null);
    }

    public BaseFragment(BaseScene... parents) {
        Logging.initOf(getClass());
        this.parents = parents;

        mRegisteredViews = new HashMap<>();
        Game.platform.onFragmentCreated(this);
    }

    //--------------------------------------------------------------------------------------------//

    // Override this to set the layout resource to be inflated and set it as root view.
    protected @LayoutRes int getLayout() {
        return 0;
    }

    // Override this to set the root view directly instead of inflate a layout resource, keep in mind
    // that if getLayout() was previously override Fragment will be inflated from layout resource ignoring this.
    protected View getRootView() {
        return null;
    }

    // Set the prefix which the resources identifiers start of, this is helpful to the usage of find() method,
    // this is only useful if you set a layout resource.
    protected String getPrefix() {
        return null;
    }

    protected abstract void onLoad();

    // This is called post draw and measure of root view besides onLoad() that's called before, mostly useful for animations.
    protected void onPost() {
    }

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

    protected void onEngineUpdate(float pSecElapsed) {
    }

    protected void onShowAttempt() {
    }

    protected void onCloseAttempt() {
    }

    protected void onSceneChange(BaseScene oldScene, BaseScene newScene) {
    }

    // Return true to consume the back press event, false to continue the propagation.
    public boolean onBackPress() {
        if (isExtra()) {
            close();
            return true;
        }
        return false;
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

        if (rootView == null && getLayout() != 0) {
            rootView = pInflater.inflate(getLayout(), pContainer, false);
        }

        if (rootView == null) {
            throw new RuntimeException("Root view cannot be null!");
        }

        rootBackground = rootView.findViewById(R.id.background);

        onLoad();
        mIsLoaded = true;

        if (rootBackground != null) {
            handleBackground();
        }
        if (getCloseTime() > 0) {
            rootView.postDelayed(mCloseTask, getCloseTime());
        }

        rootView.post(this::onPost);

        return rootView;
    }

    private void handleBackground() {
        if (!mCloseOnBackgroundClick || rootBackground.hasOnClickListeners()) {
            return;
        }
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
        if (isAdded() || !getConditionToShow()) {
            onShowAttempt();
            return false;
        }

        if (isExtra()) {
            Game.platform.closeExtras();
        }
        return Game.platform.addFragment(this);
    }

    // If added it calls show(), otherwise calls close()
    // Returns true if it'll added or false if it'll not.
    public final boolean alternate() {
        if (isAdded()) {
            close();
            return false;
        } else {
            return show();
        }
    }

    //--------------------------------------------------------------------------------------------//

    // Find a view by its string format identifier, if you previously defined the prefix you don't
    // need to write the fragment.
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

    public void notifyTouchEvent(int action) {
        if (getCloseTime() > 0) {
            if (action == MotionEvent.ACTION_DOWN) {
                rootView.removeCallbacks(mCloseTask);
            }
            if (action == MotionEvent.ACTION_UP) {
                rootView.postDelayed(mCloseTask, getCloseTime());
            }
        }
    }

    // Bind a touch listener to the view and link it to the fragment
    public final void bindTouch(View view, TouchListener listener) {
        TouchHandler handler = mRegisteredViews.get(view);
        if (handler == null) {
            handler = new TouchHandler(listener);
            mRegisteredViews.put(view, handler);
        } else {
            handler.mListener = listener;
        }
        handler.linkToFragment(this);
        handler.apply(view);
    }

    public final void bindTouch(View view, Runnable onUp) {
        bindTouch(view, new TouchListener() {
            public void onPressUp() {
                if (onUp != null) {
                    onUp.run();
                }
            }
        });
    }

    protected final void unbindTouch(View view) {
        view.setForeground(null);
        view.setOnTouchListener(null);
    }

    protected final void unbindTouchHandlers() {
        for (View view : mRegisteredViews.keySet()) {
            view.setForeground(null);
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
