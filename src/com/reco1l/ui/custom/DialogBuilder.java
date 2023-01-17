package com.reco1l.ui.custom;

import com.reco1l.ui.SimpleFragment;

import java.util.ArrayList;
import java.util.List;

// Created by Reco1l on 25/7/22 21:46

public class DialogBuilder {

    public List<Dialog.Button> buttons;

    public String title, message;
    public SimpleFragment customFragment;

    public Runnable onClose;
    public boolean
            closeOnBackPress = false,
            closeOnBackgroundClick = false;

    //--------------------------------------------------------------------------------------------//

    public void addButton(String text, Dialog.OnButtonClick onClick) {
        if (buttons == null)
            buttons = new ArrayList<>();

        addButton(text, onClick, null);
    }

    public void addButton(String text, Dialog.OnButtonClick onClick, Integer color) {
        if (onClick == null)
            return;
        buttons.add(new Dialog.Button(text, color, onClick));
    }

    //--------------------------------------------------------------------------------------------//

    public DialogBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public DialogBuilder setMessage(String message) {
        this.message = message;
        return this;
    }

    public DialogBuilder setCustomFragment(SimpleFragment customFragment) {
        this.customFragment = customFragment;
        return this;
    }

    public DialogBuilder setOnClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    public DialogBuilder setCloseOnBackPress(boolean close) {
        this.closeOnBackPress = close;
        return this;
    }

    public DialogBuilder closeOnBackgroundClick(boolean bool) {
        this.closeOnBackgroundClick = bool;
        return this;
    }

    //--------------------------------------------------------------------------------------------//


}
