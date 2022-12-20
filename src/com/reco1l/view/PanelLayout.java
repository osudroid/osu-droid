package com.reco1l.view;
// Created by Reco1l on 20/12/2022, 03:08

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.RelativeLayout.ALIGN_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_BOTTOM;
import static android.widget.RelativeLayout.ALIGN_PARENT_TOP;
import static android.widget.RelativeLayout.ALIGN_TOP;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;

import com.reco1l.utils.Res;
import com.reco1l.utils.ViewUtils;

import ru.nsu.ccfit.zuev.osuplus.R;

public class PanelLayout extends CardView {

    private TextView title;
    private LinearLayout layout;
    private StripsEffectView effect;

    private int titleHeight = 40 * 3;

    private float radius = 12 * 3;

    //--------------------------------------------------------------------------------------------//

    public PanelLayout(@NonNull Context context) {
        super(context);
        init(context, null);
    }

    public PanelLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PanelLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    //--------------------------------------------------------------------------------------------//

    private void handleAttributes(Context context, AttributeSet attrs) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.PanelLayout);

        String string = a.getString(R.styleable.PanelLayout_panelTitle);
        title.setText(string);

        boolean sync = a.getBoolean(R.styleable.PanelLayout_beatSync, true);
        effect.setBeatSyncing(sync);

        titleHeight = (int) a.getDimension(R.styleable.PanelLayout_panelTitleHeight, titleHeight);
        title.getLayoutParams().height = titleHeight;

        int color = a.getInt(R.styleable.PanelLayout_panelColor, 0xFC262626);
        setCardBackgroundColor(color);

        radius = a.getDimension(R.styleable.PanelLayout_panelRadius, radius);
        setRadius(radius);

        a.recycle();
    }

    @SuppressLint("ResourceType")
    private void init(Context context, AttributeSet attrs) {
        layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        addView(layout);

        RelativeLayout body = new RelativeLayout(context);
        body.setBackground(new ColorDrawable(0x19000000));
        layout.addView(body);

        title = new TextView(new ContextThemeWrapper(context, R.style.text));
        title.setId(0x11);
        title.setGravity(Gravity.CENTER);
        body.addView(title);
        title.getLayoutParams().width = MATCH_PARENT;

        effect = new StripsEffectView(context);
        effect.setAlpha(0.5f);
        body.addView(effect);

        ViewUtils.rule(effect)
                .add(ALIGN_TOP, ALIGN_BOTTOM)
                .apply(title.getId());

        View separator = new View(context);
        separator.setBackground(new ColorDrawable(0x33000000));
        layout.addView(separator);
        separator.getLayoutParams().width = MATCH_PARENT;

        if (!isInEditMode()) {
            effect.setStripWidth(Res.sdp(12));

            titleHeight = Res.sdp(40);
            radius = Res.sdp(12);
        }
        handleAttributes(context, attrs);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (child != layout && indexOfChild(child) > 0) {
            ViewUtils.margins(child).top(titleHeight);
        }
    }

    public void setTitle(String text) {
        title.setText(text);
    }
}
