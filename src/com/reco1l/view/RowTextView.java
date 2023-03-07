package com.reco1l.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import android.util.AttributeSet;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.reco1l.framework.Views;

public class RowTextView extends RoundLayout {

    private TextView
            mTypeText,
            mValueText;

    //--------------------------------------------------------------------------------------------//

    public RowTextView(@NonNull Context context) {
        super(context);
    }

    public RowTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public RowTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setBackground(new ColorDrawable(0x14000000));

        mTypeText = Views.styledText(this, attrs);
        mTypeText.setTextColor(0xBF819DD4);
        addView(mTypeText);

        mValueText = Views.styledText(this, null);
        addView(mValueText);

        Views.rule(mValueText, ALIGN_PARENT_RIGHT);

        Views.padding(getInternalLayout())
                .vertical(sdp(6))
                .horizontal(sdp(8));
    }

    @Override
    protected void onManageAttributes(@Nullable TypedArray t, AttributeSet a) {
        mValueText.setText(a.getAttributeValue(appNS, "valueText"));
    }

    //--------------------------------------------------------------------------------------------//

    public void setValueText(String text) {
        mValueText.setText(text);
    }
}
