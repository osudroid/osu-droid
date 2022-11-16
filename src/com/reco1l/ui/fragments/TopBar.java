package com.reco1l.ui.fragments;

import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.UI;
import com.reco1l.utils.Animation2;
import com.reco1l.utils.AnimationTable;
import com.reco1l.utils.KeyInputHandler;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.helpers.BeatmapHelper;
import com.reco1l.ui.data.DialogTable;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.view.BarButton;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 26/6/22 21:20

public class TopBar extends UIFragment {

    public static TopBar instance;

    private View back;
    public CardView body;
    public TextView author;
    private LinearLayout container, buttonsContainer;

    public UserBox userBox;
    public MusicButton musicButton;

    private final Map<Screens, ArrayList<BarButton>> buttons;

    public int barHeight;

    private boolean isAuthorVisible = false;

    //--------------------------------------------------------------------------------------------//

    public TopBar() {
        buttons = new HashMap<>();
    }

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
    protected Screens[] getParents() {
        return new Screens[] { Screens.SONG_MENU, Screens.SCORING, Screens.PLAYER_LOADER};
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onScreenChange(Screens lastScreen, Screens newScene) {
        if (isShowing) {
            reloadButtons(newScene);
        }
    }

    public void reloadButtons(Screens current) {
        showAuthorText(current == Screens.MAIN);

        if (container == null || buttonsContainer == null)
            return;

        Animation fadeIn = new Animation(container);

        fadeIn.duration(200);
        fadeIn.moveX(-60, 0);
        fadeIn.fade(0, 1);

        fadeIn.runOnStart(() -> {
            ArrayList<BarButton> toAdd = buttons.get(current);

            if (toAdd != null) {
                for (BarButton button : toAdd) {
                    buttonsContainer.addView(button);
                    bindTouchListener(button, button.getTouchListener());
                }
            }
        });

        Animation fadeOut = new Animation(container);

        fadeOut.moveX(0, -60);
        fadeOut.fade(1f, 0);
        fadeOut.runOnEnd(() -> {
            buttonsContainer.removeAllViews();

            if (current == Screens.MAIN) {
                musicButton.setVisibility(true);
                back.setVisibility(View.GONE);
            } else {
                musicButton.setVisibility(false);
                back.setVisibility(View.VISIBLE);
                bindTouchListener(back, KeyInputHandler::performBack);
            }

            fadeIn.play();
        });
        fadeOut.play(200);
    }

    @Override
    protected void onLoad() {
        setDismissMode(false, false);
        barHeight = (int) Resources.dimen(R.dimen.topBarHeight);

        body = find("body");

        musicButton = new MusicButton(this);
        userBox = new UserBox(this);

        buttonsContainer = find("buttons");
        container = find("container");
        author = find("author");
        back = find("back");

        if (library.getSizeOfBeatmaps() <= 0) {
            musicButton.setVisibility(false);
        }

        author.setText(String.format("osu!droid %s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));

        bindTouchListener(find("inbox"), UI.notificationCenter::altShow);
        bindTouchListener(find("settings"), UI.settingsPanel::altShow);

        bindTouchListener(author, () -> new Dialog(DialogTable.author()).show());

        userBox.loadUserData(false);

        reloadButtons(Game.engine.currentScreen);
    }

    @Override
    public void show() {
        super.show();

        if (isShowing && body != null) {
            Animation show = new Animation(body);

            show.moveY(-barHeight, 0);
            show.interpolator(Easing.OutExpo);

            Game.platform.animateRender(anim -> {
                anim.moveY(0, barHeight);
                anim.interpolator(Easing.OutExpo);
                anim.duration(200);
            });

            Game.platform.animateScreen(animation -> {
                animation.toTopMargin = barHeight;
                animation.duration = 200;
                animation.interpolator = Easing.OutExpo;
            });

            show.play(200);
        }
    }

    @Override
    public void close() {
        if (isShowing) {
            showAuthorText(false);

            Animation close = new Animation(body);

            close.moveY(0, -barHeight);
            close.interpolator(Easing.OutExpo);
            close.runOnEnd(super::close);

            Game.platform.animateRender(anim -> {
                anim.moveY(Resources.dimen(R.dimen.topBarHeight), 0);
                anim.interpolator(Easing.OutExpo);
                anim.duration(200);
            });

            Game.platform.animateScreen(animation -> {
                animation.toTopMargin = 0;
                animation.duration = 200;
                animation.interpolator = Easing.OutExpo;
            });

            close.play(200);
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void showAuthorText(boolean bool) {
        if (author == null || bool == isAuthorVisible)
            return;

        isAuthorVisible = bool;
        Animation anim = new Animation(author);

        if (bool) {
            anim.fade(0, 1);
            anim.moveY(50, 0);
            anim.runOnStart(() -> author.setVisibility(View.VISIBLE));
        } else {
            anim.fade(1, 0);
            anim.moveY(0, 50);
            anim.runOnEnd(() -> author.setVisibility(View.GONE));
        }
        anim.play(200);
    }

    public void addButton(Screens screen, BarButton button) {
        if (buttons.get(screen) == null) {
            buttons.put(screen, new ArrayList<>());
        }
        ArrayList<BarButton> list = buttons.get(screen);

        if (list != null) {
            list.add(button);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class MusicButton {

        private final TopBar parent;
        private final View view, body, arrow;
        private final TextView text;

        //----------------------------------------------------------------------------------------//

        public MusicButton(TopBar parent) {
            this.parent = parent;

            view = parent.find("music");
            body = parent.find("musicBody");
            arrow = parent.find("musicArrow");
            text = parent.find("musicText");

            parent.bindTouchListener(view, UI.musicPlayer::altShow);
        }

        //----------------------------------------------------------------------------------------//

        public void changeMusic(BeatmapInfo beatmap) {
            if (parent.isShowing) {
                AnimationTable.textChange(text, BeatmapHelper.getTitle(beatmap));
            }
        }

        public void animateButton(boolean show) {
            Animation bodyAnim = new Animation(body);
            Animation arrowAnim = new Animation(arrow);

            if (show) {
                bodyAnim.moveY(0, -10);
                bodyAnim.fade(1, 0);

                arrowAnim.moveY(10, 0);
                arrowAnim.fade(0, 1);
            } else {
                bodyAnim.moveY(10, 0);
                bodyAnim.fade(0, 1);

                arrowAnim.moveY(0, -10);
                arrowAnim.fade(1, 0);
            }
            arrowAnim.rotation(180, 180);
            arrowAnim.duration(150);

            bodyAnim.runOnEnd(arrowAnim::play);
            bodyAnim.play(150);
        }

        protected void setVisibility(boolean bool) {
            view.setVisibility(bool ? View.VISIBLE : View.GONE);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static class UserBox {

        private final TopBar parent;
        private final ImageView avatar;
        private final TextView rank, name;

        //----------------------------------------------------------------------------------------//

        public UserBox(TopBar parent) {
            this.parent = parent;

            View body = parent.find("userBox");
            rank = parent.find("playerRank");
            name = parent.find("playerName");
            avatar = parent.find("avatar");

            TriangleEffectView triangles = parent.find("userBoxTriangles");
            triangles.setTriangleColor(0xFFFFFFFF);

            parent.bindTouchListener(body, UI.userProfile::altShow);
        }

        //----------------------------------------------------------------------------------------//

        public void loadUserData(boolean clear) {
            if (!parent.isShowing)
                return;

            AnimationTable.fadeOutIn(avatar, () -> avatar.setImageResource(R.drawable.default_avatar));

            AnimationTable.textChange(rank, Resources.str(R.string.top_bar_offline));
            AnimationTable.textChange(name, Config.getLocalUsername());

            if (online.isStayOnline() && !clear) {
                AnimationTable.textChange(name, online.getUsername());
                AnimationTable.textChange(rank, "#" + online.getRank());

                AnimationTable.fadeOutIn(avatar, () -> {
                    if (onlineHelper.getPlayerAvatar() != null) {
                        avatar.setImageDrawable(onlineHelper.getPlayerAvatar());
                    }
                });
            }
        }
    }
}
