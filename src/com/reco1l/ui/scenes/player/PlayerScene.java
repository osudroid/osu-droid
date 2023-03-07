package com.reco1l.ui.scenes.player;

import com.reco1l.Game;
import com.reco1l.annotation.Legacy;
import com.reco1l.management.game.GameWrapper;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.UI;
import com.reco1l.ui.scenes.BaseScene;
import com.reco1l.ui.elements.FPSBadgeView.FPSProvider;
import com.reco1l.ui.scenes.player.views.IPassiveObject;
import com.reco1l.framework.Animation;

import org.anddev.andengine.entity.util.FPSCounter;

import main.audio.BassSoundProvider;
import main.osu.TrackInfo;
import main.osu.game.GameScene;

@Legacy
public final class PlayerScene extends BaseScene implements FPSProvider, IPassiveObject {

    public static final PlayerScene instance = new PlayerScene();

    private final FPSCounter mCounter;

    private float
            mFPS,
            mFrameTime;

    @Legacy
    private GameScene mGame;

    //--------------------------------------------------------------------------------------------//

    public PlayerScene() {
        super();

        mCounter = new FPSCounter() {
            public void onUpdate(float pSecondsElapsed) {
                super.onUpdate(pSecondsElapsed);

                mFPS = getFPS();
                mFrameTime = pSecondsElapsed * 1000;
            }
        };
        registerUpdateHandler(mCounter);
        setBackgroundEnabled(true);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean onBackPress() {
        if (mGame == null) {
            return false;
        }

        if (mGame.isPaused()) {
            UI.pauseMenu.close(mGame::resume);
            return true;
        }
        mGame.pause();
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onObjectUpdate(float dt, float sec) {
        Game.activity.runOnUiThread(() -> {
            UI.gameOverlay.onObjectUpdate(dt, sec);
            UI.breakOverlay.onObjectUpdate(dt, sec);
        });
    }

    @Override
    public void clear() {
        Scenes.summary.setReplayID(-1);

        clearChildScene();
        clearEntityModifiers();
        clearTouchAreas();
        clearUpdateHandlers();
        detachChildren();

        registerUpdateHandler(mCounter);

        Game.activity.runOnUiThread(() -> {
            UI.gameOverlay.clear();
            UI.breakOverlay.clear();
        });
    }

    @Override
    public void setGameWrapper(GameWrapper wrapper) {
        Game.activity.runOnUiThread(() -> {
            UI.gameOverlay.setGameWrapper(wrapper);
            UI.breakOverlay.setGameWrapper(wrapper);
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public float getFPS() {
        return mFPS;
    }

    @Override
    public float getFrameTime() {
        return mFrameTime;
    }

    // Legacy code method calls
    //--------------------------------------------------------------------------------------------//

    @Legacy
    public GameScene getLegacyClass() {
        if (mGame == null) {
            mGame = new GameScene(Game.engine);
        }
        return mGame;
    }

    @Legacy
    public void startGame(TrackInfo track, String replay) {
        mGame.startGame(track, replay);
    }

    @Legacy
    public void resume() {
        mGame.resume();
    }

    @Legacy
    public void quit() {
        mGame.quit();
    }

    @Legacy
    public void retry() {
        BassSoundProvider sound = Game.resourcesManager.getSound("failsound");
        if (sound != null) {
            sound.stop();
        }
        mGame.restartGame();
    }

    @Legacy
    public boolean isReplaying() {
        return mGame.getReplaying();
    }

    @Legacy
    public boolean saveReplay() {
        return mGame.saveFailedReplay();
    }
}
