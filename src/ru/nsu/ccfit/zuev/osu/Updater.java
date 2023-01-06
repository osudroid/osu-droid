package ru.nsu.ccfit.zuev.osu;

import android.os.Build;


import com.google.gson.Gson;
import com.reco1l.tables.NotificationTable;

import java.io.IOException;

import okhttp3.ResponseBody;
import okhttp3.Request;

import org.anddev.andengine.util.Debug;

import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.model.vo.UpdateVO;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

/**
 * @author kairusds
 */

public class Updater {

    private boolean newUpdate = false;
    private String changelogMsg, downloadUrl;
    private MainActivity mActivity;

    private static Updater instance = new Updater();

    private Updater() {
        mActivity = GlobalManager.getInstance().getMainActivity();
    }

    public static Updater getInstance() {
        return instance;
    }

    private ResponseBody httpGet(String url) throws IOException {
        Request request = new Request.Builder()
            .url(url)
            .build();
        return OnlineManager.client.newCall(request).execute().body();
    }

    public void checkForUpdates() {
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
             public void run() {
                 try {

                    String lang;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { 
                        lang = mActivity.getResources().getConfiguration().getLocales().get(0).getLanguage();
                    }else {
                        lang = mActivity.getResources().getConfiguration().locale.getLanguage();
                    }
                    ResponseBody response = httpGet(OnlineManager.endpoint + "update.php?lang=" + lang);
                    UpdateVO updateInfo = new Gson().fromJson(response.string(), UpdateVO.class);
                    if(!newUpdate && updateInfo.getVersionCode() > mActivity.getVersionCode()) {
                        changelogMsg = updateInfo.getChangelog();
                        downloadUrl = updateInfo.getLink();
                        newUpdate = true;
                    }
                }catch(IOException e) {
                    Debug.e("Updater onRun: " + e.getMessage(), e); 
                }
            }

            public void onComplete() {
                 if (newUpdate) {
                     mActivity.runOnUiThread(() -> NotificationTable.update(downloadUrl));
                 }
            }
        });
    }

}