package com.edlplan.ui;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.widget.Toast;

import com.edlplan.replay.OdrConfig;
import com.edlplan.replay.OdrDatabase;
import com.edlplan.replay.OsuDroidReplayPack;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import com.rimu.R;

public class ImportReplayActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (getIntent().getData() != null) {
            String path = getIntent().getData().getPath();
            System.out.println("path: " + path);
            File file = new File(path);
            if ((!file.exists()) || file.isDirectory()) {
                Toast.makeText(this, R.string.invalid_edr_file, Toast.LENGTH_SHORT).show();
                super.onStart();
                finish();
                return;
            }
            try {
                OsuDroidReplayPack.ReplayEntry entry = OsuDroidReplayPack.unpack(new FileInputStream(file));
                File rep = new File(OdrConfig.getScoreDir(), entry.replay.getReplayFileName());
                if (!rep.exists()) {
                    if (!rep.createNewFile()) {
                        Toast.makeText(this, R.string.failed_to_import_edr, Toast.LENGTH_SHORT).show();
                        super.onStart();
                        finish();
                        return;
                    }
                }
                FileOutputStream outputStream = new FileOutputStream(rep);
                outputStream.write(entry.replayFile);
                outputStream.close();
                entry.replay.setReplayFile(rep.getAbsolutePath());
                if (OdrDatabase.get().write(entry.replay) != -1) {
                    Toast.makeText(this, R.string.import_edr_successfully, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, R.string.failed_to_import_edr, Toast.LENGTH_SHORT).show();
                    finish();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, String.format(getResources().getString(R.string.failed_to_import_edr_with_err), e.toString()), Toast.LENGTH_SHORT).show();
                super.onStart();
                finish();
                return;
            }
        }
        super.onStart();
    }
}
