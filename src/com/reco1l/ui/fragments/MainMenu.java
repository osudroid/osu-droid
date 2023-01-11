package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.LinearLayout;

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
import com.reco1l.view.effects.CircularSpectrum;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public final class MainMenu extends BaseFragment {

    public static MainMenu instance;

    private LogoView logo;
    private LinearLayout buttonsLayout;
    private View single, multi, buttonsBackground;

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
        buttonsLayout = find("buttonsLayout");
        buttonsBackground = find("buttonsBackground");

        CircularSpectrum spectrum = find("spectrum");
        spectrum.attachToLogo(logo);

        ExpandEffect expand = find("expand");
        expand.attachToLogo(logo);

        Views.size(logo, Res.dimen(R.dimen.mainMenuLogoSize));
        Views.height(buttonsBackground, 0);
        Views.height(buttonsLayout, 0);

        Views.margins(buttonsLayout).left(Res.dimen(R.dimen.mainMenuSmallLogoSize) / 3);

        logo.post(() ->
                logo.setX(getWidth() / 2f - logo.getWidth() / 2f)
        );

        bindTouch(logo, new TouchListener() {

            public BassSoundProvider getPressUpSound() {
                return Game.resourcesManager.getSound("menuhit");
            }

            public void onPressUp() {
                if (!isMenuShowing) {
                    showMenu();
                } else {
                    hideMenu();
                }
            }
        });

        bindTouch(single, this::onSingle);
        bindTouch(multi, this::onMulti);
    }

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (!isMenuAnimInProgress && !isMenuShowing) {
            isMenuAnimInProgress = true;
            showPassTime = 0;

            UI.topBar.show();

            Animation.of(logo)
                    .toPosX((float) Res.sdp(48))
                    .toSize(Res.dimen(R.dimen.mainMenuSmallLogoSize))
                    .interpolate(Easing.InOutQuad)
                    .runOnEnd(() -> isMenuAnimInProgress = false)
                    .play(200);

            Animation.of(buttonsLayout, buttonsBackground)
                    .toHeight(Res.dimen(R.dimen.mainMenuButtonHeight))
                    .toAlpha(1)
                    .play(200);

            isMenuShowing = true;
        }
    }

    private void hideMenu() {
        if (!isMenuAnimInProgress && isMenuShowing) {
            isMenuAnimInProgress = true;

            UI.topBar.close();
            int maxSize = Res.dimen(R.dimen.mainMenuLogoSize);

            Animation.of(logo)
                    .toPosX((float) getWidth() / 2 - maxSize / 2)
                    .toSize(maxSize)
                    .interpolate(Easing.InOutQuad)
                    .runOnEnd(() -> isMenuAnimInProgress = false)
                    .play(200);

            Animation.of(buttonsLayout, buttonsBackground)
                    .toHeight(0)
                    .toAlpha(0)
                    .play(200);

            isMenuShowing = false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void onSingle() {
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
                        }

                        public void onComplete() {
                            Game.loaderScene.notifyComplete();
                        }
                    }.execute();
                })
                .toY(Res.sdp(40))
                .toAlpha(0)
                .play(200);
    }

    private void onMulti() {
        ToastLogger.showText("WIP", false);
        hideMenu();
    }

    public void onExit() {
        if (isAdded()) {
            Animation.of(logo)
                    .runOnStart(() -> {
                        Game.platform.closeAllExcept(this);
                        unbindTouchHandlers();
                        hideMenu();
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
                hideMenu();
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
