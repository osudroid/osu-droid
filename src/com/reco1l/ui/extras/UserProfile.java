package com.reco1l.ui.extras;

import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.fragment.WebViewFragment;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.ui.platform.BaseFragment;
import com.reco1l.utils.AnimationOld;
import com.reco1l.utils.Res;
import com.reco1l.utils.helpers.OnlineHelper;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 13/9/22 01:22

public class UserProfile extends BaseFragment {

    public static UserProfile instance;
    public static String message;

    private View body;
    private TextView errorText;
    private final Runnable closeTask = this::close;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.user_profile;
    }

    @Override
    protected String getPrefix() {
        return "UC";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        setDismissMode(true, true);

        body = find("body");
        body.postDelayed(closeTask, 8000);

        new AnimationOld(body)
                .height(Res.sdp(30), Res.dimen(Game.onlineManager.isStayOnline() ?
                        R.dimen.userPanelHeight : R.dimen.userPanelSmallHeight))
                .interpolatorMode(AnimationOld.Interpolate.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .moveY(-30, 0)
                .fade(0, 1)
                .play(240);

        new AnimationOld(find("userBox")).fade(0, 1).play(200);

        View message = find("messageLayout");
        View infoContainer = find("info");
        TextView name = find("name");
        errorText = find("message");

        if (!Game.onlineManager.isStayOnline()) {
            infoContainer.setVisibility(View.GONE);
            message.setVisibility(View.VISIBLE);

            name.setText(Config.getLocalUsername());
            new AnimationOld(message).fade(0, 1).play(200);
            updateMessage(null);
            return;
        }

        CircularProgressIndicator accuracyBar = find("accProgress");
        ShapeableImageView avatar = find("avatar");
        CardView goProfile = find("profile");

        TextView rank = find("rank"),
                score = find("score"),
                accuracy = find("acc");

        new AnimationOld(find("infoBody"))
                .forChildView(child ->
                        new AnimationOld(child)
                                .cancelPending(child == accuracy || child == accuracyBar)
                                .fade(0, child.getAlpha()))
                .play(200);

        avatar.setImageDrawable(OnlineHelper.getPlayerAvatar());

        infoContainer.setVisibility(View.VISIBLE);
        message.setVisibility(View.GONE);

        bindTouchListener(goProfile, () -> {
            new WebViewFragment().setURL(WebViewFragment.PROFILE_URL + Game.onlineManager.getUserId()).show();
            close();
        });

        name.setText(Game.onlineManager.getUsername());
        rank.setText(String.format("#%d", Game.onlineManager.getRank()));

        new AnimationOld().ofFloat(0, Game.onlineManager.getAccuracy() * 100f)
                .runOnUpdate(val -> accuracy.setText(String.format("%.2f%%", val)))
                .interpolator(Easing.OutExpo)
                .cancelPending(false)
                .delay(200)
                .play(1000);

        new AnimationOld().ofInt(0, (int) (Game.onlineManager.getAccuracy() * 100))
                .runOnUpdate(accuracyBar::setProgress)
                .interpolator(Easing.OutExpo)
                .cancelPending(false)
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
    public void show() {
        Game.platform.close(UI.getExtras());
        super.show();
    }

    @Override
    public void close() {
        if (!isAdded())
            return;

        body.removeCallbacks(closeTask);
        new AnimationOld(find("innerBody")).fade(1, 0)
                .play(100);
        new AnimationOld(errorText).fade(1, 0)
                .play(100);

        new AnimationOld(body)
                .height(Res.dimen(Game.onlineManager.isStayOnline() ?
                        R.dimen.userPanelHeight : R.dimen.userPanelSmallHeight), Res.sdp(30))
                .interpolatorMode(AnimationOld.Interpolate.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .runOnEnd(super::close)
                .moveY(0, -30)
                .fade(1, 0)
                .play(240);
    }


}
