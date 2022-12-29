package com.reco1l.view;
// Created by Reco1l on 20/12/2022, 03:08

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.widget.RelativeLayout.ALIGN_BOTTOM;
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

import com.reco1l.utils.ViewUtils;
import com.reco1l.view.effects.StripsEffect;

import ru.nsu.ccfit.zuev.osuplus.R;

public class PanelLayout extends CardView implements BaseView {

    private TextView title;
    private LinearLayout layout;
    private StripsEffect effect;

    private int titleHeight;

    //--------------------------------------------------------------------------------------------//

    public PanelLayout(@NonNull Context context) {
        this(context, null);
    }

    public PanelLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        onCreate(attrs);
    }

    public PanelLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
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
        return R.styleable.PanelLayout;
    }


    //--------------------------------------------------------------------------------------------//

    @Override
    @SuppressLint("ResourceType")
    public void onCreate(AttributeSet attrs) {
        layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        addView(layout);

        RelativeLayout body = new RelativeLayout(getContext());
        body.setBackground(new ColorDrawable(0x19000000));
        layout.addView(body);

        title = new TextView(new ContextThemeWrapper(getContext(), R.style.text));
        title.setId(0x11);
        title.setGravity(Gravity.CENTER);
        body.addView(title);
        title.getLayoutParams().width = MATCH_PARENT;

        effect = new StripsEffect(getContext());
        effect.setStripWidth(sdp(12));
        effect.setAlpha(0.5f);
        body.addView(effect);

        ViewUtils.rule(effect)
                .add(ALIGN_TOP)
                .add(ALIGN_BOTTOM)
                .apply(title.getId());

        View separator = new View(new ContextThemeWrapper(getContext(), R.style.panelSeparatorView));
        layout.addView(separator);

        handleAttributes(attrs);
    }

    @Override
    public void onManageAttributes(TypedArray a) {

        String string = a.getString(R.styleable.PanelLayout_panelTitle);
        title.setText(string);

        boolean sync = a.getBoolean(R.styleable.PanelLayout_beatSync, true);
        effect.setBeatSyncing(sync);

        titleHeight = (int) a.getDimension(R.styleable.PanelLayout_panelTitleHeight, sdp(40));
        title.getLayoutParams().height = titleHeight;

        int color = a.getInt(R.styleable.PanelLayout_panelColor, 0xFC262626);
        setCardBackgroundColor(color);

        float radius = a.getDimension(R.styleable.PanelLayout_panelRadius, sdp(12));
        setRadius(radius);
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);

        if (child != layout) {
            ViewUtils.margins(child).top(titleHeight);
        }
    }

    public void setTitle(String text) {
        title.setText(text);
    }
}
