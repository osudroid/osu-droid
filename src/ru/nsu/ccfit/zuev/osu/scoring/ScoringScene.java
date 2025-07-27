package ru.nsu.ccfit.zuev.osu.scoring;

import android.util.Log;

import com.edlplan.framework.utils.functionality.SmartIterator;
import com.osudroid.data.DatabaseManager;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.multiplayer.RoomScene;
import com.osudroid.ui.v2.modmenu.ModIcon;
import com.osudroid.utils.Execution;
import com.reco1l.osu.ui.entity.StatisticSelector;

import com.rian.osu.GameMode;
import com.rian.osu.beatmap.Beatmap;
import com.rian.osu.beatmap.parser.BeatmapParser;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import com.rian.osu.difficulty.attributes.DifficultyAttributes;
import com.rian.osu.difficulty.attributes.DroidDifficultyAttributes;
import com.rian.osu.difficulty.attributes.PerformanceAttributes;
import com.rian.osu.difficulty.attributes.StandardDifficultyAttributes;
import com.rian.osu.mods.ModAutoplay;
import com.rian.osu.mods.ModCustomSpeed;
import com.rian.osu.mods.ModDifficultyAdjust;
import com.rian.osu.mods.ModFlashlight;
import com.rian.osu.mods.ModNightCore;
import com.rian.osu.mods.ModOldNightCore;
import com.rian.osu.ui.SendingPanel;
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
import org.apache.commons.io.FilenameUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.DifficultyAlgorithm;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import com.osudroid.data.BeatmapInfo;

import ru.nsu.ccfit.zuev.osu.ToastLogger;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osu.online.OnlineScoring;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;

public class ScoringScene {
    private final Engine engine;
    private final GameScene game;
    private final SongMenu menu;
    private Scene scene;
    private SongService songService;
    private StatisticV2 replayStat;
    private int replayID = -1;
    public BeatmapInfo beatmapInfo;

    // Multiplayer
    public StatisticV2 currentStatistic;

    private StatisticSelector selector;


    public ScoringScene(final Engine pEngine, final GameScene pGame,
                        final SongMenu pMenu) {
        engine = pEngine;
        game = pGame;
        menu = pMenu;
    }

    public void load(final StatisticV2 stat, final BeatmapInfo beatmap,
                     final SongService player, final String replayPath, final String mapMD5,
                     final BeatmapInfo beatmapToReplay) {
        scene = new Scene();
        songService = player;
        currentStatistic = stat;
        if (replayPath != null && beatmap == null) {
            replayStat = stat;
        }
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

        beatmapInfo = beatmapToReplay;
        if (beatmapToReplay == null && beatmap != null) {
            beatmapInfo = beatmap;
        }

        final int x = 0, y = 100;
        final TextureRegion panelr = ResourceManager.getInstance().getTexture(
                "ranking-panel");
        final Sprite panel = new Sprite(x, y, Utils.toRes(panelr.getWidth() * 0.9f),
                Utils.toRes(panelr.getHeight() * 0.9f), panelr);
        scene.attachChild(panel);

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
        rankingText.setPosition((float) (Config.getRES_WIDTH() * 5) / 6 - rankingText.getWidth() / 2, 0);
        scene.attachChild(rankingText);

        StringBuilder scoreStr = new StringBuilder(String.valueOf(stat.getTotalScoreWithMultiplier()));
        while (scoreStr.length() < 8) {
            scoreStr.insert(0, "0");
        }
        final ScoreNumber scoreNum = new ScoreNumber(Utils.toRes(220 + x),
                Utils.toRes(18 + y), scoreStr.toString(), 1, false);
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
                Utils.toRes(maxComboText.getY() + 38), stat.getScoreMaxCombo() + "x", 1,
                false);
        maxCombo.attachToScene(scene);
        final String accStr = String
                .format(Locale.ENGLISH, "%2.2f%%", stat.getAccuracy() * 100);
        final ScoreNumber accuracy = new ScoreNumber(Utils.toRes(260 + x),
                Utils.toRes(accText.getY() + 38), accStr, 1, false);
        accuracy.attachToScene(scene);

        final Sprite mark = new Sprite(Utils.toRes(610), 0, ResourceManager
                .getInstance().getTexture("ranking-" + stat.getMark()));
        if (beatmap != null) {
            mark.setAlpha(0);
            mark.setScale(1.5f);
            mark.registerEntityModifier(new ParallelEntityModifier(
                    new FadeInModifier(2), new ScaleModifier(2, 2, 1)));
        }
        mark.setPosition((float) (Config.getRES_WIDTH() * 5) / 6 - mark.getWidth() / 2, 80);

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
                    back();
                    return true;
                }
                return false;
            }

        };
        backBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, Config.getRES_HEIGHT() - backBtn.getHeight() - 10);
        scene.attachChild(backBtn);

        Sprite retryBtn = null;

        if (!Multiplayer.isMultiplayer)
        {
            retryBtn = new Sprite(Utils.toRes(580), Utils.toRes(400), ResourceManager.getInstance().getTexture("ranking-retry")) {

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
                        game.startGame(null, null, null);
                        scene = null;
                        stopMusic();
                        return true;
                    }
                    return false;
                }

            };
        }

        Sprite replayBtn = null;
        var mods = stat.getMod();

        if (!Multiplayer.isMultiplayer)
        {
            replayBtn = new Sprite(Utils.toRes(580), Utils.toRes(400),
                                                ResourceManager.getInstance().getTexture("ranking-replay")) {

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
                        game.startGame(beatmapToReplay, replayPath, mods);

                        scene = null;
                        stopMusic();
                        return true;
                    }
                    return false;
                }

            };
        }

        if (stat.isPerfect()) {
            final Sprite perfect = new Sprite(0, 0, ResourceManager.getInstance().getTexture("ranking-perfect"));
            perfect.setPosition(0, accuracy.getY() + accuracy.getHeight() + 10);
            scene.attachChild(perfect);
        }

        scene.setTouchAreaBindingEnabled(true);
        if (beatmap != null && retryBtn != null) {
            scene.registerTouchArea(retryBtn);
        } else if (replayPath != null && replayBtn != null) {
            scene.registerTouchArea(replayBtn);
        }
        scene.registerTouchArea(backBtn);
        scene.attachChild(mark);

        if (beatmap != null && retryBtn != null) {
            retryBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - retryBtn.getHeight() - 10);
            scene.attachChild(retryBtn);
        } else if (replayPath != null && replayBtn != null) {
            replayBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - replayBtn.getHeight() - 10);
            scene.attachChild(replayBtn);
        }

        var modX = mark.getX() - 30;
        var modY = mark.getY() + mark.getHeight() * 2 / 3;
        for (var mod : mods.values()) {
            if (!mod.isUserPlayable()) {
                continue;
            }

            var modIcon = new ModIcon(mod);
            modIcon.setPosition(modX, modY);
            modIcon.setSize(68, 66);
            modX -= 30;
            scene.attachChild(modIcon);
        }

        String infoStr = beatmapInfo.getArtistText() + " - " + beatmapInfo.getTitleText() + " [" + beatmapInfo.getVersion() + "]";

        String mapperStr = "Beatmap by " + beatmapInfo.getCreator();
        String playerStr = "Played by " + stat.getPlayerName() + " on " +
                new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(new Date(stat.getTime()));
        playerStr += String.format("  %s(%s)", BuildConfig.VERSION_NAME, BuildConfig.BUILD_TYPE);
        if (mods.contains(ModCustomSpeed.class) ||
            mods.contains(ModDifficultyAdjust.class) ||
            (mods.contains(ModFlashlight.class) && mods.ofType(ModFlashlight.class).getFollowDelay() != ModFlashlight.DEFAULT_FOLLOW_DELAY)) {

            var customSpeed = mods.ofType(ModCustomSpeed.class);
            var difficultyAdjust = mods.ofType(ModDifficultyAdjust.class);
            var flashlight = mods.ofType(ModFlashlight.class);

            mapperStr += " [";
            if (customSpeed != null && customSpeed.getTrackRateMultiplier() != 1f) {
                mapperStr += String.format(Locale.ENGLISH, "%.2fx,", customSpeed.getTrackRateMultiplier());
            }

            if (difficultyAdjust != null) {
                if (difficultyAdjust.getAr() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "AR%.1f,", difficultyAdjust.getAr());
                }

                if (difficultyAdjust.getOd() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "OD%.1f,", difficultyAdjust.getOd());
                }

                if (difficultyAdjust.getCs() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "CS%.1f,", difficultyAdjust.getCs());
                }

                if (difficultyAdjust.getHp() != null) {
                    mapperStr += String.format(Locale.ENGLISH, "HP%.1f,", difficultyAdjust.getHp());
                }
            }

            if (flashlight != null && flashlight.getFollowDelay() != ModFlashlight.DEFAULT_FOLLOW_DELAY) {
                mapperStr += String.format(Locale.ENGLISH, "FLD%.2f,", flashlight.getFollowDelay());
            }

            if (mapperStr.endsWith(",")){
                mapperStr = mapperStr.substring(0, mapperStr.length() - 1);
            }

            mapperStr += "]";
        }
        Debug.i("playedtime " + stat.getTime());
        final Text beatmapInfoText = new Text(Utils.toRes(4), Utils.toRes(2),
                ResourceManager.getInstance().getFont("font"), infoStr);
        final Text mapperInfo = new Text(Utils.toRes(4), beatmapInfoText.getY() + beatmapInfoText.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("smallFont"), mapperStr);
        final Text playerInfo = new Text(Utils.toRes(4), mapperInfo.getY() + mapperInfo.getHeight() + Utils.toRes(2),
                ResourceManager.getInstance().getFont("smallFont"), playerStr);

        if (Config.isDisplayScoreStatistics() && !currentStatistic.isTeamStatistic()) {

            StringBuilder ppinfo = new StringBuilder();
            Beatmap beatmapData;

            try (var parser = new BeatmapParser(beatmapInfo.getPath())) {
                beatmapData = parser.parse(
                    true,
                    Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid ? GameMode.Droid : GameMode.Standard
                );
            }

            if (beatmapData != null) {
                DifficultyAttributes difficultyAttributes = null;
                PerformanceAttributes performanceAttributes = null;

                // Don't try to load online replay
                if (replayPath != null && beatmapToReplay != null && !replayPath.startsWith("https://")) {
                    var replay = new Replay();
                    replay.setObjectCount(beatmapToReplay.getTotalHitObjectCount());
                    replay.setBeatmap(beatmapToReplay.getFullBeatmapsetName(), beatmapToReplay.getFullBeatmapName(), mapMD5);

                    if (replay.load(replayPath, true)) {
                        var copiedMods = mods;

                        if (mods.contains(ModNightCore.class) && replay.replayVersion <= 3) {
                            copiedMods = mods.deepCopy();
                            copiedMods.removeOfType(ModNightCore.class);
                            copiedMods.put(ModOldNightCore.class);
                        }

                        switch (Config.getDifficultyAlgorithm()) {
                            case droid -> {
                                var playableBeatmap = beatmapData.createDroidPlayableBeatmap(copiedMods.values());

                                difficultyAttributes = BeatmapDifficultyCalculator.calculateDroidDifficulty(playableBeatmap);

                                performanceAttributes = BeatmapDifficultyCalculator.calculateDroidPerformanceWithReplayStat(
                                    playableBeatmap, (DroidDifficultyAttributes) difficultyAttributes, replay.cursorMoves,
                                    replay.objectData, stat
                                );
                            }

                            case standard -> {
                                var playableBeatmap = beatmapData.createStandardPlayableBeatmap(copiedMods.values());

                                difficultyAttributes = BeatmapDifficultyCalculator.calculateStandardDifficulty(playableBeatmap);

                                performanceAttributes = BeatmapDifficultyCalculator.calculateStandardPerformanceWithReplayStat(
                                    playableBeatmap, (StandardDifficultyAttributes) difficultyAttributes, replay.cursorMoves,
                                    replay.objectData, stat
                                );
                            }
                        }
                    }
                }

                if (difficultyAttributes == null) {
                    difficultyAttributes = switch (Config.getDifficultyAlgorithm()) {
                        case droid -> BeatmapDifficultyCalculator.calculateDroidDifficulty(beatmapData, mods.values());
                        case standard -> BeatmapDifficultyCalculator.calculateStandardDifficulty(beatmapData, mods.values());
                    };

                    performanceAttributes = switch (Config.getDifficultyAlgorithm()) {
                        case droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                            beatmapData, (DroidDifficultyAttributes) difficultyAttributes, stat
                        );

                        case standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                            beatmapData, (StandardDifficultyAttributes) difficultyAttributes, stat
                        );
                    };
                }

                var maxPerformanceAttributes = switch (Config.getDifficultyAlgorithm()) {
                    case droid -> BeatmapDifficultyCalculator.calculateDroidPerformance(
                        (DroidDifficultyAttributes) difficultyAttributes
                    );

                    case standard -> BeatmapDifficultyCalculator.calculateStandardPerformance(
                        (StandardDifficultyAttributes) difficultyAttributes
                    );
                };

                ppinfo.append(String.format(
                    Locale.ENGLISH,
                    "%.2f★ | %.2f/%.2f" + (Config.getDifficultyAlgorithm() == DifficultyAlgorithm.droid ? "dpp" : "pp"),
                    difficultyAttributes.starRating,
                    performanceAttributes.total,
                    maxPerformanceAttributes.total
                ));
            }

            if (stat.getUnstableRate() > 0) {
                if (beatmapData != null) {
                    ppinfo.append("\n");
                }
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
        scene.attachChild(beatmapInfoText);
        scene.attachChild(mapperInfo);
        scene.attachChild(playerInfo);

        // In case the scene was reloaded
        if (Multiplayer.isMultiplayer) {
            updateLeaderboard();
        }

        if (beatmap != null && beatmap.getMD5().equals(mapMD5)) {
            ResourceManager.getInstance().getSound("applause").play();

            // Save score
            if (stat.getTotalScoreWithMultiplier() <= 0 || mods.contains(ModAutoplay.class) ||
                    (Multiplayer.isMultiplayer &&
                        ((Multiplayer.room != null && Multiplayer.room.isTeamVersus()) || game.hasFailed))) {
                return;
            }

            int totalNotes = stat.getHit300() + stat.getHit100() + stat.getHit50() + stat.getMisses();

            // Do not save and submit score if note count does not match, since it indicates a corrupted score
            // (potentially from bugging the gameplay by any unnecessary means).
            if (totalNotes != beatmap.getTotalHitObjectCount()) {
                ToastLogger.showText(com.osudroid.resources.R.string.replay_corrupted, true);
                return;
            }

            stat.setReplayFilename(FilenameUtils.getName(replayPath));
            stat.setBeatmapMD5(beatmap.getMD5());

            try {
                DatabaseManager.getScoreInfoTable().insertScore(stat.toScoreInfo());
            } catch (Exception e) {
                Log.e("GameScene", "Failed to save score to database", e);
            }

            // Upload score
            if (!OnlineManager.getInstance().isStayOnline() ||
                    (Multiplayer.isMultiplayer && !Config.isSubmitScoreOnMultiplayer())) {
                return;
            }

            boolean hasUnrankedMod = SmartIterator.wrap(mods.values().iterator()).applyFilter(m -> !m.isRanked()).hasNext();

            if (hasUnrankedMod) {
                return;
            }

            var sendingPanel = new SendingPanel(OnlineManager.getInstance().getRank(),
                    OnlineManager.getInstance().getScore(), OnlineManager.getInstance().getAccuracy(),
                    OnlineManager.getInstance().getPP());

            scene.registerTouchArea(sendingPanel.getDismissTouchArea());
            scene.attachChild(sendingPanel);

            OnlineScoring.getInstance().sendRecord(beatmap, stat, sendingPanel, replayPath);
        }
    }

    public void updateLeaderboard() {

        if (Multiplayer.finalData != null) {

            if (selector != null) {
                var oldSelector = selector;
                Execution.updateThread(() -> {
                    oldSelector.detachSelf();
                    oldSelector.detachChildren();

                    if (scene != null)
                        scene.unregisterTouchArea(oldSelector);
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

        if (Multiplayer.isMultiplayer)
        {
            // Preventing NPEs when player gets disconnected while playing
            if (!Multiplayer.isConnected())
                RoomScene.INSTANCE.back();
            else
                RoomScene.INSTANCE.show();
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
            songService.preLoad(beatmapInfo.getAudioPath());
            menu.updateMusicEffects();
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
