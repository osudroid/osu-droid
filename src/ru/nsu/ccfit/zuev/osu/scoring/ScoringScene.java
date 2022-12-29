package ru.nsu.ccfit.zuev.osu.scoring;

import com.edlplan.framework.utils.functionality.SmartIterator;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.sprite.Sprite;
import org.anddev.andengine.input.touch.TouchEvent;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.GlobalManager;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.TrackInfo;
import ru.nsu.ccfit.zuev.osu.Utils;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;

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

    public void load(StatisticV2 stat, TrackInfo track, SongService player, String replay, String mapMD5, TrackInfo trackToReplay) {

        final Sprite replayBtn = new Sprite(Utils.toRes(580), Utils.toRes(400),
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
            scene.attachChild(perfect);
        }
        if (track != null) {
            /*retryBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - retryBtn.getHeight() - 10);
            scene.attachChild(retryBtn);*/
        } else if (replay != null) {
            /*replayBtn.setPosition(Config.getRES_WIDTH() - backBtn.getWidth() - 10, backBtn.getY() - replayBtn.getHeight() - 10);
            scene.attachChild(replayBtn);*/
        }

        scene.setTouchAreaBindingEnabled(true);
        if (track != null) {
            //scene.registerTouchArea(retryBtn);
        } else if (replay != null) {
            scene.registerTouchArea(replayBtn);
        }

        /*if (stat.getChangeSpeed() != 1 ||
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
        }*/

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
