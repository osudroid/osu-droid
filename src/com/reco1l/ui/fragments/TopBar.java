package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.ui.base.Layers;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;

import com.rimu.R;

// Created by Reco1l on 26/6/22 21:20

public final class TopBar extends BaseFragment {

    public static final TopBar instance = new TopBar();

    private View
            mBody,
            mBackButton;

    private LinearLayout
            mLeftAnchorLayout,
            mButtonsContainer;

    private boolean mIsClosing = false;

    //--------------------------------------------------------------------------------------------//

    public TopBar() {
        super(Scenes.selector, Scenes.summary, Scenes.loader, Scenes.listing);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "tb";
    }

    @Override
    protected int getLayout() {
        return R.layout.overlay_top_bar;
    }

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Overlay;
    }

    @Override
    public int getHeight() {
        return dimen(R.dimen.topBarHeight);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {

        mBody = find("body");
        mBackButton = find("back");
        mButtonsContainer = find("buttons");
        mLeftAnchorLayout = find("container");

        rootView.post(() -> {
            Animation.of(mBody)
                    .fromY(-getHeight())
                    .toY(0)
                    .play(200);

            Game.platform.animate(true, true)
                    .toTopMargin(getHeight())
                    .play(200);
        });

        bindTouch(mBackButton, Game.inputManager::performBack);
        bindTouch(find("userBox"), UI.userProfile::alternate);
        bindTouch(find("settings"), UI.settingsPanel::alternate);
        bindTouch(find("inbox"), UI.notificationCenter::alternate);

        handleButtonContainer(Game.engine.getCurrent());
    }

    @Override
    protected void onSceneChange(BaseScene oldScene, BaseScene newScene) {
        if (isAdded()) {

            Animation.of(mLeftAnchorLayout)
                    .toX(-60)
                    .toAlpha(0)
                    .runOnEnd(() -> {
                        if (!mIsClosing) {
                            handleButtonContainer(newScene);

                            Animation.of(mLeftAnchorLayout)
                                    .toX(0)
                                    .toAlpha(1)
                                    .play(200);
                        }
                    })
                    .play(200);
        }
    }

    private void handleButtonContainer(BaseScene scene) {
        mButtonsContainer.removeAllViews();

        if (scene == Scenes.main || scene == Scenes.loader) {
            mBackButton.setVisibility(View.GONE);
        } else {
            mBackButton.setVisibility(View.VISIBLE);
        }

        scene.onButtonContainerChange(mButtonsContainer);
    }

    @Override
    public void close() {
        if (isAdded()) {
            mIsClosing = true;

            Animation.of(mBody)
                    .toY(-getHeight())
                    .runOnEnd(() -> {
                        mButtonsContainer.removeAllViews();
                        super.close();
                    })
                    .play(200);

            Game.platform.animate(true, true)
                    .toTopMargin(0)
                    .play(200);
        }
    }

    @Override
    public boolean show() {
        mIsClosing = false;
        return super.show();
    }
}
