package com.reco1l.ui;

import com.reco1l.ui.scenes.listing.fragments.BeatmapListing;
import com.reco1l.ui.fragments.*;
import com.reco1l.ui.scenes.main.fragments.MainMenu;
import com.reco1l.ui.scenes.main.fragments.MusicPlayer;
import com.reco1l.ui.scenes.player.fragments.BreakOverlay;
import com.reco1l.ui.scenes.player.fragments.GameOverlay;
import com.reco1l.ui.scenes.player.fragments.PauseMenu;
import com.reco1l.ui.scenes.selector.fragments.BeatmapCarrousel;
import com.reco1l.ui.scenes.selector.fragments.BeatmapPanel;
import com.reco1l.ui.scenes.selector.fragments.FilterBar;
import com.reco1l.ui.scenes.selector.fragments.ModMenu;
import com.reco1l.ui.scenes.summary.fragments.GameSummary;
import com.reco1l.utils.Logging;

// Created by Reco1l on 29/6/22 22:38

public final class UI {

    static {
        Logging.loadOf(UI.class);
    }

    //--------------------------------------------------------------------------------------------//

    public static final Background background = Background.instance;

    // Global overlays
    public static final NotificationCenter notificationCenter = NotificationCenter.instance;
    public static final SettingsMenu settingsPanel = SettingsMenu.instance;
    public static final UserProfile userProfile = UserProfile.instance;
    public static final MainOverlay mainOverlay = MainOverlay.instance;
    public static final TopBar topBar = TopBar.instance;

    // Main scene
    public static final MainMenu mainMenu = MainMenu.instance;
    public static final MusicPlayer musicPlayer = MusicPlayer.instance;

    // Summary scene
    public static final GameSummary gameSummary = GameSummary.instance;

    // Selector scene
    public static final BeatmapCarrousel beatmapCarrousel = BeatmapCarrousel.instance;
    public static final BeatmapPanel beatmapPanel = BeatmapPanel.instance;
    public static final FilterBar filterBar = FilterBar.instance;
    public static final ModMenu modMenu = ModMenu.instance;

    // Player scene
    public static final PauseMenu pauseMenu = PauseMenu.instance;
    public static final GameOverlay gameOverlay = GameOverlay.instance;
    public static final BreakOverlay breakOverlay = BreakOverlay.instance;

    //--------------------------------------------------------------------------------------------//

    // Calling this method will load class and its static fields
    public static void initialize() {
        BeatmapListing.instance.init();
    }
}
