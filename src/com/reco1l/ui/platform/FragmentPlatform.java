package com.reco1l.ui.platform;

import static android.widget.RelativeLayout.LayoutParams.*;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.edlplan.ui.ActivityOverlay;

import org.anddev.andengine.opengl.view.RenderSurfaceView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;

// Created by Reco1l on 22/6/22 02:25

public class FragmentPlatform {

    protected final MainActivity mActivity = GlobalManager.getInstance().getMainActivity();

    private static FragmentPlatform instance;
    private List<Fragment> fragments;

    public RenderSurfaceView renderView;
    public FragmentManager manager;
    public FrameLayout container;
    public Context context;

    //--------------------------------------------------------------------------------------------//

    public static FragmentPlatform getInstance() {
        return instance;
    }

    private final LayoutParams params = new LayoutParams(MATCH_PARENT, MATCH_PARENT);

    //--------------------------------------------------------------------------------------------//

    public void load(AppCompatActivity activity, Context context, RenderSurfaceView renderView) {
        instance = this;
        fragments = new ArrayList<>();
        this.renderView = renderView;
        this.context = context;
        int Id = 0x90009;

        params.addRule(RelativeLayout.CENTER_IN_PARENT);

        RelativeLayout platform = new RelativeLayout(context);
        container = new FrameLayout(context);
        View view = new View(context);

        container.setId(Id);
        view.setBackgroundColor(Color.argb(0, 0, 0, 0));

        platform.addView(renderView, params);
        platform.addView(container, params);
        platform.addView(view, params);

        mActivity.setContentView(platform, params);
        manager = activity.getSupportFragmentManager();

        //I leave it for compatibility with EdrowsLuo fragments until the entire UI gets replaced.
        ActivityOverlay.initial(activity, container.getId());
    }

    //--------------------------------------------------------------------------------------------//


    public void addFragment(BaseLayout layout, String tag) {
        if (layout.isAdded() || fragments.contains(layout) || manager.findFragmentByTag(tag) != null)
            return;

        fragments.add(layout);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().add(container.getId(), layout, tag).commit());
    }

    public void removeFragment(BaseLayout fragment) {
        if (!fragment.isAdded() || !fragments.contains(fragment))
            return;

        fragments.remove(fragment);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().remove(fragment).commit());
    }

    public void closeAll() {
        for (Fragment fragment: fragments) {
            if (fragment.getClass().isAssignableFrom(BaseLayout.class))
                mActivity.runOnUiThread(((BaseLayout) fragment)::close);
        }
    }

    public void closeThis(BaseLayout... toClose) {
        for (BaseLayout layout : toClose) {
            if (fragments.contains(layout))
                mActivity.runOnUiThread(layout::close);
        }
    }

    public void closeAllExcept(BaseLayout... toExclude) {
        List<Fragment> toClose = new ArrayList<>(fragments);
        toClose.removeAll(Arrays.asList(toExclude));

        for (Fragment fragment: toClose) {
            if (fragment.getClass().isAssignableFrom(BaseLayout.class))
                mActivity.runOnUiThread(((BaseLayout) fragment)::close);
        }
    }
}