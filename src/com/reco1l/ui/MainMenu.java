package com.reco1l.ui;

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
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;
import com.reco1l.utils.interfaces.UI;

import ru.nsu.ccfit.zuev.osu.MainScene;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public class MainMenu extends BaseLayout {

    public boolean
            isMenuShowing = false,
            menuAnimInProgress = false,
            isExitAnimInProgress = false;

    private CardView logo;
    private MenuButton play, settings, exit;
    private TriangleEffectView logoTriangles;

    private final ValueAnimator
            bounceUp,
            bounceDown,
            logoFadeOut,
            triangleSpeedUp,
            triangleSpeedDown;

    private ValueAnimator logoShow, logoHide;
    private LayoutParams logoParams;

    private int logoNormalSize, smallLogoSize, showPassTime = 0;

    //--------------------------------------------------------------------------------------------//

    public MainMenu() {
        bounceUp = ValueAnimator.ofFloat(1f, 0.9f);
        bounceDown = ValueAnimator.ofFloat(0.9f, 1f);
        triangleSpeedUp = ValueAnimator.ofFloat(1f, 8f);
        triangleSpeedDown = ValueAnimator.ofFloat(8f, 1f);
        logoFadeOut = ValueAnimator.ofFloat(1f, 0f);
    }

    @Override
    protected String getPrefix() {
        return "mainM";
    }

    @Override
    protected int getLayout() {
        return R.layout.main_menu;
    }

    //--------------------------------------------------------------------------------------------//

    private void loadAnimations() {

        // Logo bounce effect
        TimeInterpolator easeInOutQuad = EasingHelper.asInterpolator(Easing.InOutQuad);

        AnimatorUpdateListener logoBounce = val -> {
            logo.setScaleX((float) val.getAnimatedValue());
            logo.setScaleY((float) val.getAnimatedValue());
        };

        bounceUp.addUpdateListener(logoBounce);
        bounceUp.setInterpolator(easeInOutQuad);

        bounceDown.addUpdateListener(logoBounce);
        bounceDown.setInterpolator(easeInOutQuad);

        // Triangles speed up effect
        AnimatorUpdateListener triangleSpeed = val ->
                logoTriangles.setTriangleSpeed((float) val.getAnimatedValue());

        triangleSpeedUp.addUpdateListener(triangleSpeed);
        triangleSpeedDown.addUpdateListener(triangleSpeed);

        // Logo size effect
        AnimatorUpdateListener logoResize = val -> {
            logoParams.width = (int) val.getAnimatedValue();
            logoParams.height = (int) val.getAnimatedValue();
            logo.setLayoutParams(logoParams);
        };

        logoShow = ValueAnimator.ofInt(logoNormalSize, smallLogoSize);
        logoShow.setDuration(300);
        logoShow.setInterpolator(easeInOutQuad);
        logoShow.addUpdateListener(logoResize);

        logoHide = ValueAnimator.ofInt(smallLogoSize, logoNormalSize);
        logoHide.setDuration(300);
        logoHide.setInterpolator(easeInOutQuad);
        logoHide.addUpdateListener(logoResize);

        // Logo transition effect
        logoFadeOut.addUpdateListener(val -> logo.setAlpha((float) val.getAnimatedValue()));
        logoFadeOut.setInterpolator(easeInOutQuad);
        logoFadeOut.setDuration(200);
    }

    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        logoNormalSize = (int) Res.dimen(R.dimen.mainMenuLogoSize);
        smallLogoSize = (int) Res.dimen(R.dimen.mainMenuSmallLogoSize);

        logo = find("logo");
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

        logoParams = logo.getLayoutParams();
        logoParams.width = logoNormalSize;
        logoParams.height = logoNormalSize;
        logo.setLayoutParams(logoParams);

        play.setWidth(0);
        exit.setWidth(0);
        settings.setWidth(0);
        isMenuShowing = false;

        new ClickListener(logo)
                .soundEffect(resources.loadSound("menuhit", "sfx/menuhit.ogg", false))
                .touchEffect(false)
                .simple(() -> {
                    if (!isMenuShowing) {
                        showMenu();
                    } else {
                        hideMenu(false);
                    }
                });

        new ClickListener(play.button)
                .onlyOnce(true)
                .touchEffect(false)
                .simple(() -> {
                    Utils.setAccelerometerSign(global.getCamera().getRotation() == 0 ? 1 : -1);
                    global.getSongService().setGaming(true);
                    playTransitionAnim();
                });

        new ClickListener(exit.button)
                .touchEffect(false)
                .simple(() -> {
                    Utils.setAccelerometerSign(global.getCamera().getRotation() == 0 ? 1 : -1);
                    global.getMainScene().showExitDialog();
                });

        new ClickListener(settings.button)
                .touchEffect(false)
                .simple(() -> {
                    global.getSongService().setGaming(true);
                    mActivity.runOnUiThread(() -> new SettingsMenu().show());
                });
    }

    //--------------------------------------------------------------------------------------------//

    private void playTransitionAnim() {

        final OsuAsyncCallback playCallback = new OsuAsyncCallback() {
            public void run() {
                UI.loadingScene.show();
                mActivity.checkNewSkins();
                mActivity.checkNewBeatmaps();
                if (!library.loadLibraryCache(mActivity, true)) {
                    library.scanLibrary(mActivity);
                    System.gc();
                }
                global.getSongMenu().reload();
            }

            public void onComplete() {
                global.getMainScene().musicControl(MainScene.MusicOption.PLAY);
                UI.loadingScene.close();
                global.getEngine().setScene(global.getSongMenu().getScene());
                global.getSongMenu().select();
            }
        };

        logoFadeOut.addListener(new BaseAnimationListener() {
            @Override
            public void onAnimationEnd(Animator animation) {
                new AsyncTaskLoader().execute(playCallback);
            }
        });

        hideMenu(true);
    }

    public void playExitAnim() {
        mActivity.runOnUiThread(() -> {
            hideMenu(false);
            isExitAnimInProgress = true;
            logo.setOnTouchListener(null);
            logo.animate().rotation(-15).setDuration(3000).alpha(0).setStartDelay(160).start();
        });
    }

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (menuAnimInProgress || isMenuShowing)
            return;

        global.getMainScene().background.zoomIn();

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

    private void hideMenu(boolean isTransition) {
        if (menuAnimInProgress || !isMenuShowing)
            return;

        global.getMainScene().background.zoomOut(isTransition);

        logoHide.addListener(new BaseAnimationListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                menuAnimInProgress = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                if (isTransition)
                    logoFadeOut.start();
            }
        });
        logoHide.start();

        exit.hide(0);
        settings.hide(60);
        play.hide(120);

        showPassTime = 0;
    }

    //--------------------------------------------------------------------------------------------//

    public void update(float secondsElapsed, boolean allowBounce, float bpm) {
        if (!isShowing)
            return;

        if (isMenuShowing) {
            if (showPassTime > 10000f) {
                mActivity.runOnUiThread(() -> hideMenu(false));
            } else {
                showPassTime += secondsElapsed * 1000f;
            }
        }

        if (logo == null || !allowBounce || isExitAnimInProgress)
            return;

        long upTime = (long) (bpm * 0.07f);
        long downTime = (long) (bpm * 0.9f);

        bounceUp.setDuration(upTime);
        triangleSpeedUp.setDuration(upTime);

        bounceDown.setDuration(downTime);
        triangleSpeedDown.setDuration(downTime);

        bounceDown.setStartDelay(upTime);
        triangleSpeedDown.setStartDelay(upTime);

        mActivity.runOnUiThread(() -> {
            bounceUp.start();
            triangleSpeedUp.start();
            bounceDown.start();
            triangleSpeedDown.start();
        });
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

                showAnim = ValueAnimator.ofInt(0, (int) Res.dimen(R.dimen.mainMenuButtonSize));
                showAnim.addUpdateListener(updateListener);
                showAnim.setInterpolator(interpolator);
                showAnim.setDuration(100);

                hideAnim = ValueAnimator.ofInt((int) Res.dimen(R.dimen.mainMenuButtonSize), 0);
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

            if (delay == 120) {
                showAnim.addListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mainMenu.menuAnimInProgress = false;
                        mainMenu.isMenuShowing = true;
                    }
                });
            }

            new com.reco1l.utils.Animator(view).fade(0, 1).delay(delay).play(100);
            new com.reco1l.utils.Animator(button).fade(0, 1).delay(delay + 100).play(100);

            showAnim.setStartDelay(delay);
            showAnim.start();
        }

        public void hide(long delay) {

            if (delay == 120) {
                hideAnim.addListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mainMenu.menuAnimInProgress = false;
                        mainMenu.isMenuShowing = false;
                    }
                });
            }

            new com.reco1l.utils.Animator(view).fade(1, 0).delay(delay).play(100);
            new com.reco1l.utils.Animator(button).fade(1, 0)
                    .delay(delay - 100 <= 0 ? 0 : delay - 100).play(100);

            hideAnim.setStartDelay(delay);
            hideAnim.start();
        }
    }
}
