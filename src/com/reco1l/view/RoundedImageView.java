package com.reco1l.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.utils.Views;

public class RoundedImageView extends RoundLayout {

    private ImageView mImage;

    //--------------------------------------------------------------------------------------------//

    public RoundedImageView(@NonNull Context context) {
        super(context);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RoundedImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        mImage = new ImageView(getContext(), attrs, defStyleAttr, defStyleRes);
        addView(mImage, getInitialLayoutParams());

        post(() -> matchLayoutParams(mImage));
    }

    @Override
    protected void onManagedDraw(Canvas canvas) {
        super.onManagedDraw(canvas);
    }

    //--------------------------------------------------------------------------------------------//

    public void setImageDrawable(Drawable drw) {
        mImage.setImageDrawable(drw);
    }

    @Override
    public void setLayoutParams(ViewGroup.LayoutParams params) {
        super.setLayoutParams(params);
        matchLayoutParams(mImage);
    }
}
