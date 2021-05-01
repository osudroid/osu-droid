package ru.nsu.ccfit.zuev.osu.menu;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public interface MenuItemListener {
    void select(MenuItem item);

    void selectTrack(TrackInfo track, boolean reloadBG);

    void stopScroll(float y);

    void setY(float y);

    void openScore(int id, boolean showOnline, final String playerName);

    void playMusic(final String filename, final int previewTime);

    boolean isSelectAllowed();

    void showPropertiesMenu(MenuItem item);
}
