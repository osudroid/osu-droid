package com.reco1l.ui.scenes.main.fragments;

import android.view.View;
import android.widget.LinearLayout;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;
import com.reco1l.framework.input.TouchListener;
import com.reco1l.tools.Views;
import com.reco1l.view.BadgeTextView;
import com.reco1l.ui.elements.LogoView;

import com.reco1l.view.effects.ExpandEffect;
import com.reco1l.view.effects.CircularSpectrum;

import com.rimu.BuildConfig;
import com.rimu.R;

// Created by Reco1l on 9/7/22 18:09

public final class MainMenu extends BaseFragment {

    public static final MainMenu instance = new MainMenu();

    private LogoView mLogo;
    private LinearLayout mButtonLayout;
    private BadgeTextView mVersionText;

    private View
            mPlayButton,
            mExploreButton,
            mButtonsBackground;

    private boolean
            mIsMenuShowing = false,
            mIsConcurrentAnimation = false;

    //--------------------------------------------------------------------------------------------//

    public MainMenu() {
        super(Scenes.main);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "main";
    }

    @Override
    protected int getLayout() {
        return R.layout.main_menu;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mIsMenuShowing = false;

        mLogo = find("logo");
        mPlayButton = find("play");
        mVersionText = find("author");
        mExploreButton = find("explore");
        mButtonLayout = find("buttonsLayout");
        mButtonsBackground = find("buttonsBackground");

        mVersionText.setText(str(R.string.app_name) + " - " + BuildConfig.VERSION_NAME);

        CircularSpectrum spectrum = find("spectrum");
        spectrum.attachTo(mLogo);

        ExpandEffect expand = find("expand");
        expand.attachTo(mLogo);

        Views.size(mLogo, dimen(R.dimen.mainMenuLogoSize));
        Views.height(mButtonsBackground, 0);

        mLogo.post(() ->
                mLogo.setX(getWidth() / 2f - mLogo.getWidth() / 2f)
        );

        bindTouch(mLogo, new TouchListener() {

            public String getPressUpSound() {
                return "menuhit";
            }

            public void onPressUp() {
                if (!mIsMenuShowing) {
                    showMenu();
                } else {
                    hideMenu();
                }
            }
        });

        bindTouch(mPlayButton, this::onSingle);
        bindTouch(mExploreButton, this::onExplore);
    }

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (!mIsConcurrentAnimation && !mIsMenuShowing) {
            mIsConcurrentAnimation = true;

            UI.topBar.show();

            Animation.of(mLogo)
                    .toPosX((float) sdp(48))
                    .toSize(dimen(R.dimen.mainMenuSmallLogoSize))
                    .interpolate(Easing.InOutQuad)
                    .runOnEnd(() -> mIsConcurrentAnimation = false)
                    .play(200);

            Animation.of(mButtonsBackground)
                    .toHeight(sdp(90))
                    .toAlpha(1)
                    .play(200);

            mIsMenuShowing = true;
        }
    }

    private void hideMenu() {
        if (!mIsConcurrentAnimation && mIsMenuShowing) {
            mIsConcurrentAnimation = true;

            UI.topBar.close();
            int maxSize = dimen(R.dimen.mainMenuLogoSize);

            Animation.of(mLogo)
                    .toPosX((float) getWidth() / 2 - maxSize / 2)
                    .toSize(maxSize)
                    .interpolate(Easing.InOutQuad)
                    .runOnEnd(() -> mIsConcurrentAnimation = false)
                    .play(200);

            Animation.of(mButtonsBackground)
                    .toHeight(0)
                    .toAlpha(0)
                    .play(200);

            mIsMenuShowing = false;
        }
    }

    //--------------------------------------------------------------------------------------------//

    private void onSingle() {
        Animation.of(rootView)
                .runOnEnd(Scenes.selector::show)
                .toY(sdp(40))
                .toAlpha(0)
                .play(200);
    }

    private void onExplore() {
        Animation.of(rootView)
                .runOnEnd(Scenes.listing::show)
                .toY(sdp(40))
                .toAlpha(0)
                .play(200);
    }

    public void onExit() {
        if (isAdded()) {
            Animation.of(mLogo)
                    .runOnStart(() -> {
                        Game.platform.onExit();
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
    protected void onEngineUpdate(float pSecElapsed) {
        mButtonLayout.setX(mLogo.getX() + mLogo.getWidth() + sdp(8));
    }

    @Override
    public void close() {
        super.close();

        if (isAdded()) {
            mIsMenuShowing = false;
        }
    }
}
