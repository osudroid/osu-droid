package com.reco1l.ui.custom;

import com.reco1l.ui.SimpleFragment;

import java.util.ArrayList;

// Created by Reco1l on 25/7/22 21:46

public class DialogBuilder {

    List<Dialog.Button> buttons;
    SimpleFragment customFragment;

    protected String
            title,
            message;

    protected boolean
            canClose = true,
            hideHeader = true;

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder() {
        this(null);
    }

    public DialogBuilder(String title) {
        buttons = new ArrayList<>();
        this.title = title;
    }

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder addCloseButton() {
        canClose = true;
        addButton("Close", Dialog::close);
        return this;
    }

    public DialogBuilder addButton(@NonNull String text, @NonNull Dialog.OnButtonClick listener) {
        return addButton(new Button() {

            protected String getText() {
                return text;
            }

            public void onButtonClick(Dialog dialog) {
                listener.onButtonClick(dialog);
            }
        });
    }

    public DialogBuilder addButton(Button button) {
        buttons.add(button);
        return this;
    }

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public DialogBuilder setCustomFragment(SimpleFragment customFragment) {
        this.fragment = customFragment;
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

    // Hide title and message text views when a custom fragment or a custom view is defined.
    public DialogBuilder setHideHeader(boolean hide) {
        hideHeader = hide;
        return this;
    }
}
