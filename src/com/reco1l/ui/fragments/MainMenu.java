package com.reco1l.ui.fragments;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;
import androidx.core.math.MathUtils;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.game.LibraryImport;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.data.DialogTable;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Animation.ValueAnimationListener;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.UI;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.listeners.TouchListener;
import com.reco1l.view.SpectrumView;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public class MainMenu extends UIFragment {

    public static MainMenu instance;

    public boolean
            isMenuShowing = false,
            menuAnimInProgress = false,
            isExitAnimInProgress = false;

    private boolean isKiai = false;

    private CardView logo;
    private View logoInnerRect;
    private MenuButton play, settings, exit;
    private TriangleEffectView triangles1, triangles2;

    private ValueAnimationListener<Float> triangleListener;
    private Animation logoShow, logoHide;

    private int showPassTime = 0;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "mainM";
    }

    @Override
    protected int getLayout() {
        return R.layout.main_menu;
    }

    @Override
    protected Screens getParent() {
        return Screens.MAIN;
    }

    //--------------------------------------------------------------------------------------------//

    public void setLogoKiai(boolean bool) {
        this.isKiai = bool;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        logo = find("logo");
        logoInnerRect = find("logoRect");
        triangles1 = find("triangles1");
        triangles2 = find("triangles2");

        play = new MenuButton(
                find("play"),
                find("playView"));
        exit = new MenuButton(
                find("exit"),
                find("exitView"));
        settings = new MenuButton(
                find("settings"),
                find("settingsView"));

        loadAnimations();
        triangles1.setTriangleColor(Resources.color(R.color.mainMenuTriangles1));
        triangles2.setTriangleColor(Resources.color(R.color.mainMenuTriangles2));
        logoInnerRect.setAlpha(0);

        ViewUtils.size(logo, (int) Resources.dimen(R.dimen.mainMenuLogoSize));

        play.setWidth(0);
        exit.setWidth(0);
        settings.setWidth(0);

        bindTouchListener(logo, new TouchListener() {
            public BassSoundProvider getClickSound() {
                return resources.getSound("menuhit");
            }

            public boolean hasTouchEffect() {
                return false;
            }

            public void onPressUp() {
                if (!isMenuShowing) {
                    showMenu();
                } else {
                    hideMenu();
                }
            }
        });

        bindTouchListener(play.view, new TouchListener() {
            public boolean isOnlyOnce() {
                return true;
            }

            public void onPressUp() {
                Utils.setAccelerometerSign(global.getCamera().getRotation() == 0 ? 1 : -1);
                global.getSongService().setGaming(true);
                playTransitionAnim();
            }
        });

        bindTouchListener(exit.view, () -> {
            Utils.setAccelerometerSign(global.getCamera().getRotation() == 0 ? 1 : -1);
            new Dialog(DialogTable.exit()).show();
        });

        bindTouchListener(settings.view, UI.settingsPanel::altShow);
    }

    //--------------------------------------------------------------------------------------------//

    private void playTransitionAnim() {

        new Animation(rootView)
                //.runOnStart(() -> global.getMainScene().spectrum.clear(true))
                .runOnEnd(() -> {
                    Game.songMenu.load();
                    Game.songMenu.show();
                    Game.musicManager.play();
                    new AsyncExec() {
                        @Override
                        public void run() {
                            LibraryImport.scan(true);
                        }
                    }.execute();
                    isMenuShowing = false;
                })
                .moveY(0, Resources.dimen(R.dimen._80sdp))
                .fade(1, 0)
                .play(400);
    }

    public void playExitAnim() {
        mActivity.runOnUiThread(() -> {
            platform.closeAllExcept(this);
            if (!isShowing)
                return;

            hideMenu();
            isExitAnimInProgress = true;
            logo.setOnTouchListener(null);
            new Animation(logo).scale(logo.getScaleX(), 1).fade(1, 0)
                    .delay(160).play(3000);
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (!menuAnimInProgress && !isMenuShowing) {
            logoShow.play();

            play.show(0);
            settings.show(60);
            exit.show(120);
        }
    }

    private void hideMenu() {
        if (!menuAnimInProgress && isMenuShowing) {
            logoHide.play();

            exit.hide(0);
            settings.hide(60);
            play.hide(120);

            showPassTime = 0;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void loadAnimations() {
        // Triangles
        triangleListener = value -> {
            triangles1.setTriangleSpeed(value);
            triangles2.setTriangleSpeed(value);
        };

        // Logo size effect
        int logoNormalSize = (int) Resources.dimen(R.dimen.mainMenuLogoSize);
        int smallLogoSize = (int) Resources.dimen(R.dimen.mainMenuSmallLogoSize);

        ValueAnimationListener<Integer> listener = value -> ViewUtils.size(logo, value);

        logoShow = new Animation().ofInt(logoNormalSize, smallLogoSize)
                .runOnUpdate(listener)
                .interpolator(Easing.InOutQuad)
                .runOnStart(() -> {
                    menuAnimInProgress = true;
                    UI.topBar.show();
                })
                .duration(300);

        logoHide = new Animation().ofInt(smallLogoSize, logoNormalSize)
                .runOnUpdate(listener)
                //.interpolator(Easing.InOutQuad)
                .runOnStart(() -> {
                    menuAnimInProgress = true;
                    UI.topBar.close();
                })
                .duration(300);
    }

    public void onBeatUpdate(float beatLength) {
        if (logo == null || isExitAnimInProgress)
            return;

        if (logoInnerRect != null && isKiai) {

            Animation fadeOut = new Animation(logoInnerRect).fade(0.2f, 0f)
                    .cancelPending(false)
                    .duration((long) (beatLength * 0.9f));

            new Animation(logoInnerRect).fade(0f, 0.2f)
                    .runOnEnd(fadeOut::play)
                    .play((long) (beatLength * 0.7f));
        }

        if (triangles1 != null) {

            Animation speedDown = new Animation().ofFloat(isKiai ? 14f : 8f, 1f)
                    .runOnUpdate(triangleListener)
                    .duration((long) (beatLength / 2f));

            new Animation().ofFloat(1f, isKiai ? 14f : 8f)
                    .runOnUpdate(triangleListener)
                    .runOnEnd(speedDown::play)
                    .play((long) (beatLength / 6f));
        }
    }

    @Override
    protected void onUpdate(float secondsElapsed) {
        if (!isShowing)
            return;

        float level = Game.songService.getLevel();
        float peak = Math.max(0.9f, 0.9f + level);

        UI.debugOverlay.logo_scale = peak;

        if (logo != null) {
            if (Game.songService.getStatus() == Status.PLAYING) {
                ViewUtils.scale(logo, peak);
            }
        }

        if (isMenuShowing) {
            if (showPassTime > 10000f) {
                mActivity.runOnUiThread(this::hideMenu);
            } else {
                showPassTime += secondsElapsed * 1000f;
            }
        }
    }

    @Override
    public void close() {
        super.close();
        if (isShowing)
            isMenuShowing = false;
    }

    //--------------------------------------------------------------------------------------------//

    private static class MenuButton {

        public final View view;
        public final LinearLayout button;
        public final LayoutParams params;

        private ValueAnimator showAnim, hideAnim;

        private MenuButton(LinearLayout button, View view) {
            this.view = view;
            this.button = button;

            params = button.getLayoutParams();

            // Loading animations
            if (showAnim == null || hideAnim == null) {
                TimeInterpolator interpolator = EasingHelper.asInterpolator(Easing.InOutQuad);

                AnimatorUpdateListener updateListener = val -> {
                    params.width = (int) val.getAnimatedValue();
                    button.setLayoutParams(params);
                };

                showAnim = ValueAnimator.ofInt(0, (int) Resources.dimen(R.dimen.mainMenuButtonSize));
                showAnim.addUpdateListener(updateListener);
                showAnim.setInterpolator(interpolator);
                showAnim.setDuration(100);

                hideAnim = ValueAnimator.ofInt((int) Resources.dimen(R.dimen.mainMenuButtonSize), 0);
                hideAnim.addUpdateListener(updateListener);
                hideAnim.setInterpolator(interpolator);
                hideAnim.setDuration(100);
            }

            button.setAlpha(0);
            view.setAlpha(0);
        }

        public void setWidth(int width) {
            params.width = width;
            button.setLayoutParams(params);
        }

        public void show(long delay) {
            view.setAlpha(1);

            if (delay == 120) {
                showAnim.removeAllListeners();
                showAnim.addListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        UI.mainMenu.menuAnimInProgress = false;
                        UI.mainMenu.isMenuShowing = true;
                    }
                });
            }

            button.animate()
                    .setStartDelay(delay + 100)
                    .setDuration(100)
                    .alpha(1)
                    .start();

            showAnim.setStartDelay(delay);
            showAnim.start();
        }

        public void hide(long delay) {

            hideAnim.removeAllListeners();
            hideAnim.addListener(new BaseAnimationListener() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    view.setAlpha(0);
                    if (delay == 120) {
                        UI.mainMenu.menuAnimInProgress = false;
                        UI.mainMenu.isMenuShowing = false;
                    }
                }
            });

            button.animate()
                    .setStartDelay(delay - 100 <= 0 ? 0 : delay - 100)
                    .setDuration(80)
                    .alpha(0)
                    .start();

            hideAnim.setStartDelay(delay);
            hideAnim.start();
        }
    }
}
