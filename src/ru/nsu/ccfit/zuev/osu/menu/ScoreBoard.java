package ru.nsu.ccfit.zuev.osu.menu;

import android.database.Cursor;

import com.reco1l.legacy.Multiplayer;

import org.anddev.andengine.entity.Entity;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.input.touch.detector.ScrollDetector;
import org.anddev.andengine.input.touch.detector.SurfaceScrollDetector;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;
import org.anddev.andengine.util.MathUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ScoreBoard extends Entity implements ScrollDetector.IScrollDetectorListener {

    private final Scene mainScene;

    private final MenuItemListener listener;

    private final ChangeableText loadingText;

    private final SurfaceScrollDetector mScrollDetector;

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();

    private float percentShow = -1;

    private boolean showOnlineScores = false;

    private TrackInfo lastTrack;

    private boolean wasOnline = false;

    private boolean isScroll = false;

    private float maxY = 100500;

    private int pointerId = -1;

    private float initialY = -1;

    private Float touchY;

    private float camY = -146;

    private float velocityY = 0;

    private float secPassed = 0;

    private float tapTime = 0;

    private float height = 0;

    private float downTime = -1;

    private int _scoreID = -1;

    private boolean moved = false;

    private ArrayList<ScoreBoardItem> scoreItems = null;

    private LoadTask currentTask;

    private Runnable currentAvatarTask;


    public ScoreBoard(final Scene scene, final Entity layer, final MenuItemListener listener) {
        this.mainScene = scene;
        layer.attachChild(this);

        this.loadingText = new ChangeableText(5, 230, ResourceManager.getInstance().getFont("strokeFont"), "", 50);
        this.attachChild(this.loadingText);

        this.listener = listener;
        this.mScrollDetector = new SurfaceScrollDetector(this);
    }

    public static String convertModString(StringBuilder sb, String s) {
        sb.setLength(0);
        String[] mods = s.split("\\|", 2);
        for (int i = 0; i < mods[0].length(); i++) {
            switch (mods[0].charAt(i)) {
                case 'a':
                    sb.append("Auto,");
                    break;
                case 'x':
                    sb.append("Relax,");
                    break;
                case 'p':
                    sb.append("AP,");
                    break;
                case 'e':
                    sb.append("EZ,");
                    break;
                case 'n':
                    sb.append("NF,");
                    break;
                case 'r':
                    sb.append("HR,");
                    break;
                case 'h':
                    sb.append("HD,");
                    break;
                case 'i':
                    sb.append("FL,");
                    break;
                case 'd':
                    sb.append("DT,");
                    break;
                case 'c':
                    sb.append("NC,");
                    break;
                case 't':
                    sb.append("HT,");
                    break;
                case 's':
                    sb.append("PR,");
                    break;
                case 'l':
                    sb.append("REZ,");
                    break;
                // Note: This is SmallCircles which is not available anymore, replaced with custom CS.
                case 'm':
                    sb.append("SC,");
                    break;
                case 'u':
                    sb.append("SD,");
                    break;
                case 'f':
                    sb.append("PF,");
                    break;
                case 'b':
                    sb.append("SU,");
                    break;
                case 'v':
                    sb.append("ScoreV2,");
                    break;
            }
        }

        if (mods.length > 1) {
            convertExtraModString(sb, mods[1]);
        }

        if (sb.length() == 0) {
            return "None";
        }

        return sb.toString().substring(0, sb.length() - 1);
    }

    private static void convertExtraModString(StringBuilder sb, String s) {
        var split = s.split("\\|");

        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < split.length; i++) {
            var str = split[i];

            if (str.isEmpty()) {
                continue;
            }

            if (str.charAt(0) == 'x' && str.length() == 5) {
                sb.append(str.substring(1)).append("x,");
            } else if (str.startsWith("AR") || str.startsWith("OD") || str.startsWith("CS") || str.startsWith("HP")) {
                sb.append(str).append(',');
            }
        }
    }

    private String formatScore(StringBuilder sb, int score) {
        sb.setLength(0);
        sb.append(Math.abs(score));
        for (int i = sb.length() - 3; i > 0; i -= 3) {
            sb.insert(i, ' ');
        }
        return sb.toString();
    }

    private void initFromOnline(final TrackInfo track) {
        loadingText.setText("Loading scores...");

        currentTask = new LoadTask(true) {

            @Override
            public void run() {

                File trackFile = new File(track.getFilename());
                List<String> scores;

                try {
                    scores = OnlineManager.getInstance().getTop(trackFile, track.getMD5());
                } catch (OnlineManager.OnlineManagerException e) {
                    Debug.e("Cannot load scores " + e.getMessage());

                    if (isActive()) {
                        loadingText.setText("Cannot load scores");
                    }
                    return;
                }

                if (!isActive()) {
                    return;
                }

                loadingText.setText(OnlineManager.getInstance().getFailMessage());

                var items = new ArrayList<ScoreBoardItem>(scores.size());
                var sb = new StringBuilder();

                int nextTotalScore = 0;

                for (int i = 0; i < scores.size() && isActive(); ++i) {
                    Debug.i(scores.get(i));

                    String[] data = scores.get(i).split("\\s+");

                    if (data.length < 8 || data.length > 9) {
                        continue;
                    }

                    final int scoreID = Integer.parseInt(data[0]);

                    var isInLeaderboard = data.length == 8;
                    var isPersonalBest = data.length == 9 || data[1].equals(OnlineManager.getInstance().getUsername());

                    var playerName = isPersonalBest ? OnlineManager.getInstance().getUsername() : data[1];
                    var currentTotalScore = Integer.parseInt(data[2]);
                    var combo = Integer.parseInt(data[3]);
                    var mark = data[4];
                    var modString = data[5];
                    var accuracy = GameHelper.Round(Integer.parseInt(data[6]) / 1000f, 2);
                    var avatarURL = data[7];
                    var beatmapRank = isPersonalBest && !isInLeaderboard ? Integer.parseInt(data[8]) : (i + 1);

                    final String titleStr = "#" + beatmapRank + " " + playerName + "\n" + StringTable.format(R.string.menu_score, formatScore(sb, currentTotalScore), combo);

                    if (i < scores.size() - 1) {
                        String[] nextData = scores.get(i + 1).split("\\s+");

                        if (nextData.length == 8 || nextData.length == 9) {
                            nextTotalScore = Integer.parseInt(nextData[2]);
                        }
                    } else {
                        nextTotalScore = 0;
                    }

                    final int diffTotalScore = currentTotalScore - nextTotalScore;
                    final String accStr = convertModString(sb, modString) +
                                          "\n" +
                                          String.format(Locale.ENGLISH, "%.2f", accuracy) +
                                          "%" +
                                          "\n" +
                                          (nextTotalScore == 0 ? "-" : ((diffTotalScore != 0 ? "+" : "") + diffTotalScore));

                    if (!isActive()) {
                        return;
                    }

                    if (isPersonalBest) {
                        attachChild(new ScoreItem(avatarExecutor, titleStr, accStr, mark, true, scoreID, avatarURL, playerName, true), 0);
                    }

                    if (isInLeaderboard) {
                        attachChild(new ScoreItem(avatarExecutor, titleStr, accStr, mark, true, scoreID, avatarURL, playerName, false));

                        ScoreBoardItem item = new ScoreBoardItem();
                        item.set(beatmapRank, playerName, combo, currentTotalScore, scoreID);

                        items.add(item);
                    }
                }
                scoreItems = items;
                percentShow = 0;
            }
        };
        loadExecutor.submit(currentTask);
    }

    private void initFromLocal(TrackInfo track) {

        currentTask = new LoadTask(false) {

            @Override
            public void run() {
                String[] columns = {"id", "playername", "score", "combo", "mark", "accuracy", "mode"};
                try (Cursor scoreSet = ScoreLibrary.getInstance().getMapScores(columns, track.getFilename())) {
                    if (scoreSet == null || scoreSet.getCount() == 0 || !isActive()) {

                        // This allows the in-game leaderboard to show even if the local database is empty, it'll append
                        // the player score (because the in-game leaderboard assumes that the board finished loading only
                        // if the scores list isn't null).
                        if (isActive()) {
                            scoreItems = new ArrayList<>();
                        }
                        return;
                    }

                    var items = new ArrayList<ScoreBoardItem>(scoreSet.getCount());
                    var sb = new StringBuilder();

                    int nextTotalScore;

                    for (int i = 0; i < scoreSet.getCount() && isActive(); ++i) {
                        scoreSet.moveToPosition(i);
                        final int scoreID = scoreSet.getInt(0);

                        final int currTotalScore = scoreSet.getInt(scoreSet.getColumnIndexOrThrow("score"));
                        final String totalScore = formatScore(sb, currTotalScore);
                        final String titleStr = "#" +
                                                (i + 1) +
                                                " " +
                                                scoreSet.getString(scoreSet.getColumnIndexOrThrow("playername")) +
                                                "\n" +
                                                StringTable.format(R.string.menu_score, totalScore, scoreSet.getInt(scoreSet.getColumnIndexOrThrow("combo")));

                        if (i < scoreSet.getCount() - 1) {
                            scoreSet.moveToPosition(i + 1);
                            nextTotalScore = scoreSet.getInt(scoreSet.getColumnIndexOrThrow("score"));
                            scoreSet.moveToPosition(i);
                        } else {
                            nextTotalScore = 0;
                        }

                        final long diffTotalScore = currTotalScore - nextTotalScore;
                        final String accStr = convertModString(sb, scoreSet.getString(scoreSet.getColumnIndexOrThrow("mode"))) +
                                              "\n" +
                                              String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(scoreSet.getFloat(scoreSet.getColumnIndexOrThrow("accuracy")) * 100, 2)) +
                                              "%" +
                                              "\n" +
                                              (nextTotalScore == 0 ? "-" : ((diffTotalScore != 0 ? "+" : "") + diffTotalScore));

                        if (!isActive()) {
                            return;
                        }

                        attachChild(new ScoreItem(avatarExecutor, titleStr, accStr, scoreSet.getString(scoreSet.getColumnIndexOrThrow("mark")), false, scoreID, null, null, false));

                        var item = new ScoreBoardItem();
                        item.set(i + 1,
                            scoreSet.getString(scoreSet.getColumnIndexOrThrow("playername")),
                            scoreSet.getInt(scoreSet.getColumnIndexOrThrow("combo")),
                            scoreSet.getInt(scoreSet.getColumnIndexOrThrow("score")),
                            scoreID);
                        items.add(item);
                    }
                    scoreItems = items;
                    percentShow = 0;
                }
            }
        };
        loadExecutor.submit(currentTask);
    }

    public synchronized void init(final TrackInfo track) {
        if (lastTrack == track && showOnlineScores == wasOnline && wasOnline) {
            return;
        }

        if (currentTask != null && currentTask.avatarExecutor != null) {
            currentTask.avatarExecutor.shutdownNow();
        }

        loadingText.setText("");
        lastTrack = track;
        wasOnline = showOnlineScores;
        scoreItems = null;

        SyncTaskManager.getInstance().run(() -> {

            detachChildren();
            currentAvatarTask = null;
            attachChild(loadingText);

            if (track == null) {
                return;
            }

            if (OnlineManager.getInstance().isStayOnline() && showOnlineScores) {
                initFromOnline(track);
                return;
            }

            initFromLocal(track);
        });
    }

    @Override
    protected void onManagedUpdate(float pSecondsElapsed) {
        super.onManagedUpdate(pSecondsElapsed);
        secPassed += pSecondsElapsed;

        // The first child is always the loading text, and only leaderboard items need to be updated.
        if (getChildCount() <= 1) {
            return;
        }

        if (percentShow == -1) {
            float y = -camY;

            var count = getChildCount();
            for (int i = 0; i < count; i++) {
                var child = getChild(i);

                if (!(child instanceof Sprite)) {
                    continue;
                }

                var sprite = (Sprite) child;
                sprite.setPosition(sprite.getX(), y);
                y += 0.8f * (sprite.getHeight() - 32);
            }

            y += camY;
            camY += velocityY * pSecondsElapsed;
            maxY = y - 0.8f * (Config.getRES_HEIGHT() - 110 - (height - 32));

            if (camY <= -146 && velocityY < 0 || camY > maxY && velocityY > 0) {
                camY -= velocityY * pSecondsElapsed;
                velocityY = 0;
                isScroll = false;
            }

            if (Math.abs(velocityY) > 500 * pSecondsElapsed) {
                velocityY -= 10 * pSecondsElapsed * Math.signum(velocityY);
            } else {
                velocityY = 0;
                isScroll = false;
            }
        } else {
            percentShow += pSecondsElapsed * 4;
            if (percentShow > 1) {
                percentShow = 1;
            }

            var count = getChildCount();
            for (int i = 0; i < count; i++) {
                var child = getChild(i);

                if (!(child instanceof Sprite)) {
                    continue;
                }

                var sprite = (Sprite) child;
                sprite.setPosition(-160, 146 + 0.8f * percentShow * i * (sprite.getHeight() - 32));
            }

            if (percentShow == 1) {
                percentShow = -1;
            }
        }

        if (downTime >= 0) {
            downTime += pSecondsElapsed;
        }

        if (downTime > 0.5f) {
            moved = true;
            if (!Multiplayer.isMultiplayer && _scoreID != -1 && !showOnlineScores) {
                GlobalManager.getInstance().getSongMenu().showDeleteScoreMenu(_scoreID);
            }
            downTime = -1;
        }
    }

    @Override
    public void onScroll(ScrollDetector pScrollDetector, TouchEvent pTouchEvent, float pDistanceX, float pDistanceY) {
        switch (pTouchEvent.getAction()) {
            case TouchEvent.ACTION_DOWN:
                velocityY = 0;
                touchY = pTouchEvent.getY();
                pointerId = pTouchEvent.getPointerID();
                tapTime = secPassed;
                initialY = touchY;
                isScroll = true;
                break;
            case TouchEvent.ACTION_MOVE:
                if (pointerId == -1 || pointerId == pTouchEvent.getPointerID()) {
                    isScroll = true;
                    if (initialY == -1) {
                        velocityY = 0;
                        touchY = pTouchEvent.getY();
                        pointerId = pTouchEvent.getPointerID();
                        tapTime = secPassed;
                        initialY = touchY;
                    }

                    final float dy = pTouchEvent.getY() - touchY;

                    camY -= dy;
                    touchY = pTouchEvent.getY();
                    if (camY <= -146) {
                        camY = -146;
                        velocityY = 0;
                    } else if (camY >= maxY) {
                        camY = maxY;
                        velocityY = 0;
                    }
                }
                break;
            default:
                if (pointerId == -1 || pointerId == pTouchEvent.getPointerID()) {
                    touchY = null;

                    if (secPassed - tapTime < 0.001f || initialY == -1) {
                        velocityY = 0;
                        isScroll = false;
                    } else {
                        velocityY = (initialY - pTouchEvent.getY()) / (secPassed - tapTime);
                        isScroll = true;
                    }

                    pointerId = -1;
                    initialY = -1;
                }
                break;
        }
    }

    public boolean isShowOnlineScores() {
        return showOnlineScores;
    }

    public void setShowOnlineScores(boolean showOnlineScores) {
        this.showOnlineScores = showOnlineScores;
    }

    @Nullable
    public ArrayList<ScoreBoardItem> getScoreBoardItems() {
        return scoreItems;
    }


    private abstract class LoadTask implements Runnable {

        protected final ExecutorService avatarExecutor;

        LoadTask(boolean fromOnline) {
            avatarExecutor = fromOnline ? Executors.newSingleThreadExecutor() : null;
        }

        protected final boolean isActive() {
            return currentTask == this;
        }

    }


    private class ScoreItem extends Sprite {


        private final ExecutorService avatarExecutor;

        private final String username;

        private final int scoreID;

        private final boolean showOnline;

        private float dx = 0;

        private float dy = 0;

        private TextureRegion avatarTexture;

        private Runnable avatarTask;


        private ScoreItem(
            ExecutorService avatarExecutor, String title, String acc, String markStr, boolean showOnline, int scoreID, String avaURL, String username, boolean isPersonalBest) {
            super(-150, 40, ResourceManager.getInstance().getTexture("menu-button-background").deepCopy());

            this.avatarExecutor = avatarExecutor;
            this.showOnline = showOnline;
            this.username = username;
            this.scoreID = scoreID;

            var shouldLoadAvatar = showOnlineScores && Config.getLoadAvatar() && avaURL != null && avatarExecutor != null;

            int baseX = shouldLoadAvatar ? 90 : 0;
            var baseY = 0f;

            if (isPersonalBest) {

                var topText = new Text(getWidth() / 2f, 0f, ResourceManager.getInstance().getFont("strokeFont"), "Personal Best");

                attachChild(topText);
                baseY = topText.getHeight() + 5;

                topText.setPosition((getWidth() + baseX - topText.getWidth()) / 2f, 20f);
                topText.setScale(0.8f);

                setHeight(baseY + 120);

            } else {
                setHeight(107);
            }

            setScale(0.65f);
            setWidth(724 * 1.1f);
            camY = -146;

            setColor(0, 0, 0);
            setAlpha(0.5f);

            float finalBaseY = baseY;
            avatarTask = shouldLoadAvatar ? new Runnable() {

                @Override
                public void run() {
                    var texture = ResourceManager.getInstance().getTexture("emptyavatar");

                    if (!avatarExecutor.isShutdown() && OnlineManager.getInstance().loadAvatarToTextureManager(avaURL)) {
                        avatarTexture = ResourceManager.getInstance().getAvatarTextureIfLoaded(avaURL);

                        if (avatarTexture != null) {
                            texture = avatarTexture;
                        }
                    }

                    if (getParent() == null) {
                        onDetached();
                        return;
                    }
                    attachChild(new Sprite(55, finalBaseY + 12, 90, 90, texture));

                    if (currentAvatarTask == this) {
                        currentAvatarTask = null;
                    }
                }
            } : null;


            var text = new Text(baseX + 160, baseY + 20, ResourceManager.getInstance().getFont("font"), title);
            var accText = new Text(670, baseY + 12, ResourceManager.getInstance().getFont("smallFont"), acc);
            var mark = new Sprite(baseX + 80, baseY + 35, ResourceManager.getInstance().getTexture("ranking-" + markStr + "-small"));

            text.setScale(1.2f);
            mark.setScale(1.5f);
            mark.setPosition(baseX + mark.getWidth() / 2 + 60, mark.getY());
            attachChild(text);
            attachChild(accText);
            attachChild(mark);

            mainScene.registerTouchArea(this);
            height = getHeight();
        }

        @Override
        public void onDetached() {
            if (avatarTexture != null) {
                ResourceManager.getInstance().unloadTexture(avatarTexture);
            }

            mainScene.unregisterTouchArea(this);
        }

        @Override
        protected void onManagedUpdate(float pSecondsElapsed) {
            super.onManagedUpdate(pSecondsElapsed);

            // This is to avoid loading avatars when the scene was changed (game started or user gone back to main menu).
            if (avatarTask != null && currentAvatarTask == null) {

                var task = avatarTask;
                avatarTask = null;
                currentAvatarTask = task;

                try {
                    avatarExecutor.submit(task);
                } catch (RejectedExecutionException e) {
                    if (currentAvatarTask == task) {
                        currentAvatarTask = null;
                    }
                }
            }
        }

        @Override
        public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
            mScrollDetector.onTouchEvent(event);
            mScrollDetector.setEnabled(true);

            if (event.isActionDown()) {
                moved = false;
                setAlpha(0.8f);
                listener.stopScroll(getY() + localY);
                dx = localX;
                dy = localY;
                downTime = 0;
                _scoreID = scoreID;
                return true;
            } else if (event.isActionUp() && !moved && !isScroll) {
                downTime = -1;
                setAlpha(0.5f);

                if (Multiplayer.isMultiplayer) {
                    return true;
                }

                listener.openScore(scoreID, showOnline, username);
                GlobalManager.getInstance().getScoring().setReplayID(scoreID);
                return true;
            } else if (event.isActionOutside() || event.isActionMove() && MathUtils.distance(dx, dy, localX, localY) > 10) {
                downTime = -1;
                setAlpha(0.5f);
                moved = true;
            }
            return false;
        }

    }

}

