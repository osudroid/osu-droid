package ru.nsu.ccfit.zuev.osu.menu;

import ru.nsu.ccfit.zuev.osu.online.OnlineMapInfo;

public interface IPropsMenu {
    void show(SongMenu menu, MenuItem item, OnlineMapInfo p, String mapState);
}
