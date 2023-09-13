package ru.nsu.ccfit.zuev.osu.menu;

import android.database.Cursor;
import com.reco1l.legacy.ui.multiplayer.Multiplayer;
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
import ru.nsu.ccfit.zuev.osu.*;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

public class ScoreBoard extends Entity implements ScrollDetector.IScrollDetectorListener {
    private final Scene mainScene;
    private final MenuItemListener listener;
    private final ChangeableText loadingText;
    private float percentShow = -1;
    private boolean showOnlineScores = false;
    private boolean wasOnline = false;
    private boolean isScroll = false;

    private final SurfaceScrollDetector mScrollDetector;

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
    private ScoreBoardItem[] scoreItems = new ScoreBoardItem[0];


    private LoadTask currentTask;

    private final ExecutorService loadExecutor = Executors.newSingleThreadExecutor();


    public ScoreBoard(final Scene scene, final Entity layer, final MenuItemListener listener) {
        this.mainScene = scene;
        layer.attachChild(this);

        this.loadingText = new ChangeableText(5, 230, ResourceManager.getInstance().getFont("strokeFont"), "", 50);
        this.attachChild(this.loadingText);

        this.listener = listener;
        this.mScrollDetector = new SurfaceScrollDetector(this);
    }

    public static String convertModString(String s) {
        String[] mods = s.split("\\|", 2);
        StringBuilder sb = new StringBuilder();

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
            sb.append(convertExtraModString(mods[1]));
        }

        if (sb.length() == 0) {
            return "None";
        }

        return sb.substring(0, sb.length() - 1);
    }

    private static String convertExtraModString(String s) {
        StringBuilder sb = new StringBuilder();
        for (String str : s.split("\\|")) {
            if (str.startsWith("x") && str.length() == 5) {
                sb.append(str.substring(1)).append("x,");
            } else if (str.startsWith("AR")) {
                sb.append(str).append(",");
            }
        }
        return sb.toString();
    }

    private String formatScore(int score) {
        StringBuilder sb = new StringBuilder();
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
                    
                    if (isActive()) 
                        loadingText.setText("Cannot load scores");
                    return;
                }

                if (!isActive())
                    return;

                loadingText.setText(OnlineManager.getInstance().getFailMessage());

                var items = new ScoreBoardItem[scores.size()];
                scoreItems = items;

                long lastTotalScore = 0;

                for (int i = scores.size() - 1; i >= 0 && isActive(); --i) {
                    Debug.i(scores.get(i));

                    String[] data = scores.get(i).split("\\s+");

                    if (data.length < 8 || data.length == 10) {
                        continue;
                    }

                    final int scoreID = Integer.parseInt(data[0]);
                    final String totalScore = formatScore(Integer.parseInt(data[2]));
                    final long currTotalScore = Long.parseLong(data[2]);

                    final String titleStr = "#"
                            + (i + 1)
                            + " "
                            + data[1] + "\n"
                            + StringTable.format(R.string.menu_score, totalScore, Integer.parseInt(data[3]));

                    final long diffTotalScore = currTotalScore - lastTotalScore;

                    final String accStr = convertModString(data[5]) + "\n" + String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(Integer.parseInt(data[6]) / 1000f, 2)) + "%" + "\n"
                            + (lastTotalScore == 0 ? "-" : ((diffTotalScore != 0 ? "+" : "") + diffTotalScore));
                    lastTotalScore = currTotalScore;

                    if (!isActive())
                        return;

                    attachChild(new ScoreItem(avatarExecutor, titleStr, accStr, data[4], true, scoreID, data[7], data[1]), 0);

                    ScoreBoardItem item = new ScoreBoardItem();
                    item.set(data[1], Integer.parseInt(data[3]), Integer.parseInt(data[2]), scoreID);
                    items[i] = item;
                }

                if (!scores.isEmpty()) {
                    String[] data = scores.get(scores.size() - 1).split("\\s+");

                    if (data.length == 10) {
                        final int scoreID = Integer.parseInt(data[0]);
                        final String totalScore = formatScore(Integer.parseInt(data[2]));
                        final String titleStr = "#"
                                + data[7]
                                + " of "
                                + "\n"
                                + StringTable.format(R.string.menu_score, totalScore, Integer.parseInt(data[3]));
                        final String accStr = convertModString(data[5]) + "\n"
                                + String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(Integer.parseInt(data[6]) / 1000f, 2)) + "%" + "\n"
                                + "-";

                        if (isActive())
                            attachChild(new ScoreItem(avatarExecutor, titleStr, accStr, data[4], true, scoreID, data[9], data[1]));
                    }
                }
                percentShow = 0;
            }
        };
        loadExecutor.submit(currentTask);
    }

    private void initFromLocal(TrackInfo track) {

        currentTask = new LoadTask(false) {

            @Override
            public void run() {
                String[] columns = { "id", "playername", "score", "combo", "mark", "accuracy", "mode" };
                try (Cursor scoreSet = ScoreLibrary.getInstance().getMapScores(columns, track.getFilename())) {
                    if (scoreSet == null || scoreSet.getCount() == 0 || !isActive()) {
                        return;
                    }

                    percentShow = 0;
                    scoreSet.moveToLast();
                    long lastTotalScore = 0;

                    var items = new ScoreBoardItem[scoreSet.getCount()];                     
                    scoreItems = items;

                    for (int i = scoreSet.getCount() - 1; i >= 0 && isActive(); --i) {
                        scoreSet.moveToPosition(i);
                        final int scoreID = scoreSet.getInt(0);

                        final String totalScore = formatScore(scoreSet.getInt(scoreSet.getColumnIndexOrThrow("score")));
                        final long currTotalScore = scoreSet.getLong(scoreSet.getColumnIndexOrThrow("score"));
                        final String titleStr = "#"
                                + (i + 1)
                                + " "
                                + scoreSet.getString(scoreSet.getColumnIndexOrThrow("playername"))
                                + "\n"
                                + StringTable.format(R.string.menu_score, totalScore, scoreSet.getInt(scoreSet.getColumnIndexOrThrow("combo")));
                        final long diffTotalScore = currTotalScore - lastTotalScore;
                        final String accStr = convertModString(scoreSet.getString(scoreSet.getColumnIndexOrThrow("mode"))) + "\n"
                                + String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(scoreSet.getFloat(scoreSet.getColumnIndexOrThrow("accuracy")) * 100, 2)) + "%" + "\n"
                                + (lastTotalScore == 0 ? "-" : ((diffTotalScore != 0 ? "+" : "") + diffTotalScore));
                        lastTotalScore = currTotalScore;

                        if (!isActive())
                            return;

                        attachChild(new ScoreItem(avatarExecutor, titleStr, accStr, scoreSet.getString(scoreSet.getColumnIndexOrThrow("mark")), false, scoreID, null, null), 0);

                        var item = new ScoreBoardItem();
                        item.set(scoreSet.getString(scoreSet.getColumnIndexOrThrow("playername")), scoreSet.getInt(scoreSet.getColumnIndexOrThrow("combo")), scoreSet.getInt(scoreSet.getColumnIndexOrThrow("score")), scoreID);
                        items[i] = item;
                    }
                }
            }
        };
        loadExecutor.submit(currentTask);
    }

    public synchronized void init(final TrackInfo track) {

        if (currentTask != null && currentTask.avatarExecutor != null)
            currentTask.avatarExecutor.shutdownNow();

        if (showOnlineScores == wasOnline && wasOnline) {
            return;
        }

        loadingText.setText("");
        wasOnline = showOnlineScores;
        scoreItems = new ScoreBoardItem[0];

        SyncTaskManager.getInstance().run(() -> {

            detachChildren();

            if (track == null)
                return;

            if (OnlineManager.getInstance().isStayOnline() && showOnlineScores) {
                initFromOnline(track);
                return;
            }
            initFromLocal(track);
        });
    }

    public void update(final float pSecondsElapsed) {
        secPassed += pSecondsElapsed;
        if (getChildCount() == 0) {
            return;
        }

        if (percentShow == -1) {
            float y = -camY;

            var count = getChildCount();
            for (int i = 0; i < count; i++)
            {
                var child = getChild(i);

                // This checks nullability in case the loading was stopped, this iteration also needs to be stopped.
                if (!(child instanceof Sprite))
                    return;

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
            for (int i = 0; i < count; i++)
            {
                var child = getChild(i);

                // This checks nullability in case the loading was stopped, this iteration also needs to be stopped.
                if (!(child instanceof Sprite))
                    return;

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
    public void onScroll(ScrollDetector pScollDetector, TouchEvent pTouchEvent, float pDistanceX, float pDistanceY) {
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
                    } else if (camY >= maxY){
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

    public ScoreBoardItem[] getScoreBoardItems() {
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


        private float dx = 0, dy = 0;

        private TextureRegion avatarTexture;

        private Runnable avatarTask;

        private final ExecutorService avatarExecutor;

        private final String avaURL;

        private final String username;

        private final int scoreID;

        private final boolean showOnline;
        
        private final boolean shouldLoadAvatar;


        private ScoreItem(ExecutorService avatarExecutor, String title, String acc, String markStr, boolean showOnline, int scoreID, String avaURL, String username) {
            super(0f, 0f,  ResourceManager.getInstance().getTexture("menu-button-background").deepCopy());
            this.avatarExecutor = avatarExecutor;
            this.showOnline = showOnline;
            this.username = username;
            this.scoreID = scoreID;
            this.avaURL = avaURL;

            setHeight(107);
            setWidth(724);
            camY = -146;

            setColor(0, 0, 0);
            setAlpha(0.5f);
            setScale(0.65f);
            setWidth(getWidth() * 1.1f);

            shouldLoadAvatar = showOnlineScores
                    && Config.getLoadAvatar()
                    && avaURL != null
                    && avatarExecutor != null;

            int pos = shouldLoadAvatar ? 90 : 0;

            var text = new Text(pos + 160, 20, ResourceManager.getInstance().getFont("font"), title);
            var accText = new Text(670, 12, ResourceManager.getInstance().getFont("smallFont"), acc);
            var mark = new Sprite(pos + 80, 35, ResourceManager.getInstance().getTexture("ranking-" + markStr + "-small"));

            text.setScale(1.2f);
            mark.setScale(1.5f);
            mark.setPosition(pos + mark.getWidth() / 2 + 60, mark.getY());
            attachChild(text);
            attachChild(accText);
            attachChild(mark);

            mainScene.registerTouchArea(this);
            height = getHeight();
        }

        @Override
        public void onDetached()
        {
            if (avatarTexture != null)
                ResourceManager.getInstance().unloadTexture(avatarTexture);
        }

        @Override
        protected void onManagedUpdate(float pSecondsElapsed)
        {
            // This is to avoid loading avatars when the scene was changed (game started or user gone back to main menu)
            // this method is called by the parent scene only if it's showing.
            if (shouldLoadAvatar && avatarTask == null) {
                avatarTask = () -> {
                    // Means the entity was detached.
                    if (getParent() == null)
                        return;

                    var texture = ResourceManager.getInstance().getTexture("emptyavatar");

                    if (!avatarExecutor.isShutdown() && OnlineManager.getInstance().loadAvatarToTextureManager(avaURL)) {
                        avatarTexture = ResourceManager.getInstance().getAvatarTextureIfLoaded(avaURL);

                        if (avatarTexture != null)
                            texture = avatarTexture;
                    }

                    var child = new Sprite(55, 12, 90, 90, texture);
                    attachChild(child);
                };

                try {
                    avatarExecutor.submit(avatarTask);
                } catch (RejectedExecutionException e) {
                    // Nothing, means the executor was shutdown.
                }
            }

            super.onManagedUpdate(pSecondsElapsed);
        }

        @Override
        public boolean onAreaTouched(TouchEvent event, float localX, float localY) {
            mScrollDetector.onTouchEvent(event);
            mScrollDetector.setEnabled(true);

            if (event.isActionDown()) {
                moved = false;
                setAlpha(0.8f);
                listener.stopScroll(getY() + localY);
                dx = localX; dy = localY;
                downTime = 0;
                _scoreID = scoreID;
                return true;
            } else if (event.isActionUp() && !moved && !isScroll) {
                downTime = -1;
                setAlpha(0.5f);

                if (Multiplayer.isMultiplayer)
                    return true;

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

