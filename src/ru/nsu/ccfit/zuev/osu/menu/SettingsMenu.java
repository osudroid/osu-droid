package ru.nsu.ccfit.zuev.osu.menu;

import android.animation.Animator;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
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
import com.edlplan.ui.fragment.LoadingFragment;
import com.edlplan.ui.fragment.SettingsFragment;
import com.edlplan.ui.EasingHelper;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.util.Objects;

import com.reco1l.osu.Execution;
import com.reco1l.osu.UpdateManager;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.skins.SkinManager;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
// import ru.nsu.ccfit.zuev.osu.game.SpritePool;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osuplus.R;

import static android.content.Intent.ACTION_VIEW;

import org.anddev.andengine.util.Debug;

public class SettingsMenu extends SettingsFragment {

    public static final String REGISTER_URL = "https://" + OnlineManager.hostname + "/user/?action=register";

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

        SkinPathPreference skinPath = findPreference("skinPath");
        if (skinPath == null) {
            Debug.w("skinPath is null");
            return;
        }

        skinPath.reloadSkinList();
        skinPath.setOnPreferenceChangeListener((preference, newValue) -> {
            if(!Objects.equals(GlobalManager.getInstance().getSkinNow(), newValue.toString())) {
                var loading = new LoadingFragment();
                loading.show();

                Execution.async(() -> {
                    GlobalManager.getInstance().setSkinNow(Config.getSkinPath());
                    SkinManager.getInstance().clearSkin();
                    ResourceManager.getInstance().loadSkin(newValue.toString());
                    GlobalManager.getInstance().getEngine().getTextureManager().reloadTextures();

                    mActivity.runOnUiThread(() -> {
                        loading.dismiss();
                        mActivity.startActivity(new Intent(mActivity, MainActivity.class));
                        Snackbar.make(mActivity.findViewById(android.R.id.content), StringTable.get(R.string.message_loaded_skin), 1500).show();
                    });
                });
            }
            return true;
        });

        // screens
        mParentScreen = parentScreen = getPreferenceScreen();

        ((PreferenceScreen) Objects.requireNonNull(findPreference("onlineOption"))).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) Objects.requireNonNull(findPreference("general"))).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) Objects.requireNonNull(findPreference("color"))).setOnPreferenceClickListener(preference -> {
            parentScreen = findPreference("general");
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) Objects.requireNonNull(findPreference("sound"))).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) Objects.requireNonNull(findPreference("beatmaps"))).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });

        ((PreferenceScreen) Objects.requireNonNull(findPreference("advancedopts"))).setOnPreferenceClickListener(preference -> {
            setPreferenceScreen((PreferenceScreen) preference);
            return true;
        });
        // screens END

        final EditTextPreference onlinePassword = Objects.requireNonNull(findPreference("onlinePassword"));
        onlinePassword.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD));

        final EditTextPreference skinToppref = Objects.requireNonNull(findPreference("skinTopPath"));
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
        Objects.requireNonNull(pref).setOnPreferenceClickListener(preference -> {
            LibraryManager.INSTANCE.clearCache();
            return true;
        });
        final Preference clearProps = findPreference("clear_properties");
        Objects.requireNonNull(clearProps).setOnPreferenceClickListener(preference -> {
            PropertiesLibrary.getInstance()
                    .clear(mActivity);
            return true;
        });
        final Preference register = findPreference("registerAcc");
        Objects.requireNonNull(register).setOnPreferenceClickListener(preference -> {
            Intent intent = new Intent(ACTION_VIEW, Uri.parse(REGISTER_URL));
            startActivity(intent);
            return true;
        });

        final Preference update = findPreference("update");
        Objects.requireNonNull(update).setOnPreferenceClickListener(preference -> {
            UpdateManager.INSTANCE.checkNewUpdates(false);
            return true;
        });

        final Preference dither = findPreference("dither");
        Objects.requireNonNull(dither).setOnPreferenceChangeListener((preference, newValue) -> {
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
                ImageButton backButton = Objects.requireNonNull(findViewById(R.id.back_button));
                backButton.setImageDrawable(mActivity.getResources().getDrawable(newDrawable));
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });
        ((ImageButton) Objects.requireNonNull(findViewById(R.id.back_button))).startAnimation(animation);
    }

    private void animateView(@IdRes int viewId, @AnimRes int anim) {
        ((View) Objects.requireNonNull(findViewById(viewId))).startAnimation(AnimationUtils.loadAnimation(mActivity, anim));
    }

    private void setTitle(String title) {
       ((TextView) Objects.requireNonNull(findViewById(R.id.title))).setText(title);
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
        ((ImageButton) Objects.requireNonNull(findViewById(R.id.back_button))).setOnClickListener(v -> navigateBack());
    }

    protected void playOnLoadAnim() {
        View body = Objects.requireNonNull(findViewById(R.id.body));
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
        View body = Objects.requireNonNull(findViewById(R.id.body));
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
            GlobalManager.getInstance().getMainScene().loadTimingPoints(false);
            GlobalManager.getInstance().getSongService().setVolume(Config.getBgmVolume());
            GlobalManager.getInstance().getSongService().setGaming(false);
            SettingsMenu.super.dismiss();
        });
    }

}
