package com.reco1l.ui.custom;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.global.Game;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.Views;
import com.reco1l.view.ButtonView;

import org.jetbrains.annotations.NotNull;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/7/22 20:43

public class Dialog extends BaseFragment {

    private final DialogBuilder mBuilder;

    private CardView mBody;
    private TextView mTitleText;
    private ScrollView mScrollBody;

    private LinearLayout
            mBodyContainer,
            mButtonsContainer;

    //--------------------------------------------------------------------------------------------//

    public Dialog(@NotNull DialogBuilder builder) {
        super();
        this.mBuilder = builder;
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

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        closeOnBackgroundClick(canClose());

        int m = Res.dimen(R.dimen.M);
        int xs = Res.dimen(R.dimen.XS);

        mButtonsContainer = find("buttonsContainer");
        mScrollBody = find("bodyParent");
        mBodyContainer = find("container");
        mTitleText = find("title");
        mBody = find("body");

        TextView message = find("message");

        Animation.of(rootBackground)
                .fromAlpha(0)
                .toAlpha(1)
                .play(500);

        Animation.of(mBody)
                .fromAlpha(0)
                .toAlpha(1)
                .interpolate(Easing.OutExpo)
                .play(500);

        Animation.of(mScrollBody)
                .fromY(getHeight() / 0.85f)
                .toY(0)
                .interpolate(Easing.OutExpo)
                .play(500);

        Animation.of(mTitleText)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        if (mBuilder.customFragment != null) {
            Game.platform.transaction()
                    .add(find("fragmentContainer").getId(), mBuilder.customFragment)
                    .runOnCommit(
                            () -> Animation.of(mBuilder.customFragment.getView())
                                    .fromAlpha(0)
                                    .toAlpha(1)
                                    .play(200)
                    )
                    .commit();
        }

        if (canClose()) {
            bindTouch(find("scrollBackground"), new TouchListener() {
                public boolean useTouchEffect() {
                    return false;
                }

                public boolean useOnlyOnce() {
                    return true;
                }

                public void onPressUp() {
                    close();
                }
            });
        }

        mScrollBody.setSmoothScrollingEnabled(true);

        if (mBuilder.buttons != null) {

            for (int i = 0; i < mBuilder.buttons.size(); i++) {

                Button button = mBuilder.buttons.get(i);
                button.build(mButtonsContainer);
                button.load(this);


                Animation.of(button.view)
                        .fromAlpha(0)
                        .toAlpha(1)
                        .delay(200L * i)
                        .play(200);

                Views.MarginUtils margins = Views.margins(button.view);
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
        }

        mTitleText.setText(mBuilder.title);

        if (mBuilder.customFragment == null) {
            if (mBuilder.message != null) {
                message.setText(mBuilder.message);
                message.setVisibility(View.VISIBLE);
            } else {
                message.setVisibility(View.GONE);
            }
        } else {
            message.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onBackPress() {
        if (canClose()) {
            close();
            return true;
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public boolean canClose() {
        return mBuilder.canClose;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded())
            return;

        Animation.of(rootBackground)
                .toAlpha(0)
                .play(500);

        Animation.of(mBody)
                .toAlpha(0)
                .interpolate(Easing.InExpo)
                .play(500);

        Animation.of(mScrollBody)
                .toY(getHeight() / 0.85f)
                .interpolate(Easing.InExpo)
                .runOnEnd(() -> {
                    super.close();
                    if (mBuilder.mOnClose != null)
                        mBuilder.mOnClose.run();
                })
                .play(500);

        Animation.of(mTitleText)
                .toAlpha(0)
                .play(300);

        Animation.of(mBodyContainer)
                .toAlpha(0)
                .play(400);

        if (mBuilder.buttons != null && !mBuilder.buttons.isEmpty()) {
            Animation.of(mButtonsContainer)
                    .toAlpha(0)
                    .play(300);
        }

    }


    public boolean show() {
        if (!isAdded() && mBuilder.closeExtras) {
            Game.platform.closeExtras();
        }
        return super.show();
    }


    //--------------------------------------------------------------------------------------------//

    public interface OnButtonClick {
        void onButtonClick(Dialog dialog);
    }

    public static class Button {

        protected String text;
        protected Integer color;
        protected OnButtonClick onClick;
        private ButtonView view;

        public Button(String text, Integer color, OnButtonClick onClick) {
            this.text = text;
            this.color = color;
            this.onClick = onClick;
        }

        private void build(LinearLayout container) {
            view = new ButtonView(Game.activity);
            container.addView(view, new LayoutParams(MATCH_PARENT, Res.dimen(R.dimen.dialogButtonHeight)));
        }

        private void load(Dialog dialog) {
            dialog.bindTouch(view, () -> onClick.onButtonClick(dialog));

            if (color != null) {
                view.setCardBackgroundColor(color);
            }
            view.setButtonText(text);
        }
    }
}
