package com.reco1l.andengine.scenes;

// Created by Reco1l on 19/11/2022, 23:31

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.data.DialogTable;
import com.reco1l.utils.NotificationTable;
import com.reco1l.utils.helpers.BeatmapHelper;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;

public class MainScene extends BaseScene {

    private static MainScene instance;

    //--------------------------------------------------------------------------------------------//

    public static MainScene getInstance() {
        if (instance == null) {
            instance = new MainScene();
        }
        return instance;
    }

    @Override
    public Screens getIdentifier() {
        return Screens.Main;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setContinuousPlay(true);

        Game.library.loadLibraryCache(Game.activity, false);

        Config.loadOnlineConfig(Game.activity);
        Game.online.Init(Game.activity);
        Game.onlineScoring.login();

        setTouchAreaBindingEnabled(true);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {

    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(BeatmapInfo beatmap) {
        super.onMusicChange(beatmap);

        if (Game.engine.getScene() != this)
            return;

        Game.activity.runOnUiThread(() -> {
            UI.musicPlayer.changeMusic(beatmap);

            if (UI.topBar.musicButton != null) {
                UI.topBar.musicButton.changeMusic(beatmap);
            }

            String text = "Now playing: "
                    + BeatmapHelper.getTitle(beatmap)
                    + " by "
                    + BeatmapHelper.getArtist(beatmap);

            NotificationTable.debug(text);
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
        Game.musicManager.stop();
        Game.resources.getSound("seeya").play();
        UI.mainMenu.onExit();
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
        if (newScene == this) {
            UI.background.setBlur(false);
        }
    }
}
