package com.reco1l.management;

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.reco1l.Game;
import com.reco1l.interfaces.SceneHandler;
import com.reco1l.ui.custom.Dialog;

import org.anddev.andengine.entity.scene.Scene;

// This only have support for Back button.
public class KeyInputManager {

    //--------------------------------------------------------------------------------------------//

    public static void performBack() {
        handle(KeyEvent.KEYCODE_BACK, MotionEvent.ACTION_DOWN);
    }

    //--------------------------------------------------------------------------------------------//

    public static boolean handle(final int key, final int action) {
        if (!Game.engine.isGameLoaded()) {
            return false;
        }

        Scene currentScene = Game.engine.getScene();

        if (currentScene == Game.loaderScene) {
            return false;
        }

        if (key == KeyEvent.KEYCODE_BACK && action == MotionEvent.ACTION_DOWN) {

            if (Game.platform.closeExtras()) {
                return true;
            }

            for (Dialog dialog : Game.platform.getDialogs()) {
                if (dialog.builder.closeOnBackPress) {
                    dialog.close();
                    return true;
                }
            }

            SceneHandler currentHandler = Game.engine.getCurrentSceneHandler();
            if (currentHandler != null) {
                return currentHandler.onBackPress();
            }

            if (currentScene == Game.gameScene.getScene()) {
                if (Game.gameScene.isPaused()) {
                    Game.gameScene.resume();
                    return true;
                }
                Game.gameScene.pause();
                return true;
            }

            if (!Game.engine.backToLastScene()) {
                Game.engine.setScene(Game.mainScene);
            }
            return true;
        }
        return false;
    }
}
