package ru.nsu.ccfit.zuev.osu.menu;

import android.annotation.SuppressLint;

import com.reco1l.andengine.sprite.UISprite;
import com.reco1l.andengine.sprite.ScaleType;
import com.reco1l.andengine.text.UIText;
import com.reco1l.andengine.Anchor;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.RotationByModifier;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.scene.background.Background;
import org.andengine.opengl.texture.region.TextureRegion;

import java.util.ArrayList;

import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.GlobalManager;

public class LoadingScreen implements IUpdateHandler {
    private final LoadingScene scene;
    private final UIText logText;
    private float percentage;

    public LoadingScreen() {
        ArrayList<String> toastLoggerLog = ToastLogger.getLog();

        if (toastLoggerLog != null) {
            toastLoggerLog.clear();
        }

        scene = new LoadingScene();
        // Default solid-color background (shown when no menu-background texture)
        scene.setBackground(new Background(70 / 255f, 129 / 255f, 252 / 255f));

        // Background sprite
        final TextureRegion tex = ResourceManager.getInstance().getTexture("menu-background");
        if (tex != null) {
            var menuBg = new UISprite();
            menuBg.setTextureRegion(tex);
            menuBg.setScaleType(ScaleType.Crop);
            menuBg.setSize(Config.getRES_WIDTH(), Config.getRES_HEIGHT());
            scene.attachChild(menuBg);
        }

        // Loading title sprite
        final TextureRegion loadingTexture = ResourceManager.getInstance().getTexture("loading-title");
        if (loadingTexture != null) {
            var loadingTitle = new UISprite();
            loadingTitle.setTextureRegion(loadingTexture);
            loadingTitle.setScaleType(ScaleType.Fit);
            loadingTitle.setSize((float) Config.getRES_WIDTH(), loadingTexture.getHeight());
            scene.attachChild(loadingTitle);
        }

        // Percentage log text
        logText = new UIText();
        logText.setFont(ResourceManager.getInstance().getFont("logFont"));
        scene.attachChild(logText);
        ToastLogger.setPercentage(-1);
        percentage = -1;

        var circle = new UISprite();
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
