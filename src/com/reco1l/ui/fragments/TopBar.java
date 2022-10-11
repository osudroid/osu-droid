package com.reco1l.ui.fragments;

import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.enums.Scenes;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.UI;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.ui.data.tables.DialogTable;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.utils.listeners.TouchListener;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class TopBar extends UIFragment {

    public static TopBar instance;

    public View body;
    public TextView author;
    public UserBox userBox;
    public MusicButton musicButton;

    public int barHeight;

    private View bar, back;
    private LinearLayout container;
    private ButtonsLayout buttons;

    private Scenes lastScene;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "tb";
    }

    @Override
    protected int getLayout() {
        return R.layout.top_bar;
    }

    @Override
    protected Scenes[] getParents() {
        return new Scenes[] {Scenes.MAIN, Scenes.LIST, Scenes.SCORING};
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onSceneChange(Scenes oldScene, Scenes newScene) {
        reload();
    }

    public void reload() {
        if (lastScene == engine.currentScene || !isShowing)
            return;

        new Animation(container).moveX(0, -60).fade(1, 0).runOnEnd(() -> {
            back.setVisibility(View.GONE);
            musicButton.setVisibility(false);
            buttons.gone();
        }).play(190);

        new Animation(container).moveX(-60, 0).fade(0, 1).runOnStart(() -> {
            switch (engine.currentScene) {

                case MAIN:
                    musicButton.setVisibility(true);
                    break;

                case LIST:
                    back.setVisibility(View.VISIBLE);
                    break;

                default:
                    // Nothing
                    break;
            }
            buttons.update(engine.currentScene);
        }).delay(200).play(200);

        showAuthorText(engine.currentScene == Scenes.MAIN);
        lastScene = engine.currentScene;
    }

    @Override
    protected void onLoad() {
        setDismissMode(false, false);
        barHeight = (int) Resources.dimen(R.dimen.topBarHeight);

        musicButton = new MusicButton(this);
        buttons = new ButtonsLayout(this);
        userBox = new UserBox(this);

        container = find("container");
        author = find("author");
        body = find("body");
        back = find("back");
        bar = find("bar");

        new Animation(body).moveY(-barHeight, 0)
                .play(300);

        author.setAlpha(0);
        showAuthorText(engine.currentScene == Scenes.MAIN);

        if (library.getSizeOfBeatmaps() <= 0) {
            musicButton.setVisibility(false);
        }

        author.setText(String.format("osu!droid %s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));

        bindTouchListener(find("inbox"), UI.notificationCenter::altShow);
        bindTouchListener(find("settings"), UI.settingsPanel::altShow);

        bindTouchListener(author, new TouchListener() {
            public void onPressUp() {
                new Dialog(DialogTable.author()).show();
            }
        });

        userBox.update(false);
    }

    @Override
    public void close() {
        if (!isShowing)
            return;
        lastScene = null;
        showAuthorText(false);
        new Animation(body).moveY(0, -barHeight).runOnEnd(super::close).play(300);
    }

    //--------------------------------------------------------------------------------------------//

    private void showAuthorText(boolean bool) {
        if (author == null || bool && author.getAlpha() == 1 || !bool && author.getAlpha() == 0)
            return;

        if (bool) {
            new Animation(author).fade(0, 1).moveY(50, 0).play(200);
            return;
        }
        new Animation(author).fade(1, 0).moveY(0, 50).play(200);
    }

    public void switchColor(boolean isFromTab) {

        int from = Resources.color(isFromTab ? R.color.backgroundPrimary : R.color.topBarBackground);
        int to = Resources.color(isFromTab ? R.color.topBarBackground : R.color.backgroundPrimary);

        ValueAnimator anim = ValueAnimator.ofArgb(from, to);

        anim.addUpdateListener(val -> bar.setBackgroundColor((int) val.getAnimatedValue()));
        anim.setDuration(300);
        anim.start();
    }

    //--------------------------------------------------------------------------------------------//

    public static class ButtonsLayout {

        private final TopBar parent;
        private final LinearLayout container;

        // Song menu
        private final ImageView mods, search, shuffle;

        public ButtonsLayout(TopBar parent) {
            this.parent = parent;

            container = parent.find("buttons");

            mods = parent.find("mods");
            search = parent.find("search");
            shuffle = parent.find("shuffle");
        }

        protected void gone() {
            for (int i = 0; i < container.getChildCount(); i++) {
                container.getChildAt(i).setVisibility(View.GONE);
            }
        }

        protected void update(Scenes scene) {
            if (!parent.isShowing)
                return;

            if (scene == Scenes.LIST) {
                mods.setVisibility(View.VISIBLE);
                search.setVisibility(View.VISIBLE);
                shuffle.setVisibility(View.VISIBLE);
            }
        }
    }

    public static class MusicButton {

        private final TopBar parent;
        private final View view, body, arrow;
        private final TextView text;

        public MusicButton(TopBar parent) {
            this.parent = parent;

            view = parent.find("music");
            body = parent.find("musicBody");
            arrow = parent.find("musicArrow");
            text = parent.find("musicText");

            UI.topBar.bindTouchListener(view, UI.musicPlayer::altShow);
        }

        public void update(BeatmapInfo beatmap) {
            if (!parent.isShowing)
                return;

            new Animation(text).fade(1, 0)
                    .play(150);
            new Animation(text).fade(0, 1)
                    .runOnStart(() -> text.setText(BeatmapHelper.getTitle(beatmap)))
                    .delay(150)
                    .play(150);
        }

        public void playAnimation(boolean show) {
            Animation bodyAnim = new Animation(body);
            Animation arrowAnim = new Animation(arrow);

            if (show) {
                bodyAnim.moveY(0, -10).fade(1, 0);
                arrowAnim.rotation(180, 180).moveY(10, 0).fade(0, 1);
            } else {
                bodyAnim.moveY(10, 0).fade(0, 1);
                arrowAnim.rotation(180, 180).moveY(0, -10).fade(1, 0);
            }
            bodyAnim.play(150);
            arrowAnim.delay(150).play(150);
        }

        protected void setVisibility(boolean bool) {
            view.setVisibility(bool ? View.VISIBLE : View.GONE);
        }

    }

    public static class UserBox {

        private final TopBar parent;
        private final TextView rank, name;
        private final ShapeableImageView avatar;

        public UserBox(TopBar parent) {
            this.parent = parent;

            View body = parent.find("userBox");
            rank = parent.find("playerRank");
            name = parent.find("playerName");
            avatar = parent.find("avatar");

            UI.topBar.bindTouchListener(body, UI.userProfile::altShow);
        }

        public void update(boolean clear) {
            if (!parent.isShowing)
                return;

            avatar.setImageResource(R.drawable.default_avatar);
            name.setText(Config.getLocalUsername());
            rank.setText(Resources.str(R.string.top_bar_offline));

            if (!online.isStayOnline() || clear)
                return;

            name.setText(online.getUsername());
            rank.setText(String.format("#%d", online.getRank()));

            if (onlineHelper.getPlayerAvatar() != null)
                avatar.setImageDrawable(onlineHelper.getPlayerAvatar());
        }
    }
}
