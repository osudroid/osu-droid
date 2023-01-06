package com.reco1l.ui.custom;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.tables.Res;
import com.reco1l.utils.Views;
import com.reco1l.utils.Views.MarginUtils;
import com.reco1l.utils.TouchListener;
import com.reco1l.view.ButtonView;

import org.jetbrains.annotations.NotNull;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/7/22 20:43

public class Dialog extends BaseFragment {

    public final DialogBuilder builder;

    private CardView body;
    private TextView title;
    private ScrollView bodyParent;
    private LinearLayout container, buttonsContainer;

    private boolean closeExtras = true;

    //--------------------------------------------------------------------------------------------//

    public Dialog(@NotNull DialogBuilder builder) {
        super();
        this.builder = builder;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "d";
    }

    @Override
    protected int getLayout() {
        return R.layout.dialog;
    }

    @Override
    protected long getCloseTime() {
        return builder.dismissTime;
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        closeOnBackgroundClick(builder.closeOnBackgroundClick);

        int m = (int) Res.dimen(R.dimen.M);
        int xs = (int) Res.dimen(R.dimen.XS);

        buttonsContainer = find("buttonsContainer");
        bodyParent = find("bodyParent");
        container = find("container");
        title = find("title");
        body = find("body");

        TextView message = find("message");

        Animation.of(rootBackground)
                .fromAlpha(0)
                .toAlpha(1)
                .play(500);

        Animation.of(body)
                .fromAlpha(0)
                .toAlpha(1)
                .interpolate(Easing.OutExpo)
                .play(500);

        Animation.of(bodyParent)
                .fromY(getHeight() / 0.85f)
                .toY(0)
                .interpolate(Easing.OutExpo)
                .play(500);

        Animation.of(title)
                .fromAlpha(0)
                .toAlpha(1)
                .play(300);

        if (builder.customFragment != null) {
            Game.platform.manager.beginTransaction()
                    .add(find("fragmentContainer").getId(), builder.customFragment)
                    .runOnCommit(
                            () -> Animation.of(builder.customFragment.getView())
                                    .fromAlpha(0)
                                    .toAlpha(1)
                                    .play(200)
                    )
                    .commit();
        }

        if (builder.closeOnBackgroundClick) {
            bindTouch(find("scrollBackground"), new TouchListener() {
                public boolean useTouchEffect() { return false; }
                public boolean useOnlyOnce() { return true; }

                public void onPressUp() {
                    close();
                }
            });
        }

        bodyParent.setSmoothScrollingEnabled(true);

        if (builder.buttons != null) {

            for (int i = 0; i < builder.buttons.size(); i++) {

                Button button = builder.buttons.get(i);
                button.build(buttonsContainer);
                button.load(this);


                Animation.of(button.view)
                        .fromAlpha(0)
                        .toAlpha(1)
                        .delay(200L * i)
                        .play(200);

                MarginUtils margins = Views.margins(button.view);
                margins.horizontal(m, m);

                if (builder.buttons.size() > 1) {
                    if (i == 0) {
                        margins.vertical(m, xs);
                    } else if (i == builder.buttons.size() - 1) {
                        margins.vertical(xs, m);
                    } else {
                        margins.vertical(xs, xs);
                    }
                } else {
                    margins.vertical(m, m);
                }
            }
        }

        title.setText(builder.title);

        if (builder.customFragment == null) {
            if (builder.message != null) {
                message.setText(builder.message);
                message.setVisibility(View.VISIBLE);
            } else {
                message.setVisibility(View.GONE);
            }
        } else {
            message.setVisibility(View.GONE);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded())
            return;

        Animation.of(rootBackground)
                .toAlpha(0)
                .play(500);

        Animation.of(body)
                .toAlpha(0)
                .interpolate(Easing.InExpo)
                .play(500);

        Animation.of(bodyParent)
                .toY(getHeight() / 0.85f)
                .interpolate(Easing.InExpo)
                .runOnEnd(() -> {
                    super.close();
                    if (builder.onClose != null)
                        builder.onClose.run();
                })
                .play(500);

        Animation.of(title)
                .toAlpha(0)
                .play(300);

        Animation.of(container)
                .toAlpha(0)
                .play(400);

        if (builder.buttons != null && !builder.buttons.isEmpty()) {
            Animation.of(buttonsContainer)
                    .toAlpha(0)
                    .play(300);
        }

    }

    public Dialog closeExtras(boolean closeExtras) {
        this.closeExtras = closeExtras;
        return this;
    }

    @Override
    public void show() {
        if (!isAdded()) {
            if (closeExtras) {
                Game.platform.closeExtras();
            }
        }
        super.show();
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
