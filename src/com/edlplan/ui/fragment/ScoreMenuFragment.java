package com.edlplan.ui.fragment;

import android.animation.Animator;
import android.content.Intent;
import android.os.Environment;
import com.google.android.material.snackbar.Snackbar;
import androidx.core.content.FileProvider;
import android.view.View;
import android.widget.Toast;

import com.edlplan.framework.easing.Easing;
import com.edlplan.replay.OdrDatabase;
import com.edlplan.replay.OsuDroidReplay;
import com.edlplan.replay.OsuDroidReplayPack;
import com.edlplan.ui.BaseAnimationListener;
import com.edlplan.ui.EasingHelper;

import java.io.File;
import java.util.List;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ScoreMenuFragment extends BaseFragment {

    private int scoreId;

    public ScoreMenuFragment() {
        setDismissOnBackgroundClick(true);
    }

    @Override
    protected int getLayoutID() {
        return R.layout.fragment_score_menu;
    }

    @Override
    protected void onLoadView() {
        findViewById(R.id.exportReplay).setOnClickListener(v -> {
            List<OsuDroidReplay> replays = OdrDatabase.get().getReplayById(scoreId);
            if (replays.size() == 0) {
                return;
            } else {
                try {
                    OsuDroidReplay replay = replays.get(0);
                    final File file = new File(
                            new File(Environment.getExternalStorageDirectory(), "osu!droid/export"),
                            String.format(Locale.getDefault(), "%s [%s]-%d.edr",
                                    replay.getFileName().subSequence(replay.getFileName().indexOf('/') + 1, replay.getFileName().lastIndexOf('.')),
                                    replay.getPlayerName(),
                                    replay.getTime())
                    );
                    if (!file.getParentFile().exists()) {
                        file.getParentFile().mkdirs();
                    }
                    OsuDroidReplayPack.packTo(file, replay);

                    Snackbar.make(v, String.format(getResources().getString(R.string.frg_score_menu_export_succeed), file.getAbsolutePath()), 2750).setAction("Share", new View.OnClickListener() {
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
                    Toast.makeText(v.getContext(), R.string.frg_score_menu_export_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
        findViewById(R.id.deleteReplay).setOnClickListener(v -> {
            ConfirmDialogFragment confirm = new ConfirmDialogFragment();
            confirm.showForResult(isAccepted -> {
                if (isAccepted) {
                    List<OsuDroidReplay> replays = OdrDatabase.get().getReplayById(scoreId);
                    if (replays.size() == 0) {
                        return;
                    } else {
                        try {
                            if (OdrDatabase.get().deleteReplay(scoreId) == 0) {
                                Snackbar.make(v, "Failed to delete replay!", 1500)
                                        .show();
                            } else {
                                Snackbar.make(v, R.string.menu_deletescore_delete_success, 1500)
                                        .show();
                            }
                            ScoreMenuFragment.this.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(v.getContext(), "Failed to delete replay!", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            });
        });
        playOnLoadAnim();
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


    public void show(int scoreId) {
        this.scoreId = scoreId;
        show();
    }
}
