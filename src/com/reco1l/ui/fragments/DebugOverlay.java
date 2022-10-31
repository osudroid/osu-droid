package com.reco1l.ui.fragments;

import android.widget.TextView;

import com.reco1l.Game;
import com.reco1l.andengine.OsuScene;
import com.reco1l.ui.platform.UIFragment;

import ru.nsu.ccfit.zuev.osuplus.R;

public class DebugOverlay extends UIFragment {

    public static DebugOverlay instance;

    private TextView text;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.debug_overlay;
    }

    @Override
    protected String getPrefix() {
        return "do";
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        text = find("text");
    }

    @Override
    protected void onUpdate(float secondsElapsed) {
        if (text == null)
            return;

        short beat = 0;

        if(Game.engine.getScene() instanceof OsuScene) {
            OsuScene scene = (OsuScene) Game.engine.getScene();

            if (scene.timingWrapper != null) {
                beat = (short) scene.timingWrapper.beat;
            }
        }

        String string =
                "audio_level: " + Game.songService.getLevel() + "\n" +
                "current_beat: " + beat;

        text.setText(string);
    }
}
