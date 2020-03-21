package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osuplus.R;

public class InGameSettingMenu extends BaseFragment {

    private static InGameSettingMenu menu;
    private CheckBox enableStoryboard;
    private SeekBar backgroundBrightness;

    public static InGameSettingMenu getInstance() {
        if (menu == null) {
            menu = new InGameSettingMenu();
        }
        return menu;
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_in_game_option;
    }

    @Override
    protected void onLoadView() {
        reload();
        findViewById(R.id.frg_header).animate().cancel();
        findViewById(R.id.frg_header).setAlpha(0);
        findViewById(R.id.frg_header).setTranslationY(100);
        findViewById(R.id.frg_header).animate()
                .alpha(1)
                .translationY(0)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .start();
    }

    @SuppressLint({"ClickableViewAccessibility", "ApplySharedPref"})
    private void reload() {
        View showMoreButton = findViewById(R.id.showMoreButton);
        if (showMoreButton == null) {
            return;
        }
        showMoreButton.setOnTouchListener((v, event) -> {
            if (event.getAction() == TouchEvent.ACTION_DOWN) {
                v.animate().cancel();
                v.animate().scaleY(0.9f).scaleX(0.9f).translationY(v.getHeight() * 0.1f).setDuration(100).start();
                toggleSettingPanel();
                return true;
            } else if (event.getAction() == TouchEvent.ACTION_UP) {
                v.animate().cancel();
                v.animate().scaleY(1).scaleX(1).setDuration(100).translationY(0).start();
                if (event.getX() < v.getWidth()
                        && event.getY() < v.getHeight()
                        && event.getX() > 0
                        && event.getY() > 0) {
                }
                return true;
            }
            return false;
        });

        findViewById(R.id.frg_background).setClickable(false);

        enableStoryboard = findViewById(R.id.enableStoryboard);
        enableStoryboard.setChecked(Config.isEnableStoryboard());
        enableStoryboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Config.setEnableStoryboard(isChecked);
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean("enableStoryboard", isChecked)
                    .commit();
        });

        CheckBox showScoreboard = findViewById(R.id.showScoreboard);
        showScoreboard.setChecked(Config.isShowScoreboard());
        showScoreboard.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Config.setShowScoreboard(isChecked);
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean("showscoreboard", isChecked)
                    .commit();
        });

        backgroundBrightness = findViewById(R.id.backgroundBrightnessBar);
        backgroundBrightness.setProgress(
                Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getContext()).getString("bgbrightness", "25")));
        backgroundBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(R.id.brightPreviewText)).setText(String.valueOf(progress));
                int p = Math.round(FMath.clamp(255 * (progress / 100f), 0, 255));
                findViewById(R.id.brightnessPreview).setBackgroundColor(Color.argb(255, p, p, p));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                findViewById(R.id.brightnessPreviewLayout).setVisibility(View.VISIBLE);
                int progress = seekBar.getProgress();
                ((TextView) findViewById(R.id.brightPreviewText)).setText(String.valueOf(progress));
                int p = Math.round(FMath.clamp(255 * (progress / 100f), 0, 255));
                findViewById(R.id.brightnessPreview).setBackgroundColor(Color.argb(255, p, p, p));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                findViewById(R.id.brightnessPreviewLayout).setVisibility(View.GONE);
                Config.setBackgroundBrightness(seekBar.getProgress() / 100f);
                PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit()
                        .putString("bgbrightness", seekBar.getProgress() + "")
                        .commit();
            }
        });
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        super.dismiss();
        ModMenu.getInstance().hideByFrag();
    }

    public boolean isSettingPanelShow() {
        return findViewById(R.id.fullLayout) != null && Math.abs(findViewById(R.id.fullLayout).getTranslationY()) < 10;
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void toggleSettingPanel() {
        if (isSettingPanelShow()) {
            playHidePanelAnim();
            findViewById(R.id.frg_background).setOnTouchListener(null);
            findViewById(R.id.frg_background).setClickable(false);
        } else {
            playShowPanelAnim();
            findViewById(R.id.frg_background).setOnTouchListener((v, event) -> {
                if (event.getAction() == TouchEvent.ACTION_DOWN) {
                    if (isSettingPanelShow()) {
                        toggleSettingPanel();
                        return true;
                    }
                }
                return false;
            });
            findViewById(R.id.frg_background).setClickable(true);
        }
    }

    protected void playShowPanelAnim() {
        View fullLayout = findViewById(R.id.fullLayout);
        if (fullLayout != null) {
            fullLayout.animate().cancel();
            fullLayout.animate()
                    .translationY(0)
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(new BaseAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            findViewById(R.id.frg_background).setClickable(true);
                            findViewById(R.id.frg_background).setOnClickListener(v -> playHidePanelAnim());
                        }
                    })
                    .start();
        }
    }

    protected void playHidePanelAnim() {
        View fullLayout = findViewById(R.id.fullLayout);
        if (fullLayout != null) {
            fullLayout.animate().cancel();
            fullLayout.animate()
                    .translationY(findViewById(R.id.optionBody).getHeight())
                    .setDuration(200)
                    .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                    .setListener(new BaseAnimationListener() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            findViewById(R.id.frg_background).setClickable(false);
                        }
                    })
                    .start();
        }
    }
}
