package com.reco1l.tables;

import android.widget.SeekBar;
import android.widget.TextView;

import com.reco1l.global.Game;
import com.reco1l.ui.SimpleFragment;
import com.reco1l.ui.custom.DialogBuilder;

import ru.nsu.ccfit.zuev.osu.BeatmapInfo;
import ru.nsu.ccfit.zuev.osu.BeatmapProperties;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

// Created by Reco1l on 25/7/22 22:30

public class DialogTable {

    //--------------------------------------------------------------------------------------------//

    // Builder for Author dialog
    public static DialogBuilder author() {
        return new DialogBuilder("Information")
                .setCustomFragment(new AuthorFragment())
                .addCloseButton();
    }

    public static class AuthorFragment extends SimpleFragment {

        public AuthorFragment() {
            super(R.layout.layout_author);
        }

        protected void onLoad() {
            TextView versionTv = find(R.id.d_author_version);
            versionTv.setText(String.format("%s", BuildConfig.VERSION_NAME + " (" + BuildConfig.BUILD_TYPE + ")"));
        }
    }

    //--------------------------------------------------------------------------------------------//

    public static DialogBuilder offset(BeatmapInfo map) {

        BeatmapProperties props = Game.propertiesLibrary.getProperties(map.getPath());
        OffsetFragment fragment = new OffsetFragment();

        fragment.mOffset = props.getOffset();

        return new DialogBuilder("Offset")
                .addCloseButton()
                .setCustomFragment(fragment)
                .setOnDismiss(() -> {
                    props.setOffset(fragment.mOffset);

                    Game.propertiesLibrary.setProperties(map.getPath(), props);
                    Game.propertiesLibrary.save();
                });
    }

    public static class OffsetFragment extends SimpleFragment {

        private int mOffset;

        //----------------------------------------------------------------------------------------//

        public OffsetFragment() {
            super(R.layout.custom_preference_seekbar);
        }

        //----------------------------------------------------------------------------------------//

        @Override
        protected void onLoad() {
            SeekBar seekBar = find(R.id.seekbar);
            TextView title = find(android.R.id.title);
            TextView valueText = find(R.id.seekbar_value);

            title.setText("Change offset");

            seekBar.setMax(500);
            seekBar.setProgress(250 + mOffset);
            valueText.setText(mOffset + "ms");

            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    mOffset = progress - 250;
                    valueText.setText((progress - 250) + "ms");
                }

                public void onStartTrackingTouch(SeekBar seekBar) {}

                public void onStopTrackingTouch(SeekBar seekBar) {}
            });
        }
    }

    //--------------------------------------------------------------------------------------------//

    // Builder for restart dialog
    public static DialogBuilder restart() {
        return new DialogBuilder("Restart")
                .setMessage("The game will be restarted to apply changes")
                .setDismiss(false)
                .addButton("Accept", d -> Game.activity.exit());
    }

    //--------------------------------------------------------------------------------------------//

    // Builder for restart dialog
    public static DialogBuilder exit() {
        return new DialogBuilder("Exit")
                .setMessage("Are you sure you want to exit the game?")
                .addButton("Accept", d -> Game.activity.exit())
                .addCloseButton();
    }

    //--------------------------------------------------------------------------------------------//

    // Builder for auto-clicker dialog
    public static DialogBuilder auto_clicker() {
        return new DialogBuilder("Warning")
                .setMessage(Res.str(R.string.message_autoclicker_detected))
                .addButton("Exit", dialog -> Game.activity.exit())
                .setDismiss(false);
    }

    //--------------------------------------------------------------------------------------------//

    // Simple builder for print trace
    public static DialogBuilder stacktrace(Exception e) {

        StackTraceElement[] ste = e.getStackTrace();
        StringBuilder str = new StringBuilder();

        str.append(e.getMessage()).append("\n");
        for (int i = 1; i < ste.length; ++i) {
            str.append("\tat ").append(ste[i]).append("\n");
        }

        return new DialogBuilder("Exception")
                .setMessage(str.toString())
                .addCloseButton();
    }
}
