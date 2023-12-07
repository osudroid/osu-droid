package ru.nsu.ccfit.zuev.osu.menu;

import android.content.Context;
import org.anddev.andengine.entity.scene.Scene;

public interface IFilterMenu {

    String getFilter();

    SongMenu.SortOrder getOrder();

    boolean isFavoritesOnly();

    String getFavoriteFolder();

    void loadConfig(Context context);

    Scene getScene();

    void hideMenu();

    void showMenu(SongMenu parent);

}
