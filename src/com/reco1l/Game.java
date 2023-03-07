package com.reco1l;

import androidx.annotation.Nullable;

import com.reco1l.management.BeatmapCollection;
import com.reco1l.management.resources.BitmapManager;
import com.reco1l.management.scoreboard.ScoreboardManager;
import com.reco1l.management.InputManager;
import com.reco1l.management.modding.ModManager;
import com.reco1l.management.music.MusicManager;
import com.reco1l.management.online.OnlineManager;
import com.reco1l.management.music.TimingWrapper;
import com.reco1l.ui.UI;
import com.reco1l.ui.base.FragmentPlatform;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.framework.Logging;

import main.audio.serviceAudio.SongService;
import main.osu.MainActivity;

// Created by Reco1l on 26/9/22 19:10

public final class Game implements References {

    static {
        Logging.loadOf(Game.class);
    }

    //----------------------------------------------------------------------------------------//

    public static final GameEngine engine = GameEngine.instance;
    public static final MainActivity activity = MainActivity.instance;
    public static final FragmentPlatform platform = FragmentPlatform.instance;

    //----------------------------------------------------------------------------------------//

    public static final ModManager modManager = ModManager.instance;
    public static final MusicManager musicManager = MusicManager.instance;
    public static final InputManager inputManager = InputManager.instance;
    public static final BitmapManager bitmapManager = BitmapManager.instance;
    public static final OnlineManager onlineManager2 = OnlineManager.instance;
    public static final TimingWrapper timingWrapper = TimingWrapper.instance;
    public static final BeatmapCollection beatmapCollection = BeatmapCollection.instance;
    public static final ScoreboardManager scoreboardManager = ScoreboardManager.instance;

    //----------------------------------------------------------------------------------------//

    @Nullable
    public static SongService songService;

    //----------------------------------------------------------------------------------------//

    private static boolean mIsInitialized = false;

    //----------------------------------------------------------------------------------------//

    public static void initialize() {
        Scenes.initialize();
        UI.initialize();

        mIsInitialized = true;

        InputManager.instance.setInputLock(false);
        OnlineManager.instance.tryLogin();
        GameEngine.instance.allowUpdate();
    }

    public static boolean isInitialized() {
        return mIsInitialized;
    }
}
