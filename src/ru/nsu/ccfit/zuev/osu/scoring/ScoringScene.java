package ru.nsu.ccfit.zuev.osu.scoring;

import com.edlplan.ui.fragment.InGameSettingMenu;
import com.edlplan.framework.utils.functionality.SmartIterator;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.modifier.FadeInModifier;
import org.anddev.andengine.entity.modifier.ParallelEntityModifier;
import org.anddev.andengine.entity.modifier.ScaleModifier;
import org.anddev.andengine.entity.primitive.Rectangle;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.scene.background.SpriteBackground;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.entity.text.Text;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.util.Debug;

import java.util.Locale;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.DifficultyReCalculator;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.SendingPanel;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class ScoringScene {
    private final Engine engine;
    private final GameScene game;
    private final SongMenu menu;
    private Scene scene;
    private SongService songService;
    private StatisticV2 replayStat;
    private int replayID = -1;
    private TrackInfo track;

    public ScoringScene(final Engine pEngine, final GameScene pGame,
                        final SongMenu pMenu) {
        engine = pEngine;
        game = pGame;
        menu = pMenu;
    }

    public void load(final StatisticV2 stat, final TrackInfo track,
                     final SongService player, final String replay, final String mapMD5,
                     final TrackInfo trackToReplay) {
        scene = new Scene();
        //music = player;
        this.songService = player;
        if (replay != null && track == null) {
            replayStat = stat;
        }
        InGameSettingMenu.getInstance().dismiss();
        TextureRegion tex = ResourceManager.getInstance()
                .getTextureIfLoaded("::background");
        if (tex == null) {
            tex = ResourceManager.getInstance().getTexture("menu-background");
        }
        float height = tex.getHeight();
        height *= Config.getRES_WIDTH() / (float) tex.getWidth();
        final Sprite bg = new Sprite(0, (Config.getRES_HEIGHT() - height) / 2,
                Config.getRES_WIDTH(), height, tex);
        scene.setBackground(new SpriteBackground(bg));

        final Rectangle bgTopRect = new Rectangle(0, 0, Config.getRES_WIDTH(), Utils.toRes(100));
        bgTopRect.setColor(0, 0, 0, 0.8f);
        scene.attachChild(bgTopRect);

        TrackInfo trackInfo = trackToReplay;
        if (trackToReplay == null && track != null) {
            trackInfo = track;
        }
        this.track = trackInfo;
        final int x = 0, y = 100;
        final TextureRegion panelr = ResourceManager.getInstance().getTexture(
                "ranking-panel");
        final Sprite panel = new Sprite(x, y, Utils.toRes(panelr.getWidth() * 0.9f),
                Utils.toRes(panelr.getHeight() * 0.9f), panelr);
        scene.attachChild(panel);

//		final float iconSize = Utils.toRes(64);

        final TextureRegion hit300sr = ResourceManager.getInstance().getTexture("hit300");
        final Sprite hit300s = new Sprite(Utils.toRes(10), Utils.toRes(130),
                Utils.toRes(hit300sr.getWidth()), Utils.toRes(hit300sr.getHeight()), hit300sr);
        hit300s.setPosition(Utils.toRes(70 - hit300s.getWidth() / 2 + x), Utils.toRes(130 - hit300s.getHeight() / 2 + y));
        scene.attachChild(hit300s);

        final TextureRegion hit100sr = ResourceManager.getInstance().getTexture("hit100");
        final Sprite hit100s = new Sprite(Utils.toRes(10), Utils.toRes(130 + 92),
                Utils.toRes(hit100sr.getWidth()), Utils.toRes(hit100sr.getHeight()), hit100sr);
        hit100s.setPosition(Utils.toRes(70 - hit100s.getWidth() / 2 + x), Utils.toRes(130 + 92 - hit100s.getHeight() / 2 + y));
        scene.attachChild(hit100s);

        final TextureRegion hit50sr = ResourceManager.getInstance().getTexture("hit50");
        final Sprite hit50s = new Sprite(0, Utils.toRes(120 + 92 * 2),
                Utils.toRes(hit50sr.getWidth()), Utils.toRes(hit50sr.getHeight()), hit50sr);
        hit50s.setPosition(Utils.toRes(70 - hit50s.getWidth() / 2 + x), Utils.toRes(130 + 92 * 2 - hit50s.getHeight() / 2 + y));
        scene.attachChild(hit50s);

        final TextureRegion hit300ksr = ResourceManager.getInstance().getTexture("hit300g");
        final Sprite hit300ks = new Sprite(Utils.toRes(300), Utils.toRes(100),
                Utils.toRes(hit300ksr.getWidth()), Utils.toRes(hit300ksr.getHeight()), hit300ksr);
        hit300ks.setPosition(Utils.toRes(340 - hit300ks.getWidth() / 2 + x), Utils.toRes(130 - hit300ks.getHeight() / 2 + y));
        scene.attachChild(hit300ks);

        final TextureRegion hit100ksr = ResourceManager.getInstance().getTexture("hit100k");
        final Sprite hit100ks = new Sprite(Utils.toRes(300), Utils.toRes(120 + 92),
                Utils.toRes(hit100ksr.getWidth()), Utils.toRes(hit100ksr.getHeight()), hit100ksr);
        hit100ks.setPosition(Utils.toRes(340 - hit100ks.getWidth() / 2 + x), Utils.toRes(130 + 92 - hit100ks.getHeight() / 2 + y));
        scene.attachChild(hit100ks);

        final TextureRegion hit0sr = ResourceManager.getInstance().getTexture("hit0");
        final Sprite hit0s = new Sprite(Utils.toRes(300), Utils.toRes(120 + 92 * 2),
                Utils.toRes(hit0sr.getWidth()), Utils.toRes(hit0sr.getHeight()), hit0sr);
        hit0s.setPosition(Utils.toRes(340 - hit0s.getWidth() / 2 + x), Utils.toRes(130 + 92 * 2 - hit0s.getHeight() / 2 + y));
        scene.attachChild(hit0s);

        final Sprite rankingText = new Sprite(Utils.toRes(580), 0,
                ResourceManager.getInstance().getTexture("ranking-title"));
        rankingText.setPosition(Config.getRES_WIDTH() * 5 / 6 - rankingText.getWidth() / 2, 0);
        scene.attachChild(rankingText);

        int totalScore = stat.getModifiedTotalScore();
        if (totalScore == 0) {
            totalScore = stat.getAutoTotalScore();
        }
        String scoreStr = String.valueOf(totalScore);
        while (scoreStr.length() < 8) {
            scoreStr = '0' + scoreStr;
        }
        final ScoreNumber scoreNum = new ScoreNumber(Utils.toRes(220 + x),
                Utils.toRes(18 + y), scoreStr, 1, false);
        scoreNum.attachToScene(scene);

        final ScoreNumber hit300num = new ScoreNumber(Utils.toRes(138 + x),
                Utils.toRes(110 + y), stat.getHit300() + "x", 1,
                false);
        hit300num.attachToScene(scene);
        final ScoreNumber hit100num = new ScoreNumber(Utils.toRes(138 + x),
                Utils.toRes(110 + 85 + y), stat.getHit100() + "x",
                1, false);
        hit100num.attachToScene(scene);
        final ScoreNumber hit50num = new ScoreNumber(Utils.toRes(138 + x),
                Utils.toRes(110 + 85 * 2 + y), stat.getHit50() + "x",
                1, false);
        hit50num.attachToScene(scene);

        final ScoreNumber hit300knum = new ScoreNumber(Utils.toRes(400 + x),
                Utils.toRes(110 + y), stat.getHit300k() + "x", 1,
                false);
        hit300knum.attachToScene(scene);
        final ScoreNumber hit100knum = new ScoreNumber(Utils.toRes(400 + x),
                Utils.toRes(110 + 85 + y), stat.getHit100k() + "x",
                1, false);
        hit100knum.attachToScene(scene);
        final ScoreNumber hit0num = new ScoreNumber(Utils.toRes(400 + x),
                Utils.toRes(110 + 85 * 2 + y), stat.getMisses() + "x",
                1, false);
        hit0num.attachToScene(scene);

        final Sprite maxComboText = new Sprite(Utils.toRes(20 + x),
                Utils.toRes(332 + y), ResourceManager.getInstance().getTexture(
                "ranking-maxcombo"));
        scene.attachChild(maxComboText);
        final Sprite accText = new Sprite(Utils.toRes(260 + x), Utils.toRes(332 + y),
                ResourceManager.getInstance().getTexture("ranking-accuracy"));
        scene.attachChild(accText);
        final ScoreNumber maxCombo = new ScoreNumber(Utils.toRes(20 + x),
                Utils.toRes(maxComboText.getY() + 38), String.valueOf(stat.getMaxCombo()) + "x", 1,
                false);
        maxCombo.attachToScene(scene);
        final String accStr = String
                .format(Locale.ENGLISH, "%2.2f%%", stat.getAccuracy() * 100);
        final ScoreNumber accuracy = new ScoreNumber(Utils.toRes(260 + x),
                Utils.toRes(accText.getY() + 38), accStr, 1, false);
        accuracy.attachToScene(scene);

        final Sprite mark = new Sprite(Utils.toRes(610), 0, ResourceManager
                .getInstance().getTexture("ranking-" + stat.getMark()));
        if (track != null) {
            mark.setAlpha(0);
            mark.setScale(1.5f);
            mark.registerEntityModifier(new ParallelEntityModifier(
                    new FadeInModifier(2), new ScaleModifier(2, 2, 1)));
        }
        mark.setPosition(Config.getRES_WIDTH() * 5 / 6 - mark.getWidth() / 2, 80);

        final Sprite backBtn = new Sprite(Utils.toRes(580), Utils.toRes(490),
                ResourceManager.getInstance().getTexture("ranking-back")) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    ResourceManager.getInstance().getSound("menuback").play();
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    ResourceManager.getInstance().getSound("applause").stop();
                    GlobalManager.getInstance().getScoring().setReplayID(-1);
                    menu.updateScore();
//					stopMusic();
                    replayMusic();
                    engine.setScene(menu.getScene());
                    scene = null;
                    return true;
                }
                return false;
            }

        };
        backBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, Config.getRES_HEIGHT() - backBtn.getHeight() - 10);
        scene.attachChild(backBtn);

        final Sprite retryBtn = new Sprite(Utils.toRes(580), Utils.toRes(400), ResourceManager.getInstance().getTexture("ranking-retry")) {

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    ResourceManager.getInstance().getSound("menuback").play();
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    ResourceManager.getInstance().getSound("applause").stop();
                    engine.setScene(menu.getScene());
                    game.startGame(null, null);
                    scene = null;
                    stopMusic();
                    return true;
                }
                return false;
            }

        };

        final Sprite replayBtn = new Sprite(Utils.toRes(580), Utils.toRes(400),
                ResourceManager.getInstance().getTexture("ranking-replay")) {

            /////////////////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////////////////////////////
            /////////////////////////////////////////////////////////////////////////////////

            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent,
                                         final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    ResourceManager.getInstance().getSound("menuback").play();
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    ResourceManager.getInstance().getSound("applause").stop();
                    SongMenu.stopMusicStatic();
                    engine.setScene(menu.getScene());

//					Replay.mod = stat.getMod();
                    game.startGame(trackToReplay, replay);

                    ModMenu.getInstance().setMod(stat.getMod());
                    ModMenu.getInstance().setChangeSpeed(stat.getChangeSpeed());
                    ModMenu.getInstance().setForceAR(stat.getForceAR());
                    ModMenu.getInstance().setEnableForceAR(stat.isEnableForceAR());
                    ModMenu.getInstance().setFLfollowDelay(stat.getFLFollowDelay());
                    scene = null;
                    stopMusic();
                    return true;
                }
                return false;
            }

        };

        if (stat.accuracy == 1 || stat.getMaxCombo() == this.track.getMaxCombo() || stat.isPerfect()) {
            final Sprite perfect = new Sprite(0, 0, ResourceManager
                    .getInstance().getTexture("ranking-perfect"));
            perfect.setPosition(0, accuracy.getY() + accuracy.getHeight() + 10);
            scene.attachChild(perfect);
        }
        if (track != null) {
            retryBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - retryBtn.getHeight() - 10);
            scene.attachChild(retryBtn);
        } else if (replay != null) {
            replayBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - replayBtn.getHeight() - 10);
            scene.attachChild(replayBtn);
        }

        scene.setTouchAreaBindingEnabled(true);
        if (track != null) {
            scene.registerTouchArea(retryBtn);
        } else if (replay != null) {
            scene.registerTouchArea(replayBtn);
        }
        scene.registerTouchArea(backBtn);
        scene.attachChild(mark);

        float modX = mark.getX() - 30;
        final float modY = mark.getY() + mark.getHeight() * 2 / 3;
        if (stat.getMod().contains(GameMod.MOD_SCOREV2)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-scorev2"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_HARDROCK)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-hardrock"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_EASY)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-easy"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }

        if (stat.getMod().contains(GameMod.MOD_HIDDEN)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-hidden"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }

        if (stat.getMod().contains(GameMod.MOD_FLASHLIGHT)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-flashlight"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_NOFAIL)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-nofail"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_SUDDENDEATH)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-suddendeath"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_PERFECT)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-perfect"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_AUTO)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-autoplay"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_AUTOPILOT)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-relax2"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_RELAX)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-relax"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_DOUBLETIME)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-doubletime"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_NIGHTCORE)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-nightcore"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_HALFTIME)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-halftime"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }

        if (stat.getMod().contains(GameMod.MOD_PRECISE)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-precise"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        //new mods in 1.6.8
        if (stat.getMod().contains(GameMod.MOD_REALLYEASY)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-reallyeasy"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_SMALLCIRCLE)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager
                    .getInstance().getTexture("selection-mod-smallcircle"));
            modX -= Utils.toRes(30);
            scene.attachChild(modSprite);
        }
        //

        String infoStr = (trackInfo.getBeatmap().getArtistUnicode() == null || Config.isForceRomanized() ? trackInfo.getBeatmap().getArtist() : trackInfo.getBeatmap().getArtistUnicode()) + " - " +
                (trackInfo.getBeatmap().getTitleUnicode() == null || Config.isForceRomanized() ? trackInfo.getBeatmap().getTitle() : trackInfo.getBeatmap().getTitleUnicode()) + " [" + trackInfo.getMode() + "]";
        String mapperStr = "Beatmap by " + trackInfo.getCreator();
        String playerStr = "Played by " + stat.getPlayerName() + " on " +
                new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date(stat.getTime()));
        playerStr += String.format("  %s(%s)", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE);
        if (stat.getChangeSpeed() != 1 ||
            stat.isEnableForceAR() ||
            stat.getFLFollowDelay() != FlashLightEntity.defaultMoveDelayS &&
            stat.getMod().contains(GameMod.MOD_FLASHLIGHT)) {

            mapperStr += " [";
            if (stat.getChangeSpeed() != 1){
                mapperStr += String.format(Locale.ENGLISH, "%.2fx,", stat.getChangeSpeed());
            }
            if (stat.isEnableForceAR()){
                mapperStr += String.format(Locale.ENGLISH, "AR%.1f,", stat.getForceAR());
            }
            if (stat.getFLFollowDelay() != FlashLightEntity.defaultMoveDelayS && stat.getMod().contains(GameMod.MOD_FLASHLIGHT)){
                mapperStr += String.format(Locale.ENGLISH, "FLD%.2f,", stat.getFLFollowDelay());
            }
            if (mapperStr.endsWith(",")){
                mapperStr = mapperStr.substring(0, mapperStr.length() - 1);
            }
            mapperStr += "]";
        }
        Debug.i("playedtime " + stat.getTime());
        final Text beatmapInfo = new Text(Utils.toRes(4), Utils.toRes(2),
                ResourceManager.getInstance().getFont("font"), infoStr);
        final Text mapperInfo = new Text(Utils.toRes(4), beatmapInfo.getY() + beatmapInfo.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("smallFont"), mapperStr);
        final Text playerInfo = new Text(Utils.toRes(4), mapperInfo.getY() + mapperInfo.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("smallFont"), playerStr);
        //calculatePP
        if (Config.isDisplayScoreStatistics()){
            StringBuilder ppinfo = new StringBuilder();
            DifficultyReCalculator diffRecalculator = new DifficultyReCalculator();
            float newstar = diffRecalculator.recalculateStar(
                trackInfo,
                diffRecalculator.getCS(stat, trackInfo),
                stat.getSpeed()
            );
            diffRecalculator.calculatePP(stat, trackInfo);
            double pp = diffRecalculator.getTotalPP();
            diffRecalculator.calculateMaxPP(stat, trackInfo);
            double max_pp = diffRecalculator.getTotalPP();
            ppinfo.append(String.format(Locale.ENGLISH, "%.2f★ | %.2f/%.2fpp", newstar, pp, max_pp));
            if (stat.getUnstableRate() > 0) {
                ppinfo.append("\n\n");
                ppinfo.append(String.format(Locale.ENGLISH, "Error: %.2fms - %.2fms avg", stat.getNegativeHitError(), stat.getPositiveHitError()));
                ppinfo.append("\n");
                ppinfo.append(String.format(Locale.ENGLISH, "Unstable Rate: %.2f", stat.getUnstableRate()));
            }
            final Text ppInfo = new Text(Utils.toRes(4), Config.getRES_HEIGHT() - playerInfo.getHeight() - Utils.toRes(2),
                    ResourceManager.getInstance().getFont("smallFont"), ppinfo.toString());
            ppInfo.setPosition(Utils.toRes(244), Config.getRES_HEIGHT() - ppInfo.getHeight() - Utils.toRes(2));
            final Rectangle statisticRectangle = new Rectangle(Utils.toRes(240), Config.getRES_HEIGHT() - ppInfo.getHeight() - Utils.toRes(4), ppInfo.getWidth() + Utils.toRes(12), ppInfo.getHeight() + Utils.toRes(4));
            statisticRectangle.setColor(0, 0, 0, 0.5f);
            scene.attachChild(statisticRectangle);
            scene.attachChild(ppInfo);
        }
        scene.attachChild(beatmapInfo);
        scene.attachChild(mapperInfo);
        scene.attachChild(playerInfo);

        //save and upload score
        if (track != null && mapMD5 != null) {
            ResourceManager.getInstance().getSound("applause").play();
            ScoreLibrary.getInstance().addScore(track.getFilename(), stat, replay);
            if (stat.getModifiedTotalScore() > 0 && OnlineManager.getInstance().isStayOnline() &&
                    OnlineManager.getInstance().isReadyToSend()) {
                boolean hasUnrankedMod = SmartIterator.wrap(stat.getMod().iterator())
                    .applyFilter(m -> m.unranked).hasNext();
                if (hasUnrankedMod
                    || Config.isRemoveSliderLock()
                    || ModMenu.getInstance().isChangeSpeed()
                    || ModMenu.getInstance().isEnableForceAR()) {
                    return;
                }

                SendingPanel sendingPanel = new SendingPanel(OnlineManager.getInstance().getRank(),
                        OnlineManager.getInstance().getScore(), OnlineManager.getInstance().getAccuracy());
                sendingPanel.setPosition(Config.getRES_WIDTH() / 2 - 400, Utils.toRes(-300));
                scene.registerTouchArea(sendingPanel.getDismissTouchArea());
                scene.attachChild(sendingPanel);
                ScoreLibrary.getInstance().sendScoreOnline(stat, replay, sendingPanel);
            }
        }
    }

    public Scene getScene() {
        return scene;
    }

    public void stopMusic() {
        if (songService != null) {
            songService.stop();
        }
    }

    public void replayMusic() {
        if (songService != null) {
            songService.stop();
//            songService.preLoadWithLoop(game.filePath);
            songService.preLoad(track.getBeatmap().getMusic());
            songService.play();
        }
    }

    public StatisticV2 getReplayStat() {
        return replayStat;
    }

    public void setReplayStat(StatisticV2 replayStat) {
        this.replayStat = replayStat;
    }

    public int getReplayID() {
        return replayID;
    }

    public void setReplayID(int id) {
        this.replayID = id;
    }
}
