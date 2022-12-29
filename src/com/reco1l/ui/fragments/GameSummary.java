package com.reco1l.ui.fragments;
// Created by Reco1l on 22/11/2022, 01:37

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.reco1l.Game;
import com.reco1l.enums.Screens;
import com.reco1l.ui.platform.BaseFragment;
import com.reco1l.utils.AnimationTable;
import com.reco1l.utils.Res;
import com.reco1l.utils.helpers.BeatmapHelper;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyReCalculator;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;
import ru.nsu.ccfit.zuev.osuplus.R;

public class GameSummary extends BaseFragment {

    public static GameSummary instance;

    private TextView title, artist, mapper, stars, difficulty;

    private ImageView avatar;
    private TextView username, date;

    private TextView hit300, hit300k, hit100, hit100k, hit50, hit0;

    private TextView accuracy, maxCombo;
    private CircularProgressIndicator accuracyIndicator, comboIndicator;

    private ImageView mark;
    private LinearLayout mods;
    private TextView score, ur, pp, error;

    private RankingSection rankingSection;
    private DifficultyReCalculator difficultyCalculator;

    private TrackInfo track;
    private StatisticV2 stats;

    //--------------------------------------------------------------------------------------------//

    @Override
    protected String getPrefix() {
        return "gr";
    }

    @Override
    protected int getLayout() {
        return R.layout.game_results;
    }

    @Override
    protected Screens getParent() {
        return Screens.Summary;
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    protected void onLoad() {
        title = find("title");
        artist = find("artist");
        mapper = find("mapper");
        stars = find("stars");
        difficulty = find("difficulty");

        username = find("username");
        avatar = find("avatar");

        hit300 = find("300");
        hit300k = find("300k");
        hit100 = find("100");
        hit100k = find("100k");
        hit50 = find("50");
        hit0 = find("0");

        mark = find("markIv");
        score = find("score");

        accuracy = find("accuracy");
        maxCombo = find("combo");

        accuracyIndicator = find("accuracyIndicator");
        comboIndicator = find("comboIndicator");

        mods = find("mods");
        date = find("date");

        pp = find("pp");
        ur = find("ur");
        error = find("error");

        if (rankingSection == null) {
            rankingSection = new RankingSection(this);
        }

        ImageView iv300 = find("300iv"),
                iv300k = find("300kiv"),
                iv100k = find("100kiv"),
                iv100 = find("100iv"),
                iv50 = find("50iv"),
                iv0 = find("0iv");

        iv300.setImageBitmap(Game.bitmapManager.get("hit300"));
        iv300k.setImageBitmap(Game.bitmapManager.get("hit300g"));
        iv100.setImageBitmap(Game.bitmapManager.get("hit100"));
        iv100k.setImageBitmap(Game.bitmapManager.get("hit100k"));
        iv50.setImageBitmap(Game.bitmapManager.get("hit50"));
        iv0.setImageBitmap(Game.bitmapManager.get("hit0"));

        if (track != null && stats != null) {
            loadData();
        }
    }

    public void setData(TrackInfo track, StatisticV2 stats) {
        this.track = track;
        this.stats = stats;
    }

    public void loadData() {
        if (track == null || stats == null || !isAdded()) {
            return;
        }

        difficultyCalculator = new DifficultyReCalculator();

        Game.activity.runOnUiThread(() -> {
            loadTrackData(track, stats);

            int totalScore = stats.getModifiedTotalScore();
            if (totalScore == 0) {
                totalScore = stats.getAutoTotalScore();
            }

            score.setText(new DecimalFormat("###,###,###,###").format(totalScore));

            hit300.setText(stats.getHit300() + "x");
            hit300k.setText(stats.getHit300k() + "x");
            hit100.setText(stats.getHit100() + "x");
            hit100k.setText(stats.getHit100k() + "x");
            hit50.setText(stats.getHit50() + "x");
            hit0.setText(stats.getMisses() + "x");

            maxCombo.setText(stats.getMaxCombo() + "/" + track.getMaxCombo());
            comboIndicator.setMax(track.getMaxCombo());
            comboIndicator.setProgress(stats.getMaxCombo());

            accuracy.setText(String.format("%.2f%%", stats.getAccuracy() * 100f));
            accuracyIndicator.setMax(100);
            accuracyIndicator.setProgress((int) (stats.getAccuracy() * 100f));

            mark.setImageBitmap(Game.bitmapManager.get("ranking-" + stats.getMark()));

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");

            date.setText("Played on " + sdf.format(new Date(stats.getTime())));
            username.setText(stats.getPlayerName());

            loadPerformanceStatistics(track, stats);
        });
    }

    private void loadTrackData(TrackInfo track, StatisticV2 stats) {

        title.setText(BeatmapHelper.getTitle(track));
        artist.setText(BeatmapHelper.getArtist(track));
        mapper.setText(track.getBeatmap().getCreator());
        difficulty.setText(track.getMode());

        float cs = difficultyCalculator.getCS(stats, track);
        float sr = difficultyCalculator.recalculateStar(track, cs, stats.getSpeed());

        stars.setText("" + sr);
    }

    private void loadPerformanceStatistics(TrackInfo track, StatisticV2 stats) {
        if (!Config.isDisplayScoreStatistics()) {
            find("performance").setVisibility(View.GONE);
            return;
        }

        difficultyCalculator.calculatePP(stats, track);
        double totalPP = difficultyCalculator.getTotalPP();

        difficultyCalculator.calculateMaxPP(stats, track);
        double maxPP = difficultyCalculator.getTotalPP();

        pp.setText(String.format("%.2f / %.2f", totalPP, maxPP));

        if (stats.getUnstableRate() <= 0) {
            find("urLayout").setVisibility(View.GONE);
            find("errorLayout").setVisibility(View.GONE);
            return;
        }

        ur.setText(String.format("%.2f", stats.getUnstableRate()));
        error.setText(String.format("%.2fms - %.2fms", stats.getNegativeHitError(), stats.getPositiveHitError()));
    }

    //--------------------------------------------------------------------------------------------//

    public void retrieveOnlineData() {
        Game.activity.runOnUiThread(rankingSection::retrieveOnlineData);
    }

    public void updateOnlineData(boolean success) {
        Game.activity.runOnUiThread(() ->
                rankingSection.updateOnlineData(success)
        );
    }

    //--------------------------------------------------------------------------------------------//

    public static class RankingSection {

        private final GameSummary parent;
        private final TextView mapRankText, rankText, accuracyText, scoreText;

        // Overall
        private long score, rank;
        private float accuracy;

        //----------------------------------------------------------------------------------------//

        private enum Difference {
            Positive(0x4D59B32D, 0xFFBBFF99),
            Negative(0x4DB32D2D, 0xFFFF9999);

            final int backgroundColor;
            final int textColor;

            Difference(int backgroundColor, int textColor) {
                this.backgroundColor = backgroundColor;
                this.textColor = textColor;
            }
        }

        //----------------------------------------------------------------------------------------//

        public RankingSection(GameSummary parent) {
            this.parent = parent;

            mapRankText = parent.find("mapRank");
            rankText = parent.find("overallRank");
            scoreText = parent.find("overallScore");
            accuracyText = parent.find("overallAccuracy");
        }

        //----------------------------------------------------------------------------------------//

        private void applyColoring(TextView text, Difference difference) {
            Drawable background = Res.drw(R.drawable.shape_rounded).mutate();
            background.setTint(difference.backgroundColor);

            text.setTextColor(difference.textColor);
            text.setBackground(background);
        }

        //----------------------------------------------------------------------------------------//

        public void retrieveOnlineData() {
            score = Game.onlineManager.getScore();
            accuracy = GameHelper.Round(Game.onlineManager.getAccuracy() * 100f, 2);
            rank = Game.onlineManager.getRank();

            if (parent.isAdded()) {
                parent.find("ranking").setVisibility(View.VISIBLE);
                parent.find("rankingStats").setAlpha(0f);
            }
        }

        public void updateOnlineData(boolean success) {
            if (!success) {
                parent.find("rankingStats").setAlpha(0f);
                parent.find("rankingFail").setAlpha(1f);
                return;
            }
            long newRank = Game.onlineManager.getRank();
            long newScore = Game.onlineManager.getScore();
            float newAccuracy = GameHelper.Round(Game.onlineManager.getAccuracy() * 100f, 2);

            mapRankText.setText("#" + Game.onlineManager.getMapRank());

            if (newScore > score) {
                applyColoring(mapRankText, Difference.Positive);
            }

            String string = "";

            if (newRank < rank) {
                string = "\n(+" + (rank - newRank) + ")";
                applyColoring(rankText, Difference.Positive);
            }
            if (newRank > rank) {
                string = "\n(" + (rank - newRank) + ")";
                applyColoring(rankText, Difference.Negative);
            }
            rankText.setText("#" + newRank + string);

            string = "";
            if (newAccuracy < accuracy) {
                string = "\n(" + (newAccuracy - accuracy) + "%)";
                applyColoring(accuracyText, Difference.Negative);
            }
            if (newAccuracy > accuracy) {
                string = "\n(+" + (newAccuracy - accuracy) + "%)";
                applyColoring(accuracyText, Difference.Positive);
            }
            accuracyText.setText(newAccuracy + "%" + string);

            DecimalFormat df = new DecimalFormat("###,###,###,###,###");
            string = "";

            if (newScore < score) {
                string = "\n(" + df.format(newScore - score) + ")";
                applyColoring(scoreText, Difference.Negative);
            } else if (newScore > score) {
                string = "\n(+" + df.format(newScore - score) + ")";
                applyColoring(scoreText, Difference.Positive);
            }
            scoreText.setText(df.format(newScore) + string);

            AnimationTable.fadeOutScaleOut(parent.find("rankingLoading"));
            parent.find("rankingStats").setAlpha(1f);
        }
    }
}
