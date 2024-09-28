package ru.nsu.ccfit.zuev.osu.menu;

import android.annotation.SuppressLint;

import com.reco1l.andengine.sprite.ExtendedSprite;
import com.reco1l.andengine.Anchor;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.RotationByModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.*;

public class LoadingScreen implements IUpdateHandler {
    private final LoadingScene scene;
    private final ChangeableText logText;
    private float percentage;

    public LoadingScreen() {
        ArrayList<String> toastLoggerLog = ToastLogger.getLog();

        if (toastLoggerLog != null) {
            toastLoggerLog.clear();
        }

        scene = new LoadingScene();

        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");
        if (tex != null) {
            float height = tex.getHeight();
            height *= Config.getRES_WIDTH()
                    / (float) tex.getWidth();
            final Sprite menuBg = new Sprite(
                    0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(),
                    height, tex);
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new ColorBackground(70 / 255f, 129 / 255f,
                    252 / 255f));
        }

        final TextureRegion loadingTexture = ResourceManager.getInstance()
                .getTexture("loading-title");
        final Sprite loadingTitle = new Sprite(0, 0,
                Config.getRES_WIDTH(), loadingTexture.getHeight(), loadingTexture);
        scene.attachChild(loadingTitle);

        logText = new ChangeableText(0, 0, ResourceManager.getInstance()
                .getFont("logFont"), "", 5);
        scene.attachChild(logText);
        ToastLogger.setPercentage(-1);
        percentage = -1;

        var circle = new ExtendedSprite();
        circle.setOrigin(Anchor.Center);
        circle.setPosition(Config.getRES_WIDTH() / 2f, Config.getRES_HEIGHT() / 2f);
        circle.setTextureRegion(ResourceManager.getInstance().getTexture("loading"));
        circle.registerEntityModifier(new LoopEntityModifier(new RotationByModifier(2.0f, 360)));
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
