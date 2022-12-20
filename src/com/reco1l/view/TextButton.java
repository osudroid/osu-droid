package com.reco1l.view;
// Created by Reco1l on 08/12/2022, 16:43

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.widget.RelativeLayout.*;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.reco1l.utils.Res;

import ru.nsu.ccfit.zuev.osuplus.R;

public class TextButton extends CardView implements BaseView {

    private StripsEffectView effect;
    private TextView text;

    private int
            paddingV = 8 * 3,
            paddingH = 20 * 3;

    private float radius = 12 * 3;

    //--------------------------------------------------------------------------------------------//

    public TextButton(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public TextButton(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public TextButton(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    //--------------------------------------------------------------------------------------------//

    private void handleAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TextButton);

        String string = a.getString(R.styleable.TextButton_buttonText);
        text.setText(string);

        boolean sync = a.getBoolean(R.styleable.TextButton_beatSync, true);
        effect.setBeatSyncing(sync);

        paddingV = (int) a.getDimension(R.styleable.TextButton_textVerticalPadding, paddingV);
        paddingH = (int) a.getDimension(R.styleable.TextButton_textHorizontalPadding, paddingH);

        text.setPadding(paddingH, paddingV, paddingH, paddingV);

        int color = a.getColor(R.styleable.TextButton_buttonColor, 0xFF2E2E2E);
        setCardBackgroundColor(color);

        radius = a.getDimension(R.styleable.TextButton_buttonRadius, radius);
        setRadius(radius);

        a.recycle();
    }

    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attrs) {
        RelativeLayout layout = new RelativeLayout(context);
        addView(layout);

        text = new TextView(new ContextThemeWrapper(context, R.style.text));
        text.setId(0x11);
        text.setElevation(1);
        text.setGravity(Gravity.CENTER);

        layout.addView(text);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.addRule(ALIGN_TOP, text.getId());
        params.addRule(ALIGN_BOTTOM, text.getId());
        params.addRule(ALIGN_END, text.getId());
        params.addRule(ALIGN_START, text.getId());

        effect = new StripsEffectView(context);
        effect.setAlpha(0.5f);

        layout.addView(effect, params);

        if (!isInEditMode()) {
            effect.setStripWidth(Res.sdp(12));

            paddingV = Res.sdp(8);
            paddingH = Res.sdp(20);
            radius = Res.sdp(12);
        }
        handleAttributes(context, attrs);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        resize(w, h);
    }

    private void resize(int w, int h) {
        ViewGroup.LayoutParams params = text.getLayoutParams();
        params.width = w;
        params.height = h;
        text.setLayoutParams(params);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        resize(getWidth(), getHeight());
        super.onDraw(canvas);
    }

    //--------------------------------------------------------------------------------------------//

    public void setButtonText(String text) {
        this.text.setText(text);
    }

    public void setBeatSyncing(boolean bool) {
        effect.setBeatSyncing(bool);
    }
}
