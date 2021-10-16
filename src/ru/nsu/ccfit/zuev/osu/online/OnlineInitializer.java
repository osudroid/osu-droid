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

public class OnlineInitializer implements View.OnClickListener {
    private Activity activity;
    private Dialog registerDialog;

    public OnlineInitializer(Activity context) {
        this.activity = context;
    }

    public void createInitDialog() {
        registerDialog = new Dialog(activity);
        registerDialog.setContentView(R.layout.register_dialog);
        registerDialog.setTitle(StringTable.get(R.string.online_registration));

        Button btn = (Button) registerDialog.findViewById(R.id.register_btn);
        if (btn != null) btn.setOnClickListener(this);
        btn = (Button) registerDialog.findViewById(R.id.cancel_btn);
        if (btn != null) btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                registerDialog.dismiss();
            }
        });

        registerDialog.show();
    }


    public void onClick(View v) {
        final String username = ((EditText) registerDialog.findViewById(R.id.username_edit))
                .getText().toString();
        final String password = ((EditText) registerDialog.findViewById(R.id.password_edit))
                .getText().toString();
        final String confirm_password = ((EditText) registerDialog.findViewById(R.id.confirm_password_edit))
                .getText().toString();
        final String email = ((EditText) registerDialog.findViewById(R.id.email_edit))
                .getText().toString();

        final TextView errorText = (TextView) registerDialog.findViewById(R.id.register_error_text);
        errorText.setText("");

        if (username.matches("^\\w{3,16}$") == false) {
            ToastLogger.showTextId(R.string.online_invlogin, true);
            return;
        }
        if (password.length() < 4 || password.length() > 32) {
            ToastLogger.showTextId(R.string.online_invpassword, true);
            return;
        }
        if (password.equals(confirm_password) == false) {
            ToastLogger.showTextId(R.string.online_invconfirm, true);
            return;
        }
        if (email.matches("^[\\w.-_]+@[\\w.-_]+$") == false) {
            ToastLogger.showTextId(R.string.online_invemail, true);
            return;
        }

        final ProgressDialog pdialog = ProgressDialog.show(activity,
                StringTable.get(R.string.online_registration),
                StringTable.get(R.string.online_registration_process));

        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            private boolean success = false;
            private String resultMessage = "";

            public void run() {

                try {
                    success = OnlineManager.getInstance().register(username, password, email,
                            Config.getOnlineDeviceID());
                } catch (OnlineManagerException e) {
                    resultMessage = e.getMessage();
                    ToastLogger.showText(resultMessage, true);
                    return;
                }

                //Error check
                resultMessage = OnlineManager.getInstance().getFailMessage();
            }

            public void onComplete() {
                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        pdialog.dismiss();
                        if (success)
                            registerDialog.dismiss();
                        errorText.setText(resultMessage);
                    }
                });
                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(activity);

                //Save changes
                Editor editor = prefs.edit();
                if (prefs.getBoolean("onlineSet", false) == false || success) {
                    editor.putBoolean("stayOnline", success);
                    editor.putString("onlineUsername", username);
                    editor.putString("onlinePassword", password);
                }
                editor.putBoolean("onlineSet", true);
                editor.commit();
                if (success) {
                    ToastLogger.showTextId(R.string.online_regcomplete, true);
                }
            }
        });
    }
}
