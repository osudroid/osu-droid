package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.content.Intent;
import android.os.Environment;
import com.google.android.material.snackbar.Snackbar;

import androidx.core.content.FileProvider;

import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.edlplan.framework.easing.Easing;
import com.edlplan.replay.OsuDroidReplayPack;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;
import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.DatabaseManager;
import com.reco1l.osu.ui.MessageDialog;
import com.reco1l.toolkt.android.Dimensions;
import com.reco1l.toolkt.android.Views;

import java.io.File;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ScoreMenuFragment extends BaseFragment {

    private BeatmapInfo beatmap;
    private int scoreId;

    public ScoreMenuFragment() {
        setDismissOnBackgroundClick(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.score_options_fragment;
    }

    @Override
    protected void onLoadView() {
        findViewById(R.id.exportReplay).setOnClickListener(v -> {

            var scoreInfo = DatabaseManager.getScoreInfoTable().getScore(scoreId);

            if (scoreInfo != null) {
                try {
                    String beatmapFilename = beatmap.getFilename();

                    final File file = new File(
                            new File(Environment.getExternalStorageDirectory(), "osu!droid/export"),
                            String.format(Locale.getDefault(), "%s [%s]-%d.edr",
                                    beatmapFilename.subSequence(0, beatmapFilename.lastIndexOf('.')),
                                    scoreInfo.getPlayerName(),
                                    scoreInfo.getTime())
                    );
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    OsuDroidReplayPack.packTo(file, beatmap, scoreInfo);

                    Snackbar.make(v, String.format(getResources().getString(com.osudroid.resources.R.string.frg_score_menu_export_succeed), file.getAbsolutePath()), 2750).setAction("Share", new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent();
                            intent.setAction(Intent.ACTION_VIEW);
                            intent.setDataAndType(FileProvider.getUriForFile(
                                    GlobalManager.getInstance().getMainActivity(),
                                    BuildConfig.APPLICATION_ID + ".fileProvider",
                                    file), "*/*");
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            GlobalManager.getInstance().getMainActivity().startActivityForResult(intent, 0);
                        }
                    }).show();
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(v.getContext(), com.osudroid.resources.R.string.frg_score_menu_export_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });

        findViewById(R.id.deleteReplay).setOnClickListener(v -> {
            new MessageDialog()
                .setTitle("Delete replay").setMessage("Are you sure?")
                .addButton("Yes", dialog -> {

                    try {
                        dialog.dismiss();
                        ScoreMenuFragment.this.dismiss();

                        var scoreInfoTable = DatabaseManager.getScoreInfoTable();
                        var scoreInfo = scoreInfoTable.getScore(scoreId);

                        if (scoreInfo != null && scoreInfoTable.deleteScore(scoreId) != 0) {
                            Snackbar.make(v, com.osudroid.resources.R.string.menu_deletescore_delete_success, 1500).show();
                            GlobalManager.getInstance().getSongMenu().reloadScoreboard();

                            new File(scoreInfo.getReplayPath()).delete();
                        } else {
                            Snackbar.make(v, "Failed to delete replay!", 1500).show();
                        }
                    } catch (Exception e) {
                        Log.e("ScoreMenuFragment", "Failed to delete replay", e);
                        Toast.makeText(v.getContext(), "Failed to delete replay!", Toast.LENGTH_SHORT).show();
                    }

                    return null;
                })
                .addButton("Cancel", dialog -> {
                    dialog.dismiss();
                    return null;
                })
                .show();
        });

        playOnLoadAnim();
        Views.setCornerRadius(findViewById(R.id.fullLayout), Dimensions.getDp(14f));
    }

    @Override
    public void dismiss() {
        playEndAnim(super::dismiss);
    }

    private void playOnLoadAnim() {
        View body = findViewById(R.id.fullLayout);
        body.setAlpha(0);
        body.setTranslationY(200);
        body.animate().cancel();
        body.animate()
                .translationY(0)
                .alpha(1)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
                .setDuration(150)
                .start();
        playBackgroundHideInAnim(150);
    }

    private void playEndAnim(Runnable action) {
        View body = findViewById(R.id.fullLayout);
        body.animate().cancel();
        body.animate()
                .translationY(200)
                .alpha(0)
                .setDuration(200)
                .setInterpolator(EasingHelper.asInterpolator(Easing.InOutQuad))
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


    public void show(BeatmapInfo beatmap, int scoreId) {
        this.beatmap = beatmap;
        this.scoreId = scoreId;
        show();
    }
}
