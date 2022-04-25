package ru.nsu.ccfit.zuev.osu.menu;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.AnimRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.SkinPathPreference;
import com.edlplan.ui.fragment.SettingsFragment;
import com.edlplan.ui.EasingHelper;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.skins.SkinManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Updater;
// import ru.nsu.ccfit.zuev.osu.game.SpritePool;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineInitializer;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SettingsMenu extends SettingsFragment {

    private PreferenceScreen mParentScreen, parentScreen;
    private boolean isOnNestedScreen = false;
    private Activity mActivity;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = GlobalManager.getInstance().getMainActivity();
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.options, rootKey);

        SkinPathPreference skinPath = (SkinPathPreference) findPreference("skinPath");
        skinPath.reloadSkinList();
        skinPath.setOnPreferenceChangeListener((preference, newValue) -> {
            if(GlobalManager.getInstance().getSkinNow() != newValue.toString()) {
                // SpritePool.getInstance().purge();
                GlobalManager.getInstance().setSkinNow(Config.getSkinPath());
                SkinManager.getInstance().clearSkin();
                ResourceManager.getInstance().loadSkin(newValue.toString());
                GlobalManager.getInstance().getEngine().getTextureManager().reloadTextures();
                mActivity.startActivity(new Intent(mActivity, MainActivity.class));
                Snackbar.make(mActivity.findViewById(android.R.id.content),
                    StringTable.get(R.string.message_loaded_skin), 1500).show();
            }
            return true;
        });

        // screens
        mParentScreen = parentScreen = getPreferenceScreen();

        ((PreferenceScreen) findPreference("onlineOption")).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) findPreference("general")).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) findPreference("color")).setOnPreferenceClickListener(preference -> {
            parentScreen = (PreferenceScreen) findPreference("general");
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) findPreference("sound")).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) findPreference("beatmaps")).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) findPreference("advancedopts")).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });
        // screens END

        final EditTextPreference onlinePassword = (EditTextPreference) findPreference("onlinePassword");
        onlinePassword.setOnBindEditTextListener(editText -> {
            editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        });

        final EditTextPreference skinToppref = (EditTextPreference) findPreference("skinTopPath");
        skinToppref.setOnPreferenceChangeListener((preference, newValue) -> {
            if (newValue.toString().trim().length() == 0) {
                skinToppref.setText(Config.getCorePath() + "Skin/");
                Config.loadConfig(mActivity);
                skinPath.reloadSkinList();
                return false;
            }

            File file = new File(newValue.toString());
            if (!file.exists()) {
                if (!file.mkdirs()) {
                    ToastLogger.showText(StringTable.get(R.string.message_error_dir_not_found), true);
                    return false;
                }
            }

            skinToppref.setText(newValue.toString());
            Config.loadConfig(mActivity);
            skinPath.reloadSkinList();
            return false;
        });

        final Preference pref = findPreference("clear");
        pref.setOnPreferenceClickListener(preference -> {
            LibraryManager.getInstance().clearCache();
            return true;
        });
        final Preference clearProps = findPreference("clear_properties");
        clearProps.setOnPreferenceClickListener(preference -> {
            PropertiesLibrary.getInstance()
                    .clear(mActivity);
            return true;
        });
        final Preference register = findPreference("registerAcc");
        register.setOnPreferenceClickListener(preference -> {
            OnlineInitializer initializer = new OnlineInitializer(getActivity());
            initializer.createInitDialog();
            return true;
        });

        final Preference update = findPreference("update");
        update.setOnPreferenceClickListener(preference -> {
            Updater.getInstance().checkForUpdates();
            return true;
        });

        final Preference dither = findPreference("dither");
        dither.setOnPreferenceChangeListener((preference, newValue) -> {
            if (Config.isUseDither() != (boolean) newValue) {
                GlobalManager.getInstance().getMainScene().restart();
            }
            return true;
        });
    }

    public void onNavigateToScreen(PreferenceScreen preferenceScreen) {
        if(preferenceScreen.getKey() != null) {
            if(!isOnNestedScreen) {
                isOnNestedScreen = true;
                animateBackButton(R.drawable.back_black);
            }
            setTitle(preferenceScreen.getTitle().toString());
            for(int v : new int[]{android.R.id.list_container, R.id.title}) {
                animateView(v, R.anim.slide_in_right);
            }
        }
    }

    private void animateBackButton(@DrawableRes int newDrawable) {
        Animation animation = AnimationUtils.loadAnimation(mActivity, R.anim.rotate_360);
        animation.setAnimationListener(new Animation.AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                ImageButton backButton = (ImageButton) findViewById(R.id.back_button);
                backButton.setImageDrawable(mActivity.getResources().getDrawable(newDrawable));
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });
        ((ImageButton) findViewById(R.id.back_button)).startAnimation(animation);
    }

    private void animateView(@IdRes int viewId, @AnimRes int anim) {
        findViewById(viewId).startAnimation(AnimationUtils.loadAnimation(mActivity, anim));
    }

    private void setTitle(String title) {
       ((TextView) findViewById(R.id.title)).setText(title); 
    }

    @Override
    public void callDismissOnBackPress() {
        navigateBack();
    }

    // only supports 1 child with an optional grandchild
    private void navigateBack() {
        for(int v : new int[]{android.R.id.list_container, R.id.title}) {
            animateView(v, R.anim.slide_in_left);
        }

        if(parentScreen.getKey() != null) {
            setPreferenceScreen(parentScreen);
            setTitle(parentScreen.getTitle().toString());
            parentScreen = mParentScreen;
            return;
        }

        if(isOnNestedScreen) {
            isOnNestedScreen = false;
            animateBackButton(R.drawable.close_black);
            setPreferenceScreen(mParentScreen);
            setTitle(StringTable.get(R.string.menu_settings_title));
        }else {
           dismiss();
        }
    }

    @Override
    protected void onLoadView() {
        ((ImageButton) findViewById(R.id.back_button)).setOnClickListener(v -> {
            navigateBack();
        });
    }

    protected void playOnLoadAnim() {
        View body = findViewById(R.id.body);
        body.setAlpha(0);
        body.setTranslationX(400);
        body.animate().cancel();
        body.animate()
                .translationX(0)
                .alpha(1)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setDuration(150)
                .start();
        playBackgroundHideInAnim(150);
    }

    protected void playOnDismissAnim(Runnable action) {
        View body = findViewById(R.id.body);
        body.animate().cancel();
        body.animate()
                .translationXBy(400)
                .alpha(0)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (action != null) {
                            action.run();
                        }
                    }
                })
                .start();
        playBackgroundHideOutAnim(200);
    }

    @Override
    public void show() {
        super.show();
    }

    @Override
    public void dismiss() {
        playOnDismissAnim(() -> {
            Config.loadConfig(mActivity);
            GlobalManager.getInstance().getMainScene().reloadOnlinePanel();
            GlobalManager.getInstance().getMainScene().loadTimeingPoints(false);
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            GlobalManager.getInstance().getSongService().setGaming(false);
            SettingsMenu.super.dismiss();
        });
    }

}