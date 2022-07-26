package com.reco1l.ui.custom;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;

import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 23/7/22 20:43

public class Dialog extends UIFragment {

    private final DialogBuilder builder;

    private CardView body;
    private ScrollView bodyParent;
    private LinearLayout container, buttonsContainer;

    private TextView title;

    private boolean closeExtras = true;

    //--------------------------------------------------------------------------------------------//

    public Dialog(DialogBuilder builder) {
        this.builder = builder;
    }

    @Override
    protected String getPrefix() {
        return "d";
    }

    @Override
    protected int getLayout() {
        return R.layout.dialog;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(builder.closeOnBackgroundClick, builder.closeOnBackPress);

        int m = (int) Res.dimen(R.dimen.M);
        int xs = (int) Res.dimen(R.dimen.XS);

        buttonsContainer = find("buttonsContainer");
        bodyParent = find("bodyParent");
        container = find("container");
        title = find("title");
        body = find("body");

        TextView message = find("message");

        new Animation(rootBackground).fade(0, 1)
                .play(500);
        new Animation(body).fade(0, 1).interpolator(Easing.OutExpo)
                .play(500);
        new Animation(bodyParent).moveY(screenHeight / 0.85f, 0).interpolator(Easing.OutExpo)
                .play(500);

        new Animation(title).fade(0, 1).play(300);

        if (builder.customFragment != null) {
            platform.manager.beginTransaction()
                    .add(find("fragmentContainer").getId(), builder.customFragment)
                    .runOnCommit(() ->
                            new Animation(builder.customFragment.root)
                                    .forChildView(child -> new Animation(child).fade(0, 1))
                                    .play(100))
                    .commit();
        }

        if (builder.closeOnBackgroundClick) {
            new ClickListener(find("scrollBackground")).touchEffect(false).onlyOnce(true)
                    .simple(this::close);
        }

        bodyParent.setSmoothScrollingEnabled(true);

        if (builder.buttons != null) {

            for (int i = 0; i < builder.buttons.size(); i++) {

                Button button = builder.buttons.get(i);
                button.inflate(buttonsContainer);
                button.load(this);

                new Animation(button.view).fade(0, 1).delay(200L * i)
                        .play(200);

                setHorizontalMargin(button.view, m, m);

                if (builder.buttons.size() > 1) {
                    if (i == 0) {
                        setVerticalMargin(button.view, m, xs);
                    } else if (i == builder.buttons.size() - 1) {
                        setVerticalMargin(button.view, xs, m);
                    } else {
                        setVerticalMargin(button.view, xs, xs);
                    }
                } else {
                    setVerticalMargin(button.view, m, m);
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

        new Animation(rootBackground).fade(1, 0)
                .play(500);
        new Animation(body).fade(1, 0).interpolator(Easing.InExpo)
                .play(500);
        new Animation(bodyParent).moveY(0, screenHeight / 0.85f).interpolator(Easing.InExpo)
                .runOnEnd(() -> {
                    super.close();
                    if (builder.onClose != null)
                        builder.onClose.run();
                })
                .play(500);

        new Animation(title).fade(1, 0)
                .play(400);
        new Animation(container).fade(1, 0)
                .play(400);

        if (builder.buttons != null && !builder.buttons.isEmpty()) {
            new Animation(buttonsContainer).fade(1, 0)
                    .play(300);
        }

    }

    public Dialog closeExtras(boolean closeExtras) {
        this.closeExtras = closeExtras;
        return this;
    }

    @Override
    public void show() {
        if (closeExtras)
            platform.closeThis(UIManager.getExtras());

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
        private View view;

        public Button(String text, Integer color, OnButtonClick onClick) {
            this.text = text;
            this.color = color;
            this.onClick = onClick;
        }

        private void inflate(LinearLayout container) {
            LayoutParams params = new LayoutParams(LayoutParams.MATCH_PARENT, (int) Res.dimen(R.dimen.dialogButtonHeight));
            view = LayoutInflater.from(platform.context).inflate(R.layout.dialog_button, null);
            container.addView(view, params);
        }

        private void load(Dialog dialog) {
            new ClickListener(view).simple(() -> onClick.onButtonClick(dialog));

            if (color != null) {
                ((CardView) view).setCardBackgroundColor(color);
            }

            TextView buttonText = view.findViewById(R.id.d_buttonText);
            buttonText.setText(text);
        }
    }
}
