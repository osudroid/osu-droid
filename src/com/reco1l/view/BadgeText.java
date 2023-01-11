package com.reco1l.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.reco1l.utils.Views;

import ru.nsu.ccfit.zuev.osuplus.R;

public class BadgeText extends BaseRoundedView {

    private ImageView icon;
    private TextView text;

    //--------------------------------------------------------------------------------------------//

    public BadgeText(@NonNull Context context) {
        this(context, null);
    }

    public BadgeText(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, R.attr.badgeTextStyle);
    }

    public BadgeText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int[] getStyleable() {
        return R.styleable.BadgeText;
    }

    @Override
    protected int getDefaultStyle() {
        return R.style.badgeText;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        LinearLayout layout = new LinearLayout(getContext());
        layout.setGravity(Gravity.CENTER_VERTICAL);
        Views.padding(layout)
                .vertical(sdp(2))
                .left(sdp(6))
                .right(sdp(8));
        addView(layout);

        icon = new ImageView(getContext());
        layout.addView(icon);
        Views.margins(icon)
                .right(sdp(4));

        text = new TextView(getContext());
        if (!isInEditMode()) {
            text.setTypeface(ResourcesCompat.getFont(getContext(), R.font.varela_regular), Typeface.BOLD);
        }
        text.setTextSize(TypedValue.COMPLEX_UNIT_PX, ssp(8));
        layout.addView(text);
    }

    @Override
    protected void onManageAttributes(TypedArray a) {
        icon.setImageDrawable(a.getDrawable(R.styleable.BadgeText_icon));
        text.setText(a.getString(R.styleable.BadgeText_android_text));
    }

    //--------------------------------------------------------------------------------------------//

    public void setText(CharSequence text) {
        this.text.setText(text);
    }

    public void setIcon(Drawable drawable) {
        icon.setImageDrawable(drawable);
    }

    public void setTextColor(int color) {
        text.setTextColor(color);
        if (icon.getDrawable() != null) {
            icon.getDrawable().setTint(color);
        }
    }

    public TextView getTextView() {
        return text;
    }

    public ImageView getIconView() {
        return icon;
    }
}
