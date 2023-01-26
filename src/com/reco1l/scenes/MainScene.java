package com.reco1l.scenes;

// Created by Reco1l on 19/11/2022, 23:31

import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.view.IconButton;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.tables.DialogTable;
import com.reco1l.tables.NotificationTable;
import com.reco1l.utils.helpers.BeatmapHelper;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Updater;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MainScene extends BaseScene {

    public static final MainScene instance = new MainScene();

    //--------------------------------------------------------------------------------------------//

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

        IconButton music = new IconButton(Game.activity);

        music.setIcon(R.drawable.v14_music);
        music.runOnTouch(UI.musicPlayer::altShow);

        UI.musicPlayer.button = music;
        UI.topBar.bindButton(getIdentifier(), music);
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
    public void onMusicChange(TrackInfo newTrack, boolean isSameAudio) {
        super.onMusicChange(newTrack, isSameAudio);

        if (Game.engine.getScene() != this) {
            return;
        }

        Game.activity.runOnUiThread(() -> {
            String text = "Now playing: "
                    + BeatmapHelper.getTitle(newTrack)
                    + " by "
                    + BeatmapHelper.getArtist(newTrack);

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
