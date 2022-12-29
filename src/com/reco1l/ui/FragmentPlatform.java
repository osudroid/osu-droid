package com.reco1l.ui;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.content.Context;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
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

    private final List<BaseFragment> fragments;
    private final Map<Screens, List<BaseFragment>> screenFragments;

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
            BaseFragment fragment = fragments.get(i);

            if (fragment == null) {
                return;
            }

            if (fragment.isAdded()) {
                Game.activity.runOnUiThread(() -> fragment.onUpdate(elapsed));
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

    public void addFragment(BaseFragment fragment) {
        if (fragment.isAdded()
                || fragments.contains(fragment)
                || manager.findFragmentByTag(fragment.getTag()) != null) {
            return;
        }
        commitTransaction(fragment);
    }

    public void removeFragment(BaseFragment fragment) {
        if (!fragment.isAdded()) {
            return;
        }
        commitTransaction(fragment);
    }

    private void commitTransaction(BaseFragment fragment) {
        FragmentTransaction transaction = manager.beginTransaction();

        FrameLayout container = fragment.isOverlay() ? overlayContainer : screenContainer;

        if (fragment.isAdded()) {
            transaction.remove(fragment);
            transaction.runOnCommit(() -> {
                Game.activity.runOnUpdateThread(() -> fragments.remove(fragment));
                fragment.onTransition();
            });
        }

        if (!fragment.isAdded()) {
            transaction.add(container.getId(), fragment, fragment.generateTag());
            transaction.runOnCommit(() -> {
                Game.activity.runOnUpdateThread(() -> fragments.add(fragment));
                fragment.onTransition();
            });
        }
        transaction.commitAllowingStateLoss();
    }

    //--------------------------------------------------------------------------------------------//

    private List<BaseFragment> getFragmentList(Screens screen) {
        return screenFragments.computeIfAbsent(screen, k -> new ArrayList<>());
    }

    public void assignToScene(Screens scene, BaseFragment fragment) {
        getFragmentList(scene).add(fragment);
    }


    public BaseFragment[] getFragmentsFrom(Screens scene) {
        BaseFragment[] array = new BaseFragment[getFragmentList(scene).size()];
        getFragmentList(scene).toArray(array);
        return array;
    }

    //--------------------------------------------------------------------------------------------//

    public void showAll(Screens scene) {
        for (BaseFragment fragment : getFragmentList(scene)) {
            Game.activity.runOnUiThread(fragment::show);
        }
    }

    public void close(BaseFragment... toClose) {
        for (BaseFragment fragment : toClose) {
            Game.activity.runOnUiThread(fragment::close);
        }
    }

    public void closeAllExcept(BaseFragment... toExclude) {
        List<BaseFragment> toClose = new ArrayList<>(fragments);
        toClose.removeAll(Arrays.asList(toExclude));

        for (BaseFragment fragment : toClose) {
            Game.activity.runOnUiThread(fragment::close);
        }
    }

    public void notifyScreenChange(Screens lastScreen, Screens newScreen) {
        for (int i = 0; i < fragments.size(); ++i) {
            BaseFragment fragment = fragments.get(i);

            Game.activity.runOnUiThread(() -> fragment.onScreenChange(lastScreen, newScreen));
        }
    }
}