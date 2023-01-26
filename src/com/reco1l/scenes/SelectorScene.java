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

    public static final SelectorScene instance = new SelectorScene();

    public TrackInfo selectedTrack;

    //--------------------------------------------------------------------------------------------//

    @Override
    public Screens getIdentifier() {
        return Screens.Selector;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setContinuousPlay(false);
        setBackgroundAutoChange(false);

        //bindDataBaseChangedListener();
        setTouchAreaBindingEnabled(true);
        createTopBarButtons();
    }

    private void createTopBarButtons() {
        IconButton mods = UI.modMenu.button = new IconButton(context);

        mods.runOnTouch(UI.modMenu::altShow);
        mods.setIcon(R.drawable.v18_tune);

        IconButton search = new IconButton(context);
        search.runOnTouch(UI.filterBar::altShow);
        search.setIcon(R.drawable.v18_search);

        /*BarButton random = new BarButton(context);
        random.setIcon(R.drawable.v_random);*/

        UI.topBar.bindButton(getIdentifier(), mods);
        UI.topBar.bindButton(getIdentifier(), search);
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
    protected void onSceneUpdate(float secondsElapsed) {
    }

    //--------------------------------------------------------------------------------------------//

    public void onTrackSelect(TrackInfo track) {
        if (Game.musicManager.getTrack() == track) {
            return;
        }
        Game.musicManager.change(track);
        Game.globalManager.setSelectedTrack(track);
    }

    public void onAudioChange() {
        TrackInfo track = Game.musicManager.getTrack();

        Game.songService.setVolume(0);

        if (track.getPreviewTime() != -1) {
            Game.songService.seekTo(track.getPreviewTime());
        } else {
            Game.songService.seekTo(Game.songService.getLength() / 2);
        }

        Animation.ofFloat(0, Config.getBgmVolume())
                .runOnUpdate(value -> Game.songService.setVolume((float) value))
                .play(400);
    }

    //--------------------------------------------------------------------------------------------//

    public void loadScore(int id, String player) {
        boolean isOnline = UI.beatmapPanel.isOnlineBoard;

        Game.summaryScene.loadFromBoard(selectedTrack, id, isOnline, player);
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void onMusicChange(TrackInfo newTrack, boolean isSameAudio) {
        super.onMusicChange(newTrack, isSameAudio);

        UI.background.changeFrom(newTrack.getBackground());
        if (!isSameAudio) {
            onAudioChange();
        }
    }

    @Override
    public void onMusicEnd() {
        onAudioChange();
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
        OdrDatabase.get().setOnDatabaseChangedListener(() ->
                Game.boardManager.load(Game.musicManager.getTrack())
        );
    }

    public void unbindDataBaseChangedListener() {
        OdrDatabase.get().setOnDatabaseChangedListener(null);
    }
}
