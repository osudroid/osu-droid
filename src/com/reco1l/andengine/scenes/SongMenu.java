package com.reco1l.andengine.scenes;

// Created by Reco1l on 26/9/22 17:40

import com.edlplan.ext.EdExtensionHelper;
import com.edlplan.replay.OdrDatabase;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.andengine.BaseScene;
import com.reco1l.utils.Animation;
import com.reco1l.utils.Resources;
import com.reco1l.view.BarButton;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SongMenu extends BaseScene {

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
        createTopBarButtons();
    }

    private void createTopBarButtons() {
        BarButton mods = new BarButton(Game.mActivity);

        mods.setAsToggle(true);
        mods.setIcon(Resources.drw(R.drawable.v_tune));

        BarButton search = new BarButton(Game.mActivity);

        search.setAsToggle(true);
        search.setIcon(Resources.drw(R.drawable.v_search));

        BarButton random = new BarButton(Game.mActivity);

        random.setIcon(Resources.drw(R.drawable.v_random));

        UI.topBar.addButton(getIdentifier(), mods);
        UI.topBar.addButton(getIdentifier(), search);
        UI.topBar.addButton(getIdentifier(), random);
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

        //UI.background.changeFrom(track.getBackground());
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
        if (newScene == this) {
            UI.background.setBlur(true);
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
