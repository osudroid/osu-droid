package com.reco1l.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.edlplan.framework.easing.Easing;
import com.reco1l.Game;
import com.reco1l.management.online.Endpoint;
import com.reco1l.management.online.IOnlineObserver;
import com.reco1l.management.online.UserInfo;
import com.reco1l.ui.base.BaseFragment;

import com.reco1l.ui.base.Layers;
import com.reco1l.framework.Animation;
import com.reco1l.tools.Views;
import com.reco1l.framework.execution.Async;
import com.reco1l.view.ButtonView;
import com.reco1l.view.RoundLayout;
import com.reco1l.view.RoundedImageView;
import com.reco1l.view.RowTextView;

import com.rimu.R;

// Created by Reco1l on 13/9/22 01:22

public final class UserProfile extends BaseFragment implements IOnlineObserver {

    public static final UserProfile instance = new UserProfile();

    private EditText
            mUsername,
            mPassword;

    private TextView mName;
    private RoundLayout mBody;
    private RoundedImageView mAvatar;

    private ButtonView
            mAccept,
            mLogOut,
            mProfile;

    private RowTextView
            mRank,
            mAccuracy,
            mPlayCount;

    private LinearLayout
            mLoginLayout,
            mLoadLayout,
            mUserLayout;

    private int
            mLoginLayoutHeight,
            mUserLayoutHeight;

    //--------------------------------------------------------------------------------------------//

    public UserProfile() {
        super();

        Game.onlineManager2.bindOnlineObserver(this);
        closeOnBackgroundClick(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.extra_user_card;
    }

    @Override
    protected String getPrefix() {
        return "up";
    }

    @NonNull
    @Override
    protected Layers getLayer() {
        return Layers.Overlay;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        mLoginLayout = find("login");
        mUserLayout = find("logged");
        mLoadLayout = find("loading");

        mName = find("name");
        mBody = find("body");
        mAvatar = find("avatar");
        mUsername = find("user");
        mPassword = find("pass");
        mAccept = find("accept");

        mRank = find("rank");
        mAccuracy = find("acc");
        mPlayCount = find("plays");

        mProfile = find("profile");
        mLogOut = find("logout");

        mUserLayout.setAlpha(0);
        mLoadLayout.setAlpha(0);
        //mLoginLayout.setAlpha(0);

        bindTouch(mAccept, () -> {
            unbindTouch(mAccept);

            mLoadLayout.setVisibility(View.VISIBLE);
            Views.scale(mUserLayout, 0.9f);

            Animation.of(mLoginLayout)
                    .toAlpha(0)
                    .toScale(0.9f)
                    .play(100);

            Animation.of(mLoadLayout)
                    .toAlpha(1)
                    .fromScale(0.9f)
                    .toScale(1)
                    .delay(100)
                    .play(100);

            Async.run(() -> {
                String user = mUsername.getText().toString();
                String pass = mPassword.getText().toString();

                try {
                    Game.onlineManager2.login(user, pass);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        });

        bindTouch(mProfile, () -> {
            String url = Endpoint.PROFILE + Game.onlineManager2.getCurrentUser().getID();

            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        bindTouch(mLogOut, Game.onlineManager2::logOut);
    }

    @Override
    protected void onPost() {
        mUserLayoutHeight = mUserLayout.getHeight();
        mLoginLayoutHeight = mLoginLayout.getHeight();

        // TODO [UserProfile] Online behavior
        /*if (Game.onlineManager2.isLogged()) {
            mLoginLayout.setVisibility(View.GONE);
            mLoadLayout.setVisibility(View.GONE);
            onLogin(Game.onlineManager2.getCurrentUser());
        } else {
            onClear();
        }*/

        Animation.of(mBody)
                .fromScale(0.95f)
                .toScale(1)
                .cancelCurrentAnimations(false)
                .interpolate(Easing.InOutBounce)
                .play(200);

        Animation.of(rootView)
                .fromAlpha(1)
                .toAlpha(1)
                .play(200);
    }

    @Override
    public void onLogin(UserInfo user) {
        if (!isLoaded()) {
            return;
        }

        Game.activity.runOnUiThread(() -> {
            mUserLayout.setVisibility(View.VISIBLE);

            Animation.of(mBody)
                    .toHeight(mUserLayoutHeight)
                    .runOnEnd(() -> mLoginLayout.setVisibility(View.GONE))
                    .play(200);

            Animation.of(mLoadLayout)
                    .toAlpha(0)
                    .toScale(0.9f)
                    .runOnEnd(() -> mLoadLayout.setVisibility(View.GONE))
                    .play(100);

            Animation.of(mUserLayout)
                    .toAlpha(1)
                    .toScale(1)
                    .delay(100)
                    .play(100);

            mAvatar.setImageBitmap(user.getAvatar());

            mName.setText(user.getUsername());
            mRank.setValueText("#" + user.getRank());
            mPlayCount.setValueText("" + user.getPlayCount());
            mAccuracy.setValueText(String.format("%.2f%%", user.getAccuracyFP()));
        });
    }

    @Override
    public void onClear() {
        if (!isLoaded()) {
            return;
        }

        Game.activity.runOnUiThread(() -> {
            rebindTouch(mAccept);

            mLoginLayout.setVisibility(View.VISIBLE);

            Animation.of(mBody)
                    .toHeight(mLoginLayoutHeight)
                    .play(200);

            Animation.of(mUserLayout)
                    .toAlpha(0)
                    .toScale(0.8f)
                    .runOnEnd(() -> mUserLayout.setVisibility(View.GONE))
                    .play(100);

            Animation.of(mLoadLayout)
                    .toAlpha(0)
                    .toScale(0.8f)
                    .runOnEnd(() -> mLoadLayout.setVisibility(View.GONE))
                    .play(100);

            Animation.of(mLoginLayout)
                    .toAlpha(1)
                    .fromScale(0.9f)
                    .toScale(1)
                    .delay(100)
                    .play(100);
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded()) {
            return;
        }

        unbindTouchHandlers();

        Animation.of(mBody)
                .toScale(0.95f)
                .play(200);

        Animation.of(rootView)
                .toAlpha(0)
                .runOnEnd(super::close)
                .play(200);
    }
}
