package ru.nsu.ccfit.zuev.osu.online;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import androidx.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.reco1l.framework.lang.execution.Async;
import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager.OnlineManagerException;
import ru.nsu.ccfit.zuev.osuplus.R;

public class OnlineInitializer implements View.OnClickListener {
    private final Activity activity;
    private Dialog registerDialog;

    public OnlineInitializer(Activity context) {
        this.activity = context;
    }

    public void createInitDialog() {
        registerDialog = new Dialog(activity);
        registerDialog.setContentView(R.layout.register_dialog);
        registerDialog.setTitle(StringTable.get(R.string.online_registration));

        Button btn = registerDialog.findViewById(R.id.register_btn);
        if (btn != null) btn.setOnClickListener(this);
        btn = registerDialog.findViewById(R.id.cancel_btn);
        if (btn != null) btn.setOnClickListener(v -> registerDialog.dismiss());

        registerDialog.show();
    }

    @Override
    public void onClick(View v) {
        final String username = ((EditText) registerDialog.findViewById(R.id.username_edit))
                .getText().toString();
        final String password = ((EditText) registerDialog.findViewById(R.id.password_edit))
                .getText().toString();
        final String confirm_password = ((EditText) registerDialog.findViewById(R.id.confirm_password_edit))
                .getText().toString();
        final String email = ((EditText) registerDialog.findViewById(R.id.email_edit))
                .getText().toString();

        final TextView errorText = registerDialog.findViewById(R.id.register_error_text);
        errorText.setText("");

        if (!username.matches("^\\w{3,16}$")) {
            ToastLogger.showTextId(R.string.online_invlogin, true);
            return;
        }
        if (password.length() < 4 || password.length() > 32) {
            ToastLogger.showTextId(R.string.online_invpassword, true);
            return;
        }
        if (!password.equals(confirm_password)) {
            ToastLogger.showTextId(R.string.online_invconfirm, true);
            return;
        }
        if (!email.matches("^[\\w.-_]+@[\\w.-_]+$")) {
            ToastLogger.showTextId(R.string.online_invemail, true);
            return;
        }

        final ProgressDialog pdialog = ProgressDialog.show(activity,
                StringTable.get(R.string.online_registration),
                StringTable.get(R.string.online_registration_process));

        Async.run(new Runnable() {
            private boolean success = false;
            private String resultMessage = "";

            @Override
            public void run() {
                try {
                    success = OnlineManager.getInstance().register(username, password, email);
                } catch (OnlineManagerException e) {
                    resultMessage = e.getMessage();
                    ToastLogger.showText(resultMessage, true);
                }

                //Error check
                resultMessage = OnlineManager.getInstance().getFailMessage();

                activity.runOnUiThread(() -> {
                    pdialog.dismiss();
                    if (success)
                        registerDialog.dismiss();
                    errorText.setText(resultMessage);
                });

                final SharedPreferences prefs = PreferenceManager
                        .getDefaultSharedPreferences(activity);

                //Save changes
                Editor editor = prefs.edit();
                if (!prefs.getBoolean("onlineSet", false) || success) {
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
