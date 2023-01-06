package com.edlplan.ui;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.view.View;

public class InvalidateView extends View {

    private Runnable runOnUpdate;

    public InvalidateView(Context context) {
        super(context);
    }

    public InvalidateView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public InvalidateView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        invalidate();

        if (runOnUpdate != null) {
            runOnUpdate.run();
        }
    }

   public void runOnUpdate(Runnable task) {
        this.runOnUpdate = task;
   }
}
