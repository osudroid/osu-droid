package com.reco1l.ui.custom;

import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.management.resources.ResourceTable;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.ui.base.Layers;
import com.reco1l.ui.base.SimpleFragment;
import com.reco1l.utils.Views;
import com.reco1l.utils.Views.MarginUtils;
import com.reco1l.view.ButtonView;

import org.jetbrains.annotations.NotNull;

import com.rimu.R;

// Created by Reco1l on 23/7/22 20:43

public class Dialog extends BaseFragment {

    private final DialogBuilder mBuilder;

    private TextView
            mTitleText,
            mMessageText;

    private LinearLayout
            mBodyContainer,
            mButtonsContainer;

    //--------------------------------------------------------------------------------------------//

    public Dialog(@NotNull DialogBuilder builder) {
        super();
        mBuilder = builder;
        closeOnBackgroundClick(mBuilder.canClose);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "d";
    }

    @Override
    protected int getLayout() {
        return R.layout.overlay_dialog;
    }

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Overlay;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {


        //mBody = find("body");
        mTitleText = find("title");
        mMessageText = find("message");
        mBodyContainer = find("container");
        mButtonsContainer = find("buttons");

        handleCustomBody(mBuilder.fragment, mBuilder.view);

        if (mBuilder.title == null) {
            mTitleText.setVisibility(View.GONE);
        }
        mTitleText.setText(mBuilder.title);

        if (mBuilder.message == null) {
            mMessageText.setVisibility(View.GONE);
        }
        mMessageText.setText(mBuilder.message);

        mBuilder.buttons.forEach(b -> {
            int i = mBuilder.buttons.indexOf(b);
            handleButton(b, i);
        });
    }

    @Override
    public boolean onBackPress() {
        if (mBuilder.canClose) {
            close();
            return true;
        }
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    private void handleButton(Button button, int i) {
        int m = sdp(12);
        int xs = sdp(4);

        mButtonsContainer.addView(button.mView, Views.params(-1, sdp(32)));

        bindTouch(button.mView, () ->
                button.onButtonClick(this)
        );

        MarginUtils margins = Views.margins(button.mView);
        margins.horizontal(m, m);

        if (mBuilder.buttons.size() > 1) {
            if (i == 0) {
                margins.vertical(m, xs);
            } else if (i == mBuilder.buttons.size() - 1) {
                margins.vertical(xs, m);
            } else {
                margins.vertical(xs, xs);
            }
        } else {
            margins.vertical(m, m);
        }
    }

    private void handleCustomBody(SimpleFragment fragment, View view) {
        if (fragment == null && view == null) {
            return;
        }

        if (mBuilder.hideHeader) {
            mBodyContainer.removeAllViews();
        }

        if (fragment != null) {
            Game.platform.transaction()
                    .add(mBodyContainer.getId(), fragment)
                    .commit();
        } else {
            mBodyContainer.addView(view);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }
        super.close();

        if (mBuilder.mOnClose != null) {
            mBuilder.mOnClose.run();
        }
    }

    //--------------------------------------------------------------------------------------------//

    @FunctionalInterface
    public interface OnButtonClick {
        void onButtonClick(Dialog dialog);
    }

    //--------------------------------------------------------------------------------------------//

    public abstract static class Button implements OnButtonClick, ResourceTable {

        private final ButtonView mView;

        //--------------------------------------------------------------------------------------- //

        public Button() {
            mView = new ButtonView(context());
            mView.setButtonText(getText());

            if (getColor() != -1) {
                mView.setBackground(new ColorDrawable(getColor()));
            }
        }

        //--------------------------------------------------------------------------------------- //

        protected abstract String getText();

        protected @ColorInt int getColor() {
            return -1;
        }
    }
}
