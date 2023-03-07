package com.reco1l.ui.scenes.player.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.framework.Views;
import com.reco1l.view.RoundLayout;

import java.util.Objects;

public class ScoreNumberView extends RoundLayout implements IPassiveObject {

    private LinearLayout mLinearLayout;
    private String mCurrentText;

    private float mNumberScale;

    //--------------------------------------------------------------------------------------------//

    public ScoreNumberView(@NonNull Context context) {
        super(context);
    }

    public ScoreNumberView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ScoreNumberView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutType() {
        return LINEAR;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        mLinearLayout = (LinearLayout) getInternalLayout();
        mLinearLayout.setOrientation(LinearLayout.HORIZONTAL);

        setRadius(0);
        setGravity(Gravity.RIGHT);
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mNumberScale = a.getAttributeFloatValue(appNS, "numberScale", 1f);
        setText(a.getAttributeValue(appNS, "text"));
    }

    //--------------------------------------------------------------------------------------------//

    public synchronized void setText(String text) {
        if (text == null) {
            text = "";
        }
        text = text.replaceAll("[^0-9,.x%]", "");

        if (Objects.equals(mCurrentText, text)) {
            return;
        }
        mCurrentText = text;

        handleSize(text.length());
        handleText(text);
    }

    private void handleText(String text) {
        char[] chars = text.toCharArray();

        int i = 0;
        while (i < chars.length) {
            char c = chars[i];

            String str;

            if (c == ',') {
                str = "comma";
            } else if (c == '.') {
                str = "dot";
            }  else if (c == '%') {
                str = "percent";
            } else {
                str = String.valueOf(c);
            }

            setCharAt(i, str);
            i++;
        }
    }

    private void setCharAt(int index, String c) {
        ImageView image = (ImageView) mLinearLayout.getChildAt(index);

        if (image != null) {
            Bitmap bitmap = Game.bitmapManager.get("score-" + c);

            handleScale(image, bitmap);
            image.setImageBitmap(bitmap);
        }
    }

    private void handleScale(ImageView image, Bitmap bitmap) {
        if (bitmap == null || mNumberScale == 0) {
            return;
        }
        int w = (int) (toEngineScale(bitmap.getWidth()) * mNumberScale);
        int h = (int) (toEngineScale(bitmap.getHeight()) * mNumberScale);

        Views.size(image, w, h);
        image.setScaleType(ImageView.ScaleType.FIT_XY);
    }

    private void handleSize(int length) {
        if (mLinearLayout.getChildCount() == length) {
            return;
        }

        int i = mLinearLayout.getChildCount();

        if (length > i) {
            while (i <= length) {
                ImageView image = new ImageView(getContext());
                mLinearLayout.addView(image, i);
                i++;
            }
        } else {
            while (i > length) {
                mLinearLayout.removeViewAt(i - 1);
                i--;
            }
        }
    }
}
