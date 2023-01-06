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
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.TouchHandler;
import com.reco1l.tables.Res;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26

public abstract class BaseFragment extends Fragment {

    protected final Screens[] parents;

    protected View
            rootView,
            rootBackground;

    private final Map<View, TouchHandler> registeredViews;
    private final Runnable close = this::close;

    private boolean
            isLoaded = false,
            closeOnBackgroundClick = false;

    //--------------------------------------------------------------------------------------------//

    public BaseFragment() {
        this((Screens) null);
    }

    public BaseFragment(Screens... parents) {
        this.registeredViews = new HashMap<>();
        this.parents = parents;

        Game.platform.onFragmentCreated(this);
    }

    //--------------------------------------------------------------------------------------------//

    protected @LayoutRes int getLayout() {
        return 0;
    }

    protected String getPrefix() {
        return null;
    }

    protected View getRootView() {
        return null;
    }


    protected abstract void onLoad();

    //--------------------------------------------------------------------------------------------//

    protected boolean isOverlay() {
        return false;
    }

    protected boolean isExtra() {
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    protected final void closeOnBackgroundClick(boolean bool) {
        closeOnBackgroundClick = bool;
    }

    //--------------------------------------------------------------------------------------------//

    protected long getCloseTime() {
        return 0;
    }

    protected boolean getConditionToShow() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onUpdate(float sec) {
    }

    protected void onShowAttempt() {
    }

    protected void onCloseAttempt() {
    }

    protected void onScreenChange(Screens lastScreen, Screens newScreen) {
    }

    //--------------------------------------------------------------------------------------------//

    public final boolean isLoaded() {
        return isLoaded;
    }

    public final int getWidth() {
        return rootView.getWidth();
    }

    public final int getHeight() {
        return rootView.getHeight();
    }

    protected final String generateTag() {
        return getClass().getSimpleName() + "@" + this.hashCode();
    }

    //--------------------------------------------------------------------------------------------//

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        rootView = getRootView();

        if (rootView != null && getLayout() != 0) {
            throw new RuntimeException("You can't override getRootView() and getLayout() at the same time!");
        } else if (rootView == null && getLayout() == 0) {
            throw new RuntimeException("You've to override getRootView() or getLayout() to create the fragment view!");
        }

        if (getLayout() != 0) {
            rootView = inflater.inflate(getLayout(), container, false);
        }
        rootBackground = rootView.findViewById(R.id.background);

        onLoad();
        isLoaded = true;

        if (rootView != null) {
            if (rootBackground != null) {
                handleBackground();
            }

            if (getCloseTime() > 0) {
                rootView.postDelayed(close, getCloseTime());
            }
        }

        return rootView;
    }

    private void handleBackground() {
        if (!closeOnBackgroundClick || rootBackground.hasOnClickListeners()) {
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

    protected void onTransaction() {
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
        isLoaded = false;
        rootView.removeCallbacks(close);
        unbindTouchHandlers();
        registeredViews.clear();
        Game.platform.removeFragment(this);
    }

    public void show() {
        if (isExtra()) {
            Game.platform.closeExtras();
        }

        if (isAdded() || !getConditionToShow()) {
            onShowAttempt();
            return;
        }
        Game.platform.addFragment(this);
    }

    public final void altShow() {
        if (isAdded()) {
            close();
        } else {
            show();
        }
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressWarnings("unchecked")
    protected <T extends View> T find(String id) {
        if (rootView == null || id == null)
            return null;

        int identifier;
        if (getPrefix() == null || id.startsWith(getPrefix() + "_")) {
            identifier = Res.id(id, "id");
        } else {
            identifier = Res.id(getPrefix() + "_" + id, "id");
        }

        Object view = rootView.findViewById(identifier);

        if (view != null && !registeredViews.containsKey((T) view)) {
            registeredViews.put((T) view, null);
        }
        return (T) view;
    }

    //--------------------------------------------------------------------------------------------//

    public void resetDismissTimer() {
        rootView.removeCallbacks(close);
        rootView.postDelayed(close, getCloseTime());
    }

    //--------------------------------------------------------------------------------------------//

    public void onTouchEventNotified(int action) {
        if (getCloseTime() > 0) {
            if (action == MotionEvent.ACTION_DOWN) {
                rootView.removeCallbacks(close);
            }
            if (action == MotionEvent.ACTION_UP) {
                rootView.postDelayed(close, getCloseTime());
            }
        }
    }

    public final void bindTouch(View view, Runnable onSingleTapUp) {
        bindTouch(view, new TouchListener() {
            public void onPressUp() {
                if (onSingleTapUp != null) {
                    onSingleTapUp.run();
                }
            }
        });
    }

    public final void bindTouch(View view, TouchListener listener) {
        TouchHandler vth = registeredViews.get(view);
        if (vth == null) {
            vth = new TouchHandler(listener);
            registeredViews.put(view, vth);
        } else {
            vth.listener = listener;
        }
        vth.linkToFragment(this);
        vth.apply(view);
    }

    protected final void unbindTouch(View view) {
        registeredViews.remove(view);
    }

    protected final void unbindTouchHandlers() {
        for (View view : registeredViews.keySet()) {
            view.setOnTouchListener(null);
        }
    }

    protected final void rebindTouchHandlers() {
        for (View view : registeredViews.keySet()) {
            TouchHandler touchHandler = registeredViews.get(view);
            if (touchHandler != null) {
                touchHandler.apply(view);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void init() {
    }
}
