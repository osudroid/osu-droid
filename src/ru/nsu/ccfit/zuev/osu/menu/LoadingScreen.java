package ru.nsu.ccfit.zuev.osu.menu;

import android.annotation.SuppressLint;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.modifier.FadeOutModifier;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.RotationByModifier;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.entity.scene.background.SpriteBackground;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;
import org.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.helper.CentredSprite;

public class LoadingScreen implements IUpdateHandler {
    private final LoadingScene scene;
    private final Text logText;
    private float percentage;

    public LoadingScreen() {
        ArrayList<String> toastLoggerLog = ToastLogger.getLog();

        if (toastLoggerLog != null) {
            toastLoggerLog.clear();
        }

        scene = new LoadingScene();
        scene.registerEntityModifier(new FadeOutModifier(0.4f));

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");
        if (tex != null) {
            float height = tex.getHeight();
            height *= Config.getRES_WIDTH()
                    / (float) tex.getWidth();
            final Sprite menuBg = new Sprite(
                    0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(),
                    height, tex, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new Background(70 / 255f, 129 / 255f,
                    252 / 255f));
        }

        final TextureRegion loadingTexture = ResourceManager.getInstance()
                .getTexture("loading-title");
        final Sprite loadingTitle = new Sprite(0, 0,
                Config.getRES_WIDTH(), loadingTexture.getHeight(), loadingTexture, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        scene.attachChild(loadingTitle);

        logText = new Text(0, 0, ResourceManager.getInstance()
                .getFont("logFont"), "", 5, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        scene.attachChild(logText);
        ToastLogger.setPercentage(-1);
        percentage = -1;

        final TextureRegion ltexture = ResourceManager.getInstance()
                .getTexture("loading");
        final Sprite circle = new CentredSprite(Config.getRES_WIDTH() / 2f,
                Config.getRES_HEIGHT() / 2f, ltexture);
        circle.registerEntityModifier(new LoopEntityModifier(
                new RotationByModifier(2.0f, 360)));
        scene.attachChild(circle);

        scene.registerUpdateHandler(this);
    }

    public Scene getScene() {
        return scene;
    }

    public void show() {
        GlobalManager.getInstance().getEngine().setScene(scene);
    }

    @SuppressLint("DefaultLocale")
    public void onUpdate(final float pSecondsElapsed) {
        if (ToastLogger.getPercentage() != percentage) {
            percentage = ToastLogger.getPercentage();
            logText.setText(String.format("%d%%", (int) percentage));
            logText.setPosition(Config.getRES_WIDTH() / 2f - logText.getWidth()
                    / 2, Config.getRES_HEIGHT() - Utils.toRes(100));
        }
    }


    public void reset() {
        // TODO Auto-generated method stub
    }


    public static class LoadingScene extends Scene { }
}
