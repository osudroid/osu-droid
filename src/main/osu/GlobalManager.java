package main.osu;

import android.util.DisplayMetrics;

import com.reco1l.Game;
import com.reco1l.ui.scenes.Scenes;
import com.reco1l.ui.scenes.loader.LoaderScene;
import com.reco1l.ui.scenes.selector.SelectorScene;
import com.reco1l.ui.scenes.main.MainScene;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;

import main.osu.game.GameScene;
import main.audio.serviceAudio.SaveServiceObject;
import main.audio.serviceAudio.SongService;
import main.osu.scoring.ScoreLibrary;
import com.reco1l.ui.scenes.summary.SummaryScene;

/**
 * Created by Fuuko on 2015/4/24.
 */
public class GlobalManager {
    private static GlobalManager instance;
    private Engine engine;
    private Camera camera;

    private GameScene gameScene;
    private MainScene mainScene;
    private SummaryScene scoring;
    private SelectorScene selectorScene;
    private LoaderScene loaderScene;

    private MainActivity mainActivity;
    private int loadingProgress;
    private String info;
    private SongService songService;
    private SaveServiceObject saveServiceObject;
    private String skinNow;

    public static GlobalManager getInstance() {
        if (instance == null) {
            instance = new GlobalManager();
        }
        return instance;
    }

    public void init() {
        Game.initialize();

        saveServiceObject = (SaveServiceObject) mainActivity.getApplication();
        songService = saveServiceObject.getSongService();
        Game.songService = songService;
        setMainScene(Scenes.main);
        skinNow = Config.getSkinPath();
        ResourceManager.getInstance().loadSkin(skinNow);
        ScoreLibrary.getInstance().load(mainActivity);
        PropertiesLibrary.getInstance().load(mainActivity);
        setGameScene(Scenes.player.getLegacyClass());
        setSongMenu(Scenes.selector);
        setScoring(Scenes.summary);
        setLoadingScene(Scenes.loader);
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

    public SummaryScene getScoring() {
        return scoring;
    }

    public void setScoring(SummaryScene scoring) {
        this.scoring = scoring;
    }

    public SelectorScene getSongMenu() {
        return selectorScene;
    }

    public void setSongMenu(SelectorScene selectorScene) {
        this.selectorScene = selectorScene;
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

    public LoaderScene getLoadingScene() {
        return loaderScene;
    }

    public void setLoadingScene(LoaderScene loaderScene) {
        this.loaderScene = loaderScene;
    }
}
