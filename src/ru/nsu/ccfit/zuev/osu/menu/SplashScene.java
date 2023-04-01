package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.FadeOutModifier;
import org.anddev.andengine.entity.modifier.LoopEntityModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.ColorBackground;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.HorizontalAlign;
import org.anddev.andengine.util.modifier.ease.EaseBounceOut;
import org.anddev.andengine.util.modifier.ease.EaseElasticOut;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.Utils;

/**
 * Created by Fuuko on 2015/4/25.
 */
public class SplashScene implements IUpdateHandler {
    private final Scene scene;
    private Rectangle progressRect;
    private Rectangle bgRect;
    private ChangeableText infoText;
    private ChangeableText progressText;
    private float currentProgressWidth = 0;

    public SplashScene() {
        scene = new Scene();
        scene.registerEntityModifier(new FadeOutModifier(0.5f));
        initializeLogo();
        initializeBackground();
        initializeProgress();
        initializeInfo();
        scene.registerUpdateHandler(this);
    }
    private void initializeLogo() {
        final TextureRegion logoTexture = ResourceManager.getInstance().getTexture("logo");
        float targetScale = 0.7f;
        float scaleDuration = 1f;

        Sprite logo = new Sprite(0, 0, logoTexture);
        logo.setWidth(500);
        logo.setHeight(500);
        logo.setPosition((Config.getRES_WIDTH() - logoTexture.getWidth()) / 2f, (Config.getRES_HEIGHT() - logoTexture.getHeight()) / 2f - 40);

        ScaleModifier scaleUpModifier = new ScaleModifier(scaleDuration, 0.6f, targetScale, EaseElasticOut.getInstance());
        ScaleModifier scaleDownModifier = new ScaleModifier(scaleDuration, targetScale, 0.6f, EaseElasticOut.getInstance());
        SequenceEntityModifier pulseModifier = new SequenceEntityModifier(scaleUpModifier, scaleDownModifier);
        LoopEntityModifier loopModifier = new LoopEntityModifier(pulseModifier);
        RotationModifier rotateModifier = new RotationModifier(1f, 0f, 360f, EaseBounceOut.getInstance());
        ParallelEntityModifier parallelModifier = new ParallelEntityModifier(loopModifier, rotateModifier);
        FadeInModifier fadeInModifier = new FadeInModifier(0.1f);
        FadeOutModifier fadeOutModifier = new FadeOutModifier(0.1f);
        SequenceEntityModifier sequenceModifier = new SequenceEntityModifier(fadeInModifier, parallelModifier, fadeOutModifier);

        logo.registerEntityModifier(sequenceModifier);
        scene.attachChild(logo);
    }

    private void initializeBackground() {
        final TextureRegion bgTexture = ResourceManager.getInstance().getTexture("loading-background");

        if (bgTexture != null) {
            float height = bgTexture.getHeight() * Config.getRES_WIDTH() / (float) bgTexture.getWidth();
            final Sprite menuBg = new Sprite(0, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH(), height, bgTexture);
            scene.setBackground(new SpriteBackground(menuBg));
        } else {
            scene.setBackground(new ColorBackground(70 / 255f, 129 / 255f, 252 / 255f));
        }
    }

    private void initializeProgress() {
        bgRect = new Rectangle(0, 0, Utils.toRes(800), Utils.toRes(50));
        bgRect.setPosition((Config.getRES_WIDTH() - bgRect.getWidth()) / 2, Config.getRES_HEIGHT() - 90);
        bgRect.setColor(0, 0, 0, 0.4f);
        scene.attachChild(bgRect);

        progressRect = new Rectangle(bgRect.getX(), bgRect.getY(), 0,bgRect.getHeight());
        scene.attachChild(progressRect);

        progressText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "0 %", HorizontalAlign.CENTER, 10);
        progressText.setPosition((Config.getRES_WIDTH() - progressText.getWidth()) / 2f, bgRect.getY() + bgRect.getHeight() / 2f - progressText.getHeight() / 2f);
        scene.attachChild(progressText);
    }
    private void initializeInfo() {
        infoText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("strokeFont"), "", HorizontalAlign.CENTER, 1024);
        infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, bgRect.getY() - infoText.getHeight() - 10);
        scene.attachChild(infoText);
    }

    public Scene getScene() { return scene; }

    @Override
    public void onUpdate(final float pSecondsElapsed) {
        float progress = GlobalManager.getInstance().getLoadingProgress();
        float targetProgressWidth = bgRect.getWidth() * (progress / 100f);

        currentProgressWidth += (targetProgressWidth - currentProgressWidth) * 0.1f;
        progressRect.setWidth(currentProgressWidth);

        progressText.setText(String.format("%.0f %%", progress));
        progressText.setPosition((Config.getRES_WIDTH() - progressText.getWidth()) / 2, bgRect.getY() + (bgRect.getHeight() - progressText.getHeight()) / 2);

        if (progress < 35) {
            progressRect.setColor(0.8f, 0, 0);
        } else if (progress < 85) {
            progressRect.setColor(0.8f, 0.8f, 0);
        } else {
            progressRect.setColor(0, 0.8f, 0);
        }

        if (GlobalManager.getInstance().getInfo() != null) {
            infoText.setText(GlobalManager.getInstance().getInfo());
            infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, bgRect.getY() - infoText.getHeight() - 10);
        }
    }

    @Override
    public void reset() { }
}

