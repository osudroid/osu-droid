package com.reco1l.view;

import static androidx.appcompat.widget.LinearLayoutCompat.VERTICAL;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.framework.drawing.Dimension;
import com.reco1l.tools.Views;

public class AlertBoxView extends RoundLayout {

    private TextView mText;
    private ImageView mIcon;

    //--------------------------------------------------------------------------------------------//

    public AlertBoxView(@NonNull Context context) {
        super(context);
    }

    public AlertBoxView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AlertBoxView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayoutType() {
        return LINEAR;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setGravity(Gravity.CENTER);
        setOrientation(VERTICAL);

        mIcon = new ImageView(getContext(), attrs);
        addView(mIcon, Views.wrap_content);

        mText = Views.styledText(this, attrs);
        mText.setGravity(Gravity.CENTER);
        addView(mText, Views.wrap_content);

        Views.resetAll(mText);
        Views.resetAll(mIcon);

        int dp6 = sdp(6);
        int dp4 = sdp(4);

        Views.margins(mIcon)
             .top(dp6)
             .bottom(dp4)
             .horizontal(dp6);

        Views.margins(mText)
             .top(dp4)
             .bottom(dp6)
             .horizontal(dp6);
    }

    //--------------------------------------------------------------------------------------------//

    public void setText(String text) {
        mText.setText(text);
    }
}
