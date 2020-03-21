package com.edlplan.ui;

import android.app.Dialog;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.EditText;

import ru.nsu.ccfit.zuev.osuplus.R;

public class InputDialog extends Dialog {

    public InputDialog(@NonNull Context context) {
        super(context, R.style.Theme_Design_BottomSheetDialog);
        setContentView(R.layout.dialog_for_input);
    }

    public void showForResult(OnResult onResult) {
        findViewById(R.id.button3).setOnClickListener(view -> {
            onResult.onResult(((EditText) findViewById(R.id.editText)).getText().toString());
            dismiss();
        });
        show();
    }

    public interface OnResult {
        void onResult(String result);
    }
}
