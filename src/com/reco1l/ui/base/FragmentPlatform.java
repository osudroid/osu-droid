package com.reco1l.ui.base;

import android.content.Context;
import android.content.res.Resources;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.reco1l.Game;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.framework.Animation;
import com.reco1l.framework.Logging;
import com.reco1l.framework.FrameCounter;
import com.reco1l.framework.Views;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;

// Created by Reco1l on 22/6/22 02:25

public final class FragmentPlatform implements Identifiers {

    public static final FragmentPlatform instance = new FragmentPlatform();

    private FragmentManager mManager;

    private RenderSurfaceView mRenderView;

    private FrameLayout
            mBackgroundContainer,
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

    public static int getNavigationBarHeight(Context context) {
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        if (resourceId > 0) {
            return resources.getDimensionPixelSize(resourceId);
        }
        return 0;
    }

    private RelativeLayout createPlatform(Context context, RenderSurfaceView render) {
        RelativeLayout frame = new RelativeLayout(context);

        mBackgroundContainer = new FrameLayout(context);
        mBackgroundContainer.setId(Platform_Background);
        frame.addView(mBackgroundContainer, Views.match_parent);

        // Centering the SurfaceView
        LinearLayout layout = new LinearLayout(context);
        layout.setGravity(Gravity.CENTER);
        layout.addView(render);
        frame.addView(layout, Views.match_parent);

        mScreenContainer = new FrameLayout(context);
        mScreenContainer.setId(Platform_Screen);
        frame.addView(mScreenContainer, Views.match_parent);

        mOverlayContainer = new FrameLayout(context);
        mOverlayContainer.setId(Platform_Overlay);
        frame.addView(mOverlayContainer, Views.match_parent);

        // For some reason drawing views below the SurfaceView causes black boxes glitches
        // this should fix it
        View view = new View(context);
        frame.addView(view, Views.match_parent);
        return frame;
    }

    public void onSetContentView(AppCompatActivity activity, RenderSurfaceView renderView) {

        mManager = activity.getSupportFragmentManager();
        mRenderView = renderView;

        RelativeLayout platform = createPlatform(activity, renderView);

        // TODO [FragmentPlatform] Fix rendering dimensions in wider devices this issue came from
        //  GLSurfaceView which seems to have the navigation bar margin always enabled
        //renderView.post(() -> Views.size(platform, renderView.getWidth(), renderView.getHeight()));

        Game.activity.setContentView(platform);
        FrameCounter.start();
    }

    //--------------------------------------------------------------------------------------------//

    public FragmentTransaction transaction() {
        return mManager.beginTransaction();
    }

    public FragmentManager getManager() {
        return mManager;
    }

    public FrameLayout getBackgroundContainer() {
        return mBackgroundContainer;
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

        synchronized (mShowing) {

            int i = 0;
            while (i < mShowing.size()) {
                BaseFragment fragment = mShowing.get(i);

                if (fragment.isExtra() || fragment.parents == null) {
                    fragment.close();
                }
                ++ i;
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

        return Animation.of(mScreenContainer).cancelCurrentAnimations(false);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean addFragment(BaseFragment fragment) {
        synchronized (mShowing) {
            if (fragment.isAdded() || mShowing.contains(fragment)) {
                return false;
            }
            mShowing.add(fragment);

            int container = fragment.getLayer().getContainerID();

            FragmentTransaction t = mManager.beginTransaction()
                                            .add(container, fragment)
                                            .runOnCommit(fragment::onTransaction);

            Game.activity.runOnUiThread(t::commitAllowingStateLoss);
            return true;
        }
    }

    public void removeFragment(BaseFragment fragment) {
        synchronized (mShowing) {
            if (! fragment.isAdded()) {
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
                ++ i;
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