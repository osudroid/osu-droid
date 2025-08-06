package ru.nsu.ccfit.zuev.osu;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.StatFs;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Surface;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.Toast;

import androidx.core.content.PermissionChecker;
import androidx.preference.PreferenceManager;

import com.edlplan.ui.ActivityOverlay;
import com.osudroid.BuildSettings;
import com.osudroid.debug.DebugPlaygroundScene;
import com.osudroid.ui.v2.GameLoaderScene;
import com.osudroid.utils.Execution;
import com.reco1l.andengine.ExtendedEngine;
import com.osudroid.multiplayer.api.LobbyAPI;
import com.osudroid.utils.AccessibilityDetector;
import com.osudroid.beatmaps.DifficultyCalculationManager;
import com.osudroid.multiplayer.Multiplayer;
import com.osudroid.UpdateManager;
import com.osudroid.multiplayer.LobbyScene;
import com.osudroid.multiplayer.RoomScene;

import com.osudroid.ui.v2.modmenu.ModMenu;
import com.rian.osu.difficulty.BeatmapDifficultyCalculator;
import net.lingala.zip4j.ZipFile;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.WakeLockOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.SplashScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MainActivity extends BaseGameActivity implements
        IAccelerometerListener {

    public static String versionName;
    public static SongService songService;
    public ServiceConnection connection;
    private String beatmapToAdd = null;
    private SaveServiceObject saveServiceObject;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean willReplay = false;
    private static boolean activityVisible = true;
    private static final ScheduledExecutorService scheduledExecutor = Executors.newSingleThreadScheduledExecutor();
    private Display display;
    private float maxRefreshRate = 60;

    // Multiplayer
    private Uri roomInviteLink;

    @Override
    public Engine onLoadEngine() {
        if (!checkPermissions()) {
            return null;
        }
        Config.loadConfig(this);
        initialGameDirectory();
        //Debug.setDebugLevel(Debug.DebugLevel.NONE);
        StringTable.setContext(this);
        ToastLogger.init(this);
        OnlineManager.getInstance().init();

        final DisplayMetrics dm = new DisplayMetrics();
        display = getWindowManager().getDefaultDisplay();
        display.getMetrics(dm);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            for (var mode : display.getSupportedModes()) {
                for (float rate : mode.getAlternativeRefreshRates()) {
                    maxRefreshRate = Math.max(maxRefreshRate, rate);
                }
            }
        }

        Camera mCamera = new SmoothCamera(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT(), 0, 1800, 1);
        final EngineOptions opt = new EngineOptions(true,
                null, new RatioResolutionPolicy(
                Config.getRES_WIDTH(), Config.getRES_HEIGHT()),
                mCamera);
        opt.setNeedsMusic(true);
        opt.setNeedsSound(true);
        opt.setWakeLockOptions(WakeLockOptions.SCREEN_DIM);
        opt.getRenderOptions().disableExtensionVertexBufferObjects();
        opt.getTouchOptions().enableRunOnUpdateThread();
        final Engine engine = new ExtendedEngine(this, opt);

        if (!MultiTouch.isSupported(this)) {
            // Warning player that they will have to single tap forever.
            ToastLogger.showText(StringTable.get(com.osudroid.resources.R.string.message_info_multitouch), false);
        }
        engine.setTouchController(new MultiTouchController());

        GlobalManager.getInstance().setCamera(mCamera);
        GlobalManager.getInstance().setEngine(engine);
        return GlobalManager.getInstance().getEngine();
    }

    private void initialGameDirectory() {
        File dir = new File(Config.getBeatmapPath());
        // Creating Osu directory if it doesn't exist
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Config.setBeatmapPath(Config.getCorePath() + "Songs/");
                dir = new File(Config.getBeatmapPath());
                if (!(dir.exists() || dir.mkdirs())) {
                    ToastLogger.showText(StringTable.format(
                                    com.osudroid.resources.R.string.message_error_createdir, dir.getPath()),
                            true);
                } else {
                    final SharedPreferences prefs = PreferenceManager
                            .getDefaultSharedPreferences(this);
                    final SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("directory", dir.getPath());
                    editor.commit();
                }

            }
            final File nomedia = new File(dir.getParentFile(), ".nomedia");
            try {
                nomedia.createNewFile();
            } catch (final IOException e) {
                Debug.e("LibraryManager: " + e.getMessage(), e);
            }
        }

        final File skinDir = new File(Config.getCorePath() + "/Skin");
        // Creating Osu/Skin directory if it doesn't exist
        if (!skinDir.exists()) {
            skinDir.mkdirs();
        }
    }

    private void initPreferences() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        if (!prefs.getBoolean("qualitySet", false)) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("qualitySet", true);
            final DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(dm);

            if (dm.densityDpi > DisplayMetrics.DENSITY_MEDIUM) {
                editor.putBoolean("lowtextures", false);
            } else {
                editor.putBoolean("lowtextures", false);
            }
            editor.commit();
        }

        if (!prefs.getBoolean("onlineSet", false)) {

            Editor editor = prefs.edit();
            editor.putBoolean("onlineSet", true);
            editor.commit();
        }
    }

    @Override
    public void onLoadResources() {
        ResourceManager.getInstance().Init(mEngine, this);
        ResourceManager.getInstance().loadHighQualityAsset("welcome", "gfx/welcome.png");
        ResourceManager.getInstance().loadHighQualityAsset("loading_start", "gfx/loading.png");

        ResourceManager.getInstance().loadSound("welcome", "sfx/welcome.ogg", false);
        ResourceManager.getInstance().loadSound("welcome_piano", "sfx/welcome_piano.ogg", false);

        // Setting the scene as fast as we can
        getEngine().setScene(SplashScene.INSTANCE.getScene());

        ResourceManager.getInstance().loadHighQualityAsset("logo", "logo.png");
        ResourceManager.getInstance().loadHighQualityAsset("play", "play.png");
        ResourceManager.getInstance().loadHighQualityAsset("solo", "solo.png");
        ResourceManager.getInstance().loadHighQualityAsset("multi", "multi.png");
        ResourceManager.getInstance().loadHighQualityAsset("back", "back.png");
        ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadHighQualityAsset("beatmap_downloader", "beatmap_downloader.png");
        ResourceManager.getInstance().loadHighQualityAsset("options", "options.png");
        ResourceManager.getInstance().loadHighQualityAsset("offline-avatar", "offline-avatar.png");
        ResourceManager.getInstance().loadHighQualityAsset("star", "gfx/star.png");
        ResourceManager.getInstance().loadHighQualityAsset("chat", "chat.png");
        ResourceManager.getInstance().loadHighQualityAsset("team_vs", "team_vs.png");
        ResourceManager.getInstance().loadHighQualityAsset("head_head", "head_head.png");
        ResourceManager.getInstance().loadHighQualityAsset("crown", "crown.png");
        ResourceManager.getInstance().loadHighQualityAsset("missing", "missing.png");
        ResourceManager.getInstance().loadHighQualityAsset("lock", "lock.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_play", "music_play.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_pause", "music_pause.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_stop", "music_stop.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_next", "music_next.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_prev", "music_prev.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_np", "music_np.png");
        ResourceManager.getInstance().loadHighQualityAsset("songselect-top", "songselect-top.png");
        ResourceManager.getInstance().loadHighQualityAsset("back-arrow", "back-arrow.png");
        ResourceManager.getInstance().loadHighQualityAsset("reset", "reset.png");
        ResourceManager.getInstance().loadHighQualityAsset("check", "check.png");
        ResourceManager.getInstance().loadHighQualityAsset("plus", "plus.png");
        ResourceManager.getInstance().loadHighQualityAsset("minus", "minus.png");

        File bg;
        if ((bg = new File(Config.getSkinPath() + "menu-background.png")).exists()
                || (bg = new File(Config.getSkinPath() + "menu-background.jpg")).exists()) {
            ResourceManager.getInstance().loadHighQualityFile("menu-background", bg);
        }
        // ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);
        ResourceManager.getInstance().loadFont("smallFont", null, 21, Color.WHITE);
        ResourceManager.getInstance().loadFont("xs", null, 16, Color.WHITE);
        ResourceManager.getInstance().loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE);

        ResourceManager.getInstance().loadSound("heartbeat", "sfx/heartbeat.ogg", false);
    }

    @Override
    public Scene onLoadScene() {
        if (BuildSettings.DEBUG_PLAYGROUND) {
            return DebugPlaygroundScene.INSTANCE;
        }
        return SplashScene.INSTANCE.getScene();
    }

    @Override
    public void onLoadComplete() {

        // Initializing this class because they contain fragments in its constructors that should be initialized in
        // main thread because of the Looper.
        LobbyScene.INSTANCE.init();
        RoomScene.INSTANCE.init();

        Execution.async(() -> {
            GlobalManager.getInstance().init();
            GlobalManager.getInstance().setLoadingProgress(50);
            checkNewSkins();
            Config.loadSkins();
            DifficultyCalculationManager.checkForOutdatedStarRatings();
            loadBeatmapLibrary();

            SplashScene.INSTANCE.playWelcomeAnimation();

            Execution.delayed(2500, () -> {

                UpdateManager.onActivityStart();
                GlobalManager.getInstance().setInfo("");
                GlobalManager.getInstance().setLoadingProgress(100);
                ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);

                if (!BuildSettings.DEBUG_PLAYGROUND) {
                    GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
                }

                GlobalManager.getInstance().getMainScene().loadBeatmap();
                initPreferences();
                availableInternalMemory();

                scheduledExecutor.scheduleAtFixedRate(() -> {
                    if (Config.isForceMaxRefreshRate() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        float refreshRate = getRefreshRate();

                        if (refreshRate != maxRefreshRate) {
                            mRenderSurfaceView.getHolder().getSurface().setFrameRate(maxRefreshRate, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT);
                        }
                    }

                    AccessibilityDetector.check(MainActivity.this);
                    BeatmapDifficultyCalculator.invalidateExpiredCache();
                }, 0, 100, TimeUnit.MILLISECONDS);

                if (roomInviteLink != null) {
                    LobbyScene.INSTANCE.connectFromLink(roomInviteLink);
                } else if (willReplay) {
                    GlobalManager.getInstance().getMainScene().watchReplay(beatmapToAdd);
                    willReplay = false;
                }

                GlobalManager.getInstance().getMainScene().loadBannerSprite();
            });
        });
    }

    /*
    Accuracy isn't the best, but it's sufficient enough
    to determine whether storage is low or not
     */
    private void availableInternalMemory() {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.HALF_EVEN);

        double availableMemory;
        double minMem = 1073741824D; //1 GiB = 1073741824 bytes
        File internal = Environment.getDataDirectory();
        StatFs stat = new StatFs(internal.getPath());
        availableMemory = (double) stat.getAvailableBytes();
        String toastMessage = String.format(StringTable.get(com.osudroid.resources.R.string.message_low_storage_space), df.format(availableMemory / minMem));
        if (availableMemory < 0.5 * minMem) { //I set 512MiB as a minimum
            Execution.mainThread(() -> Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show());
        }
        Debug.i("Free Space: " + df.format(availableMemory / minMem));
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onSetContentView() {
        this.mRenderSurfaceView = new RenderSurfaceView(this);
        this.mRenderSurfaceView.setEGLConfigChooser(8, 8, 8, 8, 24, 0);
        this.mRenderSurfaceView.getHolder().setFormat(PixelFormat.RGB_888);
        this.mRenderSurfaceView.setRenderer(this.mEngine);

        RelativeLayout mainLayout = new RelativeLayout(this);
        mainLayout.setBackgroundColor(Color.BLACK);
        mainLayout.addView(mRenderSurfaceView, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(View.generateViewId());

        mainLayout.addView(frameLayout, new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        // Adding a dummy View somehow fixes an issue with Android layout system where the layouts
        // in the frame layout (where fragments are attached, see ActivityOverlay) are not properly
        // displayed on the screen.
        mainLayout.addView(new View(this), new LayoutParams(MATCH_PARENT, MATCH_PARENT));

        setContentView(mainLayout, new FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT));
        ActivityOverlay.initial(this, frameLayout.getId());
    }

    public void loadBeatmapLibrary() {
        GlobalManager.getInstance().setInfo("Checking for new maps...");
        final File mainDir = new File(Config.getCorePath());
        if (beatmapToAdd != null) {
            File file = new File(beatmapToAdd);
            if (file.getName().toLowerCase().endsWith(".osz")) {
                ToastLogger.showText(
                        StringTable.get(com.osudroid.resources.R.string.library_importing),
                        false);

                FileUtils.extractZip(beatmapToAdd, Config.getBeatmapPath());
                // LibraryManager.INSTANCE.sort();
            } else if (file.getName().endsWith(".odr")) {
                willReplay = true;
            }
        } else if (mainDir.exists() && mainDir.isDirectory()) {
            File[] filelist = FileUtils.listFiles(mainDir, ".osz");
            final ArrayList<String> beatmaps = new ArrayList<>();
            for (final File file : filelist) {
                try (var zip = new ZipFile(file)) {
                    if (zip.isValidZipFile()) {
                        beatmaps.add(file.getPath());
                    }
                } catch (IOException ignored) {
                }
            }

            File beatmapDir = new File(Config.getBeatmapPath());
            if (beatmapDir.exists()
                    && beatmapDir.isDirectory()) {
                filelist = FileUtils.listFiles(beatmapDir, ".osz");
                for (final File file : filelist) {
                    try (var zip = new ZipFile(file)) {
                        if (zip.isValidZipFile()) {
                            beatmaps.add(file.getPath());
                        }
                    } catch (IOException ignored) {
                    }
                }
            }

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (Config.isSCAN_DOWNLOAD()
                    && downloadDir.exists()
                    && downloadDir.isDirectory()) {
                filelist = FileUtils.listFiles(downloadDir, ".osz");
                for (final File file : filelist) {
                    try (var zip = new ZipFile(file)) {
                        if (zip.isValidZipFile()) {
                            beatmaps.add(file.getPath());
                        }
                    } catch (IOException ignored) {
                    }
                }
            }

            if (beatmaps.size() > 0) {
                // final boolean deleteOsz = Config.isDELETE_OSZ();
                // Config.setDELETE_OSZ(true);
                ToastLogger.showText(StringTable.format(
                        com.osudroid.resources.R.string.library_importing_several,
                        beatmaps.size()), false);
                for (final String beatmap : beatmaps) {
                    FileUtils.extractZip(beatmap, Config.getBeatmapPath());
                }
                // Config.setDELETE_OSZ(deleteOsz);

                // LibraryManager.INSTANCE.sort();
            }
        }

        LibraryManager.scanDirectory();
        LibraryManager.loadLibrary();
    }

    public void checkNewSkins() {
        GlobalManager.getInstance().setInfo("Checking new skins...");

        final ArrayList<String> skins = new ArrayList<>();

        // Scanning skin directory
        final File skinDir = new File(Config.getSkinTopPath());

        if (skinDir.exists() && skinDir.isDirectory()) {
            final File[] files = FileUtils.listFiles(skinDir, ".osk");

            for (final File file : files) {
                try (var zip = new ZipFile(file)) {
                    if (zip.isValidZipFile()) {
                        skins.add(file.getPath());
                    }
                } catch (IOException ignored) {
                }
            }
        }

        // Scanning download directory
        final File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        if (Config.isSCAN_DOWNLOAD()
                && downloadDir.exists()
                && downloadDir.isDirectory()) {
            final File[] files = FileUtils.listFiles(downloadDir, ".osk");

            for (final File file : files) {
                try (var zip = new ZipFile(file)) {
                    if (zip.isValidZipFile()) {
                        skins.add(file.getPath());
                    }
                } catch (IOException ignored) {
                }
            }
        }

        if (skins.size() > 0) {
            ToastLogger.showText(StringTable.format(
                    com.osudroid.resources.R.string.library_skin_importing_several,
                    skins.size()), false);

            for (final String skin : skins) {
                if (FileUtils.extractZip(skin, Config.getSkinTopPath())) {
                    String folderName = skin.substring(0, skin.length() - 4);
                    // We have imported the skin!
                    ToastLogger.showText(
                            StringTable.format(com.osudroid.resources.R.string.library_imported, folderName),
                            true);
                    Config.addSkin(folderName.substring(folderName.lastIndexOf("/") + 1), skin);
                }
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        // Some components may already start using this class when onCreate is called. An example
        // is when the game is restoring after being killed by system due to low system memory.
        GlobalManager.getInstance().setMainActivity(this);

        super.onCreate(pSavedInstanceState);

        try {
            versionName = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES).versionName;
        } catch (Exception ignored) {
        }

        if (this.mEngine == null) {
            return;
        }

        if (BuildConfig.DEBUG) {
            //Toast.makeText(this,"this is debug version",Toast.LENGTH_LONG).show();
            try {
                File d = new File(Environment.getExternalStorageDirectory(), "osu!droid/Log");
                if (!d.exists()) d.mkdirs();
                File f = new File(d, "rawlog.txt");
                if (!f.exists()) f.createNewFile();
                Runtime.getRuntime().exec("logcat -f " + (f.getAbsolutePath()));
            } catch (IOException ignored) {
            }
        }
        onBeginBindService();
    }

    public void onBeginBindService() {
        if (connection == null && songService == null) {
            connection = new ServiceConnection() {
                @Override
                public void onServiceConnected(ComponentName name, IBinder service) {
                    songService = ((SongService.ReturnBindObject) service).getObject();
                    saveServiceObject = (SaveServiceObject) getApplication();
                    saveServiceObject.setSongService(songService);
                    GlobalManager.getInstance().setSongService(songService);
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {

                }

            };

            bindService(new Intent(MainActivity.this, SongService.class), connection, BIND_AUTO_CREATE);
        }
        GlobalManager.getInstance().setSongService(songService);
        GlobalManager.getInstance().setSaveServiceObject(saveServiceObject);
    }

    @Override
    protected void onStart() {
        if (getIntent().getAction() != null && getIntent().getAction().equals(Intent.ACTION_VIEW)) {

            var data = getIntent().getData();

            if (data != null) {

                if (data.toString().startsWith(LobbyAPI.INVITE_HOST))
                    roomInviteLink = data;

                if (ContentResolver.SCHEME_FILE.equals(getIntent().getData().getScheme()))
                    beatmapToAdd = getIntent().getData().getPath();
            }
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mEngine == null) {
            return;
        }

        activityVisible = true;

        var gameScene = GlobalManager.getInstance().getGameScene();
        var mainScene = GlobalManager.getInstance().getMainScene();

        if (gameScene != null && mEngine.getScene() == gameScene.getScene()) {
            mEngine.getTextureManager().reloadTextures();
        }

        if (mainScene != null && songService != null) {
            mainScene.loadBeatmapInfo();
            mainScene.loadTimingPoints(false);
            mainScene.progressBar.setTime(songService.getLength());
            mainScene.progressBar.setPassedTime(songService.getPosition());
            mainScene.musicControl(MainScene.MusicOption.SYNC);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        activityVisible = false;

        if (mEngine == null) {
            return;
        }

        var gameScene = GlobalManager.getInstance().getGameScene();

        if (gameScene != null && mEngine.getScene() == gameScene.getScene()) {
            if (Multiplayer.isMultiplayer) {
                ToastLogger.showText("You've left the match.", true);
                gameScene.quit();
                Multiplayer.log("Player left the match.");
            } else {
                gameScene.pause();
            }
        }

        if (songService != null) {
            songService.pause();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        activityVisible = false;
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (this.mEngine == null) {
            return;
        }

        if (getEngine() != null && !hasFocus) {

            if (GlobalManager.getInstance().getGameScene() != null
                    && getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()
                    && GlobalManager.getInstance().getGameScene() != null) {

                if (!GlobalManager.getInstance().getGameScene().isPaused() && !Multiplayer.isMultiplayer)
                    GlobalManager.getInstance().getGameScene().pause();
            }

            if (Multiplayer.isConnected()
                    && (getEngine().getScene() == RoomScene.INSTANCE
                    || getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene())) {
                Execution.async(() -> Execution.runSafe(RoomScene.INSTANCE::invalidateStatus));
            }
        }

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onAccelerometerChanged(final AccelerometerData arg0) {
        if (this.mEngine == null) {
            return;
        }
        if (GlobalManager.getInstance().getCamera().getRotation() == 0 && arg0.getY() < -5) {
            GlobalManager.getInstance().getCamera().setRotation(180);
        } else if (GlobalManager.getInstance().getCamera().getRotation() == 180 && arg0.getY() > 5) {
            GlobalManager.getInstance().getCamera().setRotation(0);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (this.mEngine == null) {
            return false;
        }

        if (AccessibilityDetector.isIllegalServiceDetected())
            return false;

        if (GlobalManager.getInstance().getEngine() == null) {
            return super.onKeyDown(keyCode, event);
        }

        if (ExtendedEngine.getCurrent().onKeyPress(keyCode, event)) {
            return true;
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event);
        }

        Scene currentScene = GlobalManager.getInstance().getEngine().getScene();

        if (event.getAction() == TouchEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && ActivityOverlay.onBackPress()) {
            return true;
        }

        var gameScene = GlobalManager.getInstance().getGameScene();

        if (gameScene != null && currentScene == gameScene.getScene() &&
                (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)) {
            if (gameScene.isPaused()) {
                gameScene.resume();
            } else {
                gameScene.pause();
            }
            return true;
        }

        var scoringScene = GlobalManager.getInstance().getScoring();

        if (scoringScene != null && keyCode == KeyEvent.KEYCODE_BACK && currentScene == scoringScene.getScene()) {
            scoringScene.back();
            return true;
        }

        var songMenu = GlobalManager.getInstance().getSongMenu();

        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER)
                && songMenu != null && currentScene == songMenu.getScene() && songMenu.getScene().hasChildScene()) {
            var searchBar = songMenu.getSearchBar();

            if (searchBar != null && songMenu.getScene().getChildScene() == searchBar.getScene()) {
                searchBar.hideMenu();
            }

            if (songMenu.getScene().getChildScene() == ModMenu.INSTANCE) {
                ModMenu.INSTANCE.back();
            }

            return true;
        }

        if (songMenu != null && keyCode == KeyEvent.KEYCODE_MENU
                && currentScene == songMenu.getScene() && !songMenu.getScene().hasChildScene()) {
            songMenu.stopScroll(0);
            songMenu.showPropertiesMenu(null);
            return true;
        }

        if (keyCode == KeyEvent.KEYCODE_BACK && songMenu != null) {
            if (currentScene == songMenu.getScene()) {
                //SongMenu 界面按返回按钮（系统按钮）
                songMenu.back();
            } else {
                if (currentScene instanceof LoadingScreen.LoadingScene) {
                    return true;
                }

                if (Multiplayer.isMultiplayer) {
                    if (currentScene == LobbyScene.INSTANCE) {
                        LobbyScene.INSTANCE.back();
                        return true;
                    }

                    if (currentScene == RoomScene.INSTANCE) {

                        if (RoomScene.INSTANCE.hasChildScene() && RoomScene.INSTANCE.getChildScene() == ModMenu.INSTANCE) {
                            ModMenu.INSTANCE.back();
                            return true;
                        }
                        runOnUiThread(RoomScene.INSTANCE.getLeaveDialog()::show);
                        return true;
                    }
                } else if (currentScene instanceof GameLoaderScene) {
                    if (gameScene != null) {
                        gameScene.cancelLoading();
                    }

                    GlobalManager.getInstance().getEngine().setScene(songMenu.getScene());
                    return true;
                }

                GlobalManager.getInstance().getMainScene().showExitDialog();
            }
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }

    public void forcedExit() {
        if (GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            GlobalManager.getInstance().getGameScene().quit();
        }
        GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
        GlobalManager.getInstance().getMainScene().exit();
    }

    public long getVersionCode() {
        long versionCode = 0;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                    getPackageName(), 0);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            } else {
                versionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Debug.e("PackageManager: " + e.getMessage(), e);
        }
        return versionCode;
    }

    public float getRefreshRate() {
        return display.getRefreshRate();
    }

    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()) {
            return true;
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                PermissionChecker.checkCallingOrSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        == PermissionChecker.PERMISSION_GRANTED) {
            return true;
        } else {
            Intent grantPermission = new Intent(this, PermissionActivity.class);
            startActivity(grantPermission);
            overridePendingTransition(R.anim.fast_activity_swap, R.anim.fast_activity_swap);
            finish();
            return false;
        }
    }

}
