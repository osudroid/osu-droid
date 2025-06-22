package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.Environment;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.anddev.andengine.util.Debug;

public class ConfigBackup {

    private static final Set<String> EXCLUDE_KEYS = Set.of("installID", "onlineUsername", "onlinePassword");

    public static boolean exportPreferences() {
        try {
            Context context = GlobalManager.getInstance().getMainActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            
            JSONObject json = new JSONObject();
            Map<String, ?> allPrefs = prefs.getAll();
            
            allPrefs.entrySet()
                .stream()
                .filter(entry -> !EXCLUDE_KEYS.contains(entry.getKey()))
                .forEach(entry -> {
                    try{
                        json.put(entry.getKey(), entry.getValue());
                    }catch(JSONException e){
                        Debug.e("ConfigBackup: " + e.getMessage(), e);
                    }
                });
            
            File backupFile = new File(Config.getCorePath(), "osudroid.cfg");
            try(FileOutputStream fos = new FileOutputStream(backupFile)) {
                fos.write(json.toString(2).getBytes(StandardCharsets.UTF_8));
                return true;
            }
        }catch(JSONException | IOException e) {
            Debug.e("ConfigBackup: " + e.getMessage(), e);
            return false;
        }
    }

    public static boolean importPreferences() {
        try {
            File backupFile = new File(Config.getCorePath(), "osudroid.cfg");
            if(!backupFile.exists()) {
                return false;
            }

            StringBuilder jsonBuilder = new StringBuilder();
            try(FileInputStream fis = new FileInputStream(backupFile)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while((bytesRead = fis.read(buffer)) != -1) {
                    jsonBuilder.append(new String(buffer, 0, bytesRead, StandardCharsets.UTF_8));
                }
            }
            
            String jsonString = jsonBuilder.toString();
            JSONObject json = new JSONObject(jsonString);
            Context context = GlobalManager.getInstance().getMainActivity();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            SharedPreferences.Editor editor = prefs.edit();

            Iterator<String> keys = json.keys();
            while(keys.hasNext()) {
                String key = keys.next();
                if(EXCLUDE_KEYS.contains(key)) continue;
                Object value = json.get(key);
            
                switch(value.getClass().getSimpleName()) {
                    case "Boolean":
                        editor.putBoolean(key, ((Boolean) value).booleanValue());
                        break;
                    case "Integer":
                        editor.putInt(key, ((Integer) value).intValue());
                        break;
                    case "Double":
                        editor.putFloat(key, ((Double) value).floatValue());
                        break;
                    case "Long":
                        editor.putLong(key, ((Long) value).longValue());
                        break;
                    default:
                        editor.putString(key, value.toString());
                        break;
                }
            }
            
            return editor.commit();
        }catch(JSONException | IOException e) {
            Debug.e("ConfigBackup: " + e.getMessage(), e);
            return false;
        }
    }

}
