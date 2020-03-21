package ru.nsu.ccfit.zuev.osu.menu;

import ru.nsu.ccfit.zuev.osu.TrackInfo;

public interface MenuItemListener {
    public void select(MenuItem item);

    public void selectTrack(TrackInfo track, boolean reloadBG);

    public void stopScroll(float y);

    public void setY(float y);

    public void openScore(int id, boolean showOnline, final String playerName);

    public void playMusic(final String filename, final int previewTime);

    public boolean isSelectAllowed();

    public void showPropertiesMenu(MenuItem item);
}
