package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Color;
import android.view.View;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import com.edlplan.framework.easing.Easing;
import com.edlplan.framework.math.FMath;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import java.util.Locale;

import com.reco1l.legacy.ui.multiplayer.Multiplayer;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osuplus.R;

public class InGameSettingMenu extends BaseFragment {

    private static InGameSettingMenu menu;

    private View speedModifyRow;
    private SeekBar speedModifyBar;
    private TextView speedModifyText;
    private CheckBox speedModifyToggle;

    private View followDelayRow;
    private SeekBar followDelayBar;
    private TextView followDelayText;

    private CheckBox customARToggle;
    private TextView customARText;
    private SeekBar customARBar;

    private CheckBox customODToggle;
    private TextView customODText;
    private SeekBar customODBar;

    private CheckBox customCSToggle;
    private TextView customCSText;
    private SeekBar customCSBar;

    private CheckBox customHPToggle;
    private TextView customHPText;
    private SeekBar customHPBar;


    private final int greenColor = Color.parseColor("#62c700");

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
                return true;
            }
            return false;
        });

        speedModifyRow = findViewById(R.id.speed_modify);
        followDelayRow = findViewById(R.id.follow_delay_row);

        customARBar = findViewById(R.id.custom_ar_bar);
        customARText = findViewById(R.id.custom_ar_text);
        customARToggle = findViewById(R.id.custom_ar_toggle);

        customODBar = findViewById(R.id.custom_od_bar);
        customODText = findViewById(R.id.custom_od_text);
        customODToggle = findViewById(R.id.custom_od_toggle);

        customCSBar = findViewById(R.id.custom_cs_bar);
        customCSText = findViewById(R.id.custom_cs_text);
        customCSToggle = findViewById(R.id.custom_cs_toggle);

        customHPBar = findViewById(R.id.custom_hp_bar);
        customHPText = findViewById(R.id.custom_hp_text);
        customHPToggle = findViewById(R.id.custom_hp_toggle);

        findViewById(R.id.frg_background).setClickable(false);

        CheckBox enableStoryboard = findViewById(R.id.enableStoryboard);
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

        CheckBox enableVideo = findViewById(R.id.enableVideo);
        enableVideo.setChecked(Config.isVideoEnabled());
        enableVideo.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Config.setVideoEnabled(isChecked);
            PreferenceManager.getDefaultSharedPreferences(getContext()).edit()
                    .putBoolean("enableVideo", isChecked)
                    .commit();
        });

        CheckBox enableNCWhenSpeedChange = findViewById(R.id.enableNCwhenSpeedChange);
        enableNCWhenSpeedChange.setChecked(ModMenu.getInstance().isEnableNCWhenSpeedChange());
        enableNCWhenSpeedChange.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ModMenu.getInstance().setEnableNCWhenSpeedChange(isChecked);
        });

        speedModifyText = findViewById(R.id.changeSpeedText);

        speedModifyToggle = findViewById(R.id.enableSpeedChange);
        speedModifyToggle.setChecked(ModMenu.getInstance().getChangeSpeed() != 1.0f);
        speedModifyToggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                ModMenu.getInstance().setChangeSpeed(1.0f);
                speedModifyText.setText(String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().getChangeSpeed()));
                speedModifyBar.setProgress(10);
                ModMenu.getInstance().updateMultiplierText();
            }
            else if(ModMenu.getInstance().getChangeSpeed() == 1.0f){
                speedModifyToggle.setChecked(false);
            }
        });

        SeekBar backgroundBrightness = findViewById(R.id.backgroundBrightnessBar);
        backgroundBrightness.setProgress(
                PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("bgbrightness", 25));
        backgroundBrightness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                ((TextView) findViewById(R.id.brightPreviewText)).setText(String.valueOf(progress));
                ((TextView) findViewById(R.id.bgBrightnessText)).setText(progress + "%");
                int p = Math.round(FMath.clamp(255 * (progress / 100f), 0, 255));
                findViewById(R.id.brightnessPreview).setBackgroundColor(Color.argb(255, p, p, p));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                findViewById(R.id.brightnessPreviewLayout).setVisibility(View.VISIBLE);
                int progress = seekBar.getProgress();
                ((TextView) findViewById(R.id.brightPreviewText)).setText(String.valueOf(progress));
                ((TextView) findViewById(R.id.bgBrightnessText)).setText(progress + "%");
                int p = Math.round(FMath.clamp(255 * (progress / 100f), 0, 255));
                findViewById(R.id.brightnessPreview).setBackgroundColor(Color.argb(255, p, p, p));
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                findViewById(R.id.brightnessPreviewLayout).setVisibility(View.GONE);
                int progress = seekBar.getProgress();
                ((TextView) findViewById(R.id.bgBrightnessText)).setText(progress + "%");
                Config.setBackgroundBrightness(seekBar.getProgress() / 100f);
                PreferenceManager.getDefaultSharedPreferences(getContext())
                        .edit()
                        .putInt("bgbrightness", progress)
                        .commit();
            }
        });
        ((TextView) findViewById(R.id.bgBrightnessText)).setText(
            PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("bgbrightness", 25) + "%");

        speedModifyBar = findViewById(R.id.changeSpeedBar);
        speedModifyBar.setProgress((int)(ModMenu.getInstance().getChangeSpeed() * 20 - 10));
        speedModifyBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float p = 0.5f + 0.05f * progress;
                speedModifyText.setText(String.format(Locale.getDefault(), "%.2fx", p));
                if (p == 1.0f){
                    speedModifyToggle.setChecked(false);
                }
                else {
                    speedModifyToggle.setChecked(true);
                    ModMenu.getInstance().updateMultiplierText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float p = 0.5f + 0.05f * progress;
                speedModifyText.setText(String.format(Locale.getDefault(), "%.2fx", p));
                if (p == 1.0f){
                    speedModifyToggle.setChecked(false);
                }
                else {
                    speedModifyToggle.setChecked(true);
                    ModMenu.getInstance().updateMultiplierText();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float p = 0.5f + 0.05f * progress;
                speedModifyText.setText(String.format(Locale.getDefault(), "%.2fx", p));
                ModMenu.getInstance().setChangeSpeed(p);
                if (p == 1.0f){
                    speedModifyToggle.setChecked(false);
                }
                else {
                    speedModifyToggle.setChecked(true);
                    ModMenu.getInstance().updateMultiplierText();
                }
            }
        });
        speedModifyText.setText(String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().getChangeSpeed()));

        followDelayText = findViewById(R.id.flashlightFollowDelayText);
        followDelayBar = findViewById(R.id.flashlightFollowDelayBar);
        followDelayBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            boolean containsFlashlight = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (!containsFlashlight)
                    return;

                ModMenu.getInstance().setFLfollowDelay((float) Math.round(progress * 1200f) / (10f * 1000f));
                followDelayText.setText(progress * FlashLightEntity.defaultMoveDelayMS + "ms");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                containsFlashlight = ModMenu.getInstance().getMod().contains(GameMod.MOD_FLASHLIGHT);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (containsFlashlight)
                    return;

                seekBar.setProgress(0);
                ModMenu.getInstance().resetFLFollowDelay();
                followDelayText.setText((int) (ModMenu.getInstance().getFLfollowDelay() * 1000) + "ms");
            }
        });

        initializeDifficultyAdjustViews();
        updateVisibility();
    }


    private void initializeDifficultyAdjustViews() {

        customARToggle.setChecked(ModMenu.getInstance().isCustomAR());
        customARToggle.setOnCheckedChangeListener((view, isChecked) -> {

            if (!isChecked) {
                ModMenu.getInstance().setCustomAR(null);
            }

            customARBar.setEnabled(isChecked);
            ModMenu.getInstance().updateMultiplierText();

        });

        var customAR = ModMenu.getInstance().getCustomAR();
        customARBar.setProgress(customAR != null ? ((int) (customAR * 10)) : 0);
        customARBar.setMax(125);
        customARBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ModMenu.getInstance().setCustomAR(progress / 10f);
                    customARText.setText("AR " + (progress / 10f));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        customODToggle.setChecked(ModMenu.getInstance().isCustomOD());
        customODToggle.setOnCheckedChangeListener((view, isChecked) -> {

            if (!isChecked) {
                ModMenu.getInstance().setCustomOD(null);
            }

            customODBar.setEnabled(isChecked);
            ModMenu.getInstance().updateMultiplierText();
        });

        var customOD = ModMenu.getInstance().getCustomOD();
        customODBar.setProgress(customOD != null ? ((int) (customOD * 10)) : 0);
        customODBar.setMax(125);
        customODBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ModMenu.getInstance().setCustomOD(progress / 10f);
                    customODText.setText("OD " + (progress / 10f));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        customCSToggle.setChecked(ModMenu.getInstance().isCustomCS());
        customCSToggle.setOnCheckedChangeListener((view, isChecked) -> {

            if (!isChecked) {
                ModMenu.getInstance().setCustomCS(null);
            }

            customCSBar.setEnabled(isChecked);
            ModMenu.getInstance().updateMultiplierText();
        });

        var customCS = ModMenu.getInstance().getCustomCS();
        customCSBar.setProgress(customCS != null ? ((int) (customCS * 10)) : 0);
        customCSBar.setMax(125);
        customCSBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ModMenu.getInstance().setCustomCS(progress / 10f);
                    customCSText.setText("CS " + (progress / 10f));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });


        customHPToggle.setChecked(ModMenu.getInstance().isCustomHP());
        customHPToggle.setOnCheckedChangeListener((view, isChecked) -> {
            if (!isChecked) {
                ModMenu.getInstance().setCustomHP(null);
            }
            customHPBar.setEnabled(isChecked);
            ModMenu.getInstance().updateMultiplierText();
        });

        var customHP = ModMenu.getInstance().getCustomHP();
        customHPBar.setProgress(customHP != null ? ((int) (customHP * 10)) : 0);
        customHPBar.setMax(125);
        customHPBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    ModMenu.getInstance().setCustomHP(progress / 10f);
                    customHPText.setText("HP " + (progress / 10f));
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

    }

    @Override
    public void dismiss() {
        super.dismiss();
        ModMenu.getInstance().hideByFrag();
    }

    public boolean isSettingPanelShow() {
        return findViewById(R.id.fullLayout) != null && Math.abs(findViewById(R.id.fullLayout).getTranslationY()) < 10;
    }

    private void updateVisibility() {
        // Updating FL follow delay text value
        var flFollowDelay = ModMenu.getInstance().getFLfollowDelay();
        followDelayRow.setVisibility(ModMenu.getInstance().getMod().contains(GameMod.MOD_FLASHLIGHT) ? View.VISIBLE : View.GONE);
        followDelayBar.setProgress((int) (flFollowDelay * 1000 / FlashLightEntity.defaultMoveDelayMS));
        followDelayText.setText((int) (flFollowDelay * 1000) + "ms");

        // Updating speed multiplier seekbar visibility
        if (Multiplayer.isMultiplayer)
            speedModifyRow.setVisibility(Multiplayer.isRoomHost ? View.VISIBLE : View.GONE);
    }

    @SuppressLint("ClickableViewAccessibility")
    protected void toggleSettingPanel() {
        updateVisibility();

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
