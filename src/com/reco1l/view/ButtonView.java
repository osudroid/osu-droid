package com.reco1l.view;
// Created by Reco1l on 08/12/2022, 16:43

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.util.TypedValue.COMPLEX_UNIT_SP;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.reco1l.interfaces.fields.Identifiers;
import com.reco1l.utils.Views;
import com.reco1l.view.effects.StripsEffect;

import ru.nsu.ccfit.zuev.osuplus.R;

public class ButtonView extends RoundLayout {

    private StripsEffect mEffect;
    private TextView mText;

    //--------------------------------------------------------------------------------------------//

    public ButtonView(Context context) {
        this(context, null);
    }

    public ButtonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ButtonView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onCreate() {
        mEffect = new StripsEffect(getContext());
        mEffect.setStripWidth(sdp(12));
        mEffect.setAlpha(0.5f);
        addView(mEffect, getInitialLayoutParams());

        mText = new TextView(getContext(), attrs);
        mText.setTextSize(COMPLEX_UNIT_PX, sdp(12));
        mText.setGravity(Gravity.CENTER);
        mText.setTextColor(0xFF151A23);

        if (!isInEditMode()) {
            mText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.varela_regular));
        }
        addView(mText, getInitialLayoutParams());

        setBackground(new ColorDrawable(0xFF819DD4));
        setRadius(18);
    }

    @Override
    public void onResize(ViewGroup.LayoutParams params) {
        super.onResize(params);
        matchSize(mEffect);
        matchSize(mText);
    }

    //--------------------------------------------------------------------------------------------//

    public void setButtonText(CharSequence charSequence) {
        mText.setText(charSequence);
    }

    public TextView getTextView() {
        return mText;
    }

    public StripsEffect getStripsEffect() {
        return mEffect;
    }
}
