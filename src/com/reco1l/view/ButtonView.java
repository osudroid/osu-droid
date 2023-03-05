package com.reco1l.view;
// Created by Reco1l on 08/12/2022, 16:43

import static android.util.TypedValue.COMPLEX_UNIT_PX;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

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
        mText.setTextColor(0xFF151A23);

        Views.padding(mText)
                .vertical(sdp(6))
                .horizontal(sdp(12));

        if (!isInEditMode()) {
            mText.setTypeface(ResourcesCompat.getFont(getContext(), R.font.varela_regular));
        }
        addView(mText);
        Views.rule(mText, CENTER_IN_PARENT);

        setBackground(new ColorDrawable(0xFF819DD4));
        setRadius(sdp(8));
    }

    @Override
    public void onPostLayout(ViewGroup.LayoutParams params) {
        super.onPostLayout(params);
        Views.size(mEffect, getWidth(), getHeight());
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
