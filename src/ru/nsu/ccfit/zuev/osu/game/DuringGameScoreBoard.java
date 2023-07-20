package ru.nsu.ccfit.zuev.osu.game;

import android.opengl.GLES20;

import com.reco1l.api.ibancho.data.RoomTeam;
import com.reco1l.api.ibancho.data.TeamMode;
import com.reco1l.framework.lang.Execution;
import com.reco1l.legacy.ui.multiplayer.Multiplayer;
import com.reco1l.legacy.ui.multiplayer.RoomScene;
import kotlinx.coroutines.Job;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.ChangeableText;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.opengl.texture.region.TextureRegion;

import java.text.NumberFormat;
import java.util.Locale;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.async.AsyncTask;
import ru.nsu.ccfit.zuev.osu.menu.ScoreBoard;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class DuringGameScoreBoard extends GameObject {
    private final Scene scene;
    private final StatisticV2 stat;
    private final String isNotMe;
    private Sprite[] boards;
    private ChangeableText[] ranks;
    private ScoreBoard.ScoreBoardItems[] scoreBoardData;
    private int posNow;
    private String currentUsername;
    private ChangeableText playerRank;
    private ChangeableText playerText;
    private Sprite playerSprite;
    private float itemHeight;
    private long lastRankChange;
    private Job initTask;

    private final float paddingTop = 15;
    private final float paddingLeft = 10;

    public DuringGameScoreBoard(final Scene scene, final StatisticV2 stat, String isNotMe) {
        this.scene = scene;
        this.stat = stat;
        this.isNotMe = isNotMe;

        initScoreboard();
    }

    @Override
    public void update(float dt) {

        // Updating color animation at the update thread
        if (Multiplayer.isConnected && playerSprite != null)
        {
            float passedAfterChange = (System.currentTimeMillis() - lastRankChange) * 0.001f;

            playerSprite.setColor(passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f,
                                  passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f, 1,
                                  passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f);
        }

        if (initTask == null || !initTask.isCompleted() || Multiplayer.isConnected) {
            return;
        }

        // Reinitialize scoreboard if data changes.
        // This should only be done if the scoreboard data comes from an online source.
        final ScoreBoard.ScoreBoardItems[] items = GlobalManager.getInstance().getSongMenu().getBoard();
        int replayID = GlobalManager.getInstance().getScoring().getReplayID();
        if (replayID == -1 && scoreBoardData.length != items.length) {
            initScoreboard();
            return;
        }
        updateInternal();
    }

    private void updateInternal() {

        if (!Multiplayer.isConnected)
        {
            float passedAfterChange = (System.currentTimeMillis() - lastRankChange) * 0.001f;

            playerSprite.setColor(passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f,
                                  passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f, 1,
                                  passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f);
            int score = stat.getAutoTotalScore();
            playerText.setText(currentUsername + "\n" +
                                       NumberFormat.getNumberInstance(Locale.US).format(score) + "\n" +
                                       NumberFormat.getNumberInstance(Locale.US).format(stat.getMaxCombo()) + "x");
            playerText.setScaleCenter(0, 0);
            playerText.setScale(0.65f);

            for (int i = posNow - 1; i >= 0; i--) {
                if (score > scoreBoardData[i].playScore) {
                    posNow = i;
                    ranks[i].setText("#" + (i + 2));
                    ranks[i].setPosition(100 - ranks[i].getWidth(), paddingTop * 2);
                    playerRank.setText("#" + (i + 1));
                    playerRank.setPosition(100 - playerRank.getWidth(), paddingTop * 2);
                    lastRankChange = System.currentTimeMillis();
                    if (i > 2) {
                        boards[i].setVisible(false);
                        boards[i - 2].setVisible(true);
                    }
                    playerSprite.setColor(1, 1, 1);
                    playerSprite.setAlpha(1);
                }
            }
        }
        if (boards.length > 4 && posNow > 3) {
            playerSprite.setPosition(0, Config.getRES_HEIGHT() / 2f);
            boards[posNow - 1].setPosition(0, Config.getRES_HEIGHT() / 2f - itemHeight * 1);
            boards[posNow - 2].setPosition(0, Config.getRES_HEIGHT() / 2f - itemHeight * 2);
            boards[0].setPosition(0, Config.getRES_HEIGHT() / 2f - itemHeight * 3);
        } else {
            for (int i = 0; i < 4; i++) {
                if (boards.length <= i) break;
                boards[i].setPosition(0,
                        Config.getRES_HEIGHT() / 2f + itemHeight * (i - (posNow <= i ? 2 : 3)));
            }
            playerSprite.setPosition(0, Config.getRES_HEIGHT() / 2f + itemHeight * (posNow - 3));
        }
    }

    public void initScoreboard() {
        if (initTask != null) {
            initTask.cancel(null);
        }

        initTask = Execution.async(() -> {
            ScoreBoard.ScoreBoardItems[] items;

            if (Multiplayer.isConnected)
                items = Multiplayer.getLiveData();
            else
                items = GlobalManager.getInstance().getSongMenu().getBoard();

            if (items == null || items.length == 0)
                return null;

            int replayID = Multiplayer.isConnected ? -1 : GlobalManager.getInstance().getScoring().getReplayID();
            if (replayID == -1) {
                scoreBoardData = items;
            } else {
                int replayIndex = -1;
                scoreBoardData = new ScoreBoard.ScoreBoardItems[items.length - 1];
                for (int i = 0; i < items.length; i++) {
                    if (replayID == items[i].scoreId) {
                        replayIndex = i;
                        continue;
                    }
                    scoreBoardData[i - (replayIndex != -1 ? 1 : 0)] = items[i];
                }
            }

            currentUsername = isNotMe != null ? isNotMe :
                    OnlineScoring.getInstance().getPanel() == null ?
                            Config.getLocalUsername() :
                            Config.getOnlineUsername();

            if (Multiplayer.isConnected)
            {
                assert RoomScene.getPlayer() != null;
                assert RoomScene.getRoom() != null;

                if (RoomScene.getRoom().getTeamMode() == TeamMode.TEAM_VS_TEAM)
                {
                    if (RoomScene.getPlayer().getTeam() == RoomTeam.RED)
                        currentUsername = "Red Team";
                    else
                        currentUsername = "Blue Team";
                }

                int i = scoreBoardData.length - 1;
                while (i >= 0)
                {
                    if (scoreBoardData[i].userName.equals(currentUsername))
                    {
                        if (posNow != i)
                            lastRankChange = System.currentTimeMillis();
                        posNow = i;
                        break;
                    }
                    --i;
                }
            }
            else posNow = scoreBoardData.length;

            TextureRegion tex = ResourceManager.getInstance().getTexture("menu-button-background").deepCopy();
            tex.setHeight(90);
            tex.setWidth(130);

            final var oldBoard = boards;
            Execution.glThread(() -> {
                if (oldBoard != null) {
                    for (Sprite board : oldBoard) {
                        board.detachSelf();
                    }
                }
                return null;
            });
            boards = new Sprite[Multiplayer.isConnected ? scoreBoardData.length : scoreBoardData.length + 1];
            ranks = new ChangeableText[scoreBoardData.length];
            for (int i = 0; i < scoreBoardData.length; i++) {

                if (Multiplayer.isConnected && i == posNow)
                    continue;

                Sprite s = new Sprite(0, 0, tex);
                s.setAlpha(0.5f);
                s.setColor(scoreBoardData[i].userName.equals(currentUsername) && !currentUsername.equals("osu!") ? 1 : 0.5f, 0.5f, 0.5f);
                final Text info = new Text(paddingLeft, paddingTop,
                        ResourceManager.getInstance().getFont("font"), scoreBoardData[i].get());
                info.setScaleCenter(0, 0);
                info.setScale(0.65f);
                info.setColor(0.85f, 0.85f, 0.9f);
                ranks[i] = new ChangeableText(paddingLeft, paddingTop,
                        ResourceManager.getInstance().getFont("CaptionFont"), "#1", 5);
                ranks[i].setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                ranks[i].setScaleCenter(0, 0);
                ranks[i].setScale(1.7f);
                ranks[i].setColor(0.6f, 0.6f, 0.6f, 0.9f);
                ranks[i].setText("#" + (i + 1));
                ranks[i].setPosition(100 - ranks[i].getWidth(), paddingTop * 2);
                s.attachChild(ranks[i]);
                s.attachChild(info);
                s.setVisible(i == 0 || scoreBoardData.length - i < 3);
                boards[i] = s;
                scene.attachChild(s);
            }
            playerSprite = new Sprite(0, 0, tex);
            playerSprite.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            playerSprite.setAlpha(0.5f);
            playerSprite.setColor(0.5f, 0.5f, 1);
            playerText = new ChangeableText(paddingLeft, paddingTop,
                    ResourceManager.getInstance().getFont("font"),
                    currentUsername + "\n0\n0x", 100);
            playerText.setScaleCenter(0, 0);
            playerText.setScale(0.65f);
            playerText.setColor(0.85f, 0.85f, 0.9f);
            playerRank = new ChangeableText(paddingLeft, paddingTop,
                    ResourceManager.getInstance().getFont("CaptionFont"),
                    "#1", 5);
            playerRank.setBlendFunction(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
            playerRank.setScaleCenter(0, 0);
            playerRank.setScale(1.7f);
            playerRank.setColor(0.6f, 0.6f, 0.6f, 0.9f);
            playerRank.setText("#" + (!GlobalManager.getInstance().getSongMenu().isBoardOnline() || posNow < (replayID == -1 ? 20 : 19) ?
                    String.valueOf(posNow + 1) : "?"));
            playerRank.setPosition(100 - playerRank.getWidth(), paddingTop * 2);
            playerSprite.attachChild(playerRank);
            playerSprite.attachChild(playerText);
            itemHeight = 83;
            boards[posNow] = playerSprite;
            scene.attachChild(playerSprite);

            if (Multiplayer.isConnected)
            {
                playerRank.setText("#" + (posNow + 1));
                playerRank.setPosition(100 - playerRank.getWidth(), paddingTop * 2);
                playerText.setText(scoreBoardData[posNow].get());
                playerText.setScaleCenter(0, 0);
                playerText.setScale(0.65f);
                updateInternal();
                return null;
            }
            lastRankChange = System.currentTimeMillis();
            return null;
        });
    }
}
