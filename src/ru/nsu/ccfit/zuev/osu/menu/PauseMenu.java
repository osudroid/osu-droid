package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.andengine.UIEngine;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.menu.MenuScene;
import org.anddev.andengine.entity.scene.menu.MenuScene.IOnMenuItemClickListener;
import org.anddev.andengine.entity.scene.menu.item.IMenuItem;
import org.anddev.andengine.entity.scene.menu.item.SpriteMenuItem;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.MathUtils;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.game.GameScene;

public class PauseMenu implements IOnMenuItemClickListener {
    static final int ITEM_SAVE_REPLAY = 0;
    static final int ITEM_CONTINUE = 1;
    static final int ITEM_RETRY = 2;
    static final int ITEM_BACK = 3;
    private final MenuScene scene;
    private final GameScene game;
    private final boolean fail;
    private boolean replaySaved;

    // MenuScene handles touch events differently from what is used across the game.
    // Normally, an item should only be selected on touch down, but MenuScene also selects it on touch move with no way
    // of canceling it.
    // Rationale of addressing it here instead of directly in MenuScene is to avoid engine changes whenever possible.
    private boolean itemSelected;
    private float initialSelectX, initialSelectY;

    public PauseMenu(final Engine engine, final GameScene game, final boolean fail) {
        this.game = game;
        this.fail = fail;
        replaySaved = false;
        scene = new MenuScene(engine.getCamera()) {
            @Override
            public boolean onSceneTouchEvent(Scene pScene, TouchEvent pSceneTouchEvent) {
                super.onSceneTouchEvent(pScene, pSceneTouchEvent);

                // Intercept touch event to prevent it from being passed to the game scene during pause.
                return true;
            }

            @Override
            public boolean onAreaTouched(TouchEvent pSceneTouchEvent, ITouchArea pTouchArea, float pTouchAreaLocalX, float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    itemSelected = true;
                    initialSelectX = pTouchAreaLocalX;
                    initialSelectY = pTouchAreaLocalY;
                } else if (pSceneTouchEvent.isActionMove() && itemSelected &&
                        MathUtils.distance(initialSelectX, initialSelectY, pTouchAreaLocalX, pTouchAreaLocalY) > 50) {
                    itemSelected = false;
                }

                return super.onAreaTouched(pSceneTouchEvent, pTouchArea, pTouchAreaLocalX, pTouchAreaLocalY);
            }

            @Override
            protected void onManagedUpdate(float pSecondsElapsed) {

                if (UIEngine.getCurrent().getScene() != game.getScene()) {
                    back();
                    return;
                }

                // The fade in animation duration is 1 second. We want to speed it up.
                super.onManagedUpdate(pSecondsElapsed * 2.5f);
            }
        };

        final SpriteMenuItem saveFailedReplay = new SpriteMenuItem(ITEM_SAVE_REPLAY,
                ResourceManager.getInstance().getTexture("pause-save-replay"));
        scene.addMenuItem(saveFailedReplay);
        final SpriteMenuItem itemContinue = new SpriteMenuItem(ITEM_CONTINUE,
                ResourceManager.getInstance().getTexture("pause-continue"));
        scene.addMenuItem(itemContinue);
        final SpriteMenuItem itemRetry = new SpriteMenuItem(ITEM_RETRY,
                ResourceManager.getInstance().getTexture("pause-retry"));
        scene.addMenuItem(itemRetry);
        final SpriteMenuItem itemBack = new SpriteMenuItem(ITEM_BACK,
                ResourceManager.getInstance().getTexture("pause-back"));
        scene.addMenuItem(itemBack);
        scene.setBackgroundEnabled(false);
        TextureRegion tex;
        if (fail) {
            itemContinue.setVisible(false);
            tex = ResourceManager.getInstance().getTexture("fail-background");
            if (game.getReplaying()){
                saveFailedReplay.setVisible(false);
            }
        } else {
            saveFailedReplay.setVisible(false);
            tex = ResourceManager.getInstance().getTexture("pause-overlay");
        }

        if (tex != null) {
            float height = tex.getHeight();
            height *= Config.getRES_WIDTH() / (float) (tex.getWidth());
            final Sprite bg = new Sprite(0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(), height, tex);
            scene.attachChild(bg, 0);
        }

        scene.buildAnimations();
        scene.setOnMenuItemClickListener(this);
    }

    public MenuScene getScene() {
        return scene;
    }


    public boolean onMenuItemClicked(final MenuScene pMenuScene,
                                     final IMenuItem pMenuItem, final float pMenuItemLocalX,
                                     final float pMenuItemLocalY) {
        if (!itemSelected) {
            return false;
        }

        BassSoundProvider playSnd;
        switch (pMenuItem.getID()) {
            case ITEM_SAVE_REPLAY:
                if(fail && !replaySaved && !game.getReplaying() && game.saveFailedReplay()){
                    ToastLogger.showText(com.osudroid.resources.R.string.message_save_replay_successful, true);
                    replaySaved = true;
                }
                return true;

            case ITEM_CONTINUE:
                if (fail) {
                    return false;
                }
                playSnd = ResourceManager.getInstance().getSound("menuback");
                if (playSnd != null) {
                    playSnd.play();
                }
                game.resume();
                return true;

            case ITEM_BACK:
                GlobalManager.getInstance().getScoring().setReplayID(-1);
                playSnd = ResourceManager.getInstance().getSound("menuback");
                if (playSnd != null) {
                    playSnd.play();
                }
                game.quit();
                return true;

            case ITEM_RETRY:
                ResourceManager.getInstance().getSound("failsound").stop();
                playSnd = ResourceManager.getInstance().getSound("menuhit");
                if (playSnd != null) {
                    playSnd.play();
                }
                game.restartGame();
                return true;
        }
        return false;
    }
}
