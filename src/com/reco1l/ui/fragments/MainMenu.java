package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.LinearLayout;

import com.edlplan.framework.easing.Easing;
import com.reco1l.global.Game;
import com.reco1l.global.UI;
import com.reco1l.global.Scenes;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.TouchListener;
import com.reco1l.utils.Views;
import com.reco1l.view.BadgeTextView;
import com.reco1l.view.custom.LogoView;

import com.reco1l.view.effects.ExpandEffect;
import com.reco1l.view.effects.CircularSpectrum;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 9/7/22 18:09

public final class MainMenu extends BaseFragment {

    public static final MainMenu instance = new MainMenu();

    private LogoView mLogo;
    private LinearLayout mButtonLayout;
    private BadgeTextView mVersionText;

    private View
            mSoloButton,
            mMultiButton,
            mButtonsBackground;

    private boolean
            mIsMenuShowing = false,
            mIsConcurrentAnimation = false;

    private int mShowTime = 0;

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
        mShowTime = 0;

        mLogo = find("logo");
        mSoloButton = find("solo");
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
        Views.height(mButtonLayout, 0);

        Views.margins(mButtonLayout).left(dimen(R.dimen.mainMenuSmallLogoSize) / 3);

        mLogo.post(() ->
                mLogo.setX(getWidth() / 2f - mLogo.getWidth() / 2f)
        );

        bindTouch(mLogo, new TouchListener() {

            public BassSoundProvider getPressUpSound() {
                return Game.resourcesManager.getSound("menuhit");
            }

            public void onPressUp() {
                if (!mIsMenuShowing) {
                    showMenu();
                } else {
                    hideMenu();
                }
            }
        });

        bindTouch(mSoloButton, this::onSingle);
        bindTouch(mMultiButton, this::onMulti);
    }

    //--------------------------------------------------------------------------------------------//

    private void showMenu() {
        if (!mIsConcurrentAnimation && !mIsMenuShowing) {
            mIsConcurrentAnimation = true;
            mShowTime = 0;

            UI.topBar.show();

            Animation.of(mLogo)
                    .toPosX((float) sdp(48))
                    .toSize(dimen(R.dimen.mainMenuSmallLogoSize))
                    .interpolate(Easing.InOutQuad)
                    .runOnEnd(() -> mIsConcurrentAnimation = false)
                    .play(200);

            Animation.of(mButtonLayout, mButtonsBackground)
                    .toHeight(dimen(R.dimen.mainMenuButtonHeight))
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

            Animation.of(mButtonLayout, mButtonsBackground)
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
        if (mIsMenuShowing) {
            if (mShowTime > 10000f) {
                hideMenu();
                mShowTime = 0;
            } else {
                mShowTime += pSecElapsed * 1000f;
            }
        }
    }

    @Override
    public void close() {
        super.close();

        if (isAdded()) {
            mIsMenuShowing = false;
        }
    }
}
