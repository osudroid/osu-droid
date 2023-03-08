package main.osu;

import static android.widget.LinearLayout.LayoutParams.*;

import android.Manifest;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.net.Uri;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import java.util.List;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;

public class PermissionActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout view = new LinearLayout(this);
        view.setLayoutParams(new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        view.setBackgroundColor(Color.BLACK);

        setContentView(view);
    }

    private void startGameActivity() {
        Intent game = new Intent(this, MainActivity.class);
        startActivity(game);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissions();
    }

    private void checkPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                startGameActivity();
            } else {
                Uri uri = Uri.parse("package:" + getPackageName());
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri);
                startActivityForResult(intent, 2444);
            }
        }else if(Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            if (PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PermissionChecker.PERMISSION_GRANTED) {
                startGameActivity();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2333);
            }
        }
    }

    @Override  
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {  
        super.onActivityResult(requestCode, resultCode, data);  
        if(requestCode == 2444) {
            checkPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, @NonNull List<String> perms) {
        checkPermissions();
    }

    @Override
    public void onPermissionsDenied(int requestCode, @NonNull List<String> perms) {
        if (PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PermissionChecker.PERMISSION_GRANTED
                && EasyPermissions.somePermissionPermanentlyDenied(this, perms)) {
            new AppSettingsDialog.Builder(this).build().show();
        }
    }
}
