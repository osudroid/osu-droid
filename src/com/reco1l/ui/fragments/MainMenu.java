package com.reco1l.ui.fragments;

import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.animation.Animator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.enums.Scenes;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.data.tables.DialogTable;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.UI;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.listeners.TouchListener;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public class MainMenu extends UIFragment {

    public static MainMenu instance;

    public boolean
            isMenuShowing = false,
            menuAnimInProgress = false,
            isExitAnimInProgress = false;

    private boolean
            isKiai = false,
            wasPlaying = false,
            clearAnimInProgress = false,
            isBounceAnimInProgress = false;

    private CardView logo;
    private View logoInnerRect;
    private MenuButton play, settings, exit;
    private TriangleEffectView logoTriangles;

    private Animation
            logoBounceAnim,
            triangleSpeedUp,
            triangleSpeedDown;

    private ValueAnimator logoShow, logoHide;
    private LayoutParams logoParams;

    private int
            logoNormalSize,
            smallLogoSize,
            showPassTime = 0;

    private float currentScale = 1f;

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
    protected Scenes getParent() {
        return Scenes.MAIN;
    }

    //--------------------------------------------------------------------------------------------//

    private void loadAnimations() {
        logoBounceAnim = new Animation(logo)
                .runOnStart(() -> isBounceAnimInProgress = true)
                .runOnEnd(() -> isBounceAnimInProgress = false);

        // Logo bounce effect
        TimeInterpolator easeInOutQuad = EasingHelper.asInterpolator(Easing.InOutQuad);

        // Triangles speed up effect
        triangleSpeedDown = new Animation().ofFloat(2f, 10f)
                .runOnUpdate(logoTriangles::setTriangleSpeed);

        triangleSpeedUp = new Animation().ofFloat(10f, 2f)
                .runOnUpdate(logoTriangles::setTriangleSpeed)
                .runOnEnd(triangleSpeedDown::play);

        // Logo size effect
        AnimatorUpdateListener logoResize = val -> {
            logoParams.width = (int) val.getAnimatedValue();
            logoParams.height = (int) val.getAnimatedValue();
            logo.setLayoutParams(logoParams);
        };

        logoShow = ValueAnimator.ofInt(logoNormalSize, smallLogoSize);
        logoShow.setDuration(300);
        logoShow.setInterpolator(easeInOutQuad);
        logoShow.removeAllUpdateListeners();
        logoShow.addUpdateListener(logoResize);

        logoHide = ValueAnimator.ofInt(smallLogoSize, logoNormalSize);
        logoHide.setDuration(300);
        logoHide.setInterpolator(easeInOutQuad);
        logoHide.removeAllUpdateListeners();
        logoHide.addUpdateListener(logoResize);
    }

    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        logoNormalSize = (int) Resources.dimen(R.dimen.mainMenuLogoSize);
        smallLogoSize = (int) Resources.dimen(R.dimen.mainMenuSmallLogoSize);

        logo = find("logo");
        logoInnerRect = find("logoRect");
        logoTriangles = find("logoTriangles");

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
        logoTriangles.setTriangleColor(0xFFFFFFFF);
        logoInnerRect.setAlpha(0);

        logoParams = logo.getLayoutParams();
        logoParams.width = logoNormalSize;
        logoParams.height = logoNormalSize;
        logo.setLayoutParams(logoParams);

        play.setWidth(0);
        exit.setWidth(0);
        settings.setWidth(0);

        bindTouchListener(logo, new TouchListener() {
            public BassSoundProvider getClickSound() { return resources.getSound("menuhit"); }
            public boolean hasTouchEffect() { return false; }

            public void onPressUp() {
                if (!isMenuShowing) {
                    showMenu();
                } else {
                    hideMenu();
                }
            }
        });

        bindTouchListener(play.view, new TouchListener() {
            public boolean isOnlyOnce() { return true; }

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

    private final OsuAsyncCallback asyncCallback = new OsuAsyncCallback() {
        public void run() {
            engine.setScene(UI.loadingScene.scene);
            mActivity.checkNewSkins();
            mActivity.checkNewBeatmaps();
            if (!library.loadLibraryCache(mActivity, true)) {
                library.scanLibrary(mActivity);
                System.gc();
            }
            global.getSongMenu().reload();
        }

        public void onComplete() {
            Game.musicManager.play();
            UI.loadingScene.complete(() -> engine.setScene(global.getSongMenu()));
        }
    };

    private void playTransitionAnim() {

        new Animation(rootView)
                .runOnStart(() -> {
                    global.getMainScene().background.zoomOut();
                    global.getMainScene().spectrum.clear(true);
                })
                .runOnEnd(() -> {
                    new AsyncTaskLoader().execute(asyncCallback);
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
        if (menuAnimInProgress || isMenuShowing)
            return;

        global.getMainScene().background.zoomIn();
        global.getMainScene().background.dimIn();

        logoShow.removeAllListeners();
        logoShow.addListener(new BaseAnimationListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuAnimInProgress = true;
            }
        });
        logoShow.start();

        play.show(0);
        settings.show(60);
        exit.show(120);
    }

    private void hideMenu() {
        if (menuAnimInProgress || !isMenuShowing)
            return;

        global.getMainScene().background.zoomOut();
        global.getMainScene().background.dimOut();

        logoHide.removeAllListeners();
        logoHide.addListener(new BaseAnimationListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuAnimInProgress = true;
            }
        });
        logoHide.start();

        exit.hide(0);
        settings.hide(60);
        play.hide(120);

        showPassTime = 0;
    }

    //--------------------------------------------------------------------------------------------//

    public void setLogoKiai(boolean bool) {
        this.isKiai = bool;
    }

    public void onBeatUpdate(float beatLength, final int beat) {
        if (logo == null || isExitAnimInProgress)
            return;

        long upTime = (long) (beatLength * 0.07f);
        long downTime = (long) (beatLength * 0.9f);

        if (logoBounceAnim != null) {
            logoBounceAnim.scale(currentScale, currentScale * 0.99f).play((long) (beatLength * 0.8f));
        }

        if (logoInnerRect != null && isKiai) {
            Animation fadeOut = new Animation(logoInnerRect).fade(0.2f, 0f);

            new Animation(logoInnerRect).fade(0f, 0.2f)
                    .runOnEnd(() -> fadeOut.cancelPending(false).play(downTime))
                    .play(upTime);
        }

        // Nullability checks to avoid NPE before views initialization
        if (logoTriangles != null) {
            if (triangleSpeedUp != null && triangleSpeedDown != null) {
                triangleSpeedUp.duration(upTime);
                triangleSpeedDown.duration(downTime);

                if (beat == 0) {
                    triangleSpeedUp.play();
                }
            }
        }
    }

    @Override
    protected void onUpdate(float secondsElapsed) {
        if (!isShowing)
            return;

        float multiplier = Game.songService.getLevel() * 2f;
        currentScale = Math.min(0.93f + (0.07f * multiplier), 1f);

        if (logo != null) {
            if (Game.songService.getStatus() == Status.PLAYING) {
                wasPlaying = true;
                if (!isBounceAnimInProgress && !clearAnimInProgress) {
                    logo.animate().cancel();
                    ViewUtils.scale(logo, currentScale);
                }
            } else if (wasPlaying) {
                wasPlaying = false;
                new Animation(logo).scale(currentScale, 0.93f)
                        .runOnStart(() -> clearAnimInProgress = true)
                        .runOnEnd(() -> clearAnimInProgress = false)
                        .play(100);
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
