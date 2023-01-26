package com.reco1l.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@SuppressLint("AppCompatCustomView")
public class FixedSeekBar extends SeekBar {

    //--------------------------------------------------------------------------------------------//

    public FixedSeekBar(@NonNull Context context) {
        super(context);
    }

    public FixedSeekBar(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FixedSeekBar(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    private boolean isInThumbBounds(float x, float y) {
        Rect bounds = getThumb().getBounds();
        return bounds.contains((int) x, (int) y);
    }

    private int getThumbCenterX() {
        Rect bounds = getThumb().getBounds();
        return bounds.centerX();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();

        if (isInThumbBounds(x, y)) {
            return super.onTouchEvent(event);
        }

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            setPressed(true);
            setSelected(true);

            int centerX = getThumbCenterX();

            if (x > centerX) {
                setProgress(getProgress() + 1);
            } else if (x < centerX) {
                setProgress(getProgress() - 1);
            }
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_UP) {
            setPressed(false);
            setSelected(false);
            return true;
        }
        else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            return super.onTouchEvent(event);
        }
        return true;
    }
}
