package com.reco1l.ui.elements;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.Game;
import com.reco1l.framework.Views;
import com.reco1l.framework.drawing.Dimension;
import com.reco1l.management.online.IOnlineObserver;
import com.reco1l.management.online.UserInfo;
import com.reco1l.view.RoundLayout;
import com.reco1l.view.RoundedImageView;
import com.rimu.R;

public class UserBoxView extends RoundLayout implements IOnlineObserver {

    private RoundedImageView mAvatar;
    private LinearLayout mLayout;

    private TextView
            mRank,
            mUsername;

    private Dimension mAvatarDimen;

    //--------------------------------------------------------------------------------------------//

    public UserBoxView(@NonNull Context context) {
        super(context);
    }

    public UserBoxView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public UserBoxView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setBackground(new ColorDrawable(0x25000000));
        setRadius(0);

        mRank = Views.styledText(this, null);
        mRank.setTextSize(TypedValue.COMPLEX_UNIT_PX, sdp(18));
        mRank.setText("offline");
        mRank.setAlpha(0.25f);
        addView(mRank);

        Views.rule(mRank, RelativeLayout.ALIGN_PARENT_BOTTOM);
        Views.margins(mRank)
             .left(sdp(4))
             .bottom(sdp(2));

        mLayout = new LinearLayout(getContext());
        mLayout.setGravity(Gravity.CENTER);
        addView(mLayout, getInitialLayoutParams());

        Views.padding(mLayout)
             .horizontal(sdp(18));

        mUsername = Views.styledText(this, null);
        mUsername.setTextSize(TypedValue.COMPLEX_UNIT_PX, sdp(12));
        mUsername.setText("Guest");
        mLayout.addView(mUsername);

        Views.padding(mUsername)
             .right(sdp(14));

        mAvatar = new RoundedImageView(getContext());
        mAvatar.setImageResource(R.drawable.placeholder_avatar);
        mAvatar.setRadius(8);
        mLayout.addView(mAvatar);

        int size = sdp(24);
        mAvatarDimen = new Dimension(size, size);
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        // Not listening to attributes
    }

    @Override
    protected void onDimensionChange(Dimension dimens) {
        super.onDimensionChange(dimens);
        matchSize(mLayout);
        matchSize(mAvatar, mAvatarDimen);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isInEditMode()) {
            onLogin(Game.onlineManager2.getCurrentUser());
            Game.onlineManager2.bindOnlineObserver(this);
        }
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (!isInEditMode()) {
            Game.onlineManager2.unbindOnlineObserver(this);
        }
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onLogin(UserInfo user) {
        if (user == null) {
            return;
        }

        Game.activity.runOnUiThread(() -> {
            mAvatar.setImageBitmap(user.getAvatar());
            mUsername.setText(user.getUsername());
            mRank.setText("#" + user.getRank());
        });
    }

    @Override
    public void onClear() {

        Game.activity.runOnUiThread(() -> {
            mAvatar.setImageBitmap(null);
            mUsername.setText("Guest");
            mRank.setText("offline");
        });
    }

    //--------------------------------------------------------------------------------------------//

}
