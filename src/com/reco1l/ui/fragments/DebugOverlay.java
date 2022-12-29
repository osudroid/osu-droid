package com.reco1l.ui.fragments;

import android.widget.TextView;

import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.BaseFragment;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osuplus.R;

public final class DebugOverlay extends BaseFragment {

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

    @Override
    protected Screens getParent() {
        return Screens.Main;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        text = find("text");
    }

    @Override
    protected void onUpdate(float sec) {
        if (text == null)
            return;

        String string =
                Game.activity.getRenderer() + "\n" +
                "current_beat: " + Game.timingWrapper.getBeat();

        text.setText(string);
    }
}
