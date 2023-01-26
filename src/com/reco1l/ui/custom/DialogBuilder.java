package com.reco1l.ui.custom;

import com.reco1l.ui.SimpleFragment;

import java.util.ArrayList;
import java.util.List;

// Created by Reco1l on 25/7/22 21:46

public class DialogBuilder {

    List<Dialog.Button> buttons;
    SimpleFragment customFragment;

    String title, message;
    Runnable mOnClose;

    boolean canClose = true,
            closeExtras = true;

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder(String pTitle) {
        title = pTitle;
    }

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder addCloseButton() {
        canClose = true;
        addButton("Close", Dialog::close);
        return this;
    }

    public DialogBuilder addButton(String text, Dialog.OnButtonClick onClick) {
        if (buttons == null)
            buttons = new ArrayList<>();

        return addButton(text, onClick, null);
    }

    public DialogBuilder addButton(String text, Dialog.OnButtonClick onClick, Integer color) {
        if (onClick == null) {
            return this;
        }
        buttons.add(new Dialog.Button(text, color, onClick));
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public DialogBuilder setCustomFragment(SimpleFragment customFragment) {
        this.customFragment = customFragment;
        return this;
    }

    public DialogBuilder setOnDismiss(Runnable onClose) {
        this.mOnClose = onClose;
        return this;
    }

    // Set if user can dismiss the dialog
    public DialogBuilder setDismiss(boolean enabled) {
        canClose = enabled;
        return this;
    }

    public DialogBuilder setCloseExtras(boolean bool) {
        closeExtras = bool;
        return this;
    }
}
