package com.reco1l.data;

import java.util.TreeSet;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public abstract class BeatmapAttribute<T extends Number & Comparable<T>> {

    private Value<T> value;
    private TreeSet<T> treeSet;

    private boolean lessIsBetter = true;

    public BeatmapAttribute(Value<T> value) {
        this.value = value;
    }

    @FunctionalInterface
    public interface Value<T> {
        T get();
    }

    public void lessIsBetter(boolean bool) {
        lessIsBetter = bool;
    }

}
