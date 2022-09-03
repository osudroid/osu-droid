package com.reco1l.view;

import static androidx.recyclerview.widget.RecyclerView.LayoutParams.*;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.LayoutParams;
import androidx.recyclerview.widget.RecyclerView.Recycler;
import androidx.recyclerview.widget.RecyclerView.State;

import com.edlplan.ui.BaseAnimationListener;

// Created by Reco1l on 24/8/22 12:06

/**
 * This custom LayoutManager is based in the one made by sparrow007 on Github, the design was modified
 * to work on a vertical layout with a different type of scroll animation to fit game design.
 * <p>
 * See <a href="https://www.github.com/sparrow007/CarouselRecyclerView">CarouselRecyclerView by Sparrow007</a>
 */
public class AnimatedLayoutManager extends RecyclerView.LayoutManager {

    private static final int MAX_RECT_COUNT = 100;

    protected State state;

    private ValueAnimator animation;
    private Recycler recycler;

    private final SparseArray<Rect> itemFrames;
    private final SparseBooleanArray itemsAttached;

    private int itemDecoratedHeight = 0;
    private int itemDecoratedWidth = 0;

    private float startY = 0;
    private int totalOffset = 0;

    private int currentPosition;
    private int selectedPosition;
    private int lastSelectedPosition;

    private boolean isOrientationChanged = false;

    //--------------------------------------------------------------------------------------------//

    protected OnItemSelected selectListener;

    protected float translationRatio = 0.1f;
    protected float alphaRatio = 0.25f;

    protected boolean isTranslationEffectEnabled = true;
    protected boolean isAlphaEffectEnabled = true;
    protected boolean isScrollingEnabled = true;

    //--------------------------------------------------------------------------------------------//

    private enum Scroll {BOTTOM, TOP}

    protected interface OnItemSelected {
        void run(int pos);
    }

    //--------------------------------------------------------------------------------------------//

    public AnimatedLayoutManager() {
        itemFrames = new SparseArray<>();
        itemsAttached = new SparseBooleanArray();
    }

    @Override
    public LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onLayoutChildren(Recycler recycler, State state) {
        if (recycler == null || state == null)
            return;

        if (state.getItemCount() <= 0  || state.isPreLayout()) {
            totalOffset = 0;
            return;
        }

        itemFrames.clear();
        itemsAttached.clear();

        final View scrap = recycler.getViewForPosition(0);

        addView(scrap);
        measureChildWithMargins(scrap, 0, 0);

        itemDecoratedWidth = getDecoratedMeasuredWidth(scrap);
        itemDecoratedHeight = getDecoratedMeasuredHeight(scrap);
        startY = (getVerticalSpace() - itemDecoratedHeight) * 1.0f / 2;

        float offset = startY;

        for (int i = 0; i < getItemCount() && i < MAX_RECT_COUNT; i++) {
            Rect frame = itemFrames.get(i);
            if (frame == null) {
                frame = new Rect();
            }

            frame.set(0, (int) offset, itemDecoratedWidth, (int) offset + itemDecoratedHeight);

            itemFrames.put(i, frame);
            itemsAttached.put(i, false);
            offset += itemDecoratedHeight;
        }

        detachAndScrapAttachedViews(recycler);

        if (isOrientationChanged && selectedPosition != 0) {
            isOrientationChanged = false;
            totalOffset = itemDecoratedHeight * selectedPosition;
            onSelectedCallback();
        }

        layoutItems(recycler, state, Scroll.TOP);
        this.recycler = recycler;
        this.state = state;
    }

    private void layoutItems(Recycler recycler, State state, Scroll direction) {
        if (state.isPreLayout())
            return;

        final Rect display = new Rect(0, totalOffset, getHorizontalSpace(), totalOffset + getVerticalSpace());
        int position = 0;

        for (int i = 0; i < getChildCount(); i++) {

            final View child = getChildAt(i);
            if (child == null)
                break;

            position = getPosition(child);

            final Rect rect = getFrame(position);

            if (!Rect.intersects(display, rect)) {
                removeAndRecycleView(child, recycler);
                itemsAttached.delete(position);
            } else {
                layoutSingleItem(child, rect);
                itemsAttached.put(position, true);
            }
        }

        if (position == 0) {
            position = centerPosition();
        }

        int min = position - 20;
        int max = position + 20;

        if (min < 0) {
            min = 0;
        }
        if (max > getItemCount()) {
            max = getItemCount();
        }

        for (int i = min; i < max; i++) {
            final Rect rect = getFrame(i);

            if (Rect.intersects(display, rect) && !itemsAttached.get(i)) {
                int actualPos = i % getItemCount();

                final View view = recycler.getViewForPosition(actualPos);
                measureChildWithMargins(view, 0, 0);

                if (direction == Scroll.BOTTOM) {
                    addView(view, 0);
                } else {
                    addView(view);
                }

                layoutSingleItem(view, rect);
                itemsAttached.put(i, true);
            }
        }
    }

    private void layoutSingleItem(View child, Rect rect) {
        if (child == null)
            return;

        layoutDecorated(child,
                rect.left,
                rect.top - totalOffset,
                rect.right,
                rect.bottom - totalOffset);

        if (isTranslationEffectEnabled) {
            child.setTranslationX(computeTranslationX(child.getWidth(), rect.top - totalOffset));
        }
        if (isAlphaEffectEnabled) {
            child.setAlpha(computeAlpha(rect.top - totalOffset));
        }

        if (child.getTranslationX() == 0) {
            currentPosition = getPosition(child);
        }
    }

    //--------------------------------------------------------------------------------------------//

    private Rect getFrame(int position) {
        Rect frame = itemFrames.get(position);
        if (frame == null) {
            final int offset = (int) (startY + itemDecoratedHeight * position);

            frame = new Rect();
            frame.set(0, offset, itemDecoratedWidth, offset + itemDecoratedHeight);
            return frame;
        }
        return frame;
    }

    //--------------------------------------------------------------------------------------------//

    private void fixOffset() {
        if (itemDecoratedHeight != 0) {

            int scrollPos = (int) (totalOffset * 1.0f / itemDecoratedHeight);
            final float dy = totalOffset % itemDecoratedHeight;

            if (Math.abs(dy) > itemDecoratedHeight * 0.5f) {
                if (dy > 0) {
                    scrollPos++;
                } else {
                    scrollPos--;
                }
            }

            final int finalOffset = scrollPos * itemDecoratedHeight;
            scroll(totalOffset, finalOffset, 200);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onScrollStateChanged(int state) {
        super.onScrollStateChanged(state);
        if (state == RecyclerView.SCROLL_STATE_IDLE) {
            fixOffset();
        }
    }

    @Override
    public int scrollVerticallyBy(int dy, Recycler recycler, State state) {
        if (animation != null && animation.isRunning()) {
            animation.cancel();
        }

        if (recycler == null || state == null)
            return 0;

        int travel = dy;
        int maxOffset = (getItemCount() - 1) * itemDecoratedHeight;

        if (dy + totalOffset < 0) {
            travel = -totalOffset;
        } else if (dy + totalOffset > maxOffset) {
            travel = (maxOffset - totalOffset);
        }

        totalOffset += travel;
        layoutItems(recycler, state, dy > 0 ? Scroll.TOP : Scroll.BOTTOM);
        return travel;
    }

    private void scroll(int from, int to) {
        this.scroll(from, to, 500);
    }

    private void scroll(int from, int to, long duration) {
        if (animation != null && animation.isRunning()) {
            animation.cancel();
        }

        final Scroll direction = from < to ? Scroll.BOTTOM : Scroll.TOP;

        animation = ValueAnimator.ofFloat(from * 1.0f, to * 1.0f);
        animation.setDuration(duration);
        animation.setInterpolator(new DecelerateInterpolator());

        animation.addUpdateListener(val -> {
            totalOffset = ((Float) val.getAnimatedValue()).intValue();
            layoutItems(recycler, state, direction);
        });

        animation.addListener(new BaseAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                onSelectedCallback();
            }
        });
        animation.start();
    }

    @Override
    public void smoothScrollToPosition(RecyclerView recyclerView, State state, int position) {
        if (recycler == null || state == null)
            return;

        final int oy = itemDecoratedHeight * currentPosition;
        final int fy = itemDecoratedHeight * position;

        if (fy == oy)
            return;

        scroll(oy, fy);
    }

    @Override
    public void scrollToPosition(int position) {
        if (position < 0 || position > getItemCount() - 1)
            return;

        if (recycler == null || state == null) {
            isOrientationChanged = true;
            selectedPosition = position;
            requestLayout();
            return;
        }
        totalOffset = itemDecoratedHeight * position;
        layoutItems(recycler, state, position > selectedPosition ? Scroll.TOP : Scroll.BOTTOM);
        onSelectedCallback();
    }

    protected int centerPosition() {
        final int intervalPos = itemDecoratedHeight;

        if (intervalPos == 0) {
            return intervalPos;
        }

        int pos = totalOffset / intervalPos;
        final int mPos = totalOffset % intervalPos;

        if (Math.abs(mPos) >= intervalPos * 0.5f) {
            if (mPos >= 0) {
                pos++;
            } else {
                pos--;
            }
        }
        return pos;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onAdapterChanged(@Nullable Adapter oldAdapter, @Nullable Adapter newAdapter) {
        removeAllViews();
        totalOffset = 0;
        itemsAttached.clear();
        itemFrames.clear();
    }

    private void onSelectedCallback() {
        final int intervalDistance = itemDecoratedHeight;
        if (intervalDistance == 0)
            return;

        selectedPosition = (int) ((float) totalOffset / intervalDistance);
        if (selectedPosition < 0) {
            selectedPosition += getItemCount();
        }

        selectedPosition = Math.abs(selectedPosition % getItemCount());

        if (selectListener != null && selectedPosition != lastSelectedPosition) {
            selectListener.run(selectedPosition);
        }
        lastSelectedPosition = selectedPosition;
    }

    //--------------------------------------------------------------------------------------------//

    private float computeTranslationX(int width, int y) {
        float m = 1 - Math.abs(y - startY) / Math.abs(startY + itemDecoratedHeight / translationRatio);
        float value = width - width * (m < 0 ? 0f : m > 1 ? 1f : m);

        if (value < 0) {
            value = 0;
        }
        if (value > width) {
            value = width;
        }

        return value;
    }

    private float computeAlpha(int y) {
        float alpha = 1 - Math.abs(y - startY) / Math.abs(startY + itemDecoratedHeight / alphaRatio);
        return alpha < 0.1 ? 0f : alpha > 1 ? 1f : alpha;
    }

    //--------------------------------------------------------------------------------------------//

    private int getHorizontalSpace() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    private int getVerticalSpace() {
        return getHeight() - getPaddingBottom() - getPaddingTop();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean canScrollVertically() {
        return isScrollingEnabled;
    }
}
