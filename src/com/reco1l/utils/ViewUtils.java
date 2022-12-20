package com.reco1l.utils;

// Created by Reco1l on 6/9/22 13:31

import static android.view.ViewGroup.*;
import static android.view.ViewGroup.LayoutParams.*;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.view.View;
import android.widget.RelativeLayout;

public class ViewUtils {

    public static LayoutParams match_parent = match_parent();
    public static LayoutParams wrap_content = wrap_content();

    //--------------------------------------------------------------------------------------------//

    public static LayoutParams match_parent() {
        return new LayoutParams(MATCH_PARENT, MATCH_PARENT);
    }

    public static LayoutParams wrap_content() {
        return new LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    }

    public static RelativeLayout.LayoutParams align(int align) {
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
        params.addRule(align);
        return params;
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

    public static void size(View view, int width, int height) {
        view.getLayoutParams().width = width;
        view.getLayoutParams().height = height;
        view.requestLayout();
    }

    public static void size(View view, int size) {
        view.getLayoutParams().width = size;
        view.getLayoutParams().height = size;
        view.requestLayout();
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

        public MarginUtils(View view) {
            this.view = view;
            this.params = (MarginLayoutParams) view.getLayoutParams();
        }

        public void all(int size) {
            this.params.topMargin = size;
            this.params.bottomMargin = size;
            this.params.leftMargin = size;
            this.params.rightMargin = size;
            this.view.requestLayout();
        }

        public MarginUtils vertical(int top, int bottom) {
            this.params.topMargin = top;
            this.params.bottomMargin = bottom;
            this.view.requestLayout();
            return this;
        }

        public MarginUtils horizontal(int left, int right) {
            this.params.leftMargin = left;
            this.params.rightMargin = right;
            this.view.requestLayout();
            return this;
        }

        public MarginUtils top(int size) {
            this.params.topMargin = size;
            this.view.requestLayout();
            return this;
        }

        public MarginUtils bottom(int size) {
            this.params.bottomMargin = size;
            this.view.requestLayout();
            return this;
        }

        public MarginUtils left(int size) {
            this.params.leftMargin = size;
            this.view.requestLayout();
            return this;
        }

        public MarginUtils right(int size) {
            this.params.rightMargin = size;
            this.view.requestLayout();
            return this;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static RuleUtils rule(View view) {
        return new RuleUtils(view);
    }

    public static class RuleUtils {

        private final View view;
        private final RelativeLayout.LayoutParams params;

        private int[] rules;

        //----------------------------------------------------------------------------------------//

        private RuleUtils(View view) {
            this.view = view;
            if (!(view.getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
                RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(0, 0);

                params.width = view.getLayoutParams().width;
                params.height = view.getLayoutParams().height;
                view.setLayoutParams(params);
            }
            params = (RelativeLayout.LayoutParams) view.getLayoutParams();
        }

        //----------------------------------------------------------------------------------------//

        public RuleUtils add(int... rules) {
            this.rules = rules;
            return this;
        }

        public void apply() {
            apply(null);
        }

        public void apply(Integer subject) {
            for (int verb : rules) {
                if (subject != null) {
                    params.addRule(verb, subject);
                } else {
                    params.addRule(verb);
                }
            }
            view.setLayoutParams(params);
        }
    }
}
