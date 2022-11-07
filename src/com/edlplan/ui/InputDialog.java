package com.edlplan.ui;

import android.app.Dialog;
import android.content.Context;
import androidx.annotation.NonNull;
import android.widget.EditText;

import ru.nsu.ccfit.zuev.osuplus.R;

public class InputDialog extends Dialog {

    public InputDialog(@NonNull Context context) {
        super(context, R.style.Theme_Design_BottomSheetDialog);
        setContentView(null);
    }

    public void showForResult(OnResult onResult) {
        findViewById(0).setOnClickListener(view -> {
            onResult.onResult(((EditText) findViewById(0)).getText().toString());
            dismiss();
        });
        show();
    }

    public interface OnResult {
        void onResult(String result);
    }
}
