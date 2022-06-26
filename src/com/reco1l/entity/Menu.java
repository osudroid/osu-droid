package com.reco1l.entity;

import com.reco1l.utils.IMainClasses;

import org.anddev.andengine.entity.IEntity;
import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.modifier.IEntityModifier;
import org.anddev.andengine.entity.modifier.MoveXModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.RotationModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.modifier.SequenceEntityModifier;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.modifier.IModifier;
import org.anddev.andengine.util.modifier.ease.EaseQuadInOut;
import org.anddev.andengine.util.modifier.ease.IEaseFunction;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.MainScene;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu;

// Created by Reco1l on 20/6/22 03:39

public class Menu implements IMainClasses {

    @SuppressWarnings("FieldCanBeLocal")
    private final float
            logoScale = 0.89f,
            maxScale = logoScale + 0.07f,
            downScale = 0.17f,
            animTime = 0.3f;

    private float
            logoFinalX,
            logoCenterX,
            menuX;
    private boolean
            logoAnimInProgress = false,
            isLogoSmall = false,
            isOnExitAnim = false;

    public boolean isMenuShowing = false;
    private int showPassTime = 0;

    private final IEaseFunction interpolator = EaseQuadInOut.getInstance();
    private static Sprite logo, play, exit, settings;
    private Scene scene;

    //--------------------------------------------------------------------------------------------//
    
    public void draw() {
        TextureRegion
            logoTex = resources.getTexture("logo"),
            playTex = resources.getTexture("play"),
            exitTex = resources.getTexture("exit"),
            settingTex = resources.getTexture("options");

        logo = new Sprite(0, 0, logoTex) {
            @Override
            public boolean onAreaTouched(final TouchEvent event, float X, float Y) {
                performClick(logo, event);
                return true;
            }
        };
        play = new Sprite(0, 0, playTex) {
            @Override
            public boolean onAreaTouched(final TouchEvent event, float X, float Y) {
                performClick(play, event);
                return true;
            }
        };
        settings = new Sprite(0, 0, settingTex) {
            @Override
            public boolean onAreaTouched(final TouchEvent event, float X, float Y) {
                performClick(settings, event);
                return true;
            }
        };
        exit = new Sprite(0, 0, exitTex) {
            @Override
            public boolean onAreaTouched(final TouchEvent event, float X, float Y) {
                performClick(exit, event);
                return true;
            }
        };
    }

    public void adjust() {

        int resH = Config.getRES_HEIGHT();
        int resW = Config.getRES_WIDTH();
        float logoX = (resW - logo.getWidth()) / 2f;
        float logoY = (resH - logo.getHeight()) / 2;

        logo.setPosition(logoX, logoY);
        play.setAlpha(0f);
        play.setScale(resW / 1024f);
        settings.setAlpha(0f);
        settings.setScale(resW / 1024f);
        exit.setAlpha(0f);
        exit.setScale(resW / 1024f);

        logoFinalX = Config.getRES_WIDTH() / 5f - logo.getWidth() / 2;
        logoCenterX = Config.getRES_WIDTH() / 2f - logo.getWidth() / 2;
        menuX = Config.getRES_WIDTH() / 5f;

        logo.setScale(logoScale);
        float menuY = Config.getRES_HEIGHT() / 2f - play.getHeight() / 2f;
        play.setPosition(menuX, menuY);
        settings.setPosition(menuX, menuY);
        exit.setPosition(menuX, menuY);
    }

    public void attach(Scene scene){
        this.scene = scene;
        scene.attachChild(exit);
        scene.attachChild(settings);
        scene.attachChild(play);
        scene.attachChild(logo);
        scene.registerTouchArea(logo);
    }

    private void show(Scene scene) {

        //global.getMainScene().getBackground().zoomIn();

        logo.clearEntityModifiers();
        logo.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime, logoCenterX, logoFinalX, interpolator),
                new ScaleModifier(animTime, logo.getScaleX(), logoScale - downScale, new IEntityModifier.IEntityModifierListener() {
                    @Override
                    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                        scene.unregisterTouchArea(logo);
                        logoAnimInProgress = true;
                    }
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        isLogoSmall = true;
                        logoAnimInProgress = false;
                        scene.registerTouchArea(logo);
                        scene.registerTouchArea(play);
                        scene.registerTouchArea(settings);
                        scene.registerTouchArea(exit);
                    }
                }, interpolator)
        ));

        play.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime, menuX - 20, menuX, interpolator),
                new AlphaModifier(animTime, 0, 1f, interpolator)
        ));
        settings.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime + 0.04f, menuX - 20, menuX, interpolator),
                new AlphaModifier(animTime + 0.04f, 0, 1f, interpolator)
        ));
        exit.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime + 0.08f, menuX - 20, menuX, interpolator),
                new AlphaModifier(animTime + 0.08f, 0, 1f, interpolator)
        ));
    }

    private void hide(Scene scene) {
        scene.unregisterTouchArea(play);
        scene.unregisterTouchArea(settings);
        scene.unregisterTouchArea(exit);

        //global.getMainScene().getBackground().zoomOut();

        logo.clearEntityModifiers();
        logo.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime, logoFinalX, logoCenterX, EaseQuadInOut.getInstance()),
                new ScaleModifier(animTime, logo.getScaleX(), logoScale,  new IEntityModifier.IEntityModifierListener() {
                    @Override
                    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                        logoAnimInProgress = true;
                        scene.unregisterTouchArea(logo);
                    }
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        isLogoSmall = false;
                        logoAnimInProgress = false;
                        scene.registerTouchArea(logo);
                    }
                }, EaseQuadInOut.getInstance())
        ));

        play.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime, menuX, menuX - 30, interpolator),
                new AlphaModifier(animTime, 1f, 0f, interpolator)
        ));
        settings.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime + 0.04f, menuX, menuX - 60, interpolator),
                new AlphaModifier(animTime + 0.04f, 1f, 0f, interpolator)
        ));
        exit.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime + 0.08f, menuX, menuX - 90, interpolator),
                new AlphaModifier(animTime + 0.08f, 1f, 0f, interpolator)
        ));

        showPassTime = 0;
    }

    
    //-----------------------------------------Actions--------------------------------------------//

    private final OsuAsyncCallback playTask = new OsuAsyncCallback() {
        public void run() {

            //global.getEngine().setScene(LoadingScreen.getInstance().getScene());
            global.getEngine().setScene(new LoadingScreen().getScene());
            mActivity.checkNewSkins();
            mActivity.checkNewBeatmaps();
            if (!library.loadLibraryCache(mActivity, true)) {
                library.scanLibrary(mActivity);
                System.gc();
            }
            global.getSongMenu().reload();
        }

        public void onComplete() {
            global.getMainScene().musicControl(MainScene.MusicOption.PLAY);
            global.getEngine().setScene(global.getSongMenu().getScene());
            global.getSongMenu().select();
        }
    };

    private void performClick(Sprite button, TouchEvent event) {
        if (isOnExitAnim)
            return;

        if (event.isActionDown()) {
            resources.loadSound("menuhit", "sfx/menuhit.ogg", false).play();
            if (button != logo)
                button.setColor(0.7f, 0.7f, 0.7f);
        }

        if (event.isActionUp()) {

            if (button == logo) {
                new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                    @Override public void run() {
                        if (!isMenuShowing) show(scene);
                        else hide(scene);
                    }
                    @Override
                    public void onComplete() {
                        isMenuShowing = !isMenuShowing;
                    }
                });
                return;
            }

            button.setColor(1, 1, 1);

            if (button == play || button == exit) {
                if (global.getCamera().getRotation() == 0) Utils.setAccelerometerSign(1);
                else Utils.setAccelerometerSign(-1);
            }

            if (button == play) {
                global.getSongService().setGaming(true);
                new AsyncTaskLoader().execute(playTask);
                return;
            }

            if (button == settings){
                global.getSongService().setGaming(true);
                mActivity.runOnUiThread(() -> new SettingsMenu().show());
                //mActivity.runOnUiThread(() -> Kyo.settingsPanel().show());
                return;
            }

            global.getMainScene().showExitDialog();

           /* new Dialog().create("Exit", mActivity.getString(R.string.dialog_exit_message), true, true)
                    .setButton(0, "Yes", global.getMainScene()::exit, true)
                    .setButton(1, "No", null).show();*/
        }
    }

    public void update(float secondsElapsed, boolean doBounce, float bpm) {
        if (isOnExitAnim)
            return;

        if (isMenuShowing) {
            if (showPassTime > 10000f) {
                hide(scene);
                isMenuShowing = false;
            }
            else showPassTime += secondsElapsed * 1000f;
        }

        if (logoAnimInProgress || !doBounce)
            return;

        if (isLogoSmall) {
            logo.registerEntityModifier(new SequenceEntityModifier(
                    new ScaleModifier(bpm / 1000 * 0.9f, logoScale - downScale,
                            maxScale - downScale),
                    new ScaleModifier(bpm / 1000 * 0.07f, maxScale - downScale,
                            logoScale - downScale)
            ));
            return;
        }
        logo.registerEntityModifier(new SequenceEntityModifier(
                new ScaleModifier(bpm / 1000 * 0.9f, logoScale, maxScale),
                new ScaleModifier(bpm / 1000 * 0.07f, maxScale, logoScale)
        ));
    }

    public void doExitAnim() {
        hide(scene);
        isOnExitAnim = true;
        logo.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                ModifierFactory.newScaleModifier(3.0f, 1f, 0.8f)
        ));
    }

}
