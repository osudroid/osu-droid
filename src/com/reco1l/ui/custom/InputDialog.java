package com.reco1l.ui.custom;

import android.os.Bundle;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.reco1l.ui.SimpleFragment;

import ru.nsu.ccfit.zuev.osuplus.R;

public class InputDialog extends Dialog {

    //--------------------------------------------------------------------------------------------//

    public InputDialog(@NonNull InputDialogBuilder builder) {
        super(builder);

        LinearLayout layout = new LinearLayout(getContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView textView = new TextView(new ContextThemeWrapper(getContext(), R.style.text));
        textView.setText(builder.summary);
        layout.addView(textView);


        EditText edit = new EditText(getContext());
        edit.setInputType(builder.inputType);
        edit.setHint(builder.hint);
        edit.addTextChangedListener(builder.textWatcher);
        layout.addView(edit);

        builder.customFragment = new SimpleFragment(layout) {
            @Override
            protected void onLoad() {

            }
        };
    }

    //--------------------------------------------------------------------------------------------//

}
