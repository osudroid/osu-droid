package com.reco1l.ui.platform;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.enums.Scenes;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.interfaces.IReferences;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:25

public final class FragmentPlatform implements IReferences {

    private static FragmentPlatform instance;
    private static final int containerId = 0x999999;

    private final List<Fragment> fragments;
    private final Map<Scenes, List<UIFragment>> sceneFragments;

    public RenderSurfaceView renderView;
    public FragmentManager manager;
    public FrameLayout container;
    public Context context;

    private LoaderFragment loaderFragment;

    //--------------------------------------------------------------------------------------------//

    public FragmentPlatform() {
        this.fragments = new ArrayList<>();
        this.sceneFragments = new HashMap<>();

        for (Scenes scene : Scenes.values()) {
            sceneFragments.put(scene, new ArrayList<>());
        }
    }

    public static FragmentPlatform getInstance() {
        return instance;
    }

    private final LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

    //--------------------------------------------------------------------------------------------//

    public void load(AppCompatActivity activity, Context context, RenderSurfaceView renderView) {
        instance = this;
        this.renderView = renderView;
        this.context = context;

        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        RelativeLayout platform = new RelativeLayout(context);
        container = new FrameLayout(context);
        View view = new View(context);

        container.setId(containerId);
        view.setBackgroundColor(Color.argb(0, 0, 0, 0));

        platform.addView(renderView, params);
        platform.addView(container, params);
        platform.addView(view, params);

        mActivity.setContentView(platform, params);
        manager = activity.getSupportFragmentManager();

        loaderFragment = new LoaderFragment();
    }

    public void onUpdate(float elapsed) {
        for (Fragment fragment : fragments) {
            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;

                if (frg.isShowing) {
                    mActivity.runOnUiThread(() -> frg.onUpdate(elapsed));
                }
            }
        }
    }

    //--------------------------------------------------------------------------------------------//
    
    public boolean addFragment(Fragment fragment, String tag) {
        if (fragment.isAdded() || fragments.contains(fragment) || manager.findFragmentByTag(tag) != null)
            return false;

        fragments.add(fragment);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().add(container.getId(), fragment, tag).commitAllowingStateLoss());
        return true;
    }

    public boolean removeFragment(Fragment fragment) {
        if (!fragment.isAdded() || !fragments.contains(fragment))
            return false;

        fragments.remove(fragment);
        mActivity.runOnUiThread(() -> manager.beginTransaction().remove(fragment).commitAllowingStateLoss());
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    private List<UIFragment> getFragmentList(Scenes scene) {
        List<UIFragment> list = sceneFragments.get(scene);
        if (list == null) {
            list = new ArrayList<>();
            sceneFragments.put(scene, list);
        }
        return list;
    }

    public void assignToScene(Scenes scene, UIFragment fragment) {
        getFragmentList(scene).add(fragment);
    }


    public UIFragment[] getFragmentsFrom(Scenes scene) {
        UIFragment[] array = new UIFragment[getFragmentList(scene).size()];
        getFragmentList(scene).toArray(array);
        return array;
    }

    //--------------------------------------------------------------------------------------------//

    public void showAll(Scenes scene) {
        for (UIFragment fragment : getFragmentList(scene)) {
            fragment.show();
        }
    }

    public void close(UIFragment... toClose) {
        for (UIFragment fragment : toClose) {
           if (fragment != null) {
               mActivity.runOnUiThread(fragment::close);
           }
        }
    }

    public void closeAllExcept(UIFragment... toExclude) {
        List<Fragment> toClose = new ArrayList<>(fragments);
        toClose.removeAll(Arrays.asList(toExclude));

        for (Fragment fragment: toClose) {
            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;
                mActivity.runOnUiThread(frg::close);
            }
        }
    }

    public void notifySceneChange(Scenes oldScene, Scenes newScene) {
        for (Fragment fragment: fragments) {
            if (fragment instanceof UIFragment) {
                UIFragment frg = (UIFragment) fragment;
                mActivity.runOnUiThread(() -> frg.onSceneChange(oldScene, newScene));
            }
        }
    }

    //--------------------------------------------------------------------------------------------//
    public void handleWindowFocus(boolean isResumed) {
        if (!isResumed) {
            addFragment(loaderFragment, "loaderFragment@" + loaderFragment.hashCode());
            return;
        }
        new Animation(loaderFragment.layout).fade(1, 0)
                .runOnEnd(() -> removeFragment(loaderFragment))
                .delay(50)
                .play(500);
    }

    public static class LoaderFragment extends Fragment implements IReferences {

        protected RelativeLayout layout;

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {

            layout = new RelativeLayout(platform.context);
            layout.setLayoutParams(platform.params);
            layout.setElevation(Resources.dimen(R.dimen.imposedLayer));

            CircularProgressIndicator indicator = new CircularProgressIndicator(platform.context);
            indicator.setTrackCornerRadius((int) Resources.dimen(R.dimen.progressBarTrackCornerRadius));
            indicator.setIndicatorColor(Resources.color(R.color.progressBarIndicatorColor));
            indicator.setIndeterminate(true);

            View background = new View(platform.context);
            background.setBackgroundColor(Color.BLACK);

            float size = Resources.dimen(R.dimen.loadingScreenProgressBarSize);

            LayoutParams params = new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
            params.addRule(RelativeLayout.CENTER_IN_PARENT);
            params.width = (int) size;
            params.height = (int) size;

            layout.addView(background, platform.params);
            layout.addView(indicator, params);

            return layout;
        }
    }
}