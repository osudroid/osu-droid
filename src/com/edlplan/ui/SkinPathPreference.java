package com.edlplan.ui;

import android.content.Context;
import android.util.AttributeSet;

import androidx.preference.ListPreference;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.anddev.andengine.util.Debug;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osuplus.R;

public class SkinPathPreference extends ListPreference {
    
    public SkinPathPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public SkinPathPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs);
    }
 
    public SkinPathPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SkinPathPreference(Context context) {
        super(context);
    }

    public void reloadSkinList() {
        try {
            File skinMain = new File(Config.getSkinTopPath());
            Map<String, String> skins = new HashMap<String, String>(Config.getSkins());
            int skinsSize = (skins.size() > 0) ? skins.size() + 1 : 1;
            Debug.i("Skins count:" + skinsSize);
            CharSequence[] entries = new CharSequence[skinsSize];
            CharSequence[] entryValues = new CharSequence[skinsSize];
            entries[0] = skinMain.getName() + " (Default)";
            entryValues[0] = skinMain.getPath();
            
            if(skins.size() > 0) {
                int index = 1;
                for(Map.Entry<String, String> skin : skins.entrySet()) {
                    entries[index] = skin.getKey();
                    entryValues[index] = skin.getValue();
                    index++;
                }

                Arrays.sort(entries, 1, entries.length);
                Arrays.sort(entryValues, 1, entryValues.length);
            }

            setEntries(entries);
            setEntryValues(entryValues);
        } catch (Exception e) {
            Debug.e("SkinPathPreference.reloadSkinList: ", e);
            e.printStackTrace();
        }
    }

}
