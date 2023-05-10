package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.engine.handler.IUpdateHandler;
import org.anddev.andengine.entity.modifier.*;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import org.anddev.andengine.util.HorizontalAlign;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;

/**
 * Created by Fuuko on 2015/4/25.
 */
public class SplashScene implements IUpdateHandler {

    public static final SplashScene INSTANCE = new SplashScene();

    private final Scene scene;

    private ChangeableText infoText;
    private Sprite mLoading;

    private boolean mStarting = true;

    public SplashScene() {
        scene = new Scene();
        initializeLoading();
        initializeInfo();
        scene.registerUpdateHandler(this);
    }

    private void initializeLoading() {
        var loadTex = ResourceManager.getInstance().getTexture("loading_start");

        mLoading = new Sprite(0, 0, loadTex);
        mLoading.setPosition((Config.getRES_WIDTH() - mLoading.getWidth()) / 2f, (Config.getRES_HEIGHT() - mLoading.getHeight()) / 2f);
        mLoading.setRotationCenter(mLoading.getWidth() / 2f, mLoading.getHeight() / 2f);
        mLoading.setAlpha(0);

        mLoading.registerEntityModifier(new LoopEntityModifier(new RotationByModifier(2f, 360)));
        scene.attachChild(mLoading);
    }

    private void initializeInfo() {
        infoText = new ChangeableText(0, 0, ResourceManager.getInstance().getFont("font"), "", HorizontalAlign.CENTER, 1024);
        infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, Config.getRES_HEIGHT() - infoText.getHeight() - 20);
        infoText.setAlpha(0);
        scene.attachChild(infoText);
    }

    public Scene getScene() { return scene; }

    public void playWelcomeAnimation()
    {
        mStarting = false;

        // ChangeableText isn't compatible with animations unfortunately
        infoText.detachSelf();
        mLoading.registerEntityModifier(new FadeOutModifier(0.2f));

        try
        {
            Thread.sleep(220);
        }
        catch (InterruptedException ignored)
        {
        }

        var welcomeTex = ResourceManager.getInstance().getTexture("welcome");
        var welcomeSprite = new Sprite(0, 0, ResourceManager.getInstance().getTexture("welcome"));

        var welcomeSound = ResourceManager.getInstance().getSound("welcome");
        var welcomePiano = ResourceManager.getInstance().getSound("welcome_piano");

        welcomeSprite.setPosition((Config.getRES_WIDTH() - welcomeTex.getWidth()) / 2f, (Config.getRES_HEIGHT() - welcomeTex.getHeight()) / 2f);
        welcomeSprite.setAlpha(0);
        welcomeSprite.setScaleY(0);
        scene.attachChild(welcomeSprite);
        welcomeSound.play();
        welcomePiano.play();

        welcomeSprite.registerEntityModifier(new ParallelEntityModifier(
                new FadeInModifier(2.5f),
                new SequenceEntityModifier(
                        new ScaleModifier(0.25f, 1f, 1f, 0f, 1f),
                        new ScaleModifier(2.25f, 1f, 1.1f)
                )
        ));
    }

    @Override
    public void onUpdate(float pSecondsElapsed) {

        if (mStarting)
        {
            mLoading.setAlpha(mLoading.getAlpha() + 0.1f);
        }

        if (GlobalManager.getInstance().getInfo() != null) {
            infoText.setText(GlobalManager.getInstance().getInfo());
            infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, Config.getRES_HEIGHT() - infoText.getHeight() - 20);
        }
    }

    @Override
    public void reset()
    {

    }
}

