package ru.nsu.ccfit.zuev.osu.scoring;

import com.edlplan.framework.utils.functionality.SmartIterator;
import com.edlplan.ui.fragment.InGameSettingMenu;
import com.reco1l.framework.lang.Execution;
import com.reco1l.legacy.Multiplayer;
import com.reco1l.legacy.ui.entity.StatisticSelector;
import com.reco1l.legacy.ui.multiplayer.RoomScene;
import com.rian.difficultycalculator.attributes.DifficultyAttributes;
import com.rian.difficultycalculator.attributes.PerformanceAttributes;
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
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.beatmap.BeatmapData;
import ru.nsu.ccfit.zuev.osu.beatmap.parser.BeatmapParser;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.game.cursor.flashlight.FlashLightEntity;
import ru.nsu.ccfit.zuev.osu.game.mods.GameMod;
import ru.nsu.ccfit.zuev.osu.helper.BeatmapDifficultyCalculator;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.SendingPanel;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

import java.util.Locale;

public class ScoringScene {

    private final Engine engine;

    private final GameScene game;

    private final SongMenu menu;

    public TrackInfo track;

    // Multiplayer
    public StatisticV2 currentStatistic;

    private Scene scene;

    private SongService songService;

    private StatisticV2 replayStat;

    private int replayID = -1;

    private StatisticSelector selector;


    public ScoringScene(final Engine pEngine, final GameScene pGame, final SongMenu pMenu) {
        engine = pEngine;
        game = pGame;
        menu = pMenu;
    }

    public void load(final StatisticV2 stat, final TrackInfo track, final SongService player, final String replay, final String mapMD5, final TrackInfo trackToReplay) {
        scene = new Scene();
        songService = player;
        currentStatistic = stat;
        if (replay != null && track == null) {
            replayStat = stat;
        }
        InGameSettingMenu.getInstance().dismiss();
        TextureRegion tex = ResourceManager.getInstance().getTextureIfLoaded("::background");
        if (tex == null) {
            tex = ResourceManager.getInstance().getTexture("menu-background");
        }
        float height = tex.getHeight();
        height *= Config.getRES_WIDTH() / (float) tex.getWidth();
        final Sprite bg = new Sprite(0, (Config.getRES_HEIGHT() - height) / 2, Config.getRES_WIDTH(), height, tex);
        scene.setBackground(new SpriteBackground(bg));

        final Rectangle bgTopRect = new Rectangle(0, 0, Config.getRES_WIDTH(), 100);
        bgTopRect.setColor(0, 0, 0, 0.8f);
        scene.attachChild(bgTopRect);

        TrackInfo trackInfo = trackToReplay;
        if (trackToReplay == null && track != null) {
            trackInfo = track;
        }
        this.track = trackInfo;
        final int x = 0, y = 100;
        final TextureRegion panelr = ResourceManager.getInstance().getTexture("ranking-panel");
        final float i26 = panelr.getHeight() * 0.9f;
        final float i27 = panelr.getWidth() * 0.9f;
        final Sprite panel = new Sprite(x, y, i27, i26, panelr);
        scene.attachChild(panel);

        final TextureRegion hit300sr = ResourceManager.getInstance().getTexture("hit300");
        final int i10 = hit300sr.getHeight();
        final int i11 = hit300sr.getWidth();
        final Sprite hit300s = new Sprite(10, 130, i11, i10, hit300sr);
        final float i24 = 130 - hit300s.getHeight() / 2 + y;
        final float i25 = 70 - hit300s.getWidth() / 2 + x;
        hit300s.setPosition(i25, i24);
        scene.attachChild(hit300s);

        final TextureRegion hit100sr = ResourceManager.getInstance().getTexture("hit100");
        final int i8 = hit100sr.getHeight();
        final int i9 = hit100sr.getWidth();
        final Sprite hit100s = new Sprite(10, 130 + 92, i9, i8, hit100sr);
        final float i22 = 130 + 92 - hit100s.getHeight() / 2 + y;
        final float i23 = 70 - hit100s.getWidth() / 2 + x;
        hit100s.setPosition(i23, i22);
        scene.attachChild(hit100s);

        final TextureRegion hit50sr = ResourceManager.getInstance().getTexture("hit50");
        final int i6 = hit50sr.getHeight();
        final int i7 = hit50sr.getWidth();
        final Sprite hit50s = new Sprite(0, 120 + 92 * 2, i7, i6, hit50sr);
        final float i20 = 130 + 92 * 2 - hit50s.getHeight() / 2 + y;
        final float i21 = 70 - hit50s.getWidth() / 2 + x;
        hit50s.setPosition(i21, i20);
        scene.attachChild(hit50s);

        final TextureRegion hit300ksr = ResourceManager.getInstance().getTexture("hit300g");
        final int i4 = hit300ksr.getHeight();
        final int i5 = hit300ksr.getWidth();
        final Sprite hit300ks = new Sprite(300, 100, i5, i4, hit300ksr);
        final float i18 = 130 - hit300ks.getHeight() / 2 + y;
        final float i19 = 340 - hit300ks.getWidth() / 2 + x;
        hit300ks.setPosition(i19, i18);
        scene.attachChild(hit300ks);

        final TextureRegion hit100ksr = ResourceManager.getInstance().getTexture("hit100k");
        final int i2 = hit100ksr.getHeight();
        final int i3 = hit100ksr.getWidth();
        final Sprite hit100ks = new Sprite(300, 120 + 92, i3, i2, hit100ksr);
        final float i16 = 130 + 92 - hit100ks.getHeight() / 2 + y;
        final float i17 = 340 - hit100ks.getWidth() / 2 + x;
        hit100ks.setPosition(i17, i16);
        scene.attachChild(hit100ks);

        final TextureRegion hit0sr = ResourceManager.getInstance().getTexture("hit0");
        final int i = hit0sr.getHeight();
        final int i1 = hit0sr.getWidth();
        final Sprite hit0s = new Sprite(300, 120 + 92 * 2, i1, i, hit0sr);
        final float i14 = 130 + 92 * 2 - hit0s.getHeight() / 2 + y;
        final float i15 = 340 - hit0s.getWidth() / 2 + x;
        hit0s.setPosition(i15, i14);
        scene.attachChild(hit0s);

        final Sprite rankingText = new Sprite(580, 0, ResourceManager.getInstance().getTexture("ranking-title"));
        rankingText.setPosition(Config.getRES_WIDTH() * 5f / 6f - rankingText.getWidth() / 2f, 0f);
        scene.attachChild(rankingText);

        StringBuilder scoreStr = new StringBuilder(String.valueOf(stat.getTotalScoreWithMultiplier()));
        while (scoreStr.length() < 8) {
            scoreStr.insert(0, '0');
        }
        final ScoreNumber scoreNum = new ScoreNumber(220 + x, 18 + y, scoreStr.toString(), 1, false);
        scoreNum.attachToScene(scene);

        final ScoreNumber hit300num = new ScoreNumber(138 + x, 110 + y, stat.getHit300() + "x", 1, false);
        hit300num.attachToScene(scene);
        final ScoreNumber hit100num = new ScoreNumber(138 + x, 110 + 85 + y, stat.getHit100() + "x", 1, false);
        hit100num.attachToScene(scene);
        final ScoreNumber hit50num = new ScoreNumber(138 + x, 110 + 85 * 2 + y, stat.getHit50() + "x", 1, false);
        hit50num.attachToScene(scene);

        final ScoreNumber hit300knum = new ScoreNumber(400 + x, 110 + y, stat.getHit300k() + "x", 1, false);
        hit300knum.attachToScene(scene);
        final ScoreNumber hit100knum = new ScoreNumber(400 + x, 110 + 85 + y, stat.getHit100k() + "x", 1, false);
        hit100knum.attachToScene(scene);
        final ScoreNumber hit0num = new ScoreNumber(400 + x, 110 + 85 * 2 + y, stat.getMisses() + "x", 1, false);
        hit0num.attachToScene(scene);

        final Sprite maxComboText = new Sprite(20 + x, 332 + y, ResourceManager.getInstance().getTexture("ranking-maxcombo"));
        scene.attachChild(maxComboText);
        final Sprite accText = new Sprite(260 + x, 332 + y, ResourceManager.getInstance().getTexture("ranking-accuracy"));
        scene.attachChild(accText);
        final float i13 = maxComboText.getY() + 38;
        final ScoreNumber maxCombo = new ScoreNumber(20 + x, i13, stat.getMaxCombo() + "x", 1, false);
        maxCombo.attachToScene(scene);
        final String accStr = String.format(Locale.ENGLISH, "%2.2f%%", stat.getAccuracy() * 100);
        final float i12 = accText.getY() + 38;
        final ScoreNumber accuracy = new ScoreNumber(260 + x, i12, accStr, 1, false);
        accuracy.attachToScene(scene);

        final Sprite mark = new Sprite(610, 0, ResourceManager.getInstance().getTexture("ranking-" + stat.getMark()));
        if (track != null) {
            mark.setAlpha(0);
            mark.setScale(1.5f);
            mark.registerEntityModifier(new ParallelEntityModifier(new FadeInModifier(2), new ScaleModifier(2, 2, 1)));
        }
        mark.setPosition(Config.getRES_WIDTH() * 5f/ 6f - mark.getWidth() / 2f, 80f);

        final Sprite backBtn = new Sprite(580, 490, ResourceManager.getInstance().getTexture("ranking-back")) {


            @Override
            public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                if (pSceneTouchEvent.isActionDown()) {
                    setColor(0.7f, 0.7f, 0.7f);
                    ResourceManager.getInstance().getSound("menuback").play();
                    return true;
                }
                if (pSceneTouchEvent.isActionUp()) {
                    back();
                    return true;
                }
                return false;
            }

        };
        backBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, Config.getRES_HEIGHT() - backBtn.getHeight() - 10);
        scene.attachChild(backBtn);

        Sprite retryBtn = null;

        if (!Multiplayer.isMultiplayer) {
            retryBtn = new Sprite(580, 400, ResourceManager.getInstance().getTexture("ranking-retry")) {

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
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
        }

        Sprite replayBtn = null;

        if (!Multiplayer.isMultiplayer) {
            replayBtn = new Sprite(580, 400, ResourceManager.getInstance().getTexture("ranking-replay")) {

                @Override
                public boolean onAreaTouched(final TouchEvent pSceneTouchEvent, final float pTouchAreaLocalX, final float pTouchAreaLocalY) {
                    if (pSceneTouchEvent.isActionDown()) {
                        setColor(0.7f, 0.7f, 0.7f);
                        ResourceManager.getInstance().getSound("menuback").play();
                        return true;
                    }
                    if (pSceneTouchEvent.isActionUp()) {
                        ResourceManager.getInstance().getSound("applause").stop();
                        SongMenu.stopMusicStatic();
                        engine.setScene(menu.getScene());

                        Replay.oldMod = ModMenu.getInstance().getMod();
                        Replay.oldChangeSpeed = ModMenu.getInstance().getChangeSpeed();

                        Replay.oldCustomAR = ModMenu.getInstance().getCustomAR();
                        Replay.oldCustomOD = ModMenu.getInstance().getCustomOD();
                        Replay.oldCustomCS = ModMenu.getInstance().getCustomCS();
                        Replay.oldCustomHP = ModMenu.getInstance().getCustomHP();

                        Replay.oldFLFollowDelay = ModMenu.getInstance().getFLfollowDelay();

                        ModMenu.getInstance().setMod(stat.getMod());
                        ModMenu.getInstance().setChangeSpeed(stat.getChangeSpeed());
                        ModMenu.getInstance().setFLfollowDelay(stat.getFLFollowDelay());

                        ModMenu.getInstance().setCustomAR(stat.getCustomAR());
                        ModMenu.getInstance().setCustomOD(stat.getCustomOD());
                        ModMenu.getInstance().setCustomCS(stat.getCustomCS());
                        ModMenu.getInstance().setCustomHP(stat.getCustomHP());

                        game.startGame(trackToReplay, replay);

                        scene = null;
                        stopMusic();
                        return true;
                    }
                    return false;
                }

            };
        }

        if (stat.accuracy == 1 || stat.getMaxCombo() == this.track.getMaxCombo() || stat.isPerfect()) {
            final Sprite perfect = new Sprite(0, 0, ResourceManager.getInstance().getTexture("ranking-perfect"));
            perfect.setPosition(0, accuracy.getY() + accuracy.getHeight() + 10);
            scene.attachChild(perfect);
        }
        if (track != null && retryBtn != null) {
            retryBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - retryBtn.getHeight() - 10);
            scene.attachChild(retryBtn);
        } else if (replay != null && replayBtn != null) {
            replayBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - replayBtn.getHeight() - 10);
            scene.attachChild(replayBtn);
        }

        scene.setTouchAreaBindingEnabled(true);
        if (track != null && retryBtn != null) {
            scene.registerTouchArea(retryBtn);
        } else if (replay != null && replayBtn != null) {
            scene.registerTouchArea(replayBtn);
        }
        scene.registerTouchArea(backBtn);
        scene.attachChild(mark);

        float modX = mark.getX() - 30;
        final float modY = mark.getY() + mark.getHeight() * 2 / 3;
        if (stat.getMod().contains(GameMod.MOD_SCOREV2)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-scorev2"));
            modX -= 30;
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_HARDROCK)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-hardrock"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_EASY)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-easy"));
            modX -= 30;
            scene.attachChild(modSprite);
        }

        if (stat.getMod().contains(GameMod.MOD_HIDDEN)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-hidden"));
            modX -= 30;
            scene.attachChild(modSprite);
        }

        if (stat.getMod().contains(GameMod.MOD_FLASHLIGHT)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-flashlight"));
            modX -= 30;
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_NOFAIL)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-nofail"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_SUDDENDEATH)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-suddendeath"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_PERFECT)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-perfect"));
            modX -= 30;
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_AUTO)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-autoplay"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_AUTOPILOT)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-relax2"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_RELAX)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-relax"));
            modX -= 30;
            scene.attachChild(modSprite);
        }
        if (stat.getMod().contains(GameMod.MOD_DOUBLETIME)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-doubletime"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_NIGHTCORE)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-nightcore"));
            modX -= 30;
            scene.attachChild(modSprite);
        } else if (stat.getMod().contains(GameMod.MOD_HALFTIME)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-halftime"));
            modX -= 30;
            scene.attachChild(modSprite);
        }

        if (stat.getMod().contains(GameMod.MOD_PRECISE)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-precise"));
            modX -= 30;
            scene.attachChild(modSprite);
        }
        //new mods in 1.6.8
        if (stat.getMod().contains(GameMod.MOD_REALLYEASY)) {
            final Sprite modSprite = new Sprite(modX, modY, ResourceManager.getInstance().getTexture("selection-mod-reallyeasy"));
            scene.attachChild(modSprite);
        }

        String infoStr = (trackInfo.getBeatmap().getArtistUnicode() == null || Config.isForceRomanized() ? trackInfo.getBeatmap().getArtist() : trackInfo.getBeatmap().getArtistUnicode()) + " - " + (trackInfo.getBeatmap().getTitleUnicode() == null || Config.isForceRomanized() ? trackInfo.getBeatmap().getTitle() : trackInfo.getBeatmap().getTitleUnicode()) + " [" + trackInfo.getMode() + "]";
        String mapperStr = "Beatmap by " + trackInfo.getCreator();
        String playerStr = "Played by " + stat.getPlayerName() + " on " + new java.text.SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new java.util.Date(stat.getTime()));
        playerStr += String.format("  %s(%s)", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE);
        if (stat.getChangeSpeed() != 1 || stat.isCustomAR() || stat.isCustomOD() || stat.isCustomCS() || stat.isCustomHP() || stat.getFLFollowDelay() != FlashLightEntity.defaultMoveDelayS && stat.getMod().contains(GameMod.MOD_FLASHLIGHT)) {

            mapperStr += " [";
            if (stat.getChangeSpeed() != 1) {
                mapperStr += String.format(Locale.ENGLISH, "%.2fx,", stat.getChangeSpeed());
            }
            if (stat.isCustomAR()) {
                mapperStr += String.format(Locale.ENGLISH, "AR%.1f,", stat.getCustomAR());
            }
            if (stat.isCustomOD()) {
                mapperStr += String.format(Locale.ENGLISH, "OD%.1f,", stat.getCustomOD());
            }
            if (stat.isCustomCS()) {
                mapperStr += String.format(Locale.ENGLISH, "CS%.1f,", stat.getCustomCS());
            }
            if (stat.isCustomHP()) {
                mapperStr += String.format(Locale.ENGLISH, "HP%.1f,", stat.getCustomHP());
            }
            if (stat.getFLFollowDelay() != FlashLightEntity.defaultMoveDelayS && stat.getMod().contains(GameMod.MOD_FLASHLIGHT)) {
                mapperStr += String.format(Locale.ENGLISH, "FLD%.2f,", stat.getFLFollowDelay());
            }
            if (mapperStr.endsWith(",")) {
                mapperStr = mapperStr.substring(0, mapperStr.length() - 1);
            }
            mapperStr += "]";
        }
        Debug.i("playedtime " + stat.getTime());
        final Text beatmapInfo = new Text(4, 2, ResourceManager.getInstance().getFont("font"), infoStr);
        final Text mapperInfo = new Text(4, beatmapInfo.getY() + beatmapInfo.getHeight() + 2, ResourceManager.getInstance().getFont("smallFont"), mapperStr);
        final Text playerInfo = new Text(4, mapperInfo.getY() + mapperInfo.getHeight() + 2, ResourceManager.getInstance().getFont("smallFont"), playerStr);
        //calculatePP
        if (Config.isDisplayScoreStatistics()) {
            StringBuilder ppinfo = new StringBuilder();
            BeatmapData beatmapData = new BeatmapParser(this.track.getFilename()).parse(true);

            if (beatmapData != null) {
                DifficultyAttributes difficultyAttributes = BeatmapDifficultyCalculator.calculateDifficulty(beatmapData, stat);
                PerformanceAttributes performanceAttributes = BeatmapDifficultyCalculator.calculatePerformance(difficultyAttributes, stat);
                PerformanceAttributes maxPerformanceAttributes = BeatmapDifficultyCalculator.calculatePerformance(difficultyAttributes);
                ppinfo.append(String.format(Locale.ENGLISH, "%.2f★ | %.2f/%.2fpp", difficultyAttributes.starRating, performanceAttributes.total, maxPerformanceAttributes.total));
            }
            if (stat.getUnstableRate() > 0) {
                if (beatmapData != null) {
                    ppinfo.append("\n");
                }
                ppinfo.append(String.format(Locale.ENGLISH, "Error: %.2fms - %.2fms avg", stat.getNegativeHitError(), stat.getPositiveHitError()));
                ppinfo.append("\n");
                ppinfo.append(String.format(Locale.ENGLISH, "Unstable Rate: %.2f", stat.getUnstableRate()));
            }
            final Text ppInfo = new Text(4, Config.getRES_HEIGHT() - playerInfo.getHeight() - 2, ResourceManager.getInstance().getFont("smallFont"), ppinfo.toString());
            ppInfo.setPosition(244, Config.getRES_HEIGHT() - ppInfo.getHeight() - 2);
            final Rectangle statisticRectangle = new Rectangle(240, Config.getRES_HEIGHT() - ppInfo.getHeight() - 4, ppInfo.getWidth() + 12, ppInfo.getHeight() + 4);
            statisticRectangle.setColor(0, 0, 0, 0.5f);
            scene.attachChild(statisticRectangle);
            scene.attachChild(ppInfo);
        }
        scene.attachChild(beatmapInfo);
        scene.attachChild(mapperInfo);
        scene.attachChild(playerInfo);

        // In case the scene was reloaded
        if (Multiplayer.isMultiplayer) {
            updateLeaderboard();
        }

        //save and upload score
        if (track != null && track.getMD5() != null && track.getMD5().equals(mapMD5)) {
            ResourceManager.getInstance().getSound("applause").play();
            if (!Multiplayer.isMultiplayer || !GlobalManager.getInstance().getGameScene().hasFailed) {
                ScoreLibrary.getInstance().addScore(track.getFilename(), stat, replay);
            }

            if (stat.getTotalScoreWithMultiplier() > 0 && OnlineManager.getInstance().isStayOnline() && OnlineManager.getInstance().isReadyToSend()) {

                if (GlobalManager.getInstance().getGameScene().hasFailed || (Multiplayer.isMultiplayer && !Config.isSubmitScoreOnMultiplayer())) {
                    return;
                }

                boolean hasUnrankedMod = SmartIterator.wrap(stat.getMod().iterator()).applyFilter(m -> m.unranked).hasNext();
                if (hasUnrankedMod || Config.isRemoveSliderLock() || ModMenu.getInstance().isCustomAR() || ModMenu.getInstance().isCustomOD() || ModMenu.getInstance().isCustomCS() || ModMenu.getInstance().isCustomHP() || !ModMenu.getInstance().isDefaultFLFollowDelay()) {
                    return;
                }

                SendingPanel sendingPanel = new SendingPanel(OnlineManager.getInstance().getRank(), OnlineManager.getInstance().getScore(), OnlineManager.getInstance().getAccuracy());
                sendingPanel.setPosition(Config.getRES_WIDTH() / 2f - 400, -300);
                scene.registerTouchArea(sendingPanel.getDismissTouchArea());
                scene.attachChild(sendingPanel);
                ScoreLibrary.getInstance().sendScoreOnline(stat, replay, sendingPanel);
            }
        }
    }

    public void updateLeaderboard() {

        if (Multiplayer.finalData != null) {

            if (selector != null) {
                var oldSelector = selector;
                Execution.glThread(() -> {
                    oldSelector.detachSelf();
                    oldSelector.detachChildren();

                    if (scene != null) {
                        scene.unregisterTouchArea(oldSelector);
                    }
                });
            }

            selector = new StatisticSelector(Multiplayer.finalData);

            if (scene != null) {
                scene.attachChild(selector);
                scene.registerTouchArea(selector);
            }
        }
    }

    public void back() {
        ResourceManager.getInstance().getSound("applause").stop();
        Multiplayer.finalData = null;
        currentStatistic = null;

        if (Multiplayer.isMultiplayer) {
            // Preventing NPEs when player gets disconnected while playing
            if (!Multiplayer.isConnected()) {
                RoomScene.INSTANCE.back();
            } else {
                RoomScene.INSTANCE.show();
            }
            return;
        }
        replayMusic();
        GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getSongMenu().getScene());
        GlobalManager.getInstance().getSongMenu().updateScore();
        setReplayID(-1);
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

    public int getReplayID() {
        return replayID;
    }

    public void setReplayID(int id) {
        this.replayID = id;
    }

}
