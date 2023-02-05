package com.reco1l.view;

import static android.util.TypedValue.*;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.reco1l.utils.Views;
import com.reco1l.utils.Views.MarginUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import ru.nsu.ccfit.zuev.osuplus.R;

// Written by Reco1l

// As the name says, especial type of layout with a TextView inside, this is NOT a TextView subclass
// but you can pass TextView attributes through.
public class BadgeTextView extends RoundLayout {

    public final static int S = 0;
    public final static int M = 1;
    public final static int L = 2;
    public final static int XL = 3;

    private LinearLayout mLayout;
    private ImageView mIconView;
    private TextView mTextView;

    //--------------------------------------------------------------------------------------------//

    public BadgeTextView(@NonNull Context context) {
        super(context);
    }

    public BadgeTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public BadgeTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @IntDef(value = {S, M, L, XL})
    @Retention(RetentionPolicy.SOURCE)
    public @interface BadgeSize {}

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutType() {
        return LINEAR;
    }

    @Override
    protected void onCreate() {

        // Parent
        mLayout = (LinearLayout) getInternalLayout();
        mLayout.setGravity(Gravity.CENTER);

        // Icon
        mIconView = new ImageView(getContext());
        addView(mIconView);

        // Text
        mTextView = new TextView(getContext(), attrs);
        mTextView.setAllCaps(true);
        if (!isInEditMode()) {
            mTextView.setTypeface(ResourcesCompat.getFont(getContext(), R.font.varela_regular), Typeface.BOLD);
        }
        addView(mTextView);

        // Defaults
        setSize(M);
        setBackground(new ColorDrawable(0x66000000));
        handleIconVisibility();
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mIconView.setImageResource(a.getAttributeResourceValue(appNS, "icon", 0));
        setSize(a.getAttributeIntValue(appNS, "size", M));

        handleIconVisibility();
    }

    //--------------------------------------------------------------------------------------------//

    private void handleIconVisibility() {
        MarginUtils margin = Views.margins(mIconView);

        if (mIconView.getDrawable() != null) {
            mIconView.setVisibility(VISIBLE);
            margin.right(sdp(4));
        } else {
            mIconView.setVisibility(GONE);
            margin.right(0);
        }
    }

    private void handleSize(int text, int radius, int vertical, int horizontal) {
        mTextView.setTextSize(COMPLEX_UNIT_PX, sdp(text));
        setRadius(sdp(radius));

        Views.padding(mLayout)
                .vertical(sdp(vertical))
                .horizontal(sdp(horizontal));
    }

    public void setSize(@BadgeSize int size) {
        switch (size) {
            case S:
                break;
            case M:
                handleSize(8, 5, 2, 6);
                break;
            case L:
                handleSize(9, 6, 3, 7);
                break;
            case XL:
                handleSize(18, 8, 3, 7);
                break;
        }
    }

    public void setText(CharSequence text) {
        mTextView.setText(text);
        invalidate();
    }

    public void setIcon(Drawable drawable) {
        mIconView.setImageDrawable(drawable);
        handleIconVisibility();
    }

    public void setTextColor(int color) {
        mTextView.setTextColor(color);
        mIconView.setColorFilter(color);
    }

    public TextView getTextView() {
        return mTextView;
    }

    public ImageView getIconView() {
        return mIconView;
    }
}
