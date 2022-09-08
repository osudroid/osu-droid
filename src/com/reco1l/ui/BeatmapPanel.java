package com.reco1l.ui;

import android.animation.ValueAnimator;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.AsyncTask;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.reco1l.ui.data.helpers.BeatmapHelper;
import com.reco1l.ui.data.Property;
import com.reco1l.ui.data.scoreboard.ScoreboardAdapter;
import com.reco1l.ui.data.scoreboard.ScoreboardItem;
import com.reco1l.ui.platform.UIFragment;
import com.reco1l.utils.Animation;
import com.reco1l.utils.ClickListener;
import com.reco1l.utils.Res;
import com.reco1l.utils.interfaces.IGameMods;
import com.reco1l.utils.interfaces.UI;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyReCalculator;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class BeatmapPanel extends UIFragment implements IGameMods {

    private int bodyWidth;
    private LinearLayout banner, songProperties;

    private ImageView songBackground;
    private View body, message, pageContainer, tabIndicator;
    private TextView localTab, globalTab, propertiesTab, messageText;

    private boolean
            isTabAnimInProgress = false,
            isBannerExpanded = true;

    // Map properties
    private TrackInfo track;
    private TextView title, artist, mapper, difficulty;

    private final Property.Bpm pBPM;
    private final Property.Length pLength;
    private final Property.Int pCombo, pCircles, pSliders, pSpinners;
    private final Property.Float pAR, pOD, pCS, pHP, pStars;
    // End

    private Scoreboard scoreboard;
    public boolean isOnlineBoard = false;
    private boolean initialized = false;

    //--------------------------------------------------------------------------------------------//

    public BeatmapPanel() {
        pStars = new Property.Float();

        pBPM = new Property.Bpm();
        pLength = new Property.Length();

        pCombo = new Property.Int();
        pCircles = new Property.Int();
        pSliders = new Property.Int();
        pSpinners = new Property.Int();

        pAR = new Property.Float();
        pOD = new Property.Float();
        pCS = new Property.Float();
        pHP = new Property.Float();
    }

    @Override
    protected int getLayout() {
        return R.layout.beatmap_panel;
    }

    @Override
    protected String getPrefix() {
        return "bp";
    }

    //--------------------------------------------------------------------------------------------//
    @Override
    protected void onLoad() {
        setDismissMode(false, false);

        track = global.getSelectedTrack();

        bodyWidth = (int) Res.dimen(R.dimen.beatmapPanelWidth);
        isBannerExpanded = true;

        if (scoreboard == null) {
            scoreboard = new Scoreboard(find("scoreboard"));
        }

        body = find("body");

        new Animation(body)
                .marginTop(0, (int) Res.dimen(R.dimen.topBarHeight))
                .moveX(-bodyWidth, 0)
                .fade(0, 1)
                .play(300);

        title = find("title");
        artist = find("artist");
        mapper = find("mapper");
        banner = find("banner");

        difficulty = find("difficulty");
        message = find("messageLayout");
        messageText = find("messageTv");
        songProperties = find("properties");
        pageContainer = find("pageContainer");
        songBackground = find("songBackground");

        localTab = find("localTab");
        globalTab = find("globalTab");
        propertiesTab = find("propertiesTab");
        tabIndicator = find("tabIndicator");
        ImageView expand = find("expand");

        message.setVisibility(View.GONE);

        new ClickListener(localTab).simple(() -> switchTab(localTab));
        new ClickListener(globalTab).simple(() -> switchTab(globalTab));
        new ClickListener(propertiesTab).simple(() -> switchTab(propertiesTab));

        int max = (int) GlobalManager.getInstance().getMainActivity().getResources().getDimension(R.dimen._84sdp);

        new ClickListener(expand).simple(() -> {
            if (isBannerExpanded) {
                ValueAnimator anim = ValueAnimator.ofInt(max, 0);
                anim.setDuration(300);
                anim.addUpdateListener(value -> {
                    songProperties.getLayoutParams().height = (int) value.getAnimatedValue();
                    songProperties.requestLayout();
                });
                anim.start();
                isBannerExpanded = false;
            } else {
                ValueAnimator anim = ValueAnimator.ofInt(0, max);
                anim.setDuration(300);
                anim.addUpdateListener(value -> {
                    songProperties.getLayoutParams().height = (int) value.getAnimatedValue();
                    songProperties.requestLayout();
                });
                anim.start();
                isBannerExpanded = true;
            }
        });

        pStars.view = find("stars");
        pStars.format = val -> "" + GameHelper.Round((float) val, 2);
        pStars.allowColorChange = false;

        pBPM.view = find("bpm");
        pLength.view = find("length");

        pCombo.view = find("combo");
        pCircles.view = find("circles");
        pSliders.view = find("sliders");
        pSpinners.view = find("spinners");

        pAR.view = find("ar");
        pOD.view = find("od");
        pCS.view = find("cs");
        pHP.view = find("hp");

        pBPM.format = val -> "" + GameHelper.Round((float) val, 1);
        pAR.format = val -> "" + GameHelper.Round((float) val, 2);
        pOD.format = val -> "" + GameHelper.Round((float) val, 2);
        pCS.format = val -> "" + GameHelper.Round((float) val, 2);
        pHP.format = val -> "" + GameHelper.Round((float) val, 2);

        initialized = true;
        updateProperties(track);
        switchTab(localTab);
    }

    //--------------------------------------------------------------------------------------------//

    // Code transformed from old SongMenu
    private void updateDimensionProperties() {
        if (track == null)
            return;

        EnumSet<GameMod> mod = modMenu.getMod();

        pOD.set(track.getOverallDifficulty());
        pAR.set(track.getApproachRate());
        pCS.set(track.getCircleSize());
        pHP.set(track.getHpDrain());

        pLength.set(track.getMusicLength());
        pBPM.set(track.getBpmMin(), track.getBpmMax());
        pStars.set(track.getDifficulty());

        if (mod.contains(EZ)) {
            pAR.val *= 0.5f;
            pOD.val *= 0.5f;
            pHP.val *= 0.5f;
            pCS.val -= 1f;
        }
        if (mod.contains(HR)) {

            pAR.val = Math.min(pAR.val * 1.4f, 10);
            pOD.val *= 1.4f;
            pHP.val *= 1.4f;
            pCS.val += 1f;
        }
        if (mod.contains(REZ)) {
            if (mod.contains(EZ)) {
                pAR.val *= 2f;
                pAR.val -= 0.5f;
            }
            pAR.val -= 0.5f;
            if (modMenu.getChangeSpeed() != 1) {
                pAR.val -= modMenu.getSpeed() - 1.0f;
            }
            else if (mod.contains(DT) || mod.contains(NC)) {
                pAR.val -= 0.5f;
            }
            pOD.val *= 0.5f;
            pHP.val *= 0.5f;
            pCS.val -= 1f;
        }
        if (mod.contains(SC)) {
            pCS.val += 4f;
        }

        if (modMenu.getChangeSpeed() != 1) {
            float speed = modMenu.getSpeed();

            pBPM.min *= speed;
            pBPM.max *= speed;

            pLength.val /= speed;
        } else {
            if (mod.contains(DT) || mod.contains(NC)) {
                pBPM.min *= 1.5f;
                pBPM.max *= 1.5f;
                pLength.val *= 2 / 3f;
            }
            if (mod.contains(HT)) {
                pBPM.min *= 0.7f;
                pBPM.max *= 0.7f;
                pLength.val *= 4 / 3f;
            }
        }

        pAR.val = Math.min(13.f, pAR.val);
        pOD.val = Math.min(10.f, pOD.val);
        pCS.val = Math.min(15.f, pCS.val);
        pHP.val = Math.min(10.f, pHP.val);

        if (modMenu.getChangeSpeed() != 1) {
            float speed = modMenu.getSpeed();
            pAR.val = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(pAR.val) / speed), 2);
            pOD.val = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(pOD.val) / speed), 2);
        } else if (mod.contains(DT) || mod.contains(NC)) {
            pAR.val = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(pAR.val) * 2 / 3), 2);
            pOD.val = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(pOD.val) * 2 / 3), 2);
        } else if (mod.contains(HT)) {
            pAR.val = GameHelper.Round(GameHelper.ms2ar(GameHelper.ar2ms(pAR.val) * 4 / 3), 2);
            pOD.val = GameHelper.Round(GameHelper.ms2od(GameHelper.od2ms(pOD.val) * 4 / 3), 2);
        }
        if (modMenu.isEnableForceAR()) {
            pAR.val = modMenu.getForceAR();
        }

        if (isShowing) {
            pOD.update();
            pAR.update();
            pCS.update();
            pHP.update();

            pLength.update();
            pBPM.update();
        }
    }

    public void updateProperties(TrackInfo track) {
        this.track = track;
        if (track == null || !initialized)
            return;

        mActivity.runOnUiThread(() -> {

            pCircles.set(track.getHitCircleCount());
            pSpinners.set(track.getSpinnerCount());
            pSliders.set(track.getSliderCount());
            pCombo.set(track.getMaxCombo());

            updateDimensionProperties();

            new Thread(() -> {
                DifficultyReCalculator math = new DifficultyReCalculator();
                pStars.val = math.recalculateStar(track, math.getCS(track), modMenu.getSpeed());
                pStars.set(pStars.val);
            }).start();

            if (!isShowing)
                return;

            setText(title, BeatmapHelper.getTitle(track));
            setText(artist, "by " + BeatmapHelper.getArtist(track));
            setText(mapper, track.getCreator());
            setText(difficulty, track.getMode());

            if (track.getBackground() != null) {
                songBackground.setVisibility(View.VISIBLE);
                songBackground.setImageDrawable(BeatmapHelper.getBackground(track));
                find("gradient").setAlpha(1);
            } else {
                songBackground.setVisibility(View.INVISIBLE);
                find("gradient").setAlpha(0);
            }

            pStars.update();
            pCircles.update();
            pSpinners.update();
            pSliders.update();
            pCombo.update();

            final int current = pStars.view.getBackgroundTintList().getDefaultColor();
            float[] color = BeatmapHelper.getColor(pStars.val);

            new Animation(pStars.view)
                    .ofArgb(current, Color.HSVToColor(color))
                    .runOnUpdate(val -> pStars.view.getBackground().setTint((int) val.getAnimatedValue()))
                    .runOnStart(pStars::update)
                    .cancelPending(false)
                    .play(500);

            final float[] dark = {
                    color[0],
                    pStars.val >= 10 ? 0 : 0.70f,
                    pStars.val >= 10 ? 0.08f : 0.20f
            };

            int bannerColor = ((ColorDrawable) banner.getBackground()).getColor();
            int mapperColor = mapper.getBackgroundTintList().getDefaultColor();

            new Animation(banner)
                    .ofArgb(bannerColor, Color.HSVToColor(dark))
                    .runOnUpdate(val -> banner.setBackgroundColor((int) val.getAnimatedValue()))
                    .play(500);

            new Animation(pStars.view)
                    .ofArgb(mapperColor, Color.HSVToColor(180, dark))
                    .runOnUpdate(val -> mapper.getBackground().setTint((int) val.getAnimatedValue()))
                    .cancelPending(false)
                    .play(500);

        });
    }

    private void switchTab(View button) {
        if (isTabAnimInProgress || tabIndicator.getTranslationX() == button.getX())
            return;

        boolean toRight = tabIndicator.getTranslationX() < button.getX();

        new Animation(tabIndicator).moveX(tabIndicator.getTranslationX(), button.getX())
                .play(150);
        new Animation(pageContainer).moveX(0, toRight ? -60 : 60).fade(1, 0)
                .runOnStart(() -> isTabAnimInProgress = true)
                .play(200);
        new Animation(pageContainer).moveX(toRight ? 60 : -60, 0).fade(0, 1)
                .runOnEnd(() -> isTabAnimInProgress = false)
                .delay(200)
                .play(200);

        pageContainer.postDelayed(() -> {
            if (button == propertiesTab) {
                scoreboard.setVisibility(false);
                message.setVisibility(View.GONE);
                return;
            }

            scoreboard.setVisibility(true);
            isOnlineBoard = button == globalTab;
            updateScoreboard();
        }, 200);
    }

    public void updateScoreboard() {
        if (!isShowing || !initialized)
            return;

        mActivity.runOnUiThread(() -> {
            if (scoreboard.loadScores(global.getSelectedTrack(), isOnlineBoard)) {
                message.setVisibility(View.GONE);
            } else {
                message.setVisibility(View.VISIBLE);
                messageText.setText(scoreboard.errorMessage);
            }
        });
    }

    //--------------------------------------------------------------------------------------------//

    @Override
    public void close() {
        if (!isShowing)
            return;
        initialized = false;
        new Animation(body).moveX(0, -bodyWidth)
                .marginTop((int) Res.dimen(R.dimen.topBarHeight), 0)
                .fade(1, 0)
                .runOnEnd(super::close)
                .play(300);
    }

    //--------------------------------------------------------------------------------------------//
    // Temporal workaround until DuringGameScoreBoard gets replaced (old UI)

    public ScoreboardItem[] getBoard() {
        return scoreboard.boardScores.toArray(new ScoreboardItem[0]);
    }

    //--------------------------------------------------------------------------------------------//

    public static class Scoreboard implements UI {

        private final RecyclerView container;
        private final ScoreboardAdapter adapter;
        private final List<ScoreboardItem> boardScores;

        private AsyncTask<OsuAsyncCallback, Integer, Boolean> loadingTask;
        private Cursor currentCursor;

        public String errorMessage;

        //----------------------------------------------------------------------------------------//

        public Scoreboard(RecyclerView container) {
            this.container = container;

            boardScores = new ArrayList<>();
            adapter = new ScoreboardAdapter(boardScores);

            container.setLayoutManager(new LinearLayoutManager(beatmapPanel.getContext(), LinearLayoutManager.VERTICAL, true));
            container.setAdapter(adapter);
            container.setNestedScrollingEnabled(false);
        }

        //----------------------------------------------------------------------------------------//

        public void setVisibility(boolean bool) {
            container.setVisibility(bool ? View.VISIBLE : View.GONE);
        }

        public void clear() {
            errorMessage = null;
            if (loadingTask != null && loadingTask.getStatus() != AsyncTask.Status.FINISHED) {
                loadingTask.cancel(true);
                loadingTask = null;
            }
            if (currentCursor != null) {
                currentCursor.close();
                currentCursor = null;
            }
            boardScores.clear();
            if (beatmapPanel.isShowing) {
                adapter.notifyDataSetChanged();
            }
        }

        public boolean loadScores(TrackInfo track, boolean fromOnline) {
            clear();
            return fromOnline ? loadOnlineBoard(track) : loadLocalBoard(track);
        }

        // From offline
        //----------------------------------------------------------------------------------------//
        private boolean loadLocalBoard(TrackInfo track) {
            if (track == null)
                return false;

            String[] columns = {"id", "playername", "score", "combo", "mark", "accuracy", "mode"};
            Cursor cursor = ScoreLibrary.getInstance().getMapScores(columns, track.getFilename());
            currentCursor = cursor;

            if (cursor.getCount() == 0) {
                cursor.close();
                errorMessage = Res.str(R.string.beatmap_panel_empty_prompt);
                return false;
            }
            cursor.moveToFirst();

            loadingTask = new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                @Override
                public void run() {
                    long lastScore = 0;

                    for (int i = cursor.getCount() - 1; i >= 0; i--) {
                        cursor.moveToPosition(i);
                        ScoreboardItem item = new ScoreboardItem();

                        long currentScore = cursor.getLong(cursor.getColumnIndexOrThrow("score"));

                        item.rank = "" + i + 1;
                        item.mark = cursor.getString(cursor.getColumnIndexOrThrow("mark"));
                        item.name = cursor.getString(cursor.getColumnIndexOrThrow("playername"));

                        item.setDifference(currentScore - lastScore);
                        item.setCombo(cursor.getInt(cursor.getColumnIndexOrThrow("combo")));
                        item.setScore(cursor.getLong(cursor.getColumnIndexOrThrow("score")));
                        item.setMods(cursor.getString(cursor.getColumnIndexOrThrow("mode")));
                        item.setAccuracy(cursor.getFloat(cursor.getColumnIndexOrThrow("accuracy")));

                        lastScore = currentScore;
                        boardScores.add(item);
                    }
                }

                @Override
                public void onComplete() {
                    cursor.close();
                    currentCursor = null;
                    if (beatmapPanel.isShowing) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            return true;
        }

        // From online
        //----------------------------------------------------------------------------------------//
        private boolean loadOnlineBoard(TrackInfo track) {
            if (track == null || !online.isStayOnline() || BuildConfig.DEBUG) {
                errorMessage = Res.str(R.string.beatmap_panel_offline_prompt);
                return false;
            }

            File file = new File(track.getFilename());
            ArrayList<String> scores;
            try {
                scores = online.getTop(file, FileUtils.getMD5Checksum(file));
            }
            catch (OnlineManager.OnlineManagerException exception) {
                errorMessage = Res.str(R.string.beatmap_panel_error_prompt) + "\n(" + exception.getMessage() + ")";
                return false;
            }

            if (scores.size() == 0) {
                errorMessage = Res.str(R.string.beatmap_panel_empty_prompt);
                return false;
            }

            loadingTask = new AsyncTaskLoader().execute(new OsuAsyncCallback() {
                @Override
                public void run() {
                    long lastScore = 0;

                    for (int i = scores.size() - 1; i >= 0; i--) {
                        ScoreboardItem item = new ScoreboardItem();

                        String[] data = scores.get(i).split("\\s+");
                        if (data.length < 8)
                            continue;

                        long currentScore = Long.parseLong(data[2]);

                        item.id = Integer.parseInt(data[0]);
                        item.rank = data.length == 10 ? data[7] : "" + i + 1;
                        item.mark = data[4];
                        item.name = data.length == 10 ? data[8] : data[1];
                        item.avatar = data.length == 10 ? data[9] : data[7];

                        item.setDifference(currentScore - lastScore);
                        item.setAccuracy(Float.parseFloat(data[6]));
                        item.setCombo(Integer.parseInt(data[3]));
                        item.setScore(Long.parseLong(data[2]));
                        item.setMods(data[5]);

                        lastScore = currentScore;
                        boardScores.add(item);
                    }
                }

                @Override
                public void onComplete() {
                    if (beatmapPanel.isShowing) {
                        adapter.notifyDataSetChanged();
                    }
                }
            });
            return true;
        }
    }
}
