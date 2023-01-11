package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.ContextMenu;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import java.util.Locale;

import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osuplus.R;

public class InGameSettingMenu {

    private static InGameSettingMenu menu;
    private CheckBox enableStoryboard;
    private CheckBox showScoreboard;
    private CheckBox enableNCWhenSpeedChange;
    private CheckBox enableSpeedChange;
    private CheckBox enableForceAR;
    private SeekBar backgroundBrightness;
    private SeekBar changeSpeed;
    private SeekBar forceAR;
    private SeekBar flashlightFollowDelay;

    public static InGameSettingMenu getInstance() {
        if (menu == null) {
            menu = new InGameSettingMenu();
        }
        return menu;
    }

    private Context getContext() {
        return null;
    }

    private <T extends View> T findViewById(int id) {
        return null;
    }

    @SuppressLint({"ClickableViewAccessibility", "ApplySharedPref"})
    private void reload() {

        enableStoryboard.setChecked(Config.isEnableStoryboard());
        enableStoryboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Config.setEnableStoryboard(isChecked);
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean("enableStoryboard", isChecked)
                    .commit();
        });

        showScoreboard.setChecked(Config.isShowScoreboard());
        showScoreboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Config.setShowScoreboard(isChecked);
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean("showscoreboard", isChecked)
                    .commit();
        });

    }

    public void show() {
    }

    public void dismiss() {
    }
}
