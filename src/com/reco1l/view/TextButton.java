package com.reco1l.view;
// Created by Reco1l on 08/12/2022, 16:43

import static android.widget.RelativeLayout.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.reco1l.utils.ViewUtils;

import ru.nsu.ccfit.zuev.osuplus.R;

public class TextButton extends CardView implements BaseView {

    private StripsEffectView effect;
    private TextView text;

    //--------------------------------------------------------------------------------------------//

    public TextButton(Context context) {
        this(context, null);
    }

    public TextButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    public TextButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        onCreate(attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public View getView() {
        return this;
    }

    @Override
    public int[] getStyleable() {
        return R.styleable.TextButton;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    @SuppressLint("ResourceType")
    public void onCreate(AttributeSet attrs) {
        RelativeLayout layout = new RelativeLayout(getContext());
        addView(layout);

        effect = new StripsEffectView(getContext());
        effect.setStripWidth(sdp(12));
        effect.setAlpha(0.5f);
        layout.addView(effect);

        int h = sdp(20);
        int v = sdp(8);

        text = new TextView(new ContextThemeWrapper(getContext(), R.style.text));
        text.setId(0x11);
        text.setPadding(h, v, h, v);
        text.setGravity(Gravity.CENTER);
        layout.addView(text);

        ViewUtils.rule(effect)
                .add(ALIGN_TOP, ALIGN_BOTTOM, ALIGN_END, ALIGN_START)
                .apply(text.getId());

        handleAttributes(attrs);
    }

    @Override
    public void onManageAttributes(TypedArray a) {
        String string = a.getString(R.styleable.TextButton_buttonText);
        text.setText(string);

        boolean sync = a.getBoolean(R.styleable.TextButton_beatSync, true);
        effect.setBeatSyncing(sync);

        int color = a.getColor(R.styleable.TextButton_buttonColor, 0xFF2E2E2E);
        setCardBackgroundColor(color);

        float radius = a.getDimension(R.styleable.TextButton_buttonRadius, sdp(12));
        setRadius(radius);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onResize(int w, int h) {
        ViewGroup.LayoutParams params = text.getLayoutParams();
        params.width = w;
        params.height = h;
        text.setLayoutParams(params);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        onResize(getWidth(), getHeight());
        super.onDraw(canvas);
    }

    //--------------------------------------------------------------------------------------------//

    public void setButtonText(String text) {
        this.text.setText(text);
    }

    public StripsEffectView getStripsEffect() {
        return effect;
    }
}
