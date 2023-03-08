package com.reco1l.tools;

// Created by Reco1l on 6/9/22 13:31

import static android.util.TypedValue.COMPLEX_UNIT_PX;
import static android.view.ViewGroup.*;
import static android.view.ViewGroup.LayoutParams.*;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

import com.reco1l.management.resources.ResourceTable;

import java.util.ArrayList;

public final class Views {

    public static LayoutParams match_parent = match_parent();
    public static LayoutParams wrap_content = wrap_content();

    //--------------------------------------------------------------------------------------------//

    private static LayoutParams match_parent() {
        return new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    }

    private static LayoutParams wrap_content() {
        return new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    }

    public static LayoutParams params(int w, int h) {
        return new LayoutParams(w, h);
    }

    //--------------------------------------------------------------------------------------------//

    public static void visibility(boolean bool, View... views) {
        for (View view : views) {
            view.setVisibility(bool ? View.VISIBLE : View.GONE);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static void scale(View view, float scale) {
        if (view == null) {
            return;
        }
        view.setScaleX(scale);
        view.setScaleY(scale);
    }


    public static void size(View view, int size) {
        size(view, size, size);
    }

    public static void size(View view, int width, int height) {
        width(view, width);
        height(view, height);
    }

    public static void width(View view, int width) {
        view.getLayoutParams().width = width;
        view.requestLayout();
    }

    public static void height(View view, int height) {
        view.getLayoutParams().height = height;
        view.requestLayout();
    }

    //--------------------------------------------------------------------------------------------//

    public static MarginUtils margins(View view) {
        return new MarginUtils(view);
    }

    public static class MarginUtils {

        private final View view;
        private final MarginLayoutParams params;

        //----------------------------------------------------------------------------------------//

        public MarginUtils(View view) {
            this.view = view;
            params = (MarginLayoutParams) view.getLayoutParams();
        }

        //----------------------------------------------------------------------------------------//

        public void all(int size) {
            vertical(size, size);
            horizontal(size, size);
        }

        public MarginUtils vertical(int top, int bottom) {
            top(top);
            bottom(bottom);
            return this;
        }

        public MarginUtils horizontal(int left, int right) {
            left(left);
            right(right);
            return this;
        }

        public MarginUtils top(int size) {
            params.topMargin = size;
            view.requestLayout();
            return this;
        }

        public MarginUtils bottom(int size) {
            params.bottomMargin = size;
            view.requestLayout();
            return this;
        }

        public MarginUtils left(int size) {
            params.leftMargin = size;
            view.requestLayout();
            return this;
        }

        public MarginUtils right(int size) {
            params.rightMargin = size;
            view.requestLayout();
            return this;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static void rule(View view, int rule) {
        ((RelativeLayout.LayoutParams) view.getLayoutParams()).addRule(rule);
        view.requestLayout();
    }

    public static void rule(View view, int rule, int subject) {
        ((RelativeLayout.LayoutParams) view.getLayoutParams()).addRule(rule, subject);
        view.requestLayout();
    }

    public static RuleUtils rule(View view) {
        return new RuleUtils(view);
    }

    public static class RuleUtils {

        private final View view;
        private final RelativeLayout.LayoutParams params;

        private final ArrayList<Integer> rules;

        //----------------------------------------------------------------------------------------//

        private RuleUtils(View view) {
            this.view = view;
            this.rules = new ArrayList<>();

            if (!(view.getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);

                params.width = view.getLayoutParams().width;
                params.height = view.getLayoutParams().height;
                view.setLayoutParams(params);
            }
            params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        }

        //----------------------------------------------------------------------------------------//

        public RuleUtils add(int rule, int subject) {
            params.addRule(rule, subject);
            return this;
        }

        public RuleUtils add(int rule) {
            rules.add(rule);
            return this;
        }

        public RuleUtils add(int... rules) {
            for (int rule : rules) {
                this.rules.add(rule);
            }
            return this;
        }

        //----------------------------------------------------------------------------------------//

        public void apply() {
            apply(null);
        }

        public void apply(Integer subject) {

            if (rules != null) {
                for (int verb : rules) {
                    if (subject != null) {
                        params.addRule(verb, subject);
                        continue;
                    }
                    params.addRule(verb);
                }
            }
            view.setLayoutParams(params);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static PaddingUtils padding(View view) {
        return new PaddingUtils(view);
    }

    public static class PaddingUtils {

        private final View view;

        private int left, right, top, bottom;

        //----------------------------------------------------------------------------------------//

        public PaddingUtils(View view) {
            this.view = view;
            handlePadding();
        }

        private void handlePadding() {
            top = view.getPaddingTop();
            left = view.getPaddingLeft();
            right = view.getPaddingRight();
            bottom = view.getPaddingBottom();
        }

        //----------------------------------------------------------------------------------------//

        public void all(int size) {
            vertical(size, size);
            horizontal(size, size);
        }

        public PaddingUtils vertical(int size) {
            vertical(size, size);
            return this;
        }

        public PaddingUtils horizontal(int size) {
            return horizontal(size, size);
        }

        public PaddingUtils vertical(int top, int bottom) {
            top(top);
            bottom(bottom);
            return this;
        }

        public PaddingUtils horizontal(int left, int right) {
            left(left);
            right(right);
            return this;
        }

        public PaddingUtils top(int size) {
            handlePadding();
            view.setPadding(left, size, right, bottom);
            return this;
        }

        public PaddingUtils bottom(int size) {
            handlePadding();
            view.setPadding(left, top, right, size);
            return this;
        }

        public PaddingUtils left(int size) {
            handlePadding();
            view.setPadding(size, top, right, bottom);
            return this;
        }

        public PaddingUtils right(int size) {
            handlePadding();
            view.setPadding(left, top, size, bottom);
            return this;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static TextView styledText(ResourceTable table, AttributeSet attrs) {
        TextView text = new TextView(table.context(), attrs);

        text.setTextColor(0xFF819DD4);
        text.setTextSize(COMPLEX_UNIT_PX, table.sdp(10));
        text.setTypeface(table.font("varela_regular"));
        return text;
    }

    public View inflate(Context context, @LayoutRes int layout) {
        if (context == null) {
            return null;
        }
        LayoutInflater inflater = context.getSystemService(LayoutInflater.class);
        return inflater.inflate(layout, null);
    }
}
