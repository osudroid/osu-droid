package com.reco1l.view;
// Created by Reco1l on 08/12/2022, 16:43

import android.content.Context;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewUtils;

import ru.nsu.ccfit.zuev.osuplus.R;

public class TextButton extends CardView implements BaseView {

    private TextView text;

    private String
            _text;

    private int
            _color = 0xFF242424;

    private boolean
            _syncToBeat = true;

    //--------------------------------------------------------------------------------------------//

    public TextButton(@NonNull Context context) {
        super(context);
        create(context, null);
    }

    public TextButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        create(context, attrs);
    }

    public TextButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        create(context, attrs);
    }

    //--------------------------------------------------------------------------------------------//

    private void create(Context context, AttributeSet attrs) {
        if (attrs != null) {
            _text = attrs.getAttributeValue(NS, "buttonText");
            _color = attrs.getAttributeIntValue(NS, "buttonColor", _color);
            _syncToBeat = attrs.getAttributeBooleanValue(NS, "beatSync", true);
        }
        setCardBackgroundColor(_color);

        StripsEffectView effect = new StripsEffectView(context);
        effect.setAlpha(0.5f);
        effect.setBeatSyncing(_syncToBeat);
        addView(effect, ViewUtils.match_parent);

        text = new TextView(new ContextThemeWrapper(context, R.style.text));
        text.setText(_text);
        text.setGravity(Gravity.CENTER);
        addView(text, ViewUtils.match_parent);

        if (!isInEditMode()) {
            setCardElevation(Resources.sdp(4));
            setRadius(Resources.sdp(12));

            effect.setStripWidth(Resources.sdp(12));
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
    }

    //--------------------------------------------------------------------------------------------//

    public void setButtonColor(int color) {
        setCardBackgroundColor(color);
    }

    public void setButtonText(String text) {
        this.text.setText(text);
    }

    public void setBeatSyncing(boolean bool) {
        _syncToBeat = bool;
    }
}
