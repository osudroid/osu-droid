package com.reco1l.andengine.scenes;

// Created by Reco1l on 26/9/22 17:40

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.replay.OdrDatabase;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.andengine.OsuScene;
import com.reco1l.utils.Animation;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;

public class SongMenu extends OsuScene {

    public static SongMenu instance;

    public TrackInfo currentTrack, lastTrack;

    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.SONG_MENU;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setTimingWrapper(true);
        setContinuousPlay(false);

        bindDataBaseChangedListener();
        setTouchAreaBindingEnabled(true);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {
    }

    public void load() {
        setTouchAreaBindingEnabled(false);
        bindDataBaseChangedListener();
        Game.global.getGameScene().setOldScene(this);
    }

    //--------------------------------------------------------------------------------------------//

    public void onTrackSelect(TrackInfo track) {
        Game.global.setSelectedTrack(track);

        if (currentTrack == track) {
            Game.resources.getSound("menuhit").play();
            Game.musicManager.stop();
            Game.global.getGameScene().startGame(track, null);
            lastTrack = currentTrack;
            currentTrack = null;
            return;
        }

        EdExtensionHelper.onSelectTrack(track);

        UI.beatmapPanel.updateProperties(track);
        UI.beatmapPanel.updateScoreboard();

        background.setTexture(track.getBackground(), true);
        currentTrack = track;
    }

    public void playMusic(BeatmapInfo beatmap) {
        Game.musicManager.change(beatmap);
        Game.global.getSongService().setVolume(0);

        if (beatmap.getPreviewTime() >= 0) {
            Game.global.getSongService().seekTo(beatmap.getPreviewTime());
        } else {
            Game.global.getSongService().seekTo(Game.global.getSongService().getLength() / 2);
        }

        new Animation().ofFloat(0, Config.getBgmVolume())
                .runOnUpdate(Game.global.getSongService()::setVolume)
                .play(400);
    }

    @Override
    public void onMusicEnd() {
        playMusic(Game.musicManager.beatmap);
    }

    @Override
    public boolean onBackPress() {
        Game.engine.setScene(Game.mainScene);
        return true;
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
        if (newScene != this) {
            currentTrack = null;
        }
    }

    //--------------------------------------------------------------------------------------------//

    public void bindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(UI.beatmapPanel::updateScoreboard);
    }

    public void unbindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(null);
    }
}
