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

    public long dismissTime = 0;

    //--------------------------------------------------------------------------------------------//

    /**
     * @param onBoth if true, dialog will be closed on back press and background click.
     */
    public void setCloseMode(boolean onBoth) {
        closeOnBackPress = onBoth;
        closeOnBackgroundClick = onBoth;
    }

    public void setCloseMode(boolean closeOnBackPress, boolean closeOnBackgroundClick) {
        this.closeOnBackPress = closeOnBackPress;
        this.closeOnBackgroundClick = closeOnBackgroundClick;
    }

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
}
