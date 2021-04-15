package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.edlplan.framework.easing.Easing;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyReCalculator;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MapInfoFragment extends BaseFragment {
    private TextView text;
    private String info;
    @Override
    protected int getLayoutID() {
        return R.layout.mapinfo_dialog;
    }

    @Override
    protected void onLoadView() {
        setDismissOnBackgroundClick(true);
        text = findViewById(R.id.mapinfo_text);
        text.setText(info);
        Button exit = findViewById(R.id.mapinfo_exit);
        exit.setOnClickListener(v -> {
            dismiss();
        });
        playOnLoadAnim();
    }

    private void playOnLoadAnim() {
        View body = findViewById(R.id.frg_body);
        body.setTranslationY(200);
        body.animate().cancel();
        body.animate()
                .translationY(0)
                .setDuration(200)
                .start();
        playBackgroundHideInAnim(200);
    }

    private void playEndAnim(Runnable action) {
        View body = findViewById(R.id.frg_body);
        body.animate().cancel();
        body.animate()
                .translationXBy(50)
                .alpha(0)
                .setDuration(150)
                .setInterpolator(EasingHelper.asInterpolator(Easing.OutQuad))
                .setListener(new BaseAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        if (action != null) {
                            action.run();
                        }
                    }
                })
                .start();
        playBackgroundHideOutAnim(200);
    }

    @Override
    public void dismiss() {
        playEndAnim(super::dismiss);
    }
/*
    public void showWithMap(final TrackInfo track, float speedMultiplier){
        DifficultyReCalculator diffRecalculator = new DifficultyReCalculator();
        if (!diffRecalculator.calculateMapInfo(track, speedMultiplier)){
            return;
        }
        int circleCount = track.getHitCircleCount();
        int sliderCount = track.getSliderCount();
        int spinnerCount = track.getSpinnerCount();
        int objectCount = track.getTotalHitObjectCount();
        float circlePercent = (float)circleCount / objectCount * 100;
        float sliderPercent = (float)sliderCount / objectCount * 100;
        float spinnerPercent = (float)spinnerCount / objectCount * 100;
        int singleCount = diffRecalculator.getSingleCount();
        int fastSingleCount = diffRecalculator.getFastSingleCount();
        int streamCount = diffRecalculator.getStreamCount();
        int jumpCount = diffRecalculator.getJumpCount();
        int multiCount = diffRecalculator.getMultiCount();
        int switchCount = diffRecalculator.getSwitchFingeringCount();
        float singlePercent = (float)singleCount / objectCount * 100;
        float fastSinglePercent = (float)fastSingleCount / objectCount * 100;
        float streamPercent = (float)streamCount / objectCount * 100;
        float jumpPercent = (float)jumpCount / objectCount * 100;
        float multiPercent = (float)multiCount / objectCount * 100;
        float switchPercent = (float)switchCount / objectCount * 100;
        int longestStreamCount = diffRecalculator.getLongestStreamCount();
        float realTime = diffRecalculator.getRealTime();
        float objectPerMin = objectCount / realTime * 60;

        StringBuilder string = new StringBuilder();
        //string.append(String.format(StringTable.get(R.string.binfoStr2),
        //    track.getHitCircleCount(), track.getSliderCount(), track.getSpinnerCount(), track.getBeatmapSetID()));
        //string.append("\n\r");
        string.append(String.format("圈数:%d[%.1f%%] 滑条数:%d[%.1f%%] 转盘数:%d[%.1f%%] 物件数:%d 实际时间:%.1fs %.1f物件/分",
                                    circleCount, circlePercent, sliderCount, sliderPercent, spinnerCount, spinnerPercent,
                                    objectCount, realTime, objectPerMin));
        string.append("\n\r");
        string.append(String.format("单点:%d[%.1f%%] 高速单点:%d[%.1f%%] 连打:%d[%.1f%%] 跳:%d[%.1f%%]",
                                    singleCount, singlePercent, fastSingleCount, fastSinglePercent,
                                    streamCount, streamPercent, jumpCount, jumpPercent));
        string.append("\n\r");
        string.append(String.format("多押:%d[%.1f%%] 切指:%d[%.1f%%] 最长连打:%d",
                                    multiCount, multiPercent,
                                    switchCount, switchPercent, longestStreamCount));
        info = string.toString();
        show();
        diffRecalculator = null;
    }
    */
}