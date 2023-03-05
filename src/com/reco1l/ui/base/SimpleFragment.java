package com.reco1l.ui.base;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.reco1l.utils.TouchHandler;
import com.reco1l.utils.TouchListener;

// Created by Reco1l on 22/6/22 02:26

// Intended to be attached as child Fragment.
public class SimpleFragment extends Fragment {

    private View view;

    private @LayoutRes int layoutId;

    //--------------------------------------------------------------------------------------------//

    public SimpleFragment(int layoutId) {
        this.layoutId = layoutId;
    }

    public SimpleFragment(View view) {
        this.view = view;
    }

    //--------------------------------------------------------------------------------------------//

    protected void onLoad() {}

    //--------------------------------------------------------------------------------------------//

    protected final int getWidth() {
        return view.getWidth();
    }

    protected final int getHeight() {
        return view.getHeight();
    }

    //--------------------------------------------------------------------------------------------//

    @Nullable
    @Override
    public final View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle bundle) {
        if (layoutId != 0) {
            view = inflater.inflate(layoutId, container, false);
        }

        onLoad();
        return view;
    }

    //--------------------------------------------------------------------------------------------//

    @SuppressWarnings("unchecked")
    protected <T extends View> T find(@IdRes int id) {
        if (view == null) {
            return null;
        }

        Object view = this.view.findViewById(id);
        return (T) view;
    }

    //--------------------------------------------------------------------------------------------//

    public final void bindTouch(View view, Runnable onSingleTapUp) {
        bindTouch(view, new TouchListener() {
            public void onPressUp() {
                if (onSingleTapUp != null) {
                    onSingleTapUp.run();
                }
            }
        });
    }

    public final void bindTouch(View view, TouchListener listener) {
        new TouchHandler(listener).apply(view);
    }
}
