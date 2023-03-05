package com.reco1l.ui.elements;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.utils.FrameCounter;
import com.reco1l.view.BadgeTextView;

import java.text.DecimalFormat;

public final class FPSBadgeView extends BadgeTextView {

    private final DecimalFormat mDF = new DecimalFormat("##");

    private FPSProvider mProvider;

    //--------------------------------------------------------------------------------------------//

    public FPSBadgeView(@NonNull Context context) {
        super(context);
    }

    public FPSBadgeView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FPSBadgeView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    public interface FPSProvider {
        float getFPS();
        float getFrameTime();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        super.onCreate();

        setText("99fps - 99ms");
        setConstantInvalidation(true);

        if (!isInEditMode()) {
            // Setting a default provider
            setProvider(new FPSProvider() {

                public float getFPS() {
                    return FrameCounter.getFPS();
                }

                public float getFrameTime() {
                    return FrameCounter.getFrameTime();
                }
            });
        }
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        // This view cannot be modified
    }

    @Override
    protected void onManagedDraw(Canvas canvas) {
        if (isInEditMode()) {
            return;
        }

        float fps = FrameCounter.getFPS();
        float ft = FrameCounter.getFrameTime();

        setTextColor(getFpsColorJudgment(fps));
        setText(mDF.format(fps) + "fps - " + mDF.format(ft) + "ms");
    }

    //--------------------------------------------------------------------------------------------//

    private int getFpsColorJudgment(float fps) {
        int color = 0xFF36AE7C;
        float hz = Game.activity.getRefreshRate();

        if (fps <= hz - 2) {
            color = 0xFFF9D923;
        }
        if (fps <= hz / 1.25) {
            color = 0xFFEB5353;
        }
        return color;
    }

    public void setProvider(FPSProvider provider) {
        mProvider = provider;
    }
}
