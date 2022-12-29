package com.reco1l.scenes;

// Created by Reco1l on 26/9/22 17:40

import com.edlplan.replay.OdrDatabase;
import com.reco1l.Game;
import com.reco1l.UI;
import com.reco1l.enums.Screens;
import com.reco1l.utils.Animation;
import com.reco1l.view.IconButton;

import org.anddev.andengine.entity.scene.Scene;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SelectorScene extends BaseScene {

    private static SelectorScene instance;

    public TrackInfo selectedTrack;

    //--------------------------------------------------------------------------------------------//

    public static SelectorScene getInstance() {
        if (instance == null) {
            instance = new SelectorScene();
        }
        return instance;
    }

    @Override
    public Screens getIdentifier() {
        return Screens.Selector;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setContinuousPlay(false);
        setBackgroundAutoChange(false);

        bindDataBaseChangedListener();
        setTouchAreaBindingEnabled(true);
        createTopBarButtons();
    }

    private void createTopBarButtons() {
        IconButton mods = UI.modMenu.button = new IconButton(context);

        mods.runOnTouch(UI.modMenu::altShow);
        mods.setIcon(R.drawable.v_tune);

        IconButton search = new IconButton(context);
        search.runOnTouch(UI.filterMenu::altShow);
        search.setIcon(R.drawable.v_search);

        /*BarButton random = new BarButton(context);
        random.setIcon(R.drawable.v_random);*/

        UI.topBar.addButton(getIdentifier(), mods);
        UI.topBar.addButton(getIdentifier(), search);
        /*UI.topBar.addButton(getIdentifier(), random);*/
    }


    @Override
    public void show() {
        super.show();

        setTouchAreaBindingEnabled(false);
        bindDataBaseChangedListener();
        Game.gameScene.setOldScene(this);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {}

    //--------------------------------------------------------------------------------------------//

    public void onTrackSelect(TrackInfo track) {
        if (Game.musicManager.getTrack() == track) {
            return;
        }

        Game.musicManager.change(track);
        Game.songService.setVolume(0);

        if (track.getPreviewTime() >= 0) {
            Game.songService.seekTo(track.getPreviewTime());
        } else {
            Game.songService.seekTo(Game.songService.getLength() / 2);
        }

        Animation.ofFloat(0, Config.getBgmVolume())
                .runOnUpdate(value -> Game.songService.setVolume((float) value))
                .play(400);

        Game.globalManager.setSelectedTrack(track);
        UI.beatmapPanel.updateProperties(track);
        UI.beatmapPanel.updateScoreboard();
        UI.background.changeFrom(track.getBackground());
    }

    public void playMusic(TrackInfo track) {

    }

    //--------------------------------------------------------------------------------------------//

    public void loadScore(int id, String player) {
        boolean isOnline = UI.beatmapPanel.isOnlineBoard;

        Game.summaryScene.loadFromBoard(selectedTrack, id, isOnline, player);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(TrackInfo track, boolean wasAudioChanged) {
        super.onMusicChange(track, wasAudioChanged);

        if (track != null) {
            onTrackSelect(track);
        }
    }

    @Override
    public void onMusicEnd() {
        playMusic(Game.musicManager.getTrack());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public boolean onBackPress() {
        Game.engine.setScene(Game.mainScene);
        return true;
    }

    @Override
    public void onSceneChange(Scene oldScene, Scene newScene) {
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
