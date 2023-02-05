package com.reco1l.management;

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.reco1l.global.Game;

// This only have support for Back button.
public class InputManager {

    public static final InputManager instance = new InputManager();

    private boolean mIsInputLocked = true;

    //--------------------------------------------------------------------------------------------//

    public void performBack() {
        handleKey(KeyEvent.KEYCODE_BACK, MotionEvent.ACTION_DOWN);
    }

    //--------------------------------------------------------------------------------------------//

    public boolean handleKey(final int key, final int action) {
        if (mIsInputLocked) {
            return false;
        }

        if (key == KeyEvent.KEYCODE_BACK) {
            return handleBackKey(action);
        }
        return false;
    }

    private static boolean handleBackKey(int action) {

        if (action == MotionEvent.ACTION_DOWN) {
            if (Game.platform.onBackPress()) {
                return true;
            }
            return Game.engine.onBackPress();
        }
        return false;
    }

    //--------------------------------------------------------------------------------------------//

    public void setInputLock(boolean bool) {
        mIsInputLocked = bool;
    }
}
