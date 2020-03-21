package com.edlplan.ui;

import android.app.Activity;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;

import com.edlplan.ui.fragment.BaseFragment;

import java.util.ArrayList;
import java.util.List;

public class ActivityOverlay {

    private static FragmentManager fragmentManager;

    private static List<BaseFragment> displayingOverlay = new ArrayList<>();

    private static Activity context;

    private static int containerId;

    public static void initial(AppCompatActivity activity, int id) {
        context = activity;
        containerId = id;
        fragmentManager = activity.getSupportFragmentManager();
        if (fragmentManager == null) {
            throw new RuntimeException("FragmentManager not found!");
        }
    }

    public static synchronized boolean onBackPress() {
        if (fragmentManager != null && displayingOverlay.size() > 0) {
            displayingOverlay.get(displayingOverlay.size() - 1).dismiss();
            return true;
        }
        return false;
    }

    public static synchronized void dismissOverlay(BaseFragment fragment) {
        if (fragmentManager != null) {
            if (displayingOverlay.contains(fragment)) {
                displayingOverlay.remove(fragment);
                fragmentManager.beginTransaction().remove(fragment).commit();
            }
        }
    }

    public static synchronized void addOverlay(BaseFragment fragment, String tag) {
        if (fragmentManager != null) {
            if (displayingOverlay.contains(fragment) || fragmentManager.findFragmentByTag(tag) != null) {
                displayingOverlay.remove(fragment);
                fragmentManager.beginTransaction()
                        .remove(fragment)
                        .add(containerId, fragment, tag)
                        .commit();
                return;
            }
            displayingOverlay.add(fragment);
            fragmentManager.beginTransaction()
                    .add(containerId, fragment, tag)
                    .commit();
        }
    }

    public static void runOnUiThread(Runnable runnable) {
        context.runOnUiThread(runnable);
    }


}
