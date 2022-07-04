package com.reco1l.ui;

import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.edlplan.framework.easing.Easing;
import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.EngineMirror;
import com.reco1l.ui.data.GameNotification;
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.Animator;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;
import com.reco1l.utils.interfaces.UI;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class TopBar extends BaseLayout {

    public View musicBody, musicArrow;
    public TextView musicText;
    public UserBox userBox;

    private EngineMirror.Scenes lastScene;
    private View body, bar, music, back;
    private LinearLayout container;
    private TextView author;

    private boolean isAuthorShown = false;
    private int barHeight;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "tb";
    }

    @Override
    protected int getLayout() {
        return R.layout.top_bar;
    }

    //--------------------------------------------------------------------------------------------//

    public void reload() {
        if (lastScene == engine.currentScene || !isShowing)
            return;
        lastScene = engine.currentScene;

        if (container != null) {

            Runnable onBack = null;

            Animator inAnim = new Animator(container).moveX(-60, 0).fade(0, 1);
            Animator outAnim = new Animator(container).moveX(0, -60).fade(1, 0);

            outAnim.onEnd = () -> setVisible(false, music, back);

            outAnim.interpolator(Easing.OutExpo);
            inAnim.interpolator(Easing.OutExpo);

            switch (engine.currentScene) {

                case MAIN_MENU:
                    inAnim.onStart = () -> setVisible(music);
                    break;

                case SONG_MENU:
                    inAnim.onStart = () -> setVisible(back);
                    onBack = () -> global.getSongMenu().back();
                    break;

                case LOADING_SCREEN:
                case PAUSE_MENU:
                case SCORING:
                case GAME:
                    // Nothing because the top bar is hidden in these scenes.
                    break;
            }

            outAnim.play(200);
            inAnim.delay(200).play(200);

            new ClickListener(back).simple(onBack);
        }

        showAuthorText(engine.currentScene == EngineMirror.Scenes.MAIN_MENU);
    }

    @Override
    protected void onLoad() {
        setDismissMode(false, false);
        barHeight = (int) Res.dimen(R.dimen.topBarHeight);
        userBox = new UserBox(this);

        author = find("author");
        body = find("body");
        bar = find("bar");

        new Animator(body).moveY(-barHeight, 0).play(300);

        ImageView settings = find("settings");
        ImageView inbox = find("inbox");

        container = find("container");
        back = find("back");
        musicBody = find("musicChildLayout");
        musicArrow = find("musicArrow");
        musicText = find("musicText");
        music = find("music");

        showAuthorText(engine.currentScene == EngineMirror.Scenes.MAIN_MENU);

        if (library.getSizeOfBeatmaps() <= 0)
            setVisible(false, musicText);

        author.setText(String.format("osu!droid %s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));

        new ClickListener(inbox).simple(UI.inbox::altShow);
        new ClickListener(music).simple(musicPlayer::altShow);
        new ClickListener(settings).simple(() -> new SettingsMenu().show());

        userBox.update(false);
    }

    @Override
    public void close() {
        if (!isShowing)
            return;
        lastScene = null;
        showAuthorText(false);
        new Animator(body).moveY(0, -barHeight).runOnEnd(super::close).play(300);
    }

    //--------------------------------------------------------------------------------------------//

    private void showAuthorText(boolean bool) {
        if (author == null || isAuthorShown == bool)
            return;

        if (bool) {
            new Animator(author).fade(0, 1).moveY(50, 0)
                    .runOnEnd(() -> isAuthorShown = bool)
                    .play(200);
            return;
        }
        new Animator(author).fade(1, 0).moveY(0, 50)
                .runOnEnd(() -> isAuthorShown = bool)
                .play(200);
    }

    public void switchColor(boolean isFromTab) {

        int from = Res.color(isFromTab ? R.color.backgroundPrimary : R.color.topBarBackground);
        int to = Res.color(isFromTab ? R.color.topBarBackground : R.color.backgroundPrimary);

        ValueAnimator anim = ValueAnimator.ofArgb(from, to);

        anim.addUpdateListener(val -> bar.setBackgroundColor((int) val.getAnimatedValue()));
        anim.setDuration(300);
        anim.start();
    }

    //--------------------------------------------------------------------------------------------//

    public static class UserBox {

        private final TopBar parent;
        private final TextView rank, name;
        private final ShapeableImageView avatar;

        private int clickCount = 0;

        public UserBox(TopBar parent) {
            this.parent = parent;

            View body = parent.find("userBox");
            rank = parent.find("playerRank");
            name = parent.find("playerName");
            avatar = parent.find("avatar");

            // Debug
            new ClickListener(body).simple(() -> {
                GameNotification notification = new GameNotification("Test notification");
                notification.message = "Notification number " + clickCount;
                UI.inbox.add(notification);
                clickCount++;
            });
        }

        public void update(boolean clear) {
            if (!parent.isShowing)
                return;

            avatar.setImageResource(R.drawable.default_avatar);
            name.setText(Config.getLocalUsername());
            rank.setText(topBar.res().getString(R.string.top_bar_offline));

            if (!online.isStayOnline() || clear)
                return;

            name.setText(online.getUsername());
            rank.setText(String.format("#%d", online.getRank()));

            if (onlineHandler.getPlayerAvatar() != null)
                avatar.setImageDrawable(onlineHandler.getPlayerAvatar());
        }
    }

    //--------------------------------------------------------------------------------------------//


}
