package ru.nsu.ccfit.zuev.osu.menu;

import android.content.Context;
import android.database.Cursor;
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
import ru.nsu.ccfit.zuev.osu.async.AsyncTask;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

import java.io.File;
import java.text.NumberFormat;
import java.util.*;

public class ScoreBoard implements ScrollDetector.IScrollDetectorListener {
    private final Scene scene;
    private final Scene mainScene;
    private final MenuItemListener listener;
    private final ChangeableText loadingText;
    private final List<Sprite> sprites;
    private final List<ScoreBoard.Avatar> avatars;
    private float percentShow = -1;
    private long viewNumber = 0;
    private boolean showOnlineScores = false;
    private TrackInfo lastTrack;
    private boolean wasOnline = false;
    private boolean isCanceled = false;
    private boolean isScroll = false;

    private AsyncTask onlineTask;
    private AsyncTask avatarTask;
    private final Context context;
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
    private ScoreBoard.ScoreBoardItems[] scoreItems = new ScoreBoard.ScoreBoardItems[0];

    private static final Object mutex = new Object();

    public ScoreBoard(final Scene scene, final Entity layer, final MenuItemListener listener, final Context context) {
        this.scene = new Scene();
        this.mainScene = scene;
        this.context = context;
        this.scene.setBackgroundEnabled(false);
        layer.attachChild(this.scene);

        this.loadingText = new ChangeableText(Utils.toRes(5), Utils.toRes(230), ResourceManager.getInstance().getFont("strokeFont"), "", 50);
        this.scene.attachChild(this.loadingText);

        this.sprites = Collections.synchronizedList(new ArrayList<>(50));
        this.avatars = Collections.synchronizedList(new ArrayList<>(50));

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

    private synchronized void initSprite(String title, String acc, String markStr, final  boolean showOnline, final int scoreID, String avaURL, final String username) {
        final TextureRegion tex = ResourceManager.getInstance().getTexture(
                "menu-button-background").deepCopy();
        tex.setHeight(107);
        tex.setWidth(724);
        camY = -146;

        Sprite sprite = new Sprite(Utils.toRes(-150), Utils.toRes(40), tex) {
            private float dx = 0, dy = 0;

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX,
                                         final float pTouchAreaLocalY) {
                mScrollDetector.onTouchEvent(pSceneTouchEvent);
                mScrollDetector.setEnabled(true);

                if (pSceneTouchEvent.isActionDown()) {
                    moved = false;
                    this.setAlpha(0.8f);
                    listener.stopScroll(getY() + pTouchAreaLocalY);
                    dx = pTouchAreaLocalX; dy = pTouchAreaLocalY;
                    downTime = 0;
                    _scoreID = scoreID;
                    return true;
                } else if (pSceneTouchEvent.isActionUp() && !moved && !isScroll) {
                    downTime = -1;
                    this.setAlpha(0.5f);
                    listener.openScore(scoreID, showOnline, username);
                    GlobalManager.getInstance().getScoring().setReplayID(scoreID);
                    return true;
                } else if (pSceneTouchEvent.isActionOutside() || pSceneTouchEvent.isActionMove() && MathUtils.distance(dx, dy, pTouchAreaLocalX, pTouchAreaLocalY) > 10) {
                    downTime = -1;
                    this.setAlpha(0.5f);
                    moved = true;
                }
                return false;
            }
        };
        sprite.setColor(0, 0, 0);
        sprite.setAlpha(0.5f);
        sprite.setScale(0.65f);
        sprite.setWidth(sprite.getWidth() * 1.1f);


        int pos = 0;
        if (showOnlineScores) {
            pos = 90;
            avatars.add(new ScoreBoard.Avatar(username, avaURL));
        }

        final Text text = new Text(Utils.toRes(pos + 160), Utils.toRes(20),
                ResourceManager.getInstance().getFont("font"), title);
        final Text accText = new Text(Utils.toRes(670), Utils.toRes(12),
                ResourceManager.getInstance().getFont("smallFont"), acc);
        final Sprite mark = new Sprite(Utils.toRes(pos + 80), Utils.toRes(35),
                ResourceManager.getInstance().getTexture(
                        "ranking-" + markStr + "-small"));

        text.setScale(1.2f);
        mark.setScale(1.5f);
        mark.setPosition(pos + mark.getWidth() / 2 + 60, mark.getY());
        sprite.attachChild(text);
        sprite.attachChild(accText);
        sprite.attachChild(mark);

        scene.attachChild(sprite);
        mainScene.registerTouchArea(sprite);

        height = sprite.getHeight();
        sprites.add(sprite);
    }

    private void initFromOnline(final TrackInfo track) {
        final long currentNumber = viewNumber;
        loadingText.setText("Loading scores...");
        this.onlineTask = new AsyncTask() {
            @Override
            public void run() {
                File trackFile = new File(track.getFilename());
                String hash = FileUtils.getMD5Checksum(trackFile);
                List<String> scores;

                try {
                    scores = OnlineManager.getInstance().getTop(trackFile, hash);
                } catch (OnlineManager.OnlineManagerException e) {
                    Debug.e("Cannot load scores " + e.getMessage());
                    synchronized (mutex) {
                        if (currentNumber == viewNumber) {
                            loadingText.setText("Cannot load scores");
                        }
                    }
                    return;
                }

                synchronized (mutex) {
                    if (currentNumber != viewNumber) {
                        return;
                    }

                    loadingText.setText(OnlineManager.getInstance().getFailMessage());

//                    sprites = Collections.synchronizedList(new ArrayList<>());
//                    avatars = Collections.synchronizedList(new ArrayList<>());

                    List<ScoreBoard.ScoreBoardItems> scoreBoardItems = new ArrayList<>();
                    long lastTotalScore = 0;

                    for (int i = 0; i < scores.size(); i++) {
                        Debug.i(scores.get(i));

                        String[] data = scores.get(i).split(",");

                        if(data.length < 8 || data.length == 10) {
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

                        initSprite(titleStr, accStr, data[4], true, scoreID, data[7], data[1]);

                        ScoreBoard.ScoreBoardItems items = new ScoreBoard.ScoreBoardItems();
                        items.set(data[1], Integer.parseInt(data[3]), Integer.parseInt(data[2]), scoreID);
                        scoreBoardItems.add(items);
                    }
                    scoreItems = scoreBoardItems.toArray(new ScoreBoard.ScoreBoardItems[0]);

                    if (scores.size() > 0) {
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

                            initSprite(titleStr, accStr, data[4], true, scoreID, data[9], data[1]);
                        }
                    }

                    percentShow = 0;
                }
            }

            @Override
            public void onComplete() {
                isCanceled = false;
                if (Utils.isWifi(context) || Config.getLoadAvatar()) {
                    loadAvatar();
                }
            }
        };
        onlineTask.execute();
    }

    public void init(final TrackInfo track) {
        if (lastTrack == track && showOnlineScores == wasOnline && wasOnline) {
            return;
        }

        scoreItems = new ScoreBoard.ScoreBoardItems[0];
        lastTrack = track;
        wasOnline = showOnlineScores;

        synchronized (mutex) {
            viewNumber++;
        }

        clear();
        if (track == null) {
            return;
        }

        loadingText.setText("");
        if (OnlineManager.getInstance().isStayOnline() && showOnlineScores) {
            initFromOnline(track);
            return;
        }

        String[] columns = { "id", "playername", "score", "combo", "mark", "accuracy", "mode" };
        try (Cursor scoreSet = ScoreLibrary.getInstance().getMapScores(columns, track.getFilename())) {
            if (scoreSet == null || scoreSet.getCount() == 0) {
                return;
            }

            percentShow = 0;
            scoreSet.moveToFirst();
            long lastTotalScore = 0;
            scoreItems = new ScoreBoard.ScoreBoardItems[scoreSet.getCount()];
            for (int i = 0; i < scoreSet.getCount(); i++) {
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

                initSprite(titleStr, accStr, scoreSet.getString(scoreSet.getColumnIndexOrThrow("mark")), false, scoreID, null, null);

                scoreItems[i] = new ScoreBoard.ScoreBoardItems();
                scoreItems[i].set(scoreSet.getString(scoreSet.getColumnIndexOrThrow("playername")), scoreSet.getInt(scoreSet.getColumnIndexOrThrow("combo")), scoreSet.getInt(scoreSet.getColumnIndexOrThrow("score")), scoreID);
            }
        }
    }

    public void clear() {
        if (sprites == null) {
            return;
        }
        synchronized (sprites) {
            final Sprite[] spritesArray = sprites.toArray(new Sprite[0]);
            sprites.clear();
            SyncTaskManager.getInstance().run(() -> {
                for (final Sprite sprite : spritesArray) {
                    if (sprite != null) {
                        mainScene.unregisterTouchArea(sprite);
                        sprite.detachSelf();
                    }
                }
            });
        }
    }

    public void update(final float pSecondsElapsed) {
        secPassed += pSecondsElapsed;
        if (sprites.isEmpty()) {
            return;
        }

        if (percentShow == -1) {
            float y = -camY;
            synchronized (sprites) {
                for (final Sprite sprite : sprites) {
                    if (sprite == null) {
                        break;
                    }
                    sprite.setPosition(sprite.getX(), y);
                    y += 0.8f * (sprite.getHeight() - Utils.toRes(32));
                }
            }

            y += camY;
            camY += velocityY * pSecondsElapsed;
            maxY = y - 0.8f * (Config.getRES_HEIGHT() - 110 - (height - Utils.toRes(32)));

            if (camY <= -146 && velocityY < 0 || camY > maxY && velocityY > 0) {
                camY -= velocityY * pSecondsElapsed;
                velocityY = 0;
                isScroll = false;
            }

            if (Math.abs(velocityY) > Utils.toRes(500) * pSecondsElapsed) {
                velocityY -= Utils.toRes(10) * pSecondsElapsed * Math.signum(velocityY);
            } else {
                velocityY = 0;
                isScroll = false;
            }
        } else {
            percentShow += pSecondsElapsed * 4;
            if (percentShow > 1) {
                percentShow = 1;
            }

            synchronized (sprites) {
                for (int i = 0; i < sprites.size(); i++) {
                    sprites.get(i).setPosition(Utils.toRes(-160), Utils.toRes(146) + 0.8f * percentShow * i * (sprites.get(i).getHeight() - Utils.toRes(32)));
                }
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
            if (_scoreID != -1 && !showOnlineScores) {
                GlobalManager.getInstance().getSongMenu().showDeleteScoreMenu(_scoreID);
            }
            downTime = -1;
        }
    }

    public void loadAvatar() {
        this.avatarTask = new AsyncTask() {
            private final List<TextureRegion> avatarTextureRegions = new ArrayList<>();

            @Override
            public void run() {
                try {
                    synchronized (avatars) {
                        for (ScoreBoard.Avatar avatar : avatars) {
                            if (isCanceled) {
                                break;
                            }

                            String avaName;
                            if (OnlineManager.getInstance().loadAvatarToTextureManager(avatar.getAvaUrl(), avaName = "ava@" + avatar.getUserName())) {
                                avatarTextureRegions.add(ResourceManager.getInstance().getTextureIfLoaded(avaName));
                            } else {
                                avatarTextureRegions.add(ResourceManager.getInstance().getTexture("emptyavatar"));
                            }
                        }
                    }
                } catch (Exception e) {
                    isCanceled = false;
                }
            }

            @Override
            public void onComplete() {
                try {
                    if (showOnlineScores) {
                        for (int i = 0; i < avatarTextureRegions.size(); i++) {
                            synchronized (sprites) {
                                sprites.get(i).attachChild(new Sprite(55, 12, Utils.toRes(90), Utils.toRes(90), avatarTextureRegions.get(i)));
                            }
                        }
                    }
                } catch (Exception e) {
                    Debug.e(e.getMessage());
                }
            }
        };
        this.avatarTask.execute();
    }

    public void cancelLoadOnlineScores() {
        if (this.onlineTask != null) {
            this.onlineTask.cancel(true);
        }
    }

    public void cancelLoadAvatar() {
        if (this.avatarTask != null) {
            isCanceled = true;
            this.avatarTask.cancel(true);
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
                if (pointerId == 1 || pointerId == pTouchEvent.getPointerID()) {
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
        synchronized (mutex) {
            this.showOnlineScores = showOnlineScores;
        }
    }

    public ScoreBoardItems[] getScoreBoardItems() {
        return scoreItems;
    }

    public Scene getScene() {
        return scene;
    }

    public static class ScoreBoardItems {
        public String userName;
        public int playScore;
        public int scoreId;
        private int maxCombo;

        public void set(String name, int com, int scr, int id) {
            userName = name;
            maxCombo = com;
            playScore = scr;
            scoreId = id;
        }

        public String get() {
            return userName + "\n" + NumberFormat.getNumberInstance(Locale.US).format(playScore) + "\n" + NumberFormat.getNumberInstance(Locale.US).format(maxCombo) + "x";
        }
    }

    static class Avatar {
        private String userName;
        private String avaUrl;

        public Avatar(String userName, String avaUrl) {
            this.userName = userName;
            this.avaUrl = "https://" + OnlineManager.hostname + "/user/avatar/?s=100&id=" + avaUrl;
        }

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getAvaUrl() {
            return avaUrl;
        }

        public void setAvaUrl(String avaUrl) {
            this.avaUrl = avaUrl;
        }
    }
}

