package com.reco1l.ui;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.annotation.SuppressLint;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.reco1l.global.Game;
import com.reco1l.global.Scenes;
import com.reco1l.interfaces.fields.Identifiers;
import com.reco1l.scenes.BaseScene;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Logging;
import com.reco1l.utils.FrameCounter;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;

// Created by Reco1l on 22/6/22 02:25

public final class FragmentPlatform {

    public static final FragmentPlatform instance = new FragmentPlatform();

    private FragmentManager mManager;

    private RenderSurfaceView mRenderView;

    private FrameLayout
            mOverlayContainer,
            mScreenContainer;

    private final ArrayList<BaseFragment>
            mShowing,
            mCreated;

    private boolean mLockUpdate = false;

    //--------------------------------------------------------------------------------------------//

    public FragmentPlatform() {
        Logging.initOf(getClass());

        mShowing = new ArrayList<>();
        mCreated = new ArrayList<>();
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressLint("ResourceType")
    public void load(AppCompatActivity activity, RenderSurfaceView renderView) {

        this.mRenderView = renderView;
        this.mManager = activity.getSupportFragmentManager();

        ConstraintLayout platform = new ConstraintLayout(activity);
        RelativeLayout renderLayout = new RelativeLayout(activity);

        mScreenContainer = new FrameLayout(activity);
        mOverlayContainer = new FrameLayout(activity);

        mScreenContainer.setId(Identifiers.Platform_ScreenFrame);
        mOverlayContainer.setId(Identifiers.Platform_OverlayFrame);

        LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

        platform.addView(renderLayout, params);
        platform.addView(mScreenContainer, params);
        platform.addView(mOverlayContainer, params);

        Game.activity.setContentView(platform, params);

        renderLayout.setGravity(Gravity.CENTER);
        renderLayout.addView(renderView, params);

        FrameCounter.start();
    }

    //--------------------------------------------------------------------------------------------//

    public FragmentTransaction transaction() {
        return mManager.beginTransaction();
    }

    public FragmentManager getManager() {
        return mManager;
    }

    public FrameLayout getScreenContainer() {
        return mScreenContainer;
    }

    public FrameLayout getOverlayContainer() {
        return mOverlayContainer;
    }

    public RenderSurfaceView getRenderView() {
        return mRenderView;
    }

    //--------------------------------------------------------------------------------------------//

    void onFragmentCreated(BaseFragment fragment) {
        if (fragment.parents != null) {
            mCreated.add(fragment);
        }
    }

    public void onEngineUpdate(float sec) {
        if (mLockUpdate) {
            return;
        }

        synchronized (mShowing) {
            mShowing.forEach(fragment -> {
                if (fragment.isAdded() && fragment.isLoaded()) {
                    Game.activity.runOnUiThread(() -> fragment.onEngineUpdate(sec));
                }
            });
        }
    }

    public void onSceneChange(BaseScene lastScene, BaseScene newScene) {
        mLockUpdate = true;
        FrameCounter.countFromEngine(newScene == Scenes.player);

        synchronized (mShowing) {

            int i = 0;
            while (i < mShowing.size()) {
                BaseFragment fragment = mShowing.get(i);

                if (fragment.isExtra() || fragment.parents == null) {
                    fragment.close();
                }
                ++i;
            }

            mCreated.forEach(fragment -> {
                for (BaseScene parent : fragment.parents) {
                    if (parent == newScene) {
                        fragment.show();
                        return;
                    }
                }
                fragment.close();
            });
        }

        notifySceneChange(lastScene, newScene);
        mLockUpdate = false;
    }

    public boolean onBackPress() {
        synchronized (mShowing) {
            for (BaseFragment f : mShowing) {
                if (f.onBackPress()) {
                    return true;
                }
            }
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public Animation animate(boolean render, boolean screen) {
        View[] views = {
                (render ? mRenderView : null),
                (screen ? mScreenContainer : null)
        };

        return Animation.of(views).cancelCurrentAnimations(false);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean addFragment(BaseFragment fragment) {
        synchronized (mShowing) {
            if (fragment.isAdded() || mShowing.contains(fragment)) {
                return false;
            }
            mShowing.add(fragment);

            FrameLayout container = fragment.isOverlay() ? mOverlayContainer : mScreenContainer;

            FragmentTransaction t = mManager.beginTransaction()
                    .add(container.getId(), fragment)
                    .runOnCommit(fragment::onTransaction);

            Game.activity.runOnUiThread(t::commitAllowingStateLoss);
            return true;
        }
    }

    public void removeFragment(BaseFragment fragment) {
        synchronized (mShowing) {
            if (!fragment.isAdded()) {
                return;
            }
            mShowing.remove(fragment);

            FragmentTransaction t = mManager.beginTransaction()
                    .remove(fragment)
                    .runOnCommit(fragment::onTransaction);

            Game.activity.runOnUiThread(t::commitAllowingStateLoss);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void onExit() {

    }

    public void closeExtras() {
        synchronized (mShowing) {
            int i = 0;
            while (i < mShowing.size()) {
                BaseFragment fragment = mShowing.get(i);

                if (fragment.isExtra()) {
                    fragment.close();
                }
                ++i;
            }
        }
    }

    private void notifySceneChange(BaseScene lastScene, BaseScene newScene) {
        synchronized (mShowing) {
            mShowing.forEach(fragment ->
                    fragment.onSceneChange(lastScene, newScene)
            );
        }
    }
}