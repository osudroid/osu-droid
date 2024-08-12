package ru.nsu.ccfit.zuev.osu.menu;

import com.reco1l.osu.BeatmapInfo;

public interface MenuItemListener {
    void select(BeatmapSetItem item);

    void selectBeatmap(BeatmapInfo track, boolean reloadBG);

    void stopScroll(float y);

    void setY(float y);

    void openScore(int id, boolean showOnline, final String playerName);

    void playMusic(final String filename, final int previewTime);

    boolean isSelectAllowed();

    void showPropertiesMenu(BeatmapSetItem item);
}
