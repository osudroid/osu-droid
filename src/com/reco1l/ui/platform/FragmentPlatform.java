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
import com.reco1l.Scenes;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.interfaces.IMainClasses;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 22/6/22 02:25

public class FragmentPlatform implements IMainClasses {

    private static FragmentPlatform instance;
    private static final int containerId = 0x999999;

    private final List<Fragment> fragments;
    private final Map<Scenes, FrameLayout> sceneLayouts;

    public RenderSurfaceView renderView;
    public FragmentManager manager;
    public FrameLayout container;
    public Context context;

    private LoaderFragment loaderFragment;

    //--------------------------------------------------------------------------------------------//

    public FragmentPlatform() {
        this.fragments = new ArrayList<>();
        this.sceneLayouts = new HashMap<>();
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
        initializeContainers(container);
    }
    
    private void initializeContainers(FrameLayout mainContainer) {
        for (Scenes scene : Scenes.values()) {
            FrameLayout container = new FrameLayout(context);
            container.setId(containerId + scene.ordinal() + 1);

            sceneLayouts.put(scene, container);
            mainContainer.addView(container, params);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void addSceneFragment(Scenes type, Fragment fragment, String tag) {
        if (fragment.isAdded() || fragments.contains(fragment) || manager.findFragmentByTag(tag) != null)
            return;

        if (sceneLayouts.get(type) == null)
            return;

        fragments.add(fragment);

        mActivity.runOnUiThread(() -> {
            @SuppressWarnings("ConstantConditions")
            final int id = sceneLayouts.get(type).getId();

            manager.beginTransaction().add(id, fragment, tag).commit();
        });
    }
    
    public void addFragment(Fragment fragment, String tag) {
        if (fragment.isAdded() || fragments.contains(fragment) || manager.findFragmentByTag(tag) != null)
            return;

        fragments.add(fragment);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().add(container.getId(), fragment, tag).commit());
    }

    public void removeFragment(Fragment fragment) {
        if (!fragment.isAdded() || !fragments.contains(fragment))
            return;

        fragments.remove(fragment);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().remove(fragment).commit());
    }

    //--------------------------------------------------------------------------------------------//

    /**
     * Close all fragments currently showing
     */
    public void closeAll() {
        for (int i = 0; i < fragments.size(); i++) {
            Fragment fragment = fragments.get(i);
            if (fragment.getClass().getSuperclass() == UIFragment.class) {
                mActivity.runOnUiThread(((UIFragment) fragment)::close);
            }
        }
    }

    /**
     * @param toClose fragments to close
     */
    public void closeThis(UIFragment... toClose) {
        for (UIFragment fragment : toClose) {
           if (fragment != null)
               mActivity.runOnUiThread(fragment::close);
        }
    }

    /**
     * @param toExclude fragments to exclude from closing
     */
    public void closeAllExcept(UIFragment... toExclude) {
        List<Fragment> toClose = new ArrayList<>(fragments);
        toClose.removeAll(Arrays.asList(toExclude));

        for (Fragment fragment: toClose) {
            if (fragment.getClass().getSuperclass() == UIFragment.class)
                mActivity.runOnUiThread(((UIFragment) fragment)::close);
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
                .play(300);
    }

    public static class LoaderFragment extends Fragment implements IMainClasses {

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