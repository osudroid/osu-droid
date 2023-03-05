package com.reco1l.ui.scenes.player.views;

import android.widget.FrameLayout;

import com.reco1l.Game;
import com.reco1l.management.game.GameWrapper;

public interface IPassiveObject {

    default void setGameWrapper(GameWrapper wrapper) {}

    default void onObjectUpdate(float dt, float sec) {}

    default void clear() {}

    //--------------------------------------------------------------------------------------------//

    default int toEngineScale(float width) {
        return (int) (width * getEngineScale());
    }

    default float getEngineScale() {
        FrameLayout screen = Game.platform.getScreenContainer();

        float w = screen.getWidth();
        float h = screen.getHeight();

        return 1f + (h / w);
    }
}
