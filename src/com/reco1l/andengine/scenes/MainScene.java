package com.reco1l.andengine.scenes;

// Created by Reco1l on 19/11/2022, 23:31

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;
import com.reco1l.game.TimingWrapper;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.data.DialogTable;

import org.anddev.andengine.entity.modifier.AlphaModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public class MainScene extends BaseScene {


    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.Main;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setContinuousPlay(true);
        setTimingWrapper(true);

        timingWrapper.setObserver(new TimingWrapper.Observer() {
            @Override
            public void onKiaiStart() {
                UI.mainMenu.setLogoKiai(true);
                UI.background.setKiai(true);
            }

            @Override
            public void onKiaiEnd() {
                UI.mainMenu.setLogoKiai(false);
                UI.background.setKiai(false);
            }

            @Override
            public void onBeatUpdate(float BPMLength, int beat) {
                UI.mainMenu.onBeatUpdate(BPMLength);
                UI.background.onBeatUpdate(BPMLength, beat);
            }
        });

        Game.library.loadLibraryCache(Game.activity, false);

        Config.loadOnlineConfig(Game.activity);
        Game.online.Init(Game.activity);
        Game.onlineScoring.login();

        setTouchAreaBindingEnabled(true);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {

    }

    @Override
    public void onMusicChange(BeatmapInfo beatmap) {
        super.onMusicChange(beatmap);

        Game.activity.runOnUiThread(() -> {
            UI.musicPlayer.changeMusic(beatmap);

            if (UI.topBar.musicButton != null) {
                UI.topBar.musicButton.changeMusic(beatmap);
            }
        });
    }

    @Override
    public boolean onBackPress() {
        new Dialog(DialogTable.exit()).show();
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    public void loadMusic() {
        Game.library.shuffleLibrary();

        if (Game.library.getLibrary() != null && Game.library.getSizeOfBeatmaps() > 0) {
            Game.musicManager.play();
        }
        UI.debugOverlay.show();
    }

    public void onExit() {
        Rectangle dim = new Rectangle(0, 0, screenWidth, screenHeight);
        dim.setColor(0, 0, 0, 0f);
        attachChild(dim, 2);

        UI.mainMenu.onExit();
        Game.resources.getSound("seeya").play();
        dim.registerEntityModifier(new AlphaModifier(3.0f, 0, 1));
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
        super.onSceneChange(oldScene, newScene);
        if (newScene == this) {
            UI.background.setBlur(false);
        }
    }
}
