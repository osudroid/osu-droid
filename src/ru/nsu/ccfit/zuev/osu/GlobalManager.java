package ru.nsu.ccfit.zuev.osu;

import com.acivev.ui.menu.main.MainMenuV2;
import com.osudroid.data.BeatmapInfo;
import com.osudroid.data.DatabaseManager;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.UIEngine;

import org.andengine.engine.camera.Camera;

import java.util.concurrent.CountDownLatch;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class GlobalManager {
    private static GlobalManager instance;
    private UIEngine engine;
    private Camera camera;
    private GameScene gameScene;
    private MainScene mainScene;
    private MainMenuV2 mainMenuV2;
    private ScoringScene scoring;
    private SongMenu songMenu;
    private MainActivity mainActivity;
    private int loadingProgress;
    private String info;
    private SongService songService;
    private BeatmapInfo selectedBeatmap;
    private SaveServiceObject saveServiceObject;

    public static GlobalManager getInstance() {
        if (instance == null) {
            instance = new GlobalManager();
        }
        return instance;
    }

    public BeatmapInfo getSelectedBeatmap() {
        return selectedBeatmap;
    }

    public void setSelectedBeatmap(BeatmapInfo selectedBeatmap) {
        this.selectedBeatmap = selectedBeatmap;
    }

    public void init() {
        DatabaseManager.load(mainActivity);
        saveServiceObject = (SaveServiceObject) mainActivity.getApplication();
        songService = saveServiceObject.getSongService();
        SongService.initBASS();
        setLoadingProgress(10);
        setMainScene(new MainScene());
        setInfo("Loading skin...");
        ResourceManager.getInstance().loadSkin(Config.getSkinPath());
        // Construct MainMenuV2 after loadSkin() so its eager getTexture()/getFont()
        // calls resolve skin-provided resources rather than fallbacks.
        // Construction is posted to the update thread because MainMenuV2's init block
        // performs scene-graph mutations (attachChild) that must not run off-thread.
        // The latch ensures init() does not return until mainMenuV2 is ready,
        // so callers cannot observe a null mainMenuV2 after init() completes.
        CountDownLatch latch = new CountDownLatch(1);
        Execution.updateThread(() -> {
            setMainMenuV2(new MainMenuV2());
            setLoadingProgress(30);
            latch.countDown();
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        setGameScene(new GameScene(getEngine()));
        setSongMenu(new SongMenu());
        setLoadingProgress(40);
        getSongMenu().init(mainActivity, getEngine(), getGameScene());
        getSongMenu().load();
        setScoring(new ScoringScene(getEngine(), getGameScene(), getSongMenu()));
        getSongMenu().setScoringScene(getScoring());
        getGameScene().setScoringScene(getScoring());
        getGameScene().setOldScene(getSongMenu().getScene());
        if (songService != null) {
            songService.stop();
        }
    }

    public UIEngine getEngine() {
        return engine;
    }

    public void setEngine(UIEngine engine) {
        this.engine = engine;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public GameScene getGameScene() {
        return gameScene;
    }

    public void setGameScene(GameScene gameScene) {
        this.gameScene = gameScene;
    }

    public MainScene getMainScene() {
        return mainScene;
    }

    public void setMainScene(MainScene mainScene) {
        this.mainScene = mainScene;
    }

    public MainMenuV2 getMainMenuV2() {
        return mainMenuV2;
    }

    public void setMainMenuV2(MainMenuV2 mainMenuV2) {
        this.mainMenuV2 = mainMenuV2;
    }

    public ScoringScene getScoring() {
        return scoring;
    }

    public void setScoring(ScoringScene scoring) {
        this.scoring = scoring;
    }

    public SongMenu getSongMenu() {
        return songMenu;
    }

    public void setSongMenu(SongMenu songMenu) {
        this.songMenu = songMenu;
    }

    public MainActivity getMainActivity() {
        return mainActivity;
    }

    public void setMainActivity(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    public int getLoadingProgress() {
        return loadingProgress;
    }

    public void setLoadingProgress(int loadingProgress) {
        this.loadingProgress = loadingProgress;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public SongService getSongService() {
        return songService;
    }

    public void setSongService(SongService songService) {
        this.songService = songService;
    }

    public SaveServiceObject getSaveServiceObject() {
        return saveServiceObject;
    }

    public void setSaveServiceObject(SaveServiceObject saveServiceObject) {
        this.saveServiceObject = saveServiceObject;
    }

}
