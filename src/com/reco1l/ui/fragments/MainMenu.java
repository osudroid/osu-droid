package com.reco1l.ui.fragments;

import static android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT;
import static android.graphics.drawable.GradientDrawable.Orientation.RIGHT_LEFT;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.AsyncExec;
import com.reco1l.utils.Resources;
import com.reco1l.UI;
import com.reco1l.utils.ViewUtils;
import com.reco1l.utils.listeners.TouchListener;
import com.reco1l.view.LogoView;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public class MainMenu extends UIFragment {

    public static MainMenu instance;

    private LogoView logo;
    private CardView buttons;
    private View single, multi;

    private boolean
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
        buttons = find("buttonsLayout");

        single.setAlpha(0);
        multi.setAlpha(0);

        ViewUtils.size(logo, Resources.dimen(R.dimen.mainMenuLogoSize));
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

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (!isMenuAnimInProgress && !isMenuShowing) {
            isMenuAnimInProgress = true;
            showPassTime = 0;

            UI.topBar.show();

            Animation.of(logo)
                    .toSize(Resources.dimen(R.dimen.mainMenuSmallLogoSize))
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

            Animation.of(logo)
                    .toSize(Resources.dimen(R.dimen.mainMenuLogoSize))
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
                            Game.library.findBeatmap(Game.musicManager.getBeatmap());
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

    @Override
    protected void onUpdate(float secondsElapsed) {
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
