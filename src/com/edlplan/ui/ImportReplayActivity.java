package com.edlplan.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.edlplan.replay.OsuDroidReplayPack;
import com.osudroid.data.DatabaseManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImportReplayActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        Uri data = getIntent().getData();

        if (data != null) {
            try {
                final InputStream inputStream;

                if (ContentResolver.SCHEME_CONTENT.equals(data.getScheme())) {
                    inputStream = getContentResolver().openInputStream(data);

                    if (inputStream == null) {
                        Toast.makeText(this, com.osudroid.resources.R.string.invalid_edr_file, Toast.LENGTH_SHORT).show();
                        return;
                    }
                } else {
                    File file = new File(data.getPath());

                    if (!file.exists() || file.isDirectory()) {
                        Toast.makeText(this, com.osudroid.resources.R.string.invalid_edr_file, Toast.LENGTH_SHORT).show();
                        return;
                    }

                    inputStream = new FileInputStream(file);
                }

                try (inputStream) {
                    OsuDroidReplayPack.ReplayEntry entry = OsuDroidReplayPack.unpack(inputStream);
                    File rep = new File(entry.scoreInfo.getReplayPath());

                    if (!rep.exists()) {
                        if (!rep.createNewFile()) {
                            Toast.makeText(this, com.osudroid.resources.R.string.failed_to_import_edr, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }

                    try (var outputStream = new FileOutputStream(rep)) {
                        outputStream.write(entry.replayFile);
                    }

                    if (DatabaseManager.getScoreInfoTable().insertScore(entry.scoreInfo) >= 0) {
                        Toast.makeText(this, com.osudroid.resources.R.string.import_edr_successfully, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, com.osudroid.resources.R.string.failed_to_import_edr, Toast.LENGTH_SHORT).show();
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, String.format(getResources().getString(com.osudroid.resources.R.string.failed_to_import_edr_with_err), e), Toast.LENGTH_SHORT).show();
            } finally {
                finish();
            }
        }
    }
}
