package com.reco1l.view;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

// Created by Reco1l on 24/8/22 12:06

/**
 * This custom RecyclerView is based in the one made by sparrow007 on Github, the design was modified
 * to work on a vertical layout with a different type of scroll animation to fit game design.
 * <p>
 * See <a href="https://www.github.com/sparrow007/CarouselRecyclerView">CarouselRecyclerView by Sparrow007</a>
 */
public class AnimatedRecyclerView extends RecyclerView {

    public final AnimatedLayoutManager layoutManager;
    private Parcelable layoutManagerState;

    private static final String SUPER_STATE = "super-state";
    private static final String LAYOUT_STATE = "layout-state";

    //--------------------------------------------------------------------------------------------//

    public AnimatedRecyclerView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.layoutManager = new AnimatedLayoutManager();
        super.setLayoutManager(this.layoutManager);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected Parcelable onSaveInstanceState() {
        final Bundle bundle = new Bundle();

        bundle.putParcelable(SUPER_STATE, super.onSaveInstanceState());
        bundle.putParcelable(LAYOUT_STATE, layoutManager.onSaveInstanceState());
        return bundle;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof Bundle)) {
            super.onRestoreInstanceState(state);
            return;
        }
        final Bundle bundle = (Bundle) state;

        layoutManagerState = bundle.getParcelable(LAYOUT_STATE);
        super.onRestoreInstanceState(bundle.getParcelable(SUPER_STATE));
    }

    //--------------------------------------------------------------------------------------------//

    private void restorePosition() {
        if (layoutManagerState == null)
            return;
        layoutManager.onRestoreInstanceState(layoutManagerState);
        layoutManagerState = null;
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        final int center = layoutManager.getCenterPosition();

        int position = -1;
        int order = i;

        final View child = layoutManager.getChildAt(i);

        if (child != null) {
            position = layoutManager.getPosition(child);
        }
        if (position != -1) {
            final int distance = position - center;
            order = distance < 0 ? i : childCount - 1 - distance;
        }

        if (order < 0) {
            order = 0;
        }
        if (order >= childCount) {
            order = childCount - 1;
        }
        return order;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void scrollToPosition(int position) {
        layoutManager.scrollToPosition(position);
    }

    @Override
    public void smoothScrollToPosition(int position) {
        layoutManager.smoothScrollToPosition(this, layoutManager.state, position);
    }


    //--------------------------------------------------------------------------------------------//

    @Override
    public void setAdapter(@Nullable Adapter adapter) {
        super.setAdapter(adapter);
        restorePosition();
    }

    public void setOnItemSelectListener(AnimatedLayoutManager.OnItemSelected listener) {
        layoutManager.selectListener = listener;
    }

    public void setScrolling(boolean bool) {
        layoutManager.isScrollingEnabled = bool;
    }

    //--------------------------------------------------------------------------------------------//

    public boolean isScrollingEnabled() {
        return layoutManager.isScrollingEnabled;
    }
}
