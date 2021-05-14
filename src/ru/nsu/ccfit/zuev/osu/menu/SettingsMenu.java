package ru.nsu.ccfit.zuev.osu.menu;

import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.view.KeyEvent;

import com.tencent.bugly.beta.Beta;

//disable umeng
//import com.umeng.analytics.MobclickAgent;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.LibraryManager;
import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osu.PropertiesLibrary;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineInitializer;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SettingsMenu extends PreferenceActivity {

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        resetBetaStrings();
        addPreferencesFromResource(R.xml.options);
        reloadSkinList();
		/*if (MP3Decoder.isAvailable == false) {
			final Preference pref = findPreference("nativeplayer");
			pref.setEnabled(false);
		}*/

        final EditTextPreference skinToppref = (EditTextPreference) findPreference("skinTopPath");
        skinToppref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue.toString().trim().length() == 0) {
                    skinToppref.setText(Config.getCorePath() + "Skin/");
                    Config.loadConfig(SettingsMenu.this);
                    reloadSkinList();
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
                Config.loadConfig(SettingsMenu.this);
                reloadSkinList();
                return false;
            }
        });

        final Preference pref = findPreference("clear");
        pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {


            public boolean onPreferenceClick(final Preference preference) {
                LibraryManager.getInstance().clearCache(SettingsMenu.this);
                return true;
            }
        });
        final Preference clearProps = findPreference("clear_properties");
        clearProps.setOnPreferenceClickListener(new OnPreferenceClickListener() {


            public boolean onPreferenceClick(final Preference preference) {
                PropertiesLibrary.getInstance()
                        .clear(SettingsMenu.this);
                return true;
            }
        });
        final Preference register = findPreference("registerAcc");
        register.setOnPreferenceClickListener(new OnPreferenceClickListener() {


            public boolean onPreferenceClick(Preference preference) {
                OnlineInitializer initializer = new OnlineInitializer(SettingsMenu.this);
                initializer.createInitDialog();
                return true;
            }
        });
        //final Preference downloadExtension = findPreference("downloadExtension");
        //downloadExtension.setOnPreferenceClickListener(preference -> EdExtensionHelper.downloadExtension());
		/*
		final Preference clearPP = findPreference("clearPP");
		clearPP.setOnPreferenceClickListener(preference -> {
			Dialog dialog=new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialog_clear_pp);
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(true);
			dialog.findViewById(R.id.button_cancel).setOnClickListener(view -> dialog.dismiss());
			dialog.findViewById(R.id.button_ok).setOnClickListener(new View.OnClickListener() {
			    boolean clicked=false;
                @Override
                public void onClick(View view) {
                    if(clicked){
                        PPHelper.clearPP();
                        Toast.makeText(SettingsMenu.this,R.string.dialog_delete_pp_deleted,Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    }else{
                        clicked=true;
                        ((TextView)dialog.findViewById(R.id.dialog_clear_pp_msg)).setText(R.string.dialog_delete_pp_make_sure);
                    }
                }
            });
			dialog.show();
			return true;
		});
		*/
        final Preference update = findPreference("update");
        update.setOnPreferenceClickListener(preference -> {
            Beta.checkUpgrade();
            return true;
        });
    }

    @Override
    public void onResume() {
        super.onResume();
//        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
//        MobclickAgent.onPause(this);
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            Config.loadConfig(this);
            final Intent intent = new Intent(GlobalManager.getInstance().getMainActivity(),
                    MainActivity.class);
            GlobalManager.getInstance().getMainActivity().startActivity(intent);
            GlobalManager.getInstance().getMainScene().reloadOnlinePanel();
            GlobalManager.getInstance().getMainScene().loadTimeingPoints(false);
            GlobalManager.getInstance().getSongService().setIsSettingMenu(false);
            GlobalManager.getInstance().getSongService().setGaming(false);
            finish();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void resetBetaStrings() {
        Beta.strToastYourAreTheLatestVersion = StringTable.get(R.string.beta_you_are_the_latest_version);
        Beta.strToastCheckUpgradeError = StringTable.get(R.string.beta_check_upgrade_error);
        Beta.strToastCheckingUpgrade = StringTable.get(R.string.beta_checking_upgrade);

        //just wasn't sure if "NOTIFICATION" translations might exceed given spaces. Toast strings are otherwise

//		Beta.strNotificationDownloading = StringTable.get(R.string.beta_downloading);
//		Beta.strNotificationClickToView = StringTable.get(R.string.beta_click_to_view);
//		Beta.strNotificationClickToInstall = StringTable.get(R.string.beta_click_to_install);
//		Beta.strNotificationClickToRetry = StringTable.get(R.string.beta_click_to_retry);
//		Beta.strNotificationClickToContinue = StringTable.get(R.string.beta_click_to_continue);
//		Beta.strNotificationDownloadSucc = StringTable.get(R.string.beta_download_success);
//		Beta.strNotificationDownloadError = StringTable.get(R.string.beta_download_error);
//		Beta.strNotificationHaveNewVersion = StringTable.get(R.string.beta_have_new_version);

    }

    private void reloadSkinList() {
        try {
            final ListPreference skinPathPref = (ListPreference) findPreference("skinPath");
            File skinMain = new File(Config.getSkinTopPath());
            if (!skinMain.exists()) skinMain.mkdir();
            File[] skinFolders = skinMain.listFiles(file -> file.isDirectory() && !file.getName().startsWith("."));
            CharSequence[] entries = new CharSequence[skinFolders.length + 1];
            CharSequence[] entryValues = new CharSequence[skinFolders.length + 1];
            entries[0] = skinMain.getName() + " (Default)";
            entryValues[0] = skinMain.getPath();
            for (int i = 1; i < entries.length; i++) {
                entries[i] = skinFolders[i - 1].getName();
                entryValues[i] = skinFolders[i - 1].getPath();
            }
            skinPathPref.setEntries(entries);
            skinPathPref.setEntryValues(entryValues);
            skinPathPref.setValue(Config.getSkinPath());

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("LoadSkinListError: path: " + Config.getSkinTopPath());
        }
    }
}
