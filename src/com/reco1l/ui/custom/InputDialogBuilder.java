package com.reco1l.ui.custom;

import android.text.InputType;
import android.text.TextWatcher;

public class InputDialogBuilder extends DialogBuilder {

    TextWatcher textWatcher;
    String hint;

    int inputType;

    public InputDialogBuilder setTextWatcher(TextWatcher textWatcher) {
        this.textWatcher = textWatcher;
        return this;
    }

    public InputDialogBuilder setHint(String hint) {
        this.hint = hint;
        return this;
    }

    public InputDialogBuilder setInputType(int inputType) {
        this.inputType = inputType;
        return this;
    }
}
