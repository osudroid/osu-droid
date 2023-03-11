package com.reco1l.management.music;

// Created by Reco1l on 18/9/22 20:45

import androidx.annotation.Nullable;

import main.osu.TrackInfo;

public interface IMusicObserver {

    default void onMusicChange(@Nullable TrackInfo newTrack, boolean isSameAudio) {}

    default void onMusicPause() {}

    default void onMusicPlay() {}

    default void onMusicStop() {}

    default void onMusicEnd() {}
}
