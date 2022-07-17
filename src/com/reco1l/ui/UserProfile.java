package com.reco1l.ui;

import android.view.View;
import android.widget.TextView;

import androidx.cardview.widget.CardView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.fragment.WebViewFragment;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.ui.platform.BaseLayout;
import com.reco1l.ui.platform.UIManager;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class UserProfile extends BaseLayout {

    public static String message;
    private View body;
    private TextView errorText;

    @Override
    protected int getLayout() {
        return R.layout.user_profile;
    }    private final Runnable closeTask = this::close;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "UC";
    }

    @Override
    protected void onLoad() {
        setDismissMode(true, true);

        body = find("body");
        body.postDelayed(closeTask, 8000);

        new Animation(body)
                .height(Res.sdp(30), Res.dimen(online.isStayOnline() ?
                        R.dimen.userPanelHeight : R.dimen.userPanelSmallHeight))
                .interpolatorMode(Animation.InterpolatorTo.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .moveY(-30, 0)
                .fade(0, 1)
                .play(240);

        new Animation(find("userBox")).fade(0, 1).play(200);

        View message = find("messageLayout");
        View infoContainer = find("info");
        TextView name = find("name");
        errorText = find("message");

        if (!online.isStayOnline()) {
            setVisible(false, infoContainer);
            setVisible(message);

            name.setText(Config.getLocalUsername());
            new Animation(message).fade(0, 1).play(200);
            updateMessage(null);
            return;
        }

        CircularProgressIndicator accuracyBar = find("accProgress");
        ShapeableImageView avatar = find("avatar");
        CardView goProfile = find("profile");

        TextView rank = find("rank"),
                score = find("score"),
                accuracy = find("acc");

        new Animation(find("infoBody"))
                .forChildView(child ->
                        new Animation(child)
                                .cancelPending(child == accuracy || child == accuracyBar)
                                .fade(0, child.getAlpha()))
                .play(200);

        avatar.setImageDrawable(onlineHandler.getPlayerAvatar());

        setVisible(false, message);
        setVisible(infoContainer);
        new ClickListener(goProfile).simple(() -> {
            new WebViewFragment().setURL(WebViewFragment.PROFILE_URL + online.getUserId()).show();
            close();
        });

        name.setText(online.getUsername());
        rank.setText(String.format("#%d", online.getRank()));

        new Animation(accuracy).ofFloat(0, online.getAccuracy() * 100f)
                .runOnUpdate(val -> accuracy.setText(String.format("%.2f%%", (float) val.getAnimatedValue())))
                .interpolator(Easing.OutExpo)
                .cancelPending(false)
                .delay(200)
                .play(1000);

        new Animation(accuracyBar).ofInt(0, (int) (online.getAccuracy() * 100))
                .runOnUpdate(val -> accuracyBar.setProgress((int) val.getAnimatedValue()))
                .interpolator(Easing.OutExpo)
                .cancelPending(false)
                .delay(200)
                .play(1000);

        DecimalFormat df = new DecimalFormat("###,###,###,###");
        score.setText(df.format(online.getScore()));
    }

    //--------------------------------------------------------------------------------------------//

    public void updateMessage(String text) {
        message = text != null ? text : Res.str(R.string.user_profile_offline_message);
        if (BuildConfig.DEBUG)
            message = text != null ? text : Res.str(R.string.user_profile_debug_message);
        if (!isShowing)
            return;
        mActivity.runOnUiThread(() -> errorText.setText(message));
    }
    
    @Override
    public void show() {
        platform.closeThis(UIManager.getExtras());
        super.show();
    }

    @Override
    public void close() {
        if (!isShowing)
            return;

        body.removeCallbacks(closeTask);
        new Animation(find("innerBody")).fade(1, 0).play(100);
        new Animation(errorText).fade(1, 0).play(100);

        new Animation(body)
                .height(Res.dimen(online.isStayOnline() ?
                        R.dimen.userPanelHeight : R.dimen.userPanelSmallHeight), Res.sdp(30))
                .interpolatorMode(Animation.InterpolatorTo.VALUE_ANIMATOR)
                .interpolator(Easing.OutExpo)
                .runOnEnd(super::close)
                .moveY(0, -30)
                .fade(1, 0)
                .play(240);
    }


}
