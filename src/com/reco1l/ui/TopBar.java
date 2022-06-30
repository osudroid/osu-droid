package com.reco1l.ui;

import android.animation.ValueAnimator;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.imageview.ShapeableImageView;
import com.reco1l.EngineMirror;
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.Animator;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.UI;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class TopBar extends BaseLayout {

    public static TopBar instance;
    public UserBox userBox;

    private View body, bar, music, inbox;
    private TextView musicText;
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

    @Override
    protected void onLoad() {
        setDismissMode(false, false);
        barHeight = (int) res().getDimension(R.dimen.topBarHeight);
        userBox = new UserBox(this);

        ImageView settings = find("settings");
        musicText = find("mText");
        author = find("author");
        music = find("music");
        inbox = find("inbox");
        body = find("body");
        bar = find("bar");

        author.setAlpha(0);
        new Animator(body).moveY(-barHeight, 0).play(300);

        if (library.getSizeOfBeatmaps() <= 0)
            setVisible(false, music);

        String ver = BuildConfig.VERSION_NAME;
        String build = BuildConfig.BUILD_TYPE;
        author.setText(String.format("osu!droid %s by osu!droid Team", ver + " (" + build + ")"));

        new ClickListener(inbox).simple(UI.inbox::altShow);
        new ClickListener(music).simple(null);
        new ClickListener(settings).simple(() -> new SettingsMenu().show());

        setAuthorVisibility(engine.currentScene == EngineMirror.Scenes.MAIN_MENU);

        userBox.update();
        updateMusicText();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        setAuthorVisibility(false);
        new Animator(body).moveY(0, -barHeight).runOnEnd(super::close).play(300);
    }

    //--------------------------------------------------------------------------------------------//

    public void updateMusicText() {
        if (!isShowing || musicText == null)
            return;

        BeatmapInfo info = library.getBeatmap();
        mActivity.runOnUiThread(() -> {
            if (info == null) {
                musicText.setText("Error loading song title!");
                return;
            }

            if (info.getTitleUnicode() != null && !Config.isForceRomanized()) {
                musicText.setText(info.getTitleUnicode());
            } else if (info.getTitle() != null) {
                musicText.setText(info.getTitle());
            } else {
                musicText.setText("Error loading song title!");
            }
        });
    }

    private void setAuthorVisibility(boolean bool) {
        if (author == null || isAuthorShown == bool)
            return;
        if (bool) {
            new Animator(author).fade(0, 1).moveY(50, 0).play(300);
        } else {
            new Animator(author).fade(1, 0).moveY(0, 50).play(300);
        }
        isAuthorShown = bool; // This avoids animation duplicate.
    }

    public void switchColor(boolean isOnTab) {
        int normalColor = res().getColor(R.color.topBarBackground);
        int onTabColor = res().getColor(R.color.backgroundPrimary);

        ValueAnimator anim = ValueAnimator.ofArgb(isOnTab ? onTabColor : normalColor, isOnTab ? normalColor : onTabColor);
        anim.setDuration(300);
        anim.addUpdateListener(value -> bar.setBackgroundColor((int) value.getAnimatedValue()));
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

            new ClickListener(body).simple(() -> {
                Inbox.Notification notification =
                        new Inbox.Notification("test notification " + clickCount, "testing");

                UI.inbox.add(notification);
                clickCount++;
            });
        }

        public void update() {
            if (!parent.isShowing || online == null)
                return;

            avatar.setImageResource(R.drawable.default_avatar);
            name.setText(Config.getLocalUsername());
            rank.setText(topBar.res().getString(R.string.top_bar_offline));

            if (!online.isStayOnline())
                return;

            name.setText(online.getUsername());
            rank.setText(String.format("#%d", online.getRank()));

            if (onlineHandler.getPlayerAvatar() != null)
                avatar.setImageDrawable(onlineHandler.getPlayerAvatar());
        }
    }

    //--------------------------------------------------------------------------------------------//


}
