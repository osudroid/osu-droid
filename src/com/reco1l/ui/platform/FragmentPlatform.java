package com.reco1l.ui.platform;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Animation2;
import com.reco1l.utils.Resources;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:25

public final class FragmentPlatform {

    private static FragmentPlatform instance;

    private static final int SCREEN_CONTAINER_ID = 0x999999;
    private static final int OVERLAY_CONTAINER_ID = 0x999990;

    public final List<Dialog> dialogs;

    private final List<Fragment> fragments;
    private final Map<Screens, List<UIFragment>> screenFragments;

    private final LayoutParams params;

    public RenderSurfaceView renderView;
    public FragmentManager manager;
    public Context context;

    public FrameLayout screenContainer;
    public FrameLayout overlayContainer;

    private LoaderFragment loaderFragment;

    //--------------------------------------------------------------------------------------------//

    public FragmentPlatform() {
        dialogs = new ArrayList<>();
        fragments = new ArrayList<>();
        screenFragments = new HashMap<>();

        params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

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

        screenContainer = new FrameLayout(context);
        overlayContainer = new FrameLayout(context);

        screenContainer.setId(SCREEN_CONTAINER_ID);
        overlayContainer.setId(OVERLAY_CONTAINER_ID);

        platform.addView(renderView, params);
        platform.addView(screenContainer, params);
        platform.addView(overlayContainer, params);

        Game.mActivity.setContentView(platform, params);

        loaderFragment = new LoaderFragment();
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
                    Game.mActivity.runOnUiThread(() -> frg.onUpdate(elapsed));
                }
            }
            i++;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void animateRender(Animation.IAnimationHandler animation) {
        Animation anim = new Animation(renderView);
        animation.onAnimate(anim);
        anim.cancelPending(false);
        anim.play();
    }

    public void animateScreen(Animation2.BehaviorHandler handler) {
        Animation2 animation = Animation2.of(screenContainer);
        handler.onAnimate(animation);
        animation.cancelCurrentAnimations = false;
        animation.play();
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
        Game.mActivity.runOnUpdateThread(() -> commitTransaction(fragment, null, false));
        return true;
    }

    private void commitTransaction(Fragment fragment, String tag, boolean isOverlay) {
        Game.mActivity.runOnUiThread(() -> {
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
               Game.mActivity.runOnUiThread(fragment::close);
           }
        }
    }

    public void closeAllExcept(UIFragment... toExclude) {
        List<Fragment> toClose = new ArrayList<>(fragments);
        toClose.removeAll(Arrays.asList(toExclude));

        for (Fragment fragment: toClose) {
            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;
                Game.mActivity.runOnUiThread(frg::close);
            }
        }
    }

    public void notifyScreenChange(Screens lastScreen, Screens newScreen) {
        for (Fragment fragment: fragments) {
            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;
                Game.mActivity.runOnUiThread(() -> frg.onScreenChange(lastScreen, newScreen));
            }
        }
    }

    //--------------------------------------------------------------------------------------------//
    public void handleWindowFocus(boolean isResumed) {
        if (!isResumed) {
            addFragment(loaderFragment, "loaderFragment@" + loaderFragment.hashCode(), true);
            return;
        }

        Animation anim = new Animation(loaderFragment.layout);

        anim.fade(1, 0);
        anim.runOnEnd(() -> removeFragment(loaderFragment));
        anim.delay(50);
        anim.play(500);
    }

    //--------------------------------------------------------------------------------------------//

    public static class LoaderFragment extends Fragment {

        protected RelativeLayout layout;

        //----------------------------------------------------------------------------------------//

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
            layout = new RelativeLayout(Game.platform.context);
            layout.setLayoutParams(Game.platform.params);
            layout.setElevation(Resources.dimen(R.dimen.imposedLayer));
            layout.setBackground(new ColorDrawable(Color.BLACK));
            return layout;
        }
    }
}