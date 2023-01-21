package com.reco1l.ui;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.Gravity;
import android.view.View;
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
import com.reco1l.utils.NativeFrameCounter;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;

// Created by Reco1l on 22/6/22 02:25

public final class FragmentPlatform {

    public static final FragmentPlatform instance = new FragmentPlatform();

    public RenderSurfaceView renderView;
    public FragmentManager manager;
    public Context context;

    public FrameLayout screenContainer;
    public FrameLayout overlayContainer;

    private final ArrayList<Dialog> dialogs;
    private final ArrayList<BaseFragment> showing, created;

    private final Object listMutex;

    //--------------------------------------------------------------------------------------------//

    public FragmentPlatform() {
        dialogs = new ArrayList<>();
        showing = new ArrayList<>();
        created = new ArrayList<>();
        listMutex = new Object();
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressLint("ResourceType")
    public void load(AppCompatActivity activity, RenderSurfaceView renderView) {

        this.context = activity;
        this.renderView = renderView;
        this.manager = activity.getSupportFragmentManager();

        ConstraintLayout platform = new ConstraintLayout(context);
        RelativeLayout renderLayout = new RelativeLayout(context);

        screenContainer = new FrameLayout(context);
        overlayContainer = new FrameLayout(context);

        screenContainer.setId(Identifiers.Platform_ScreenFrame);
        overlayContainer.setId(Identifiers.Platform_OverlayFrame);

        LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

        platform.addView(renderLayout, params);
        platform.addView(screenContainer, params);
        platform.addView(overlayContainer, params);

        Game.activity.setContentView(platform, params);

        renderLayout.setGravity(Gravity.CENTER);
        renderLayout.addView(renderView, params);

        NativeFrameCounter.startCounter();
    }

    //--------------------------------------------------------------------------------------------//

    public FragmentTransaction transaction() {
        return manager.beginTransaction();
    }

    public FragmentManager getManager() {
        return manager;
    }

    //--------------------------------------------------------------------------------------------//

    void onFragmentCreated(BaseFragment fragment) {
        if (fragment.parents != null) {
            created.add(fragment);
        }
    }

    public void onEngineUpdate(float sec) {
        synchronized (listMutex) {
            showing.forEach(f -> {
                if (f.isAdded()) {
                    Game.activity.runOnUiThread(() -> f.onUpdate(sec));
                }
            });
        }
    }

    public void onScreenChange(Screens last, Screens current) {
        Game.activity.runOnUiThread(() -> {
            closeExtras();

            synchronized (listMutex) {
                showing.forEach(f -> {
                    if (f.parents == null) {
                        f.close();
                    }
                });

                created.forEach(f -> {
                    if (Arrays.asList(f.parents).contains(current)) {
                        f.show();
                        return;
                    }
                    f.close();
                });
            }

            notifyScreenChange(last, current);
        });
    }

    //--------------------------------------------------------------------------------------------//

    public ArrayList<Dialog> getDialogs() {
        return dialogs;
    }

    public Animation animate(boolean render, boolean screen) {
        View[] views = {
                (render ? renderView : null),
                (screen ? screenContainer : null)
        };

        return Animation.of(views).cancelCurrentAnimations(false);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean addFragment(BaseFragment fragment) {
        synchronized (listMutex) {
            if (fragment.isAdded() || showing.contains(fragment)) {
                return false;
            }

            showing.add(fragment);
            if (fragment instanceof Dialog) {
                dialogs.add((Dialog) fragment);
            }

            FrameLayout container = fragment.isOverlay() ? overlayContainer : screenContainer;

            manager.beginTransaction()
                    .add(container.getId(), fragment)
                    .runOnCommit(fragment::onTransaction)
                    .commitAllowingStateLoss();

            return true;
        }
    }

    public void removeFragment(BaseFragment fragment) {
        synchronized (listMutex) {
            if (!fragment.isAdded()) {
                return;
            }

            showing.remove(fragment);
            if (fragment instanceof Dialog) {
                dialogs.remove((Dialog) fragment);
            }

            manager.beginTransaction()
                    .remove(fragment)
                    .runOnCommit(fragment::onTransaction)
                    .commitAllowingStateLoss();
        }
    }

    //--------------------------------------------------------------------------------------------//

    public boolean closeExtras() {
        synchronized (listMutex) {
            for (BaseFragment f : showing) {
                if (f.isExtra()) {
                    f.close();
                    return true;
                }
            }
        }
        return false;
    }

    public void closeAllExcept(BaseFragment fragment) {
        synchronized (listMutex) {
            showing.remove(fragment);
            showing.forEach(BaseFragment::close);
            showing.add(fragment);
        }
    }

    public void notifyScreenChange(Screens lastScreen, Screens newScreen) {
        synchronized (listMutex) {
            showing.forEach(fragment ->
                    fragment.onScreenChange(lastScreen, newScreen)
            );
        }
    }
}