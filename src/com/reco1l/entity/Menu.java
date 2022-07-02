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

    private Scene parent;

    @SuppressWarnings("FieldCanBeLocal")
    private final float
            logoScale = 0.89f,
            maxScale = logoScale + 0.07f,
            downScale = 0.17f,
            animTime = 0.3f;

    private float logoFinalX, logoCenterX, menuX;
    public boolean isMenuShowing = false;

    private boolean
            logoAnimInProgress = false,
            isOnExitAnim = false,
            isLogoSmall = false;

    private final IEaseFunction interpolator = EaseQuadInOut.getInstance();
    private static Sprite logo, play, exit, settings;

    private final int resW = Config.getRES_WIDTH();
    private final int resH = Config.getRES_HEIGHT();
    private int showPassTime = 0;

    //--------------------------------------------------------------------------------------------//
    
    public void draw(Scene scene) {
        parent = scene;

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
        logo.setScale(logoScale);
        logo.setPosition((resW - logo.getWidth()) / 2f, (resH - logo.getHeight()) / 2f);

        float menuY = resH / 2f - play.getHeight() / 2f;
        menuX = resW / 5f;

        play.setAlpha(0f);
        play.setScale(resW / 1024f);
        play.setPosition(menuX, menuY);

        settings.setAlpha(0f);
        settings.setScale(resW / 1024f);
        settings.setPosition(menuX, menuY);

        exit.setAlpha(0f);
        exit.setScale(resW / 1024f);
        exit.setPosition(menuX, menuY);

        logoFinalX = resW / 5f - logo.getWidth() / 2;
        logoCenterX = resW / 2f - logo.getWidth() / 2;
    }

    public void attach(){
        if (parent == null)
            return;
        parent.attachChild(exit);
        parent.attachChild(settings);
        parent.attachChild(play);
        parent.attachChild(logo);
        parent.registerTouchArea(logo);
    }

    private void show() {

        global.getMainScene().background.zoomIn();

        logo.clearEntityModifiers();
        logo.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime, logoCenterX, logoFinalX, interpolator),
                new ScaleModifier(animTime, logo.getScaleX(), logoScale - downScale, new IEntityModifier.IEntityModifierListener() {
                    @Override
                    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                        parent.unregisterTouchArea(logo);
                        logoAnimInProgress = true;
                    }
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        isLogoSmall = true;
                        logoAnimInProgress = false;
                        parent.registerTouchArea(logo);
                        parent.registerTouchArea(play);
                        parent.registerTouchArea(settings);
                        parent.registerTouchArea(exit);
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

    private void hide() {
        parent.unregisterTouchArea(play);
        parent.unregisterTouchArea(settings);
        parent.unregisterTouchArea(exit);

        global.getMainScene().background.zoomOut();

        logo.clearEntityModifiers();
        logo.registerEntityModifier(new ParallelEntityModifier(
                new MoveXModifier(animTime, logoFinalX, logoCenterX, EaseQuadInOut.getInstance()),
                new ScaleModifier(animTime, logo.getScaleX(), logoScale,  new IEntityModifier.IEntityModifierListener() {
                    @Override
                    public void onModifierStarted(IModifier<IEntity> pModifier, IEntity pItem) {
                        logoAnimInProgress = true;
                        parent.unregisterTouchArea(logo);
                    }
                    @Override
                    public void onModifierFinished(IModifier<IEntity> pModifier, IEntity pItem) {
                        isLogoSmall = false;
                        logoAnimInProgress = false;
                        parent.registerTouchArea(logo);
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
            global.getEngine().setScene(LoadingScreen.getInstance().getScene());
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
                        if (!isMenuShowing) show();
                        else hide();
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
                hide();
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
        hide();
        isOnExitAnim = true;
        logo.registerEntityModifier(new ParallelEntityModifier(
                new RotationModifier(3.0f, 0, -15),
                ModifierFactory.newScaleModifier(3.0f, 1f, 0.8f)
        ));
    }
}
