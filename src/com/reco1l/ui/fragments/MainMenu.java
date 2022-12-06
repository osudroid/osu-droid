package com.reco1l.ui.fragments;

import static android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT;
import static android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.TriangleEffectView;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.UI;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.listeners.TouchListener;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public class MainMenu extends UIFragment {

    public static MainMenu instance;

    private CardView logo, buttons;
    private View logoEffect, single, multi;

    private TriangleEffectView triangles1, triangles2;

    private Animation
            logoEffectIn,
            logoEffectOut,
            triangleSpeedIn,
            triangleSpeedOut;

    private Animation.UpdateListener logoUpdateListener;

    private boolean
            isKiai = false,
            isMenuShowing = false,
            isMenuAnimInProgress = false;

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
        return Screens.Main;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        isMenuShowing = false;
        showPassTime = 0;

        setDismissMode(false, false);

        logo = find("logo");
        single = find("solo");
        multi = find("multi");
        logoEffect = find("logoRect");
        buttons = find("buttonsLayout");
        triangles1 = find("triangles1");
        triangles2 = find("triangles2");

        loadAnimations();

        triangles1.setTriangleColor(Resources.color(R.color.mainMenuTriangles1));
        triangles2.setTriangleColor(Resources.color(R.color.mainMenuTriangles2));

        logoEffect.setAlpha(0);
        single.setAlpha(0);
        multi.setAlpha(0);

        int logoSize = Resources.dimen(R.dimen.mainMenuLogoSize);

        ViewUtils.size(logo, logoSize);
        ViewUtils.width(buttons, 0);

        bindTouchListener(logo, new TouchListener() {

            public BassSoundProvider getClickSound() {
                return Game.resources.getSound("menuhit");
            }

            public void onPressUp() {
                if (!isMenuShowing) {
                    showMenu();
                } else {
                    hideMenu(false);
                }
            }
        });

        int[] gradient = {Color.WHITE, Color.TRANSPARENT};

        bindTouchListener(single, new TouchListener() {

            public GradientDrawable getCustomTouchEffect() {
                return new GradientDrawable(LEFT_RIGHT, gradient);
            }

            public void onPressUp() {
                onSingle();
            }
        });

        bindTouchListener(multi, new TouchListener() {

            public GradientDrawable getCustomTouchEffect() {
                return new GradientDrawable(RIGHT_LEFT, gradient);
            }

            public void onPressUp() {
                onMulti();
            }
        });
    }

    private void loadAnimations() {
        // Logo
        logoEffectIn = Animation.of(logoEffect).toAlpha(0.2f);
        logoEffectOut = Animation.of(logoEffect).toAlpha(0);

        // Triangles
        Animation.UpdateListener onUpdate = value -> {
            float speed = (float) value;

            if (isKiai) {
                speed *= 2f;
            }
            triangles1.setTriangleSpeed(speed);
            triangles2.setTriangleSpeed(speed);
        };

        triangleSpeedIn = Animation.ofFloat(1f, 8f).runOnUpdate(onUpdate);
        triangleSpeedOut = Animation.ofFloat(8f, 1f).runOnUpdate(onUpdate);

        logoUpdateListener = value -> {
            int size = (int) value;

            ViewUtils.size(logo, size);
            logo.setRadius(size / 2f);
        };
    }

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (!isMenuAnimInProgress && !isMenuShowing) {
            isMenuAnimInProgress = true;
            showPassTime = 0;

            UI.topBar.show();

            Animation.ofInt(logo.getWidth(), Resources.dimen(R.dimen.mainMenuSmallLogoSize))
                    .runOnUpdate(logoUpdateListener)
                    .interpolator(Easing.InOutQuad)
                    .play(300);

            Animation.of(buttons)
                    .toWidth(Resources.dimen(R.dimen.mainMenuButtonLayoutWidth))
                    .interpolator(Easing.InOutQuad)
                    .play(300);

            Animation.of(single, multi)
                    .runOnEnd(() -> isMenuAnimInProgress = false)
                    .toAlpha(1)
                    .play(300);

            isMenuShowing = true;
        }
    }

    private void hideMenu(boolean isTransition) {
        if (!isMenuAnimInProgress && isMenuShowing) {
            isMenuAnimInProgress = true;

            if (!isTransition) {
                UI.topBar.close();
            }

            Animation.ofInt(logo.getWidth(), Resources.dimen(R.dimen.mainMenuLogoSize))
                    .runOnUpdate(logoUpdateListener)
                    .interpolator(Easing.InOutQuad)
                    .play(300);

            Animation.of(buttons)
                    .toWidth(0)
                    .interpolator(Easing.InOutQuad)
                    .play(300);

            Animation.of(single, multi)
                    .runOnEnd(() -> isMenuAnimInProgress = false)
                    .toAlpha(0)
                    .play(300);

            isMenuShowing = false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void onSingle() {
        hideMenu(true);

        Animation.of(rootView)
                .runOnEnd(() -> {
                    Game.loaderScene.show();
                    Game.loaderScene.runOnComplete(() -> {
                        Game.selectorScene.show();
                        Game.musicManager.play();
                    });

                    new AsyncExec() {
                        public void run() {
                            Game.activity.checkNewBeatmaps();
                            if (!Game.library.loadLibraryCache(Game.activity, true)) {
                                Game.library.scanLibrary(Game.activity);
                            }
                        }

                        public void onComplete() {
                            Game.loaderScene.notifyComplete();
                        }
                    }.execute();
                })
                .toX(Resources.dimen(R.dimen._80sdp))
                .toAlpha(0)
                .play(400);
    }

    private void onMulti() {
        ToastLogger.showText("WIP", false);
        hideMenu(false);
    }

    public void onExit() {
        if (isShowing) {
            Animation.of(logo)
                    .runOnStart(() -> {
                        Game.platform.closeAllExcept(this);
                        unbindTouchListeners();
                        hideMenu(false);
                    })
                    .toScale(1)
                    .toAlpha(0)
                    .play(3000);
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void setLogoKiai(boolean bool) {
        this.isKiai = bool;
    }

    //--------------------------------------------------------------------------------------------//

    public void onBeatUpdate(float beatLength) {
        if (!isShowing) {
            return;
        }

        long in = (long) (beatLength * 0.07f);
        long out = (long) (beatLength * 0.9f);

        if (Game.musicManager.isPlaying()) {
            if (isKiai) {
                logoEffectOut.duration(out);
                logoEffectIn.runOnEnd(logoEffectOut::play).play(in);
            }
        }

        if (triangleSpeedOut != null && triangleSpeedIn != null) {
            triangleSpeedOut.delay(in).play(out);
            triangleSpeedIn.play(in);
        }
    }

    @Override
    protected void onUpdate(float secondsElapsed) {

        float level = Game.songService.getLevel();
        float peak = Math.max(0.9f, 0.9f + level);

        UI.debugOverlay.logo_scale = peak;

        if (Game.musicManager.isPlaying()) {
            ViewUtils.scale(logo, peak);
        }

        if (isMenuShowing) {
            if (showPassTime > 10000f) {
                hideMenu(false);
                showPassTime = 0;
            } else {
                showPassTime += secondsElapsed * 1000f;
            }
        }
    }

    @Override
    public void close() {
        super.close();

        if (isShowing) {
            isMenuShowing = false;
        }
    }
}
