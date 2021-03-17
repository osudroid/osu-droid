package ru.nsu.ccfit.zuev.osu.menu;

import com.edlplan.framework.math.FMath;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.Scene.IOnSceneTouchListener;
import org.anddev.andengine.entity.scene.menu.MenuScene;
import org.anddev.andengine.entity.scene.menu.MenuScene.IOnMenuItemClickListener;
import org.anddev.andengine.entity.scene.menu.item.IMenuItem;
import org.anddev.andengine.entity.scene.menu.item.SpriteMenuItem;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osuplus.R;

public class PauseMenu implements IOnMenuItemClickListener {
    static final int ITEM_CONTINUE = 0;
    static final int ITEM_RETRY = 1;
    static final int ITEM_BACK = 2;
    private final MenuScene scene;
    private final GameScene game;
    private final boolean fail;
    private boolean savedFailedReplay;

    public PauseMenu(final Engine engine, final GameScene game,
                     final boolean fail) {
        this.game = game;
        this.fail = fail;
        savedFailedReplay = false;
        final ChangeableText saveFailedReplay = new ChangeableText(Utils.toRes(4), Utils.toRes(2),
                ResourceManager.getInstance().getFont("font"), StringTable.get(R.string.str_save_failed_replay));
        class PauseMenuScene extends MenuScene implements IOnSceneTouchListener{
            PauseMenuScene(final Camera pCamera){
                super(pCamera);
            }
            public boolean onSceneTouchEvent(final Scene pScene, final TouchEvent pSceneTouchEvent) {
                float pTouchX = FMath.clamp(pSceneTouchEvent.getX(), 0, Config.getRES_WIDTH());
                float pTouchY = FMath.clamp(pSceneTouchEvent.getY(), 0, Config.getRES_HEIGHT());
                if (fail && pSceneTouchEvent.isActionUp() && pTouchX < Config.getRES_WIDTH() / 2 && pTouchY < 50 && savedFailedReplay == false && game.getReplaying() == false){
                    //save replay
                    if (game.saveFailedReplay()){
                        saveFailedReplay.setText(StringTable.get(R.string.str_saved_failed_replay));
                        savedFailedReplay = true;
                    }
                }
                return true;
            }
        }
        scene = new PauseMenuScene(engine.getCamera());

        final SpriteMenuItem itemContinue = new SpriteMenuItem(ITEM_CONTINUE,
                ResourceManager.getInstance().getTexture("pause-continue"));
        scene.addMenuItem(itemContinue);
        final SpriteMenuItem itemRetry = new SpriteMenuItem(ITEM_RETRY,
                ResourceManager.getInstance().getTexture("pause-retry"));
        scene.addMenuItem(itemRetry);
        final SpriteMenuItem itemBack = new SpriteMenuItem(ITEM_BACK,
                ResourceManager.getInstance().getTexture("pause-back"));
        scene.addMenuItem(itemBack);
        scene.attachChild(saveFailedReplay);
        scene.setBackgroundEnabled(false);
        TextureRegion tex = null;
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
//			scene.setBackgroundEnabled(true);
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

        if (pMenuItem.getAlpha() < 0.75f) {
            return false;
        }
        BassSoundProvider playSnd;
        switch (pMenuItem.getID()) {
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
                game.resume();
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
