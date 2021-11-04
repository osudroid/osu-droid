package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.RotationByModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.HorizontalAlign;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;

/**
 * Created by Fuuko on 2015/4/25.
 */
public class SplashScene implements IUpdateHandler {
    private final Scene scene;
    //private Sprite logo2;
    private final Rectangle progressRect;
    private final Rectangle bgRect;
    private final ChangeableText infoText;
    private final ChangeableText progressText;

    public SplashScene() {
        scene = new Scene();
        scene.registerEntityModifier(new FadeOutModifier(0.5f));
        final TextureRegion logotex = ResourceManager.getInstance().getTexture("logo");
        Sprite logo = new Sprite(0, 0, logotex);
        logo.setWidth(500);
        logo.setHeight(500);
        logo.setPosition((Config.getRES_WIDTH() - logotex.getWidth()) / 2f, (Config.getRES_HEIGHT() - logotex.getHeight()) / 2f - 40);
        logo.setRotationCenter(250, 250);
        scene.attachChild(logo);

        //   final TextureRegion welcometex = ResourceManager.getInstance().getTexture("welcome");
        //   logo2 = new Sprite(0, 0, welcometex);
        // logo2.setWidth(375);
        //  logo2.setHeight(78);
        //  logo2.setPosition((Config.getRES_WIDTH() - welcometex.getWidth()) / 2, (Config.getRES_HEIGHT() - welcometex.getHeight()) / 2 - 40);
        //  logo2.setRotationCenter(0, 0);
        //  logo2.set((((Dimmer scene)))
        //  scene.attachChild(logo2);

        final TextureRegion bgtex = ResourceManager.getInstance().getTexture("loading-background");
        if (bgtex != null) {
            float height = bgtex.getHeight();
            height *= Config.getRES_WIDTH()
                    / (float) bgtex.getWidth();
            final Sprite menuBg = new Sprite(
                    0,
                    (Config.getRES_HEIGHT() - height) / 2,
                    Config.getRES_WIDTH(),
                    height, bgtex);
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new ColorBackground(70 / 255f, 129 / 255f,
                    252 / 255f));
        }
        logo.registerEntityModifier(new LoopEntityModifier(
                new RotationByModifier(4.0f, 360)));

        bgRect = new Rectangle(0, 0, Utils.toRes(800), Utils.toRes(50));
        bgRect.setPosition((Config.getRES_WIDTH() - bgRect.getWidth()) / 2, Config.getRES_HEIGHT() - 90);
        bgRect.setColor(0, 0, 0, 0.4f);
        scene.attachChild(bgRect);

        progressRect = new Rectangle(bgRect.getX(), bgRect.getY(), 0,
                bgRect.getHeight());
        progressRect.setColor(0, 0.8f, 0);
        scene.attachChild(progressRect);

        progressText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "0 %", HorizontalAlign.CENTER, 10);
        progressText.setPosition((Config.getRES_WIDTH() - progressText.getWidth()) / 2, bgRect.getY());
        scene.attachChild(progressText);

        infoText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("strokeFont"), "", HorizontalAlign.CENTER, 1024);
        infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, bgRect.getY() - infoText.getHeight() - 10);
        scene.attachChild(infoText);

        scene.registerUpdateHandler(this);
    }

    public Scene getScene() {
        return scene;
    }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        progressRect.setWidth(bgRect.getWidth() * (GlobalManager.getInstance().getLoadingProgress() / 100f));
        progressText.setText(GlobalManager.getInstance().getLoadingProgress() + " %");
        progressText.setPosition((Config.getRES_WIDTH() - progressText.getWidth()) / 2, bgRect.getY() + (bgRect.getHeight() - progressText.getHeight()) / 2);
        if (GlobalManager.getInstance().getInfo() != null) {
            infoText.setText(GlobalManager.getInstance().getInfo());
            infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, bgRect.getY() - infoText.getHeight() - 10);
        }
    }

    @Override
    public void reset() {

    }
}
