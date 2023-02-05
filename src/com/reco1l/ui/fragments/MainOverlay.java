package com.reco1l.ui.fragments;

import android.view.View;
import android.widget.TextView;

import com.reco1l.global.Game;
import com.reco1l.global.Scenes;
import com.reco1l.scenes.BaseScene;
import com.reco1l.tables.DialogTable;
import com.reco1l.tables.Res;
import com.reco1l.ui.BaseFragment;
import com.reco1l.ui.custom.Dialog;
import com.reco1l.utils.Animation;
import com.reco1l.utils.NativeFrameCounter;

import java.text.DecimalFormat;

import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public final class MainOverlay extends BaseFragment {

    public static final MainOverlay instance = new MainOverlay();

    private TextView
            author,
            mPerformanceText;

    private final DecimalFormat df = new DecimalFormat("##");

    //--------------------------------------------------------------------------------------------//

    public MainOverlay() {
        super(Scenes.all());
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected int getLayout() {
        return R.layout.overlay_main;
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
        author = find("author");
        mPerformanceText = find("fps");

        author.setText(Res.str(R.string.app_name) + " " + BuildConfig.VERSION_NAME);

        bindTouch(author, () -> new Dialog(DialogTable.author()).show());
    }

    @Override
    protected void onEngineUpdate(float pSecElapsed) {
        if (Game.engine.getScene() == Scenes.player) {
            return;
        }

        float fps = NativeFrameCounter.getFPS();
        float ft = NativeFrameCounter.getFrameTime();

        updatePerformanceText(fps, ft);
    }

    @Override
    protected void onSceneChange(BaseScene oldScene, BaseScene newScene) {
        showAuthorText(newScene == Scenes.main);
    }

    //--------------------------------------------------------------------------------------------//

    private int getFpsColorJudgment(float fps) {
        int color = 0xFF36AE7C;
        float hz = Game.activity.getRefreshRate();

        if (fps <= hz - 2) {
            color = 0xFFF9D923;
        }
        if (fps <= hz / 1.25) {
            color = 0xFFEB5353;
        }
        return color;
    }

    public void updatePerformanceText(float rawFPS, float rawFT) {
        Game.activity.runOnUiThread(() -> {
            String fps = df.format(rawFPS);
            String ft = df.format(rawFT);

            mPerformanceText.setTextColor(getFpsColorJudgment(rawFPS));
            mPerformanceText.setText(fps + "fps - " + ft + "ms");
        });
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
