package com.reco1l.ui.platform;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.utils.Animation;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Created by Reco1l on 22/6/22 02:25

public final class FragmentPlatform {

    private static FragmentPlatform instance;

    private static final int SCREEN_CONTAINER_ID = 0x999999;
    private static final int OVERLAY_CONTAINER_ID = 0x999990;

    public final List<Dialog> dialogs;

    private final List<Fragment> fragments;
    private final Map<Screens, List<UIFragment>> screenFragments;

    public RenderSurfaceView renderView;
    public FragmentManager manager;
    public Context context;

    public FrameLayout screenContainer;
    public FrameLayout overlayContainer;

    //--------------------------------------------------------------------------------------------//

    public FragmentPlatform() {
        dialogs = new ArrayList<>();
        fragments = new ArrayList<>();
        screenFragments = new HashMap<>();

        for (Screens scene : Screens.values()) {
            screenFragments.put(scene, new ArrayList<>());
        }
    }

    public static FragmentPlatform getInstance() {
        if (instance == null) {
            instance = new FragmentPlatform();
        }
        return instance;
    }

    //--------------------------------------------------------------------------------------------//

    public void load(AppCompatActivity activity, Context context, RenderSurfaceView renderView) {

        this.manager = activity.getSupportFragmentManager();
        this.renderView = renderView;
        this.context = context;

        ConstraintLayout platform = new ConstraintLayout(context);
        RelativeLayout renderLayout = new RelativeLayout(context);

        screenContainer = new FrameLayout(context);
        overlayContainer = new FrameLayout(context);

        screenContainer.setId(SCREEN_CONTAINER_ID);
        overlayContainer.setId(OVERLAY_CONTAINER_ID);

        LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

        platform.addView(renderLayout, params);
        platform.addView(screenContainer, params);
        platform.addView(overlayContainer, params);

        Game.activity.setContentView(platform, params);

        renderLayout.setGravity(Gravity.CENTER);
        renderLayout.addView(renderView, params);
    }

    public void onUpdate(float elapsed) {
        int i = 0;
        while (i < fragments.size()) {
            Fragment fragment = fragments.get(i);

            if (fragment == null)
                return;

            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;

                if (frg.isShowing) {
                    Game.activity.runOnUiThread(() -> frg.onUpdate(elapsed));
                }
            }
            i++;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public Animation animate(boolean render, boolean screen) {
        if (!render && !screen) {
            return null;
        }
        return Animation.of((render ? renderView : null), (screen ? screenContainer : null))
                .cancelCurrentAnimations(false);
    }

    public Animation animateRender() {
        return animate(true, false);
    }

    public Animation animateScreen() {
        return animate(false, true);
    }

    //--------------------------------------------------------------------------------------------//
    
    public boolean addFragment(Fragment fragment, String tag, boolean isOverlay) {
        if (fragment.isAdded() || fragments.contains(fragment) || manager.findFragmentByTag(tag) != null)
            return false;

        fragments.add(fragment);
        commitTransaction(fragment, tag, isOverlay);
        return true;
    }

    public boolean removeFragment(Fragment fragment) {
        if (!fragment.isAdded() || !fragments.contains(fragment))
            return false;

        fragments.remove(fragment);
        Game.activity.runOnUpdateThread(() -> commitTransaction(fragment, null, false));
        return true;
    }

    private void commitTransaction(Fragment fragment, String tag, boolean isOverlay) {
        Game.activity.runOnUiThread(() -> {
            FragmentTransaction transaction = manager.beginTransaction();

            int containerId = isOverlay ? overlayContainer.getId() : screenContainer.getId();

            if (tag == null) {
                transaction.remove(fragment);
            } else {
                transaction.add(containerId, fragment, tag);
            }
            transaction.commitAllowingStateLoss();
        });
    }

    //--------------------------------------------------------------------------------------------//

    private List<UIFragment> getFragmentList(Screens scene) {
        List<UIFragment> list = screenFragments.get(scene);
        if (list == null) {
            list = new ArrayList<>();
            screenFragments.put(scene, list);
        }
        return list;
    }

    public void assignToScene(Screens scene, UIFragment fragment) {
        getFragmentList(scene).add(fragment);
    }


    public UIFragment[] getFragmentsFrom(Screens scene) {
        UIFragment[] array = new UIFragment[getFragmentList(scene).size()];
        getFragmentList(scene).toArray(array);
        return array;
    }

    //--------------------------------------------------------------------------------------------//

    public void showAll(Screens scene) {
        for (UIFragment fragment : getFragmentList(scene)) {
            fragment.show();
        }
    }

    public void close(UIFragment... toClose) {
        for (UIFragment fragment : toClose) {
           if (fragment != null) {
               Game.activity.runOnUiThread(fragment::close);
           }
        }
    }

    public void closeAllExcept(UIFragment... toExclude) {
        List<Fragment> toClose = new ArrayList<>(fragments);
        toClose.removeAll(Arrays.asList(toExclude));

        for (Fragment fragment: toClose) {
            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;
                Game.activity.runOnUiThread(frg::close);
            }
        }
    }

    public void notifyScreenChange(Screens lastScreen, Screens newScreen) {
        for (int i = 0; i < fragments.size(); ++i) {
            Fragment fragment = fragments.get(i);

            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;
                Game.activity.runOnUiThread(() -> frg.onScreenChange(lastScreen, newScreen));
            }
        }
    }
}