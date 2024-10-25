package ru.nsu.ccfit.zuev.osu;

import com.reco1l.osu.data.BeatmapInfo;
import com.reco1l.osu.data.DatabaseManager;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SongMenu;
import ru.nsu.ccfit.zuev.osu.scoring.ScoringScene;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class GlobalManager {
    private static GlobalManager instance;
    private Engine engine;
    private Camera camera;
    private GameScene gameScene;
    private MainScene mainScene;
    private ScoringScene scoring;
    private SongMenu songMenu;
    private MainActivity mainActivity;
    private int loadingProgress;
    private String info;
    private SongService songService;
    private BeatmapInfo selectedBeatmap;
    private SaveServiceObject saveServiceObject;
    private String skinNow;

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
        getMainScene().load(mainActivity);
        setInfo("Loading skin...");
        skinNow = Config.getSkinPath();
        ResourceManager.getInstance().loadSkin(skinNow);
        setLoadingProgress(30);
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
            songService.hideNotification();
        }
    }

    public Engine getEngine() {
        return engine;
    }

    public void setEngine(Engine engine) {
        this.engine = engine;
    }

    public String getSkinNow() {
        return skinNow;
    }

    public void setSkinNow(String skinNow) {
        this.skinNow = skinNow;
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
