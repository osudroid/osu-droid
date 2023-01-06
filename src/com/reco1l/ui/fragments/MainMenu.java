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
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.execution.AsyncTask;
import com.reco1l.tables.Res;
import com.reco1l.UI;
import com.reco1l.utils.Views;
import com.reco1l.utils.TouchListener;
import com.reco1l.view.effects.ExpandEffect;
import com.reco1l.view.LogoView;
import com.reco1l.view.CircularSpectrum;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public final class MainMenu extends BaseFragment {

    public static MainMenu instance;

    private LogoView logo;
    private CardView buttons;
    private View single, multi;

    private boolean
            isMenuShowing = false,
            isMenuAnimInProgress = false;

    private int showPassTime = 0;

    //--------------------------------------------------------------------------------------------//

    public MainMenu() {
        super(Screens.Main);
    }


    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "mainM";
    }

    @Override
    protected int getLayout() {
        return R.layout.main_menu;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        isMenuShowing = false;
        showPassTime = 0;

        logo = find("logo");
        single = find("solo");
        multi = find("multi");
        buttons = find("buttonsLayout");

        CircularSpectrum spectrum = find("spectrum");
        spectrum.attachToLogo(logo);

        ExpandEffect expand = find("expand");
        expand.attachToLogo(logo);

        single.setAlpha(0);
        multi.setAlpha(0);

        Views.size(logo, Res.dimen(R.dimen.mainMenuLogoSize));
        Views.width(buttons, 0);

        bindTouch(logo, new TouchListener() {

            public BassSoundProvider getPressUpSound() {
                return Game.resourcesManager.getSound("menuhit");
            }

            public void onPressUp() {
                if (!isMenuShowing) {
                    showMenu();
                } else {
                    hideMenu(false);
                }
            }
        });

        bindTouch(single, new TouchListener() {

            public boolean useBorderlessEffect() {
                return true;
            }

            public void onPressUp() {
                onSingle();
            }
        });

        bindTouch(multi, new TouchListener() {

            public boolean useBorderlessEffect() {
                return true;
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
                    .toSize(Res.dimen(R.dimen.mainMenuSmallLogoSize))
                    .interpolate(Easing.InOutQuad)
                    .play(300);

            Animation.of(buttons)
                    .toWidth(Res.dimen(R.dimen.mainMenuButtonLayoutWidth))
                    .interpolate(Easing.InOutQuad)
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
                    .toSize(Res.dimen(R.dimen.mainMenuLogoSize))
                    .interpolate(Easing.InOutQuad)
                    .play(300);

            Animation.of(buttons)
                    .toWidth(0)
                    .interpolate(Easing.InOutQuad)
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

                    new AsyncTask() {
                        public void run() {
                            Game.activity.checkNewBeatmaps();
                            if (!Game.libraryManager.loadLibraryCache(Game.activity, true)) {
                                Game.libraryManager.scanLibrary(Game.activity);
                            }
                            Game.libraryManager.findBeatmap(Game.musicManager.getTrack().getBeatmap());
                        }

                        public void onComplete() {
                            Game.loaderScene.notifyComplete();
                        }
                    }.execute();
                })
                .toX(Res.dimen(R.dimen._80sdp))
                .toAlpha(0)
                .play(400);
    }

    private void onMulti() {
        ToastLogger.showText("WIP", false);
        hideMenu(false);
    }

    public void onExit() {
        if (isAdded()) {
            Animation.of(logo)
                    .runOnStart(() -> {
                        Game.platform.closeAllExcept(this);
                        unbindTouchHandlers();
                        hideMenu(false);
                    })
                    .toScale(1)
                    .toAlpha(0)
                    .play(3000);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onUpdate(float sec) {
        if (isMenuShowing) {
            if (showPassTime > 10000f) {
                hideMenu(false);
                showPassTime = 0;
            } else {
                showPassTime += sec * 1000f;
            }
        }
    }

    @Override
    public void close() {
        super.close();

        if (isAdded()) {
            isMenuShowing = false;
        }
    }
}
