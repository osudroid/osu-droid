package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.global.Game;
import com.reco1l.interfaces.fields.Endpoint;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.Animation;

import com.reco1l.utils.helpers.OnlineHelper;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 13/9/22 01:22

public final class UserProfile extends BaseFragment {

    public static final UserProfile instance = new UserProfile();

    public static String message;

    private View body;
    private TextView errorText;
    private final Runnable closeTask = this::close;


    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.extra_user_card;
    }

    @Override
    protected String getPrefix() {
        return "UC";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        closeOnBackgroundClick(true);

        body = find("body");
        body.postDelayed(closeTask, 8000);

        Animation.of(body)
                .fromHeight(Res.sdp(30))
                .toHeight(Res.dimen(Game.onlineManager.isStayOnline() ?
                        R.dimen.userPanelHeight : R.dimen.userPanelSmallHeight))
                .interpolate(Easing.OutExpo)
                .fromY(-30)
                .toY(0)
                .fromAlpha(0)
                .toAlpha(1)
                .play(240);

        Animation.of(find("userBox"))
                .fromAlpha(0)
                .toAlpha(1)
                .play(200);

        View message = find("messageLayout");
        View infoContainer = find("info");
        TextView name = find("name");
        errorText = find("message");

        if (!Game.onlineManager.isStayOnline()) {
            infoContainer.setVisibility(View.GONE);
            message.setVisibility(View.VISIBLE);

            name.setText(Config.getLocalUsername());

            Animation.of(message)
                    .fromAlpha(0)
                    .toAlpha(1)
                    .play(200);

            updateMessage(null);
            return;
        }

        CircularProgressIndicator accuracyBar = find("accProgress");
        ShapeableImageView avatar = find("avatar");
        CardView goProfile = find("profile");

        TextView rank = find("rank"),
                score = find("score"),
                accuracy = find("acc");

        Animation.of(find("infoBody"))
                .fromAlpha(0)
                .toAlpha(1)
                .play(200);

        avatar.setImageDrawable(OnlineHelper.getPlayerAvatar());

        infoContainer.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);

        bindTouch(goProfile, () -> {
            new WebViewPanel().show(Endpoint.PROFILE_URL + Game.onlineManager.getUserId());
            close();
        });

        name.setText(Game.onlineManager.getUsername());
        rank.setText(String.format("#%d", Game.onlineManager.getRank()));



        Animation.ofFloat(0, Game.onlineManager.getAccuracy() * 100f)
                .runOnUpdate(val -> accuracy.setText(String.format("%.2f%%", (float) val)))
                .interpolate(Easing.OutExpo)
                .delay(200)
                .play(1000);

        Animation.ofInt(0, (int) Game.onlineManager.getAccuracy() * 100)
                .runOnUpdate(v -> accuracyBar.setProgress((int) v))
                .interpolate(Easing.OutExpo)
                .delay(200)
                .play(1000);

        DecimalFormat df = new DecimalFormat("###,###,###,###");
        score.setText(df.format(Game.onlineManager.getScore()));
    }

    //--------------------------------------------------------------------------------------------//

    public void updateMessage(String text) {
        message = text != null ? text : Res.str(R.string.user_profile_offline_message);
        if (BuildConfig.DEBUG)
            message = text != null ? text : Res.str(R.string.user_profile_debug_message);
        if (!isAdded())
            return;
        Game.activity.runOnUiThread(() -> errorText.setText(message));
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isAdded())
            return;

        body.removeCallbacks(closeTask);

        Animation.of(find("innerBody"))
                .toAlpha(0)
                .play(100);

        Animation.of(errorText)
                .toAlpha(0)
                .play(100);

        Animation.of(body)
                .toHeight(Res.sdp(30))
                .toY(-30)
                .toAlpha(0)
                .interpolate(Easing.OutExpo)
                .runOnEnd(super::close)
                .play(240);
    }


}
