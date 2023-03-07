package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.reco1l.Game;
import com.reco1l.ui.UI;
import com.reco1l.ui.base.Layers;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.tables.AnimationTable;
import com.reco1l.tables.Res;
import com.reco1l.ui.base.BaseFragment;
import com.reco1l.framework.Animation;

import com.reco1l.utils.helpers.OnlineHelper;

import main.osu.Config;
import com.rimu.R;

// Created by Reco1l on 26/6/22 21:20

public final class TopBar extends BaseFragment {

    public static final TopBar instance = new TopBar();

    public UserBox userBox;

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
        return Res.dimen(R.dimen.topBarHeight);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        userBox = new UserBox(this);

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
        bindTouch(find("settings"), UI.settingsPanel::alternate);
        bindTouch(find("inbox"), UI.notificationCenter::alternate);

        userBox.loadUserData(false);
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

    //--------------------------------------------------------------------------------------------//

    // TODO [TopBar] Replace this with a custom view
    @Deprecated
    public static class UserBox {

        private final TopBar parent;
        private final ImageView avatar;
        private final TextView rank, name;

        //----------------------------------------------------------------------------------------//

        public UserBox(TopBar parent) {
            this.parent = parent;

            View body = parent.find("userBox");
            rank = parent.find("playerRank");
            name = parent.find("playerName");
            avatar = parent.find("avatar");

            TriangleEffectView triangles = parent.find("userBoxTriangles");
            triangles.setTriangleColor(0xFFFFFFFF);

            parent.bindTouch(body, UI.userProfile::alternate);
        }

        //----------------------------------------------------------------------------------------//

        public void loadUserData(boolean clear) {
            if (!parent.isAdded())
                return;

            AnimationTable.fadeOutIn(avatar, () -> avatar.setImageResource(R.drawable.placeholder_avatar));

            AnimationTable.textChange(rank, Res.str(R.string.top_bar_offline));
            AnimationTable.textChange(name, Config.getLocalUsername());

            if (Game.onlineManager.isStayOnline() && !clear) {
                AnimationTable.textChange(name, Game.onlineManager.getUsername());
                AnimationTable.textChange(rank, "#" + Game.onlineManager.getRank());

                AnimationTable.fadeOutIn(avatar, () -> {
                    if (OnlineHelper.getPlayerAvatar() != null) {
                        avatar.setImageDrawable(OnlineHelper.getPlayerAvatar());
                    }
                });
            }
        }
    }
}
