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

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.utils.listeners.TouchListener;
import com.reco1l.utils.ViewTouchHandler;
import com.reco1l.utils.Res;

import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:26

public abstract class BaseFragment extends Fragment {

    protected View rootView, rootBackground;

    protected boolean
            isDismissOnBackgroundPress = false,
            isDismissOnBackPress = true;

    protected int screenWidth = Config.getRES_WIDTH();
    protected int screenHeight = Config.getRES_HEIGHT();

    private final Map<View, ViewTouchHandler> registeredViews;
    private final Runnable close = this::close;

    private boolean isLoaded = false;

    //--------------------------------------------------------------------------------------------//

    public BaseFragment() {
        registeredViews = new HashMap<>();

        if (getParent() != null) {
            Game.platform.assignToScene(getParent(), this);
        }
        if (getParents() != null) {
            for (Screens scene : getParents()) {
                Game.platform.assignToScene(scene, this);
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    public final boolean isLoaded() {
        return isLoaded;
    }

    //--------------------------------------------------------------------------------------------//

    protected abstract @LayoutRes int getLayout();

    protected abstract String getPrefix();

    protected abstract void onLoad();

    //--------------------------------------------------------------------------------------------//

    protected Screens getParent() {
        return null;
    }

    protected Screens[] getParents() {
        return null;
    }

    protected boolean isOverlay() {
        return false;
    }

    protected long getCloseTime() {
        return 0;
    }

    protected boolean getConditionToShow() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onUpdate(float sec) {}

    protected void onShowAttempt() {}

    protected void onCloseAttempt() {}

    protected void onScreenChange(Screens lastScreen, Screens newScreen) {}

    //--------------------------------------------------------------------------------------------//

    protected final String generateTag() {
        return getClass().getSimpleName() + "@" + hashCode();
    }

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

        rootView = inflater.inflate(getLayout(), container, false);

        rootBackground = rootView.findViewById(R.id.background);
        onLoad();
        isLoaded = true;
        if (isDismissOnBackgroundPress && rootBackground != null) {
            rootBackground.setClickable(true);

            if (!rootBackground.hasOnClickListeners()) {
                bindTouchListener(rootBackground, new TouchListener() {
                    public boolean hasTouchEffect() {
                        return false;
                    }

                    public void onPressUp() {
                        close();
                    }
                });
            }
        }
        if (getCloseTime() > 0) {
            rootView.postDelayed(close, getCloseTime());
        }
        return rootView;
    }

    //---------------------------------------Management-------------------------------------------//

    protected void onTransition() {
        String name = getClass().getSimpleName();

        if (isAdded()) {
            Log.i("FragmentPlatform", "Added fragment " + name);
            return;
        }
        Log.i("FragmentPlatform", "Removed fragment " + name);
        System.gc();
    }

    public void close() {
        if (!isAdded()) {
            onCloseAttempt();
            return;
        }
        isLoaded = false;
        rootView.removeCallbacks(close);
        unbindTouchListeners();
        registeredViews.clear();
        Game.platform.removeFragment(this);
    }

    public void show() {
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

    protected final void setDismissMode(boolean onBackgroundPress, boolean onBackPress) {
        isDismissOnBackgroundPress = onBackgroundPress;
        isDismissOnBackPress = onBackPress;
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

    //--------------------------------------------------------------------------------------------//

    public void init() {}
}
