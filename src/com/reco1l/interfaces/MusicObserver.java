package com.reco1l.interfaces;

// Created by Reco1l on 18/9/22 20:45

import androidx.annotation.Nullable;

import com.reco1l.enums.Screens;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public interface MusicObserver {

    default void onMusicChange(@Nullable TrackInfo newTrack, boolean wasAudioChanged) {}

    default void onMusicPause() {}

    default void onMusicPlay() {}

    default void onMusicStop() {}

    default void onMusicEnd() {}

    default Screens getAttachedScreen() {
        return null;
    }
}
