package ru.nsu.ccfit.zuev.osu.menu;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;

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

import java.io.File;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.GameHelper;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
import ru.nsu.ccfit.zuev.osuplus.R;

public class ScoreBoard implements ScrollDetector.IScrollDetectorListener {
    private final Scene scene;
    private final Scene mainScene;
    private final MenuItemListener listener;
    private final ChangeableText loadingText;
    private Sprite[] sprites = null;
    private Avatar[] avatars = null;
    private float percentShow = -1;

    private final Boolean trackMutex = false;
    private long viewNumber = 0;

    private boolean showOnlineScores = false;
    private TrackInfo lastTrack = null;
    private boolean wasOnline = false;
    private boolean isCanceled = false;
    private boolean isScroll = false;

    private AsyncTask<OsuAsyncCallback, Integer, Boolean> onlineTask;
    private final LinkedList<AsyncTask<OsuAsyncCallback, Integer, Boolean>> avatarTasks;
    private final Context context;

    private final SurfaceScrollDetector mScrollDetector;

    private float maxY = 100500;
    private int pointerId = -1;
    private float initalY = -1;
    private Float touchY = null;
    private float camY = -146;
    private float velocityY;
    private float secPassed = 0, tapTime;
    private float height = 0;

    private float downTime = -1;
    private int _scoreID = -1;
    private boolean moved = false;
    private ScoreBoardItems[] scoreItems = new ScoreBoardItems[0];

    public ScoreBoard(final Scene scene, final Entity layer, final MenuItemListener listener, final Context context) {
        this.scene = new Scene();
        this.mainScene = scene;
        this.context = context;
        this.scene.setBackgroundEnabled(false);
        layer.attachChild(this.scene);

        loadingText = new ChangeableText(Utils.toRes(5), Utils.toRes(230),
                ResourceManager.getInstance().getFont("strokeFont"), "", 50);
        this.scene.attachChild(loadingText);

        this.listener = listener;
        this.mScrollDetector = new SurfaceScrollDetector(this);
        avatarTasks = new LinkedList<>();
    }

    public static String ConvertModString(String s) {
        String[] strMod = s.split("\\|", 2);
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < strMod[0].length(); i++) {
            switch (strMod[0].charAt(i)) {
                case 'a':
                    result.append("Auto,");
                    break;
                case 'x':
                    result.append("Relax,");
                    break;
                case 'p':
                    result.append("AP,");
                    break;
                case 'e':
                    result.append("EZ,");
                    break;
                case 'n':
                    result.append("NF,");
                    break;
                case 'r':
                    result.append("HR,");
                    break;
                case 'h':
                    result.append("HD,");
                    break;
                case 'i':
                    result.append("FL,");
                    break;
                case 'd':
                    result.append("DT,");
                    break;
                case 'c':
                    result.append("NC,");
                    break;
                case 't':
                    result.append("HT,");
                    break;
                case 's':
                    result.append("PR,");
                    break;
                case 'l':
                    result.append("REZ,");
                    break;
                case 'm':
                    result.append("SC,");
                    break;
                case 'u':
                    result.append("SD,");
                    break;
                case 'f':
                    result.append("PF,");
                    break;
                case 'b':
                    result.append("SU,");
                    break;
                case 'v':
                    result.append("ScoreV2,");
                    break;
            }
        }

        if (strMod.length > 1){
            result.append(ConvertExtraModString(strMod[1]));
        }
        if (result.length() == 0) {
            return "None";
        }

        return result.substring(0, result.length() - 1);
    }

    private static String ConvertExtraModString(String s) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str: s.split("\\|")){
            if (str.startsWith("x") && str.length() == 5){
                stringBuilder.append(str.substring(1)).append("x,");
            } else if (str.startsWith("AR")){
                stringBuilder.append(str).append(",");
            }
        }
        return stringBuilder.toString();
    }

    public ScoreBoardItems[] getScoreBoardItems() {
        return scoreItems;
    }

    private String formatScore(int score) {
        StringBuilder scoreBuilder = new StringBuilder();
        scoreBuilder.append(Math.abs(score));
        for (int i = scoreBuilder.length() - 3; i > 0; i -= 3) {
            scoreBuilder.insert(i, ' ');
        }
        return scoreBuilder.toString();
    }

    private void initSprite(int i, String title, String acc, String markStr, final boolean showOnline, final int scoreID, String avaUrl, final String userName) {
        final TextureRegion tex = ResourceManager.getInstance().getTexture(
                "menu-button-background").deepCopy();
        tex.setHeight(107);
        tex.setWidth(724);
        camY = -146;
        sprites[i] = new Sprite(Utils.toRes(-150), Utils.toRes(40), tex) {
            private float dx = 0, dy = 0;

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX,
                                         final float pTouchAreaLocalY) {
                mScrollDetector.onTouchEvent(pSceneTouchEvent);
                mScrollDetector.setEnabled(true);
                if (pSceneTouchEvent.isActionDown()) {
                    moved = false;
                    setAlpha(0.8f);
                    listener.stopScroll(getY() + pTouchAreaLocalY);
                    dx = pTouchAreaLocalX;
                    dy = pTouchAreaLocalY;
                    downTime = 0;
                    _scoreID = scoreID;
                    return true;
                } else if (pSceneTouchEvent.isActionUp() && !moved && !isScroll) {
                    downTime = -1;
                    setAlpha(0.5f);
                    listener.openScore(scoreID, showOnline, userName);
                    GlobalManager.getInstance().getScoring().setReplayID(scoreID);
                    return true;
                } else if (pSceneTouchEvent.isActionOutside()
                        || pSceneTouchEvent.isActionMove()
                        && (MathUtils.distance(dx, dy, pTouchAreaLocalX,
                        pTouchAreaLocalY) > 50)) {
                    downTime = -1;
                    setAlpha(0.5f);
                    moved = true;
                    return false;
                } else if (pSceneTouchEvent.isActionUp()) {
                    return false;
                }
                return false;
            }
        };

        sprites[i].setColor(0, 0, 0);
        sprites[i].setAlpha(0.5f);
        sprites[i].setScale(0.65f);
        sprites[i].setWidth(sprites[i].getWidth() * 1.1f);
        int pos = 0;
        if (showOnlineScores) {
            pos = 90;
            avatars[i] = new Avatar(userName, avaUrl);
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
        mark.setPosition(pos + 60 + mark.getWidth() / 2, mark.getY());
        sprites[i].attachChild(text);
        sprites[i].attachChild(accText);
        sprites[i].attachChild(mark);
        scene.attachChild(sprites[i]);
        mainScene.registerTouchArea(sprites[i]);
        height = sprites[i].getHeight();
    }

    private void initFromOnline(final TrackInfo track) {
        final long currentNumber = viewNumber;
        loadingText.setText("Loading scores...");
        onlineTask = new AsyncTaskLoader().execute(new OsuAsyncCallback() {


            public void run() {
                File trackFile = new File(track.getFilename());
                String hash = FileUtils.getMD5Checksum(trackFile);
                ArrayList<String> scores;

                try {
                    scores = OnlineManager.getInstance().getTop(trackFile, hash);
                } catch (OnlineManager.OnlineManagerException e) {
                    Debug.e("Cannot load scores " + e.getMessage());
                    synchronized (trackMutex) {
                        if (currentNumber == viewNumber)
                            loadingText.setText(OnlineManager.getInstance().getFailMessage());
                    }
                    return;
                }

                synchronized (trackMutex) {
                    if (currentNumber != viewNumber)
                        return;

                    loadingText.setText(OnlineManager.getInstance().getFailMessage());
                    sprites = new Sprite[scores.size() + 1];
                    avatars = new Avatar[scores.size() + 1];
                    long lastTotalScore = 0;
                    List<ScoreBoardItems> scoreBoardItems = new ArrayList<>();
                    for (int i = scores.size() - 1; i >= 0; i--) {
                        Debug.i(scores.get(i));
                        String[] data = scores.get(i).split("\\s+");
                        if (data.length < 8 || data.length == 10)
                            continue;
                        final int scoreID = Integer.parseInt(data[0]);

                        String totalScore = formatScore(Integer.parseInt(data[2]));
                        long currTotalScore = Long.parseLong(data[2]);
                        String titleStr = "#"
                                + (i + 1)
                                + " "
                                + data[1]
                                + "\n"
                                + StringTable.format(R.string.menu_score,
                                totalScore, Integer.parseInt(data[3]));
                        long diffTotalScore = currTotalScore - lastTotalScore;
                        String accStr = ConvertModString(data[5]) + "\n"
                                + String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(Integer.parseInt(data[6]) / 1000f, 2)) + "%" + "\n"
                                + (lastTotalScore == 0 ? "-" : ((diffTotalScore != 0 ? "+" : "") + diffTotalScore));
                        lastTotalScore = currTotalScore;
                        initSprite(i, titleStr, accStr, data[4], true, scoreID, data[7], data[1]);
                        ScoreBoardItems item = new ScoreBoardItems();
                        item.set(data[1], Integer.parseInt(data[3]), Integer.parseInt(data[2]), scoreID);
                        scoreBoardItems.add(item);
                    }
                    scoreItems = new ScoreBoardItems[scoreBoardItems.size()];
                    for (int i = 0; i < scoreItems.length; i++) {
                        scoreItems[i] = scoreBoardItems.get(scoreBoardItems.size() - 1 - i);
                    }
                    if (scores.size() > 0) {
                        String[] data = scores.get(scores.size() - 1).split("\\s+");
                        if (data.length == 10) {
                            final int scoreID = Integer.parseInt(data[0]);
                            String totalScore = formatScore(Integer.parseInt(data[2]));
                            String titleStr = "#"
                                    + data[7]
                                    + " of "
                                    + data[8]
                                    + "\n"
                                    + StringTable.format(R.string.menu_score,
                                    totalScore, Integer.parseInt(data[3]));
                            String accStr = ConvertModString(data[5]) + "\n"
                                    + String.format(Locale.ENGLISH, "%.2f", GameHelper.Round(Integer.parseInt(data[6]) / 1000f, 2)) + "%" + "\n"
                                    + "-";
                            initSprite(scores.size(), titleStr, accStr, data[4], true, scoreID, data[9], data[1]);
                        } else {
                            sprites[scores.size()] = null;
                        }
                    }

                    percentShow = 0;
                }
            }


            public void onComplete() {
                isCanceled = false;
                if (Utils.isWifi(context) || Config.getLoadAvatar())
                    loadAvatar();
            }

        });
    }

    public void init(final TrackInfo track) {
        if (lastTrack == track && showOnlineScores == wasOnline && wasOnline) {
            return;
        }

        scoreItems = new ScoreBoardItems[0];
        lastTrack = track;
        wasOnline = showOnlineScores;

        synchronized (trackMutex) {
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

        String[] columns = {"id", "playername", "score", "combo", "mark", "accuracy", "mode"};
        Cursor scoresSet = ScoreLibrary.getInstance().getMapScores(columns, track.getFilename());
        if (scoresSet == null || scoresSet.getCount() == 0) {
            if (scoresSet != null) {
                scoresSet.close();
            }

            return;
        }
        percentShow = 0;
        scoresSet.moveToFirst();
        sprites = new Sprite[scoresSet.getCount()];
        long lastTotalScore = 0;
        scoreItems = new ScoreBoardItems[scoresSet.getCount()];
        for (int i = scoresSet.getCount() - 1; i >= 0; i--) {
            scoresSet.moveToPosition(i);
            final int scoreID = scoresSet.getInt(0);

            String totalScore = formatScore(scoresSet.getInt(scoresSet.getColumnIndexOrThrow("score")));
            long currTotalScore = scoresSet.getLong(scoresSet.getColumnIndexOrThrow("score"));
            String titleStr = "#"
                    + (i + 1)
                    + " "
                    + scoresSet.getString(scoresSet.getColumnIndexOrThrow("playername"))
                    + "\n"
                    + StringTable.format(R.string.menu_score,
                    totalScore, scoresSet.getInt(scoresSet.getColumnIndexOrThrow("combo")));
            long diffTotalScore = currTotalScore - lastTotalScore;
            @SuppressLint("DefaultLocale") String accStr = ConvertModString(scoresSet.getString(scoresSet.getColumnIndexOrThrow("mode"))) + "\n"
                    + String.format("%.2f", GameHelper.Round( scoresSet.getFloat(scoresSet.getColumnIndexOrThrow("accuracy")) * 100, 2)) + "%" + "\n"
                    + (lastTotalScore == 0 ? "-" : ((diffTotalScore != 0 ? "+" : "") + diffTotalScore));
            lastTotalScore = currTotalScore;
            initSprite(i, titleStr, accStr, scoresSet.getString(scoresSet.getColumnIndexOrThrow("mark")),
                    false, scoreID, null, null);
            scoreItems[i] = new ScoreBoardItems();
            scoreItems[i].set(scoresSet.getString(scoresSet.getColumnIndexOrThrow("playername")),
                    scoresSet.getInt(scoresSet.getColumnIndexOrThrow("combo")),
                    scoresSet.getInt(scoresSet.getColumnIndexOrThrow("score")),
                    scoreID);
        }
        scoresSet.close();
    }

    public void clear() {
        if (sprites == null) {
            return;
        }
        final Sprite[] sprs = sprites;
        sprites = null;
        SyncTaskManager.getInstance().run(() -> {
            for (final Sprite sp : sprs) {
                if (sp == null) continue;
                mainScene.unregisterTouchArea(sp);
                sp.detachSelf();
            }
        });

    }

    public void update(final float pSecondsElapsed) {
        secPassed += pSecondsElapsed;
        if (sprites == null || sprites.length <= 0) {
            return;
        }
        if (percentShow == -1) {
            float oy = -camY;
            for (final Sprite item : sprites) {
                if (item == null)
                    break;
                item.setPosition(item.getX(), oy);
                oy += (item.getHeight() - Utils.toRes(32))
                        * 0.8f;
            }
            oy += camY;
            camY += velocityY * pSecondsElapsed;
            maxY = oy - (Config.getRES_HEIGHT() - 110 - (height - Utils.toRes(32))
                    * 0.8f);
            if (camY <= -146 && velocityY < 0 || camY >= maxY && velocityY > 0) {
                camY -= velocityY * pSecondsElapsed;
                velocityY = 0;
                isScroll = false;
            }
            if (Math.abs(velocityY) > Utils.toRes(500) * pSecondsElapsed) {
                velocityY -= Utils.toRes(10) * pSecondsElapsed
                        * Math.signum(velocityY);
            } else {
                velocityY = 0;
                isScroll = false;
            }
        } else {
            percentShow += pSecondsElapsed * 4;
            if (percentShow > 1) {
                percentShow = 1;
            }
            for (int i = 0; i < sprites.length; i++) {
                if (sprites[i] != null) {
//                if (i == 5 && showOnlineScores == true) {
//                    sprites[i].setPosition(
//                            Utils.toRes(-160),
//                            Utils.toRes(194) + percentShow * 5
//                                    * (sprites[5].getHeight() - Utils.toRes(32))
//                                    * 0.95f) ;
//                    continue;
//                }
                    sprites[i].setPosition(
                            Utils.toRes(-160),
                            Utils.toRes(146) + percentShow * i
                                    * (sprites[i].getHeight() - Utils.toRes(32))
                                    * 0.8f);
                }
            }
            if (percentShow == 1) {
                percentShow = -1;
            }
        }
        if (downTime >= 0) {
            downTime += pSecondsElapsed;
        }
        if (downTime > 0.5) {
            moved = true;
            if (_scoreID != -1 && !showOnlineScores) {
                GlobalManager.getInstance().getSongMenu().showDeleteScoreMenu(_scoreID);
            }
            downTime = -1;
        }
    }

    public boolean isShowOnlineScores() {
        return showOnlineScores;
    }

    public void setShowOnlineScores(boolean showOnlineScores) {
        synchronized (trackMutex) {
            this.showOnlineScores = showOnlineScores;
        }
    }

    public void loadAvatar() {

        try {
            if (avatars == null) {
                return;
            }
            for (int i = 0; i < avatars.length; i++) {
                if (isCanceled) {
                    isCanceled = false;
                    break;
                }
                if (sprites[i] == null) {
                    continue;
                }
                final int finalI = i;
                AsyncTask<OsuAsyncCallback, Integer, Boolean> avatarTask = new AsyncTaskLoader().execute(new OsuAsyncCallback() {

                    private final TextureRegion[] avatarTexRegion = new TextureRegion[avatars.length];

                    public void run() {

                        Avatar ava = avatars[finalI];
                        boolean bool = OnlineManager.getInstance().loadAvatarToTextureManager(ava.getAvaUrl(), "ava@" + ava.getUserName());
                        if (bool) {
                            avatarTexRegion[finalI] = ResourceManager.getInstance().getTextureIfLoaded("ava@" + ava.getUserName());
                        } else {
                            avatarTexRegion[finalI] = ResourceManager.getInstance().getTexture("emptyavatar");
                        }
                    }

                    public void onComplete() {
                        try {
                            if (avatarTexRegion[finalI] != null && showOnlineScores) {
                                final Sprite avatar = new Sprite(55, 12, Utils.toRes(90), Utils.toRes(90), avatarTexRegion[finalI]);
                                sprites[finalI].attachChild(avatar);
                            }
                        } catch (Exception ignored) {}

                        isCanceled = false;
                    }
                });

                avatarTasks.add(avatarTask);
            }
            isCanceled = false;
        } catch (Exception ex) {
//                        Debug.e(ex.toString());
            isCanceled = false;
        }
    }

    public void cancleLoadOnlineScores() {
        if (onlineTask != null && onlineTask.getStatus() != AsyncTask.Status.FINISHED) {
            onlineTask.cancel(true);
        }
    }

    public void cancleLoadAvatar() {
        if (avatarTasks != null) {
            for (AsyncTask<OsuAsyncCallback, Integer, Boolean> avatarTask : avatarTasks) {
                if (avatarTask.getStatus() != AsyncTask.Status.FINISHED) {
                    isCanceled = true;
                    avatarTask.cancel(true);
                    /* if (OnlineManager.get != null) {
                        OnlineManager.get.abort();
                    } */
                }
            }
        }
    }

    @Override
    public void onScroll(ScrollDetector scrollDetector, TouchEvent touchEvent, float distanceX, float distanceY) {
        switch (touchEvent.getAction()) {
            case (TouchEvent.ACTION_DOWN):
                velocityY = 0;
                touchY = touchEvent.getY();
                pointerId = touchEvent.getPointerID();
                tapTime = secPassed;
                initalY = touchY;
                isScroll = true;
                break;
            case (TouchEvent.ACTION_MOVE):
                if (pointerId != -1 && pointerId != touchEvent.getPointerID()) {
                    break;
                }
                isScroll = true;
                if (initalY == -1) {
                    velocityY = 0;
                    touchY = touchEvent.getY();
                    initalY = touchY;
                    tapTime = secPassed;
                    pointerId = touchEvent.getPointerID();
                }
                final float dy = touchEvent.getY() - touchY;

                camY -= dy;
                touchY = touchEvent.getY();
                if (camY <= -146) {
                    camY = -146;
                    velocityY = 0;
                } else if (camY >= maxY) {
                    camY = maxY;
                    velocityY = 0;
                }

                // velocityY = -3f * dy;
                break;
            default: {
                if (pointerId != -1 && pointerId != touchEvent.getPointerID()) {
                    break;
                }
                touchY = null;
                if (secPassed - tapTime < 0.001f || initalY == -1) {
                    velocityY = 0;
                    isScroll = false;
                } else {
                    velocityY = (initalY - touchEvent.getY())
                            / (secPassed - tapTime);
                    isScroll = true;
                }
                pointerId = -1;
                initalY = -1;

            }
            break;
        }
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

