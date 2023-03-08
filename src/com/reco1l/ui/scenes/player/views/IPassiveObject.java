package com.reco1l.ui.scenes.player.views;

import android.util.DisplayMetrics;

import androidx.coordinatorlayout.widget.CoordinatorLayout;

import com.reco1l.Game;
import com.reco1l.management.game.GameWrapper;
import com.reco1l.tools.Logging;

import main.osu.Config;

public interface IPassiveObject {

    default void setGameWrapper(GameWrapper wrapper) {}

    default void onObjectUpdate(float dt, float sec) {}

    default void clear() {}

    //--------------------------------------------------------------------------------------------//

    default int toEngineScale(float size) {
        return (int) (size * getEngineScale());
    }

    default float getEngineScale() {
        CoordinatorLayout frame = Game.platform.getScreenContainer();


        float factor = (float) frame.getHeight() / Config.getRES_HEIGHT();

        Logging.i(this, "Scale factor: " + factor);
        return factor;
    }
}
