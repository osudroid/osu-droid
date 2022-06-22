package com.reco1l.ui.platform;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import androidx.appcompat.app.AppCompatActivity;
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
    // It places a overlay as MainActivity content view, sets the AndEngine SurfaceView
    // and manages the UI layouts in a easy way.

    protected final MainActivity mActivity = GlobalManager.getInstance().getMainActivity();
    private static FragmentPlatform instance;
    protected FragmentManager manager;
    private List<BaseLayout> layouts;
    public FrameLayout container;
    
    //--------------------------------------------------------------------------------------------//
    public static FragmentPlatform getInstance() {
        return instance;
    }

    private final RelativeLayout.LayoutParams matchParent = new RelativeLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT) {
        { addRule(RelativeLayout.CENTER_IN_PARENT);}
    };

    //--------------------------------------------------------------------------------------------//

    public void load(AppCompatActivity activity, Context context, RenderSurfaceView engine) {
        instance = this;
        layouts = new ArrayList<>();
        int Id = 0x90009;

        RelativeLayout platform = new RelativeLayout(context);
        container = new FrameLayout(context);
        View view = new View(context);

        container.setId(Id);
        view.setBackgroundColor(Color.argb(0, 0, 0, 0));

        platform.addView(engine, matchParent);
        platform.addView(container, matchParent);
        platform.addView(view, matchParent);

        mActivity.setContentView(platform, matchParent);
        manager = activity.getSupportFragmentManager();
        
        //I leave it for compatibility with EdrowsLuo fragments until the entire UI gets replaced.
        ActivityOverlay.initial(activity, container.getId());
    }

    //--------------------------------------------------------------------------------------------//


    public void addFragment(BaseLayout layout, String tag){
        if (manager == null || layout.isAdded() || layouts.contains(layout) 
                || manager.findFragmentByTag(tag) != null)
            return;

        layouts.add(layout);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().add(container.getId(), layout, tag).commit());
    }

    public void removeFragment(BaseLayout fragment){
        if (manager == null || !fragment.isAdded() || !layouts.contains(fragment))
            return;

        layouts.remove(fragment);
        mActivity.runOnUiThread(() ->
                manager.beginTransaction().remove(fragment).commit());
    }

    public void closeAll() {
        for (BaseLayout layout: layouts) {
            if (layout.isShowing)
                mActivity.runOnUiThread(layout::close);
        }
    }

    public void closeThis(BaseLayout... toClose) {
        for (BaseLayout layout: toClose) {
            if (layouts.contains(layout) && layout.isShowing)
                mActivity.runOnUiThread(layout::close);
        }
    }

    public void closeAllExcept(BaseLayout... toExclude) {
        List<BaseLayout> toClose = new ArrayList<>(layouts);
        toClose.removeAll(Arrays.asList(toExclude));

        for (BaseLayout layout: toClose) {
            if (layout.isShowing)
                mActivity.runOnUiThread(layout::close);
        }
    }

}
