package com.reco1l.scenes;

// Created by Reco1l on 19/11/2022, 23:31

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.tables.DialogTable;
import com.reco1l.tables.NotificationTable;
import com.reco1l.utils.helpers.BeatmapHelper;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Updater;

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

        Game.libraryManager.loadLibraryCache(Game.activity, false);

        Config.loadOnlineConfig(Game.activity);
        Game.onlineManager.Init(Game.activity);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {

    }

    @Override
    protected void onFirstShow() {
        Game.onlineScoring.login();
        Updater.getInstance().checkForUpdates();
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(TrackInfo track, boolean wasAudioChanged) {
        super.onMusicChange(track, wasAudioChanged);

        if (Game.engine.getScene() != this) {
            return;
        }

        Game.activity.runOnUiThread(() -> {
            if (UI.topBar.musicButton != null) {
                UI.topBar.musicButton.changeMusic(track.getBeatmap());
            }

            String text = "Now playing: "
                    + BeatmapHelper.getTitle(track)
                    + " by "
                    + BeatmapHelper.getArtist(track);

            NotificationTable.debug(text);
        });
    }

    @Override
    public boolean onBackPress() {
        new Dialog(DialogTable.exit()).show();
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    public void onExit() {
        Game.musicManager.stop();
        Game.resourcesManager.getSound("seeya").play();
        UI.mainMenu.onExit();
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
        super.onSceneChange(oldScene, newScene);
        if (newScene == this) {
            UI.background.setBlur(false);
        }
    }
}
