package com.reco1l.ui.fragments;

import android.widget.TextView;

import com.reco1l.Game;
import com.reco1l.andengine.BaseScene;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.UIFragment;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osuplus.R;

public class DebugOverlay extends UIFragment {

    public static DebugOverlay instance;

    private TextView text;
    private final DecimalFormat df = new DecimalFormat("#.###");

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

        String string =
                Game.activity.getRenderer() + "\n" +
                "current_beat: " + Game.timingWrapper.getBeat();

        text.setText(string);
    }
}
