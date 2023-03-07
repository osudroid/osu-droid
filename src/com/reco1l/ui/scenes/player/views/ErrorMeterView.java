package com.reco1l.ui.scenes.player.views;

// Written by Reco1l

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.management.game.GameWrapper;
import com.reco1l.framework.Animation;
import com.reco1l.view.RoundLayout;
import com.reco1l.framework.drawing.CompoundRect;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import main.osu.helper.DifficultyHelper;

public class ErrorMeterView extends RoundLayout implements IPassiveObject {

    private CompoundRect
            m50Compound,
            m100Compound,
            m300Compound,
            mCenter;

    private LinkedList<Line> mCurrentLines;
    private Queue<Line> mPendingLines;

    private float
            m50Window = -1,
            m100Window,
            m300Window;

    //--------------------------------------------------------------------------------------------//

    public ErrorMeterView(@NonNull Context context) {
        super(context);
    }

    public ErrorMeterView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ErrorMeterView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        mCurrentLines = new LinkedList<>();
        mPendingLines = new LinkedList<>();

        setConstantInvalidation(true);

        m50Compound = new CompoundRect();
        m50Compound.paint.setAlpha(255);
        m50Compound.paint.setColor(0xFFFDE964);

        m100Compound = new CompoundRect(m50Compound);
        m100Compound.paint.setColor(0xFF64FD6C);

        m300Compound = new CompoundRect(m50Compound);
        m300Compound.paint.setColor(0xFF64B6FD);

        mCenter = new CompoundRect();
        mCenter.paint.setColor(Color.WHITE);

        if (isInEditMode()) {
            m50Window = 0.27f;
            m100Window = 0.17f;
            m300Window = 0.085f;
        }
    }

    private void adjustWindows(int w) {
        float scale100 = m100Window / m50Window;
        float scale300 = m300Window / m50Window;

        float width100 = w * scale100;
        float width300 = w * scale300;

        float span100 = (w - width100) / 2f;
        float span300 = (w - width300) / 2f;

        m100Compound.rect.left = (int) span100;
        m100Compound.rect.right = (int) (w - span100);

        m300Compound.rect.left = (int) span300;
        m300Compound.rect.right = (int) (w - span300);
    }

    @Override
    protected void onPostLayout(ViewGroup.LayoutParams params) {
        super.onPostLayout(params);

        int w = getWidth();
        int h = getHeight();

        m50Compound.rect.right = w;

        int centerWidth = sdp(3);
        mCenter.rect.bottom = h;
        mCenter.rect.left = w / 2 - centerWidth / 2;
        mCenter.rect.right = w / 2 + centerWidth / 2;

        if (m50Window > 0) {
            adjustWindows(w);
        }

        float height = h * 0.60f;
        float span = (h - height) / 2f;

        m50Compound.rect.top = (int) span;
        m50Compound.rect.bottom = (int) (h - span);

        m100Compound.rect.top = (int) span;
        m100Compound.rect.bottom = (int) (h - span);

        m300Compound.rect.top = (int) span;
        m300Compound.rect.bottom = (int) (h - span);
    }

    @Override
    protected void onManagedDraw(Canvas canvas) {
        super.onManagedDraw(canvas);

        float r = getHeight() * 0.75f / 2f;

        m50Compound.drawTo(canvas, r, r);
        m100Compound.drawTo(canvas);
        m300Compound.drawTo(canvas);

        float cr = mCenter.rect.width() / 2f;

        mCenter.drawTo(canvas, cr, cr);

        while (!mPendingLines.isEmpty()) {
            Line line = mPendingLines.poll();
            if (line != null) {
                mCurrentLines.add(line);
            }
        }

        Iterator<Line> iterator = mCurrentLines.iterator();
        while (iterator.hasNext()) {
            Line line = iterator.next();

            line.mCompound.drawTo(canvas);
            if (line.mCompound.paint.getAlpha() == 0) {
                iterator.remove();
            }
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void clear() {
        m50Window = -1;
    }

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        if (wrapper == null) {
            clear();
            return;
        }

        float od = wrapper.overallDifficulty;
        DifficultyHelper helper = wrapper.difficultyHelper;

        m50Window = helper.hitWindowFor50(od);
        m100Window = helper.hitWindowFor100(od);
        m300Window = helper.hitWindowFor300(od);

        post(() -> adjustWindows(getWidth()));
    }

    public void putErrorAt(float result) {
        if (Math.abs(result) > m50Window) {
            return;
        }

        float rate = result / m50Window;
        float span = (getWidth() / 2f) * rate;

        int x = (int) (getWidth() / 2 - span);

        Line line = new Line(x);
        mPendingLines.add(line);

        Animation.ofInt(255, 0)
                .runOnUpdate(v -> line.mCompound.paint.setAlpha((int) v))
                .play(2000);
    }

    //--------------------------------------------------------------------------------------------//

    private class Line {

        private final CompoundRect mCompound;

        //----------------------------------------------------------------------------------------//

        private Line(int x) {
            Rect rect = new Rect();
            rect.left = x - sdp(1);
            rect.right = x + sdp(1);
            rect.bottom = getWidth();

            mCompound = new CompoundRect(rect);
            mCompound.paint.setColor(Color.WHITE);
        }
    }
}
