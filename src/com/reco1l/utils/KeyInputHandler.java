package com.reco1l.utils;

import android.view.KeyEvent;
import android.view.MotionEvent;

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.andengine.ISceneHandler;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.platform.UIFragment;

import org.anddev.andengine.entity.scene.Scene;

import java.util.List;

public class KeyInputHandler {

    private static final List<Dialog> dialogs = Game.platform.dialogs;

    //--------------------------------------------------------------------------------------------//

    public static void performBack() {
        handle(KeyEvent.KEYCODE_BACK, MotionEvent.ACTION_DOWN);
    }

    public static boolean handle(final int key, final int action) {
        if (!Game.engine.isGlobalInitialized)
            return false;

        Scene currentScene = Game.engine.getScene();
        Scene lastScene = Game.engine.lastScene;

        if (key == KeyEvent.KEYCODE_BACK && action == MotionEvent.ACTION_DOWN) {

            for (UIFragment fragment : UI.getExtras()) {
                if (fragment.isShowing) {
                    fragment.close();
                    return true;
                }
            }

            if (dialogs.size() > 0) {
                Dialog dialog = dialogs.get(dialogs.size() - 1);
                if (dialog.builder.closeOnBackPress) {
                    dialog.close();
                    return true;
                }
                return true;
            }

            ISceneHandler currentHandler = Game.engine.getCurrentSceneHandler();
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

            if (currentScene == Game.scoringScene.getScene() && lastScene == Game.gameScene.getScene()) {
                Game.engine.setScene(Game.songMenu);
                return true;
            }

            if (lastScene != null) {
                Game.engine.setScene(lastScene);
                return true;
            }
            Game.engine.setScene(Game.mainScene);
            return true;
        }
        return false;
    }
}
