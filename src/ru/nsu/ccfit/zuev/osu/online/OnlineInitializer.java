package ru.nsu.ccfit.zuev.osu.online;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.preference.PreferenceManager;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osuplus.R;

//TODO remove this whole class
public class OnlineInitializer implements View.OnClickListener {
    private Activity activity;
    private Dialog registerDialog;

    public OnlineInitializer(Activity context) {
        this.activity = context;
    }

    public void createInitDialog() {
       //TODO replace with WebView
    }


    @Override
    public void onClick(View v) {

    }
}
