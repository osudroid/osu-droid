package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.annotation.SuppressLint;
import android.graphics.Color;
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

import com.reco1l.legacy.ui.multiplayer.Multiplayer;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osuplus.R;

public class InGameSettingMenu extends BaseFragment {

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
    private TextView followDelayText;
    private TextView speedLabel;
    private TextView speedText;

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

    private void applyCustomModColor() {
        final TextView customizedModsText = findViewById(R.id.customize_mods);
        int color = ModMenu.getInstance().getFLfollowDelay() != FlashLightEntity.defaultMoveDelayS ? Color.RED : greenColor;
        customizedModsText.setTextColor(color);
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

        showScoreboard = findViewById(R.id.showScoreboard);
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

        enableNCWhenSpeedChange = findViewById(R.id.enableNCwhenSpeedChange);
        enableNCWhenSpeedChange.setChecked(ModMenu.getInstance().isEnableNCWhenSpeedChange());
        enableNCWhenSpeedChange.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ModMenu.getInstance().setEnableNCWhenSpeedChange(isChecked);
        });

        speedText = findViewById(R.id.changeSpeedText);

        enableSpeedChange = findViewById(R.id.enableSpeedChange);
        enableSpeedChange.setChecked(ModMenu.getInstance().getChangeSpeed() != 1.0f);
        enableSpeedChange.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (!isChecked) {
                ModMenu.getInstance().setChangeSpeed(1.0f);
                speedText.setText(String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().getChangeSpeed()));
                changeSpeed.setProgress(10);
                ModMenu.getInstance().updateMultiplierText();
            }
            else if(ModMenu.getInstance().getChangeSpeed() == 1.0f){
                enableSpeedChange.setChecked(false);
            }
        });

        enableForceAR = findViewById(R.id.enableForceAR);
        enableForceAR.setChecked(ModMenu.getInstance().isEnableForceAR());
        enableForceAR.setOnCheckedChangeListener((buttonView, isChecked) -> {
            ModMenu.getInstance().setEnableForceAR(isChecked);
            ModMenu.getInstance().updateMultiplierText();
        });

        backgroundBrightness = findViewById(R.id.backgroundBrightnessBar);
        backgroundBrightness.setProgress(
                PreferenceManager.getDefaultSharedPreferences(getContext()).getInt("bgbrightness", 25));
        backgroundBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
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

        changeSpeed = findViewById(R.id.changeSpeedBar);
        changeSpeed.setProgress((int)(ModMenu.getInstance().getChangeSpeed() * 20 - 10));
        changeSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float p = 0.5f + 0.05f * progress;
                speedText.setText(String.format(Locale.getDefault(), "%.2fx", p));
                if (p == 1.0f){
                    enableSpeedChange.setChecked(false);
                }
                else {
                    enableSpeedChange.setChecked(true);
                    ModMenu.getInstance().updateMultiplierText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float p = 0.5f + 0.05f * progress;
                speedText.setText(String.format(Locale.getDefault(), "%.2fx", p));
                if (p == 1.0f){
                    enableSpeedChange.setChecked(false);
                }
                else {
                    enableSpeedChange.setChecked(true);
                    ModMenu.getInstance().updateMultiplierText();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float p = 0.5f + 0.05f * progress;
                speedText.setText(String.format(Locale.getDefault(), "%.2fx", p));
                ModMenu.getInstance().setChangeSpeed(p);
                if (p == 1.0f){
                    enableSpeedChange.setChecked(false);
                }
                else {
                    enableSpeedChange.setChecked(true);
                    ModMenu.getInstance().updateMultiplierText();
                }
            }
        });
        speedText.setText(String.format(Locale.getDefault(), "%.2fx", ModMenu.getInstance().getChangeSpeed()));

        forceAR = findViewById(R.id.forceARBar);
        forceAR.setProgress((int)(ModMenu.getInstance().getForceAR() * 10));
        forceAR.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float p = 0.1f * progress;
                ((TextView) findViewById(R.id.forceARText)).setText(String.format(Locale.getDefault(), "AR%.1f", p));
                if(ModMenu.getInstance().isEnableForceAR()){
                    ModMenu.getInstance().updateMultiplierText();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float p = 0.1f * progress;
                ((TextView) findViewById(R.id.forceARText)).setText(String.format(Locale.getDefault(), "AR%.1f", p));
                if(ModMenu.getInstance().isEnableForceAR()){
                    ModMenu.getInstance().updateMultiplierText();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                int progress = seekBar.getProgress();
                float p = 0.1f * progress;
                ((TextView) findViewById(R.id.forceARText)).setText(String.format(Locale.getDefault(), "AR%.1f", p));
                ModMenu.getInstance().setForceAR(p);
                if(ModMenu.getInstance().isEnableForceAR()){
                    ModMenu.getInstance().updateMultiplierText();
                }
            }
        });
        
        ((TextView) findViewById(R.id.forceARText)).setText(String.format(Locale.getDefault(), "AR%.1f", ModMenu.getInstance().getForceAR()));

        followDelayText = findViewById(R.id.flashlightFollowDelayText);

        flashlightFollowDelay = findViewById(R.id.flashlightFollowDelayBar);
        flashlightFollowDelay.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            boolean containsFlashlight = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                if (!containsFlashlight)
                    return;

                ModMenu.getInstance().setFLfollowDelay((float) Math.round(progress * 1200f) / (10f * 1000f));
                applyCustomModColor();
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
        ((TextView) findViewById(R.id.forceARText)).setText(String.format("AR%.1f", ModMenu.getInstance().getForceAR()));

        speedLabel = findViewById(R.id.speed_modify_label);
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

        // Updating FL follow delay text value
        var flFollowDelay = ModMenu.getInstance().getFLfollowDelay();
        followDelayText.setText((int) (flFollowDelay * 1000) + "ms");
        flashlightFollowDelay.setProgress((int) (flFollowDelay * 1000 / FlashLightEntity.defaultMoveDelayMS));

        // Updating speed multiplier seekbar visibility
        int visibility = Multiplayer.isMultiplayer && !Multiplayer.isRoomHost ? View.GONE : View.VISIBLE;
        enableSpeedChange.setVisibility(visibility);
        changeSpeed.setVisibility(visibility);
        speedText.setVisibility(visibility);
        speedLabel.setVisibility(visibility);


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
