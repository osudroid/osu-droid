package ru.nsu.ccfit.zuev.osu.menu;

import org.andengine.engine.handler.IUpdateHandler;
import org.andengine.entity.modifier.FadeInModifier;
import org.andengine.entity.modifier.FadeOutModifier;
import org.andengine.entity.modifier.LoopEntityModifier;
import org.andengine.entity.modifier.ParallelEntityModifier;
import org.andengine.entity.modifier.RotationByModifier;
import org.andengine.entity.modifier.ScaleModifier;
import org.andengine.entity.modifier.SequenceEntityModifier;
import org.andengine.entity.scene.Scene;
import org.andengine.entity.sprite.Sprite;
import org.andengine.entity.text.Text;

import org.andengine.entity.text.TextOptions;
import org.andengine.util.HorizontalAlign;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;

/**
 * Created by Fuuko on 2015/4/25.
 */
public class SplashScene implements IUpdateHandler {

    public static final SplashScene INSTANCE = new SplashScene();
    private final Scene scene;
    private Text infoText;
    private Text progressText;
    private Sprite mLoading;

    public SplashScene() {
        scene = new Scene();
        initializeLoading();
        initializeProgress();
        initializeInfo();
        scene.registerUpdateHandler(this);
    }

    private void initializeLoading() {
        var loadTex = ResourceManager.getInstance().getTexture("loading_start");

        mLoading = new Sprite(0, 0, loadTex, GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        mLoading.setPosition((Config.getRES_WIDTH() - mLoading.getWidth()) / 2f, (Config.getRES_HEIGHT() - mLoading.getHeight()) / 2f);
        mLoading.setRotationCenter(mLoading.getWidth() / 2f, mLoading.getHeight() / 2f);
        mLoading.setScale(0.4f);
        mLoading.setAlpha(0);

        mLoading.registerEntityModifier(new FadeInModifier(0.2f));
        mLoading.registerEntityModifier(new LoopEntityModifier(new RotationByModifier(2f, 360)));
        scene.attachChild(mLoading);
    }

    private void initializeInfo() {
        infoText = new Text(0, 0, ResourceManager.getInstance().getFont("font"), "", 1024, new TextOptions(HorizontalAlign.CENTER), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        infoText.setPosition((Config.getRES_WIDTH() - infoText.getWidth()) / 2, Config.getRES_HEIGHT() - infoText.getHeight() - 20);
        infoText.setAlpha(0);
        infoText.setScale(0.6f);
        scene.attachChild(infoText);
    }

    public Scene getScene() { return scene; }

    public void playWelcomeAnimation()
    {
        mLoading.clearEntityModifiers();
        mLoading.registerEntityModifier(new FadeOutModifier(0.2f));

        // Text isn't compatible with animations unfortunately
        SyncTaskManager.getInstance().run(() -> {
            infoText.detachSelf();
            progressText.detachSelf();
        });

        try
        {
            Thread.sleep(220);
        }
        catch (InterruptedException ignored)
        {
        }

        var welcomeTex = ResourceManager.getInstance().getTexture("welcome");
        var welcomeSprite = new Sprite(0, 0, ResourceManager.getInstance().getTexture("welcome"), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());

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

    private void initializeProgress() {
        progressText = new Text(0, 0, ResourceManager.getInstance().getFont("font"), "0 %", 10, new TextOptions(HorizontalAlign.CENTER), GlobalManager.getInstance().getEngine().getVertexBufferObjectManager());
        progressText.setPosition((Config.getRES_WIDTH() - progressText.getWidth()) / 2f, (Config.getRES_HEIGHT() + mLoading.getHeight()) / 2f - mLoading.getHeight() / 4f);
        progressText.setAlpha(0);
        progressText.setScale(0.5f);
        scene.attachChild(progressText);
    }

    @Override
    public void onUpdate(float pSecondsElapsed) {
        float progress = GlobalManager.getInstance().getLoadingProgress();

        progressText.setText(String.format("%.0f %%", progress));
        progressText.setPosition((Config.getRES_WIDTH() - progressText.getWidth()) / 2f, (Config.getRES_HEIGHT() + mLoading.getHeight()) / 2f - mLoading.getHeight() / 4f);

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

