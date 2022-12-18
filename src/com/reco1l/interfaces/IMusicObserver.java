package com.reco1l.interfaces;

// Created by Reco1l on 18/9/22 20:45

import com.reco1l.enums.MusicOption;
import com.reco1l.enums.Screens;

import ru.nsu.ccfit.zuev.audio.Status;
import ru.nsu.ccfit.zuev.osu.BeatmapInfo;

public interface IMusicObserver {

    default void onMusicControlRequest(MusicOption option, Status status) {}

    default void onMusicStateChange(MusicOption option, Status status) {}

    default void onMusicChange(BeatmapInfo newBeatmap) {}

    default void onMusicSync(Status status) {}

    default void onMusicEnd() {}

    default Screens getAttachedScreen() {
        return null;
    }
}
