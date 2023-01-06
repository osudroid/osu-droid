package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.TextView;

import com.edlplan.ui.InvalidateView;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.tables.DialogTable;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.utils.DrawFPSHandler;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.utils.Animation;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class MainOverlay extends BaseFragment {

    public static MainOverlay instance;

    private TextView
            author,
            memory,
            drawFPS,
            engineFPS;

    private final DecimalFormat df = new DecimalFormat("#.##");

    private boolean isUpdateTimeout = false;

    //--------------------------------------------------------------------------------------------//

    public MainOverlay() {
        super(Screens.values());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.main_overlay;
    }

    @Override
    protected String getPrefix() {
        return "mo";
    }

    @Override
    protected boolean isOverlay() {
        return true;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        InvalidateView invalidateView = find("invalidate");

        author = find("author");
        memory = find("memory");
        drawFPS = find("drawFPS");
        engineFPS = find("engineFPS");

        invalidateView.runOnUpdate(() -> {
            if (!isUpdateTimeout) {
                isUpdateTimeout = true;

                invalidateView.postDelayed(() -> {
                    if (Game.engine.getScene() != Game.gameScene.getScene()) {
                        updateEngineFPS(Game.engine.getFPS(), Game.engine.getFrameTime());
                    }
                    updateDrawFPS();
                    isUpdateTimeout = false;
                }, 100);
            }
        });

        author.setText(Res.str(R.string.app_name) + " " + BuildConfig.VERSION_NAME);

        bindTouch(author, () -> new Dialog(DialogTable.author()).show());
    }

    @Override
    protected void onUpdate(float sec) {
        if (!isLoaded()) {
            return;
        }

        long free = Runtime.getRuntime().freeMemory() / 1024 / 1024;
        long total = Runtime.getRuntime().totalMemory() / 1024 / 1024;

        memory.setText("Memory: " + free + "/" + total + "mb");
    }

    @Override
    protected void onScreenChange(Screens lastScreen, Screens newScreen) {
        showAuthorText(newScreen == Screens.Main);
    }

    //--------------------------------------------------------------------------------------------//

    private int getFpsColorJudgment(float fps) {
        int color = 0xFF36AE7C;

        if (fps <= getHz() - 2) {
            color = 0xFFF9D923;
        }
        if (fps <= getHz() / 1.25) {
            color = 0xFFEB5353;
        }
        return color;
    }

    private float getHz() {
        return GameHelper.Round(Game.activity.getRefreshRate(), 0);
    }

    public void updateEngineFPS(float rawFPS, float rawFT) {
        Game.activity.runOnUiThread(() -> {
            float fps = GameHelper.Round(rawFPS, 1);
            float ft = GameHelper.Round(rawFT, 1);

            engineFPS.setTextColor(getFpsColorJudgment(fps));
            engineFPS.setText("Engine: " + fps + "/" + getHz() + "fps - " + ft + "ms");
        });
    }

    private void updateDrawFPS() {
        float fps = GameHelper.Round(DrawFPSHandler.getFPS(), 1);
        float ft = DrawFPSHandler.getFrameTime();

        drawFPS.setTextColor(getFpsColorJudgment(fps));
        drawFPS.setText("UI: " + fps + "/" + getHz() + "fps - " + ft + "ms");
    }

    //--------------------------------------------------------------------------------------------//

    private void showAuthorText(boolean bool) {
        if (author == null) {
            return;
        }

        Animation anim = Animation.of(author);

        if (bool && author.getVisibility() != View.VISIBLE) {
            anim.toAlpha(1);
            anim.toY(0);
            anim.runOnStart(() -> author.setVisibility(View.VISIBLE));
        } else {
            anim.toAlpha(0);
            anim.toY(50);
            anim.runOnEnd(() -> author.setVisibility(View.GONE));
        }
        anim.play(200);
    }

    @Override
    public void close() {
        showAuthorText(false);

        Animation.of(rootView)
                .runOnEnd(super::close)
                .play(200);
    }
}
