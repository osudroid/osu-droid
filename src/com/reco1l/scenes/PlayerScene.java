package com.reco1l.scenes;

import com.reco1l.global.Game;
import com.reco1l.annotation.Legacy;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameScene;

@Legacy
public final class PlayerScene extends BaseScene {

    public static final PlayerScene instance = new PlayerScene();

    private GameScene mGame;

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean onBackPress() {
        if (mGame == null) {
            return false;
        }

        if (mGame.isPaused()) {
            mGame.resume();
            return true;
        }
        mGame.pause();
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    public PlayerScene New() {
        clearChildScene();
        clearEntityModifiers();
        clearTouchAreas();
        clearUpdateHandlers();
        detachChildren();
        return this;
    }

    @Legacy
    public GameScene getLegacyClass() {
        if (mGame == null) {
            mGame = new GameScene(Game.engine);
        }
        return mGame;
    }

    //--------------------------------------------------------------------------------------------//

    public void startGame(TrackInfo track, String replay) {
        if (mGame != null) {
            mGame.startGame(track, replay);
        }
    }
}
