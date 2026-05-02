package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.andengine.UIEngine;

import org.andengine.engine.Engine;
import org.andengine.entity.modifier.AlphaModifier;
import org.andengine.entity.scene.CameraScene;
import org.andengine.entity.scene.IOnAreaTouchListener;
import org.andengine.entity.scene.ITouchArea;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.input.touch.TouchEvent;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.vbo.VertexBufferObjectManager;
import org.andengine.util.math.MathUtils;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.game.GameScene;

public class PauseMenu implements IOnAreaTouchListener {

    private final CameraScene scene;
    private final GameScene   game;
    private final boolean     fail;
    private boolean replaySaved;

    private boolean itemSelected;
    private float   initialSelectX, initialSelectY;

    private Sprite btnSaveReplay;
    private Sprite btnContinue;
    private final Sprite btnRetry;
    private final Sprite btnBack;

    public PauseMenu(final Engine engine, final GameScene game, final boolean fail) {
        this.game = game;
        this.fail = fail;

        final VertexBufferObjectManager vbo =
                GlobalManager.getInstance().getEngine().getVertexBufferObjectManager();

        final TextureRegion texReplay   = ResourceManager.getInstance().getTexture("pause-save-replay");
        final TextureRegion texContinue = ResourceManager.getInstance().getTexture("pause-continue");
        final TextureRegion texRetry    = ResourceManager.getInstance().getTexture("pause-retry");
        final TextureRegion texBack     = ResourceManager.getInstance().getTexture("pause-back");

        final boolean showSaveReplay = fail && !game.getReplaying();
        final boolean showContinue   = !fail;

        // Compute proportional scale so all visible buttons fit in 85% of screen height.
        float totalNaturalH = texRetry.getHeight() + texBack.getHeight();
        int   count         = 2;
        if (showSaveReplay) { totalNaturalH += texReplay.getHeight();   count++; }
        if (showContinue)   { totalNaturalH += texContinue.getHeight(); count++; }
        totalNaturalH += (count - 1); // 1 px spacing between items

        final float maxH  = Config.getRES_HEIGHT() * 0.85f;
        final float scale = (totalNaturalH > maxH) ? maxH / totalNaturalH : 1.0f;

        scene = new CameraScene(engine.getCamera()) {
            @Override
            public boolean onSceneTouchEvent(final TouchEvent pSceneTouchEvent) {
                super.onSceneTouchEvent(pSceneTouchEvent);
                // Intercept touch events to prevent them from passing through to the game scene.
                return true;
            }

            @Override
            protected void onManagedUpdate(float pSecondsElapsed) {
                if (UIEngine.getCurrent().getScene() != game.getScene()) {
                    back();
                    return;
                }
                // Speed-up the fade-in animation (original was 1 s; 2.5× makes it ~0.4 s).
                super.onManagedUpdate(pSecondsElapsed * 2.5f);
            }
        };

        scene.setBackgroundEnabled(false);
        scene.setOnAreaTouchListener(this);

        // Background overlay
        final TextureRegion bgTex = fail
                ? ResourceManager.getInstance().getTexture("fail-background")
                : ResourceManager.getInstance().getTexture("pause-overlay");

        if (bgTex != null) {
            float bgH = bgTex.getHeight() * Config.getRES_WIDTH() / bgTex.getWidth();
            scene.attachChild(
                    new Sprite(
                            0,
                            (Config.getRES_HEIGHT() - bgH) / 2f,
                            Config.getRES_WIDTH(),
                            bgH,
                            bgTex,
                            vbo
                    )
            );
        }

        // Position all visible buttons centred on screen
        final float displayTotalH = totalNaturalH * scale;
        final float cx = Config.getRES_WIDTH() / 2f;
        float y = (Config.getRES_HEIGHT() - displayTotalH) / 2f;

        if (showSaveReplay) {
            btnSaveReplay = addButton(texReplay, cx, y, scale, vbo);
            y += texReplay.getHeight() * scale + scale;
        }
        if (showContinue) {
            btnContinue = addButton(texContinue, cx, y, scale, vbo);
            y += texContinue.getHeight() * scale + scale;
        }
        btnRetry = addButton(texRetry, cx, y, scale, vbo);
        y += texRetry.getHeight() * scale + scale;
        btnBack  = addButton(texBack, cx, y, scale, vbo);
    }

    private Sprite addButton(TextureRegion tex, float cx, float y,
                             float scale, VertexBufferObjectManager vbo) {
        final float w = tex.getWidth()  * scale;
        final float h = tex.getHeight() * scale;
        final Sprite btn = new Sprite(cx - w / 2f, y, w, h, tex, vbo);
        btn.setAlpha(0f);
        btn.registerEntityModifier(new AlphaModifier(0.4f, 0f, 1f));
        scene.attachChild(btn);
        scene.registerTouchArea(btn);
        return btn;
    }

    public Scene getScene() {
        return scene;
    }

    @Override
    public boolean onAreaTouched(TouchEvent pSceneTouchEvent, ITouchArea pTouchArea,
                                 float pTouchAreaLocalX, float pTouchAreaLocalY) {
        if (pSceneTouchEvent.isActionDown()) {
            itemSelected    = true;
            initialSelectX  = pTouchAreaLocalX;
            initialSelectY  = pTouchAreaLocalY;
        } else if (pSceneTouchEvent.isActionMove() && itemSelected &&
                MathUtils.distance(initialSelectX, initialSelectY,
                        pTouchAreaLocalX, pTouchAreaLocalY) > 50) {
            itemSelected = false;
        } else if (pSceneTouchEvent.isActionUp() && itemSelected) {
            itemSelected = false;
            handleClick((Sprite) pTouchArea);
        }
        return true;
    }

    private void handleClick(final Sprite btn) {
        BassSoundProvider playSnd;

        if (btn == btnSaveReplay) {
            if (fail && !replaySaved && !game.getReplaying() && game.saveFailedReplay()) {
                ToastLogger.showText(
                        com.osudroid.resources.R.string.message_save_replay_successful, true);
                replaySaved = true;
            }

        } else if (btn == btnContinue) {
            if (fail) return;
            playSnd = ResourceManager.getInstance().getSound("menuback");
            if (playSnd != null) playSnd.play();
            game.resume();

        } else if (btn == btnBack) {
            GlobalManager.getInstance().getScoring().setReplayID(-1);
            playSnd = ResourceManager.getInstance().getSound("menuback");
            if (playSnd != null) playSnd.play();
            game.quit();

        } else if (btn == btnRetry) {
            ResourceManager.getInstance().getSound("failsound").stop();
            playSnd = ResourceManager.getInstance().getSound("menuhit");
            if (playSnd != null) playSnd.play();
            game.restartGame();
        }
    }
}
