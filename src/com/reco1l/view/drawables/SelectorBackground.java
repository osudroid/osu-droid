package com.reco1l.view.drawables;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.tables.ResourceTable;

import ru.nsu.ccfit.zuev.osuplus.R;

public class SelectorBackground extends Drawable implements ResourceTable {

    @Override
    public void setAlpha(int alpha) {

    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {

    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSPARENT;
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Paint fillPaint = new Paint();
        fillPaint.setColor(Color.BLACK);
        fillPaint.setAlpha(45);

        canvas.drawRect(getBounds(), fillPaint);

        RectF cropRect = new RectF(getBounds());
        Paint cropPaint = new Paint();
        cropPaint.setColor(Color.TRANSPARENT);
        cropPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));

        float radius = canvas.getWidth() / 2f;

        canvas.translate(dimen(R.dimen.beatmapPanelContentWidth) + sdp(16), 0);
        canvas.scale(1, 2, getBounds().centerX(), getBounds().centerY());
        canvas.drawRoundRect(cropRect, radius, radius, cropPaint);
    }
}
