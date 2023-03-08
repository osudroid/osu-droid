package com.reco1l.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.reco1l.framework.Animation;
import com.reco1l.framework.drawing.Dimension;

public final class FadeImageView extends RoundLayout {

    @Size(value = 2)
    private ImageView[] mLayers;

    private Animation mAnimation;

    private int mCursor = 0;

    private long mAnimationDuration = 300;

    //--------------------------------------------------------------------------------------------//

    public FadeImageView(@NonNull Context context) {
        super(context);
    }

    public FadeImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FadeImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        mLayers = new ImageView[] {
                new ImageView(getContext(), attrs),
                new ImageView(getContext(), attrs),
        };

        addView(mLayers[0], getInitialLayoutParams());
        addView(mLayers[1], getInitialLayoutParams());

        getFrontImage().setElevation(1);
        getBackImage().setImageDrawable(null);
        setRadius(0);
    }

    @Override
    protected void onSizeChange(Dimension dimens) {
        super.onSizeChange(dimens);
        matchSize(mLayers[0]);
        matchSize(mLayers[1]);
    }

    //--------------------------------------------------------------------------------------------//

    private ImageView getFrontImage() {
        return mLayers[mCursor];
    }

    private ImageView getBackImage() {
        return mLayers[mCursor == 0 ? 1 : 0];
    }

    //--------------------------------------------------------------------------------------------//

    private void handleChange(Drawable drawable) {
        if (mAnimation != null) {
            mAnimation.cancel();
        }

        final ImageView front = getFrontImage();
        final ImageView back = getBackImage();

        back.setImageDrawable(drawable);
        back.setAlpha(1f);

        Runnable callback = () -> {
            mCursor = mCursor == 0 ? 1 : 0;

            front.setImageDrawable(null);
            front.setElevation(0f);

            back.setElevation(1f);
        };

        mAnimation = Animation.of(front)
                .toAlpha(0)
                .runOnCancel(callback)
                .runOnEnd(callback);

        mAnimation.play(mAnimationDuration);
    }

    //--------------------------------------------------------------------------------------------//

    public void setImageBitmap(Bitmap bitmap) {
        handleChange(new BitmapDrawable(resources(), bitmap));
    }

    public void setImageDrawable(Drawable drawable) {
        handleChange(drawable);
    }

    public void setAnimationDuration(long duration) {
        mAnimationDuration = duration;
    }
}
