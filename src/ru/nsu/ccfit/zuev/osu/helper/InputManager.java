package ru.nsu.ccfit.zuev.osu.helper;

import android.content.Context;
import android.view.inputmethod.InputMethodManager;

public class InputManager {

    private static Context context;

    private static final InputManager instance = new InputManager();

    private boolean inputStarted = false;

    private boolean changed = true;

    private StringBuilder builder;

    private int maxlength;

    private InputManager() {
    }

    public static void setContext(final Context context) {
        InputManager.context = context;
    }

    public static InputManager getInstance() {
        return instance;
    }

    public void startInput(final String start, final int maxlength) {
        this.maxlength = maxlength;
        builder = new StringBuilder(start);
        changed = true;
        inputStarted = true;
        toggleKeyboard();
    }

    public void toggleKeyboard() {
        final InputMethodManager mgr = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        mgr.toggleSoftInput(0, 0);
    }

    public void append(final char c) {
        if (!inputStarted) {
            return;
        }
        if (builder.length() >= maxlength) {
            return;
        }
        changed = true;
        builder.append(c);
    }

    public void pop() {
        if (!inputStarted || builder.length() == 0) {
            return;
        }
        changed = true;
        builder.deleteCharAt(builder.length() - 1);
    }

    public String getText() {
        if (!inputStarted) {
            return "";
        }
        changed = false;
        return builder.toString();
    }

    public boolean isChanged() {
        return changed;
    }

    public boolean isStarted() {
        return inputStarted;
    }

}
