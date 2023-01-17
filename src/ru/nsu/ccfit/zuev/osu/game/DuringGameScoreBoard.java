package ru.nsu.ccfit.zuev.osu.game;

import android.opengl.GLES20;

import com.reco1l.Game;
import com.reco1l.data.ScoreInfo;
import com.reco1l.UI;

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
import ru.nsu.ccfit.zuev.osu.scoring.StatisticV2;

public class DuringGameScoreBoard extends GameObject {
    private final StatisticV2 stat;
    private Sprite[] boards;
    private ChangeableText[] ranks;
    private ScoreInfo[] scoreBoardDatas;
    private int posNow;
    private String currentUsername;
    private ChangeableText playerRank;
    private ChangeableText playerText;
    private Sprite playerSprite;
    private float itemHeight;
    private long lastRankChange;

    private float paddingTop = 15, paddingLeft = 10;

    public DuringGameScoreBoard(final Scene scene, final StatisticV2 stat, String isNotMe) {
        final ScoreInfo[] items = UI.beatmapPanel.getBoard();
        this.stat = stat;
        int replayid = GlobalManager.getInstance().getScoring().replayID;
        if (replayid == -1) scoreBoardDatas = items;
        else {
            int replayIndex = -1;
            scoreBoardDatas = new ScoreInfo[items.length - 1];
            for (int i = 0; i < items.length; i++) {
                if (replayid == items[i].getId()) {
                    replayIndex = i;
                    continue;
                }
                scoreBoardDatas[i - (replayIndex != -1 ? 1 : 0)] = items[i];
            }
        }
        posNow = scoreBoardDatas.length;
        currentUsername = isNotMe != null ? isNotMe :
                !Game.onlineManager.isStayOnline() ?
                        Config.getLocalUsername() :
                        Config.getOnlineUsername();
        TextureRegion tex = ResourceManager.getInstance().getTexture("menu-button-background").deepCopy();
        tex.setHeight(90);
        tex.setWidth(130);
        boards = new Sprite[scoreBoardDatas.length + 1];
        ranks = new ChangeableText[scoreBoardDatas.length];
        for (int i = 0; i < scoreBoardDatas.length; i++) {
            Sprite s = new Sprite(0, 0, tex);
            s.setAlpha(0.5f);
            s.setColor(scoreBoardDatas[i].getName().equals(currentUsername) && !currentUsername.equals("osu!") ? 1 : 0.5f, 0.5f, 0.5f);
            final Text info = new Text(paddingLeft, paddingTop,
                    ResourceManager.getInstance().getFont("font"), scoreBoardDatas[i].get());
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
            s.setVisible(i == 0 || scoreBoardDatas.length - i < 3);
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
        playerRank.setText("#" + (!UI.beatmapPanel.isOnlineBoard || posNow < (replayid == -1 ? 20 : 19) ?
                String.valueOf(posNow + 1) : "?"));
        playerRank.setPosition(100 - playerRank.getWidth(), paddingTop * 2);
        playerSprite.attachChild(playerRank);
        playerSprite.attachChild(playerText);
        itemHeight = 83;//playerSprite.getHeight();
        boards[posNow] = playerSprite;
        scene.attachChild(playerSprite);
        lastRankChange = System.currentTimeMillis();
    }

    @Override
    public void update(float dt) {
        float passedAfterChange = (System.currentTimeMillis() - lastRankChange) * 0.001f;
        playerSprite.setColor(passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f, passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f, 1, passedAfterChange < 1 ? 1 - 0.5f * passedAfterChange : 0.5f);
        int score = stat.getAutoTotalScore();
        playerText.setText(currentUsername + "\n" +
                NumberFormat.getNumberInstance(Locale.US).format(score) + "\n" +
                NumberFormat.getNumberInstance(Locale.US).format(stat.getMaxCombo()) + "x");
        playerText.setScaleCenter(0, 0);
        playerText.setScale(0.65f);
        for (int i = posNow - 1; i >= 0; i--) {
            if (score > scoreBoardDatas[i].getScore()) {
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
        if (boards.length > 4 && posNow > 3) {
            playerSprite.setPosition(0, Config.getRES_HEIGHT() / 2);
            boards[posNow - 1].setPosition(0, Config.getRES_HEIGHT() / 2 - itemHeight * 1);
            boards[posNow - 2].setPosition(0, Config.getRES_HEIGHT() / 2 - itemHeight * 2);
            boards[0].setPosition(0, Config.getRES_HEIGHT() / 2 - itemHeight * 3);
        } else {
            for (int i = 0; i < 4; i++) {
                if (boards.length <= i) break;
                boards[i].setPosition(0,
                        Config.getRES_HEIGHT() / 2 + itemHeight * (i - (posNow <= i ? 2 : 3)));
            }
            playerSprite.setPosition(0, Config.getRES_HEIGHT() / 2 + itemHeight * (posNow - 3));
        }
    }
}
