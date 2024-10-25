package ru.nsu.ccfit.zuev.osu.menu;

import org.anddev.andengine.util.Debug;

import java.util.LinkedList;
import java.util.Queue;

public class SongMenuPool {
    private static final SongMenuPool instance = new SongMenuPool();
    private final Queue<MenuItemBackground> backgrounds = new LinkedList<>();
    private final Queue<BeatmapItem> beatmapItems = new LinkedList<>();
    private int count = 0;
    private SongMenuPool() {
    }

    public static SongMenuPool getInstance() {
        return instance;
    }

    public void init() {
        count = 0;
        beatmapItems.clear();
        backgrounds.clear();
        for (int i = 0; i < 15; i++) {
            backgrounds.add(new MenuItemBackground());
        }
        for (int i = 0; i < 5; i++) {
            beatmapItems.add(new BeatmapItem());
        }
        count = 20;
    }

    public MenuItemBackground newBackground() {
        if (!backgrounds.isEmpty()) {
            return backgrounds.poll();
        }
        count++;
        Debug.i("Count = " + count);
        return new MenuItemBackground();
    }

    public void putBackground(final MenuItemBackground background) {
        backgrounds.add(background);
    }

    public BeatmapItem newBeatmapItem() {
        if (!beatmapItems.isEmpty()) {
            return beatmapItems.poll();
        }
        count++;
        Debug.i("Count = " + count);
        return new BeatmapItem();
    }

    public void putBeatmapItem(final BeatmapItem beatmapItem) {
        beatmapItems.add(beatmapItem);
    }

}
