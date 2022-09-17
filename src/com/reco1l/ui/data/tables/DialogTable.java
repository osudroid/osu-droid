package com.reco1l.ui.data.tables;

import android.content.Intent;
import android.widget.TextView;

import com.reco1l.ui.custom.Dialog;
import com.reco1l.ui.custom.DialogBuilder;
import com.reco1l.ui.custom.DialogFragment;
import com.reco1l.interfaces.IMainClasses;

import ru.nsu.ccfit.zuev.osu.MainActivity;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 25/7/22 22:30

public class DialogTable implements IMainClasses {

    // Builder for Author dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder author() {
        DialogBuilder builder = new DialogBuilder();

        DialogFragment fragment = new DialogFragment(R.layout.dialog_author_layout, root -> {
            TextView versionTv = root.findViewById(R.id.d_author_version);
            versionTv.setText(String.format("%s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));
        });

        builder.title = "Information";
        builder.customFragment = fragment;
        builder.setCloseMode(true);
        builder.addButton("Close", Dialog::close);

        return builder;
    }

    // Builder for restart dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder restart() {
        DialogBuilder builder = new DialogBuilder();

        builder.title = "Restart";
        builder.message = "The game will be restarted to apply changes";
        builder.setCloseMode(true);
        builder.addButton("Accept", dialog -> {
            Intent intent = new Intent(mActivity, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            mActivity.startActivity(intent);
            System.exit(0);
        });

        return builder;
    }

    // Builder for restart dialog
    //--------------------------------------------------------------------------------------------//
    public static DialogBuilder exit() {
        DialogBuilder builder = new DialogBuilder();

        builder.title = "Exit";
        builder.message = "Are you sure you want to exit the game?";
        builder.setCloseMode(false);
        builder.addButton("Accept", dialog -> {
            dialog.close();
            global.getMainScene().exit();
        });
        builder.addButton("Cancel", Dialog::close);

        return builder;
    }

}
