package com.reco1l.ui.custom;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.AnimationOld;
import com.reco1l.utils.Resources;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.ViewUtils.MarginUtils;
import com.reco1l.utils.listeners.TouchListener;
import com.reco1l.view.TextButton;

import org.jetbrains.annotations.NotNull;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/7/22 20:43

public class Dialog extends UIFragment {

    public final DialogBuilder builder;

    private CardView body;
    private TextView title;
    private ScrollView bodyParent;
    private LinearLayout container, buttonsContainer;

    private boolean closeExtras = true;

    //--------------------------------------------------------------------------------------------//

    public Dialog(@NotNull DialogBuilder builder) {
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
    protected long getDismissTime() {
        return builder.dismissTime;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(builder.closeOnBackgroundClick, builder.closeOnBackPress);

        int m = (int) Resources.dimen(R.dimen.M);
        int xs = (int) Resources.dimen(R.dimen.XS);

        buttonsContainer = find("buttonsContainer");
        bodyParent = find("bodyParent");
        container = find("container");
        title = find("title");
        body = find("body");

        TextView message = find("message");

        new AnimationOld(rootBackground).fade(0, 1)
                .play(500);
        new AnimationOld(body).fade(0, 1)
                .interpolator(Easing.OutExpo)
                .play(500);
        new AnimationOld(bodyParent).moveY(screenHeight / 0.85f, 0)
                .interpolator(Easing.OutExpo)
                .play(500);
        new AnimationOld(title).fade(0, 1)
                .play(300);



        if (builder.customFragment != null) {
            platform.manager.beginTransaction()
                    .add(find("fragmentContainer").getId(), builder.customFragment)
                    .runOnCommit(() ->
                            new AnimationOld(builder.customFragment.root)
                                    .forChildView(child -> new AnimationOld(child).fade(0, 1))
                                    .play(100))
                    .commit();
        }

        if (builder.closeOnBackgroundClick) {
            bindTouchListener(find("scrollBackground"), new TouchListener() {
                public boolean hasTouchEffect() { return false; }
                public boolean isOnlyOnce() { return true; }

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

                new AnimationOld(button.view).fade(0, 1).delay(200L * i)
                        .play(200);

                MarginUtils margins = ViewUtils.margins(button.view);
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
        if (!isShowing)
            return;

        Game.platform.dialogs.remove(this);

        new AnimationOld(rootBackground).fade(1, 0)
                .play(500);
        new AnimationOld(body).fade(1, 0).interpolator(Easing.InExpo)
                .play(500);
        new AnimationOld(bodyParent).moveY(0, screenHeight / 0.85f).interpolator(Easing.InExpo)
                .runOnEnd(() -> {
                    super.close();
                    if (builder.onClose != null)
                        builder.onClose.run();
                })
                .play(500);

        new AnimationOld(title).fade(1, 0)
                .play(400);
        new AnimationOld(container).fade(1, 0)
                .play(400);

        if (builder.buttons != null && !builder.buttons.isEmpty()) {
            new AnimationOld(buttonsContainer).fade(1, 0)
                    .play(300);
        }

    }

    public Dialog closeExtras(boolean closeExtras) {
        this.closeExtras = closeExtras;
        return this;
    }

    @Override
    public void show() {
        if (!isShowing) {
            if (closeExtras) {
                platform.close(UI.getExtras());
            }
            Game.platform.dialogs.add(this);
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
        private TextButton view;

        public Button(String text, Integer color, OnButtonClick onClick) {
            this.text = text;
            this.color = color;
            this.onClick = onClick;
        }

        private void build(LinearLayout container) {
            view = new TextButton(Game.activity);
            container.addView(view, new LayoutParams(MATCH_PARENT, (int) Resources.dimen(R.dimen.dialogButtonHeight)));
        }

        private void load(Dialog dialog) {
            dialog.bindTouchListener(view, () -> onClick.onButtonClick(dialog));

            if (color != null) {
                view.setButtonColor(color);
            }
            view.setButtonText(text);
        }
    }
}
