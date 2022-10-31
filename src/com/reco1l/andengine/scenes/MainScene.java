package com.reco1l.andengine.scenes;

// Created by Reco1l on 18/9/22 19:28

import com.reco1l.Game;
import com.reco1l.andengine.OsuScene;
import com.reco1l.andengine.entity.BeatMarker;
import com.reco1l.game.TimingWrapper;
import com.reco1l.enums.Scenes;
import com.reco1l.andengine.entity.ParticleScatter;
import com.reco1l.andengine.entity.Spectrum;
import com.reco1l.management.MusicManager;
import com.reco1l.enums.MusicOption;
import com.reco1l.UI;

import org.anddev.andengine.entity.primitive.Rectangle;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.helper.ModifierFactory;

public class MainScene extends OsuScene {

    public static MainScene instance;

    public Spectrum spectrum;

    private ParticleScatter scatter;
    private BeatMarker beatMarker;
    private int particleBeginTime = 0;

    //--------------------------------------------------------------------------------------------//

    @Override
    public Scenes getIdentifier() {
        return Scenes.MAIN;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onCreate() {
        setContinuousPlay(true);
        setTimingWrapper(true);

        this.spectrum = new Spectrum();
        this.scatter = new ParticleScatter();
        this.beatMarker = new BeatMarker();

        timingWrapper.setObserver(new TimingWrapper.Observer() {
            @Override
            public void onKiaiStart() {
                if (Game.songService != null) {
                    int position = Game.songService.getPosition();

                    scatter.start();
                    particleBeginTime = position;
                }
                beatMarker.setKiai(true);
                UI.mainMenu.setLogoKiai(true);
            }

            @Override
            public void onKiaiEnd() {
                beatMarker.setKiai(false);
                if (scatter.isEnabled) {
                    scatter.end();
                }
                UI.mainMenu.setLogoKiai(false);
            }

            @Override
            public void onBeatUpdate(float BPMLength, int beat) {
                UI.mainMenu.onBeatUpdate(BPMLength, beat);
                beatMarker.onBeatUpdate(BPMLength, beat);
            }
        });

        beatMarker.draw(this, 1);
        spectrum.draw(this, 1);
        scatter.draw(this, 1);

        Game.library.loadLibraryCache(Game.mActivity, false);

        Config.loadOnlineConfig(Game.mActivity);
        Game.online.Init(Game.mActivity);
        Game.onlineScoring.login();

        setTouchAreaBindingEnabled(true);
    }

    @Override
    public void onMusicControlRequest(MusicOption option, Status current) {
        UI.musicPlayer.currentOption = option;
        super.onMusicControlRequest(option, current);
    }

    @Override
    protected void onSceneUpdate(float secondsElapsed) {
        if (Game.songService != null) {
            if (Game.songService.getStatus() == Status.PLAYING) {
                spectrum.update();
                beatMarker.update();
            } else {
                spectrum.clear(false);
            }

            int position = Game.songService.getPosition();

            if (scatter.isEnabled && (position - particleBeginTime > 2000)) {
                scatter.end();
            }
        }

        UI.musicPlayer.update();
    }

    //--------------------------------------------------------------------------------------------//

    public void loadMusic() {
        Game.library.shuffleLibrary();
        MusicManager.beatmap = Game.library.getBeatmap();

        if (MusicManager.beatmap != null && Game.library.getSizeOfBeatmaps() > 0) {
            Game.musicManager.play();
            loadSong(MusicManager.beatmap);
        }
        UI.debugOverlay.show();
    }

    public void playExitAnim() {
        Rectangle dim = new Rectangle(0, 0, screenWidth, screenHeight);
        dim.setColor(0, 0, 0, 0.0f);
        attachChild(dim, 2);
        dim.registerEntityModifier(ModifierFactory.newAlphaModifier(3.0f, 0, 1));
    }
}
