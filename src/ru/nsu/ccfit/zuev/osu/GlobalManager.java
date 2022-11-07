package ru.nsu.ccfit.zuev.osu;

import android.util.DisplayMetrics;

import com.reco1l.andengine.scenes.PlayerLoader;
import com.reco1l.andengine.scenes.SongMenu;
import com.reco1l.andengine.scenes.MainScene;
import com.reco1l.management.BitmapManager;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.game.GameScene;
import ru.nsu.ccfit.zuev.osu.scoring.ScoreLibrary;
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
    private TrackInfo selectedTrack;
    private SaveServiceObject saveServiceObject;
    private String skinNow;
    private PlayerLoader playerLoader;

    public static GlobalManager getInstance() {
        if (instance == null) {
            instance = new GlobalManager();
        }
        return instance;
    }

    public TrackInfo getSelectedTrack() {
        return selectedTrack;
    }

    public void setSelectedTrack(TrackInfo selectedTrack) {
        if (selectedTrack == null) return;
        this.selectedTrack = selectedTrack;
    }

    public void init() {
        saveServiceObject = (SaveServiceObject) mainActivity.getApplication();
        songService = saveServiceObject.getSongService();
        setLoadingProgress(10);
        setMainScene(new MainScene());
        setInfo("Loading skin...");
        skinNow = Config.getSkinPath();
        ResourceManager.getInstance().loadSkin(skinNow);
        BitmapManager.getInstance().loadAssets(skinNow);
        ScoreLibrary.getInstance().load(mainActivity);
        setLoadingProgress(20);
        PropertiesLibrary.getInstance().load(mainActivity);
        setLoadingProgress(30);
        setGameScene(new GameScene(getEngine()));
        setSongMenu(new SongMenu());
        setPlayerLoader(new PlayerLoader());
        setLoadingProgress(40);
        setScoring(new ScoringScene(getEngine(), getGameScene(), null)); // TODO SCORING
        getGameScene().setScoringScene(getScoring());
        getGameScene().setOldScene(getSongMenu());
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

    public DisplayMetrics getDisplayMetrics() {
        final DisplayMetrics dm = new DisplayMetrics();
        mainActivity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        return dm;
    }

    public PlayerLoader getPlayerLoader() {
        return playerLoader;
    }

    public void setPlayerLoader(PlayerLoader playerLoader) {
        this.playerLoader = playerLoader;
    }
}
