package ru.nsu.ccfit.zuev.osu;

import android.Manifest;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.*;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.core.content.PermissionChecker;

import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import android.widget.Toast;
import com.edlplan.ui.ActivityOverlay;
import com.edlplan.ui.fragment.ConfirmDialogFragment;
import com.edlplan.ui.fragment.BuildTypeNoticeFragment;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.Camera;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.extension.input.touch.exception.MultiTouchException;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.security.Security;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.lingala.zip4j.ZipFile;

import pub.devrel.easypermissions.AppSettingsDialog;
import pub.devrel.easypermissions.EasyPermissions;
import ru.nsu.ccfit.zuev.audio.BassAudioPlayer;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.SpritePool;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.InputManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.FilterMenu;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SettingsMenu;
import ru.nsu.ccfit.zuev.osu.menu.SplashScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MainActivity extends BaseGameActivity implements
        IAccelerometerListener {
    public static SongService songService;
    public ServiceConnection connection;
    public BroadcastReceiver onNotifyButtonClick;
    private PowerManager.WakeLock wakeLock = null;
    private String beatmapToAdd = null;
    private SaveServiceObject saveServiceObject;
    private IntentFilter filter;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private FirebaseAnalytics analytics;
    private FirebaseCrashlytics crashlytics;
    private boolean willReplay = false;
    private static boolean activityVisible = true;
    private boolean autoclickerDialogShown = false;

    @Override
    public Engine onLoadEngine() {
        if (!checkPermissions()) {
            return null;
        }
        analytics = FirebaseAnalytics.getInstance(this);
        crashlytics = FirebaseCrashlytics.getInstance();
        Config.loadConfig(this);
        initialGameDirectory();
        //Debug.setDebugLevel(Debug.DebugLevel.NONE);
        StringTable.setContext(this);
        ToastLogger.init(this);
        SyncTaskManager.getInstance().init(this);
        InputManager.setContext(this);
        OnlineManager.getInstance().Init(getApplicationContext());
        crashlytics.setUserId(Config.getOnlineDeviceID());

        final DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
/*        final double screenSize = Math.sqrt(Utils.sqr(dm.widthPixels / dm.xdpi)
                + Utils.sqr(dm.heightPixels / dm.ydpi));*/
        double screenInches = Math.sqrt(Math.pow(dm.heightPixels, 2) + Math.pow(dm.widthPixels, 2)) / (dm.density * 160.0f);
        Debug.i("screen inches: " + screenInches);
        Config.setScaleMultiplier((float) ((11 - 5.2450170716245195) / 5));

        Config.setTextureQuality(1);
        final PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "osudroid:osu");

        Camera mCamera = new SmoothCamera(0, 0, Config.getRES_WIDTH(),
                Config.getRES_HEIGHT(), 0, 1800, 1);
        final EngineOptions opt = new EngineOptions(true,
                null, new RatioResolutionPolicy(
                Config.getRES_WIDTH(), Config.getRES_HEIGHT()),
                mCamera);
        opt.setNeedsMusic(true);
        opt.setNeedsSound(true);
        opt.getRenderOptions().disableExtensionVertexBufferObjects();
        opt.getTouchOptions().enableRunOnUpdateThread();
        final Engine engine = new Engine(opt);
        try {
            if (MultiTouch.isSupported(this)) {
                engine.setTouchController(new MultiTouchController());
            } else {
                ToastLogger.showText(
                        StringTable.get(R.string.message_error_multitouch),
                        false);
            }
        } catch (final MultiTouchException e) {
            ToastLogger.showText(
                    StringTable.get(R.string.message_error_multitouch),
                    false);
        }
        GlobalManager.getInstance().setCamera(mCamera);
        GlobalManager.getInstance().setEngine(engine);
        GlobalManager.getInstance().setMainActivity(this);
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
                            R.string.message_error_createdir, dir.getPath()),
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

        if (prefs.getString("playername", "").equals("")) {
            final SharedPreferences.Editor editor = prefs.edit();
            editor.putString("playername", "Guest");
            editor.commit();

            final AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setTitle(StringTable.get(R.string.dialog_playername_title));
            alert.setMessage(StringTable
                    .get(R.string.dialog_playername_message));

            final EditText input = new EditText(this);
            input.setText("Guest");
            alert.setView(input);

            alert.setPositiveButton(StringTable.get(R.string.dialog_ok),
                    new DialogInterface.OnClickListener() {

                        public void onClick(final DialogInterface dialog,
                                            final int whichButton) {
                            final String value = input.getText().toString();
                            editor.putString("playername", value);
                            editor.commit();
                        }
                    });

            alert.show();
        }

        if (prefs.getBoolean("qualitySet", false) == false) {
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

        if (prefs.getBoolean("onlineSet", false) == false) {

            Editor editor = prefs.edit();
            editor.putBoolean("onlineSet", true);
            editor.commit();

            //TODO removed auto registration at first launch
            /*OnlineInitializer initializer = new OnlineInitializer(this);
            initializer.createInitDialog();*/
        }
    }

    @Override
    public void onLoadResources() {
        Config.setTextureQuality(1);
        ResourceManager.getInstance().Init(mEngine, this);
        ResourceManager.getInstance().loadHighQualityAsset("logo", "logo.png");
        ResourceManager.getInstance().loadHighQualityAsset("play", "play.png");
        //ResourceManager.getInstance().loadHighQualityAsset("multiplayer", "multiplayer.png");
        //ResourceManager.getInstance().loadHighQualityAsset("solo", "solo.png");
        ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadHighQualityAsset("options", "options.png");
        ResourceManager.getInstance().loadHighQualityAsset("offline-avatar", "offline-avatar.png");
        ResourceManager.getInstance().loadHighQualityAsset("star", "gfx/star.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_play", "music_play.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_pause", "music_pause.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_stop", "music_stop.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_next", "music_next.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_prev", "music_prev.png");
        ResourceManager.getInstance().loadHighQualityAsset("music_np", "music_np.png");
        ResourceManager.getInstance().loadHighQualityAsset("songselect-top", "songselect-top.png");
        File bg;
        if ((bg = new File(Config.getSkinPath() + "menu-background.png")).exists()
                || (bg = new File(Config.getSkinPath() + "menu-background.jpg")).exists()) {
            ResourceManager.getInstance().loadHighQualityFile("menu-background", bg);
        }
        // ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);
        ResourceManager.getInstance().loadFont("smallFont", null, 21, Color.WHITE);
        ResourceManager.getInstance().loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE);

        BassAudioPlayer.initDevice();

    }

    @Override
    public Scene onLoadScene() {
        return new SplashScene().getScene();
    }

    @Override
    public void onLoadComplete() {
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            public void run() {
                GlobalManager.getInstance().init();
                analytics.logEvent(FirebaseAnalytics.Event.APP_OPEN, null);
                GlobalManager.getInstance().setLoadingProgress(50);
                Config.loadSkins();
                checkNewSkins();
                checkNewBeatmaps();
                if (!LibraryManager.getInstance().loadLibraryCache(MainActivity.this, true)) {
                    LibraryManager.getInstance().scanLibrary(MainActivity.this);
                    System.gc();
                }
            }

            public void onComplete() {  
                GlobalManager.getInstance().setInfo("");
                GlobalManager.getInstance().setLoadingProgress(100);
                ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);
                GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
                GlobalManager.getInstance().getMainScene().loadBeatmap();
                initPreferences();
                availableInternalMemory();
                initAccessibilityDetector();
                if (willReplay) {
                    GlobalManager.getInstance().getMainScene().watchReplay(beatmapToAdd);
                    willReplay = false;
                }
            }
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
        String toastMessage = String.format(StringTable.get(R.string.message_low_storage_space), df.format(availableMemory / minMem));
        if(availableMemory < 0.5 * minMem) { //I set 512MiB as a minimum
            Toast.makeText(this, toastMessage, Toast.LENGTH_SHORT).show();
        }
        Debug.i("Free Space: " + df.format(availableMemory / minMem));
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onSetContentView() {
        this.mRenderSurfaceView = new RenderSurfaceView(this);
        if(Config.isUseDither()) {
            this.mRenderSurfaceView.setEGLConfigChooser(8,8,8,8,24,0);
            this.mRenderSurfaceView.getHolder().setFormat(PixelFormat.RGBA_8888);
        } else {
            this.mRenderSurfaceView.setEGLConfigChooser(true);
        }
        this.mRenderSurfaceView.setRenderer(this.mEngine);

        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.argb(255, 0, 0, 0));
        layout.addView(
                mRenderSurfaceView,
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT){{
                    addRule(RelativeLayout.CENTER_IN_PARENT);
                }});

        FrameLayout frameLayout = new FrameLayout(this);
        frameLayout.setId(0x28371);
        layout.addView(frameLayout, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        View c = new View(this);
        c.setBackgroundColor(Color.argb(0, 0, 0, 0));
        layout.addView(c, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        this.setContentView(
                layout,
                new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT) {{
                    gravity = Gravity.CENTER;
                }});

        ActivityOverlay.initial(this, frameLayout.getId());

        if ("pre_release".equals(BuildConfig.BUILD_TYPE) || BuildConfig.DEBUG) {
            BuildTypeNoticeFragment.single.get().show();
        }
    }

    public void checkNewBeatmaps() {
        GlobalManager.getInstance().setInfo("Checking for new maps...");
        final File mainDir = new File(Config.getCorePath());
        if (beatmapToAdd != null) {
            File file = new File(beatmapToAdd);
            if (file.getName().toLowerCase().endsWith(".osz")) {
                ToastLogger.showText(
                        StringTable.get(R.string.message_lib_importing),
                        false);

                if(FileUtils.extractZip(beatmapToAdd, Config.getBeatmapPath())) {
                    String folderName = beatmapToAdd.substring(0, beatmapToAdd.length() - 4);
                    // We have imported the beatmap!
                    ToastLogger.showText(
                            StringTable.format(R.string.message_lib_imported, folderName),
                            true);
                }

                // LibraryManager.getInstance().sort();
                LibraryManager.getInstance().savetoCache(MainActivity.this);
            } else if (file.getName().endsWith(".odr")) {
                willReplay = true;
            }
        } else if (mainDir.exists() && mainDir.isDirectory()) {
            File[] filelist = FileUtils.listFiles(mainDir, ".osz");
            final ArrayList<String> beatmaps = new ArrayList<String>();
            for (final File file : filelist) {
                ZipFile zip = new ZipFile(file);
                if(zip.isValidZipFile()) {
                    beatmaps.add(file.getPath());
                }
            }

            File beatmapDir = new File(Config.getBeatmapPath());
            if (beatmapDir.exists()
                    && beatmapDir.isDirectory()) {
                filelist = FileUtils.listFiles(beatmapDir, ".osz");
                for (final File file : filelist) {
                    ZipFile zip = new ZipFile(file);
                    if(zip.isValidZipFile()) {
                        beatmaps.add(file.getPath());
                    }
                }
            }

            File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (Config.isSCAN_DOWNLOAD()
                    && downloadDir.exists()
                    && downloadDir.isDirectory()) {
                filelist = FileUtils.listFiles(downloadDir, ".osz");
                for (final File file : filelist) {
                    ZipFile zip = new ZipFile(file);
                    if(zip.isValidZipFile()) {
                        beatmaps.add(file.getPath());
                    }
                }
            }

            if (beatmaps.size() > 0) {
                // final boolean deleteOsz = Config.isDELETE_OSZ();
                // Config.setDELETE_OSZ(true);
                ToastLogger.showText(StringTable.format(
                        R.string.message_lib_importing_several,
                        beatmaps.size()), false);
                for (final String beatmap : beatmaps) {
                    if(FileUtils.extractZip(beatmap, Config.getBeatmapPath())) {
                        String folderName = beatmap.substring(0, beatmap.length() - 4);
                        // We have imported the beatmap!
                        ToastLogger.showText(
                                StringTable.format(R.string.message_lib_imported, folderName),
                                true);
                    }
                }
                // Config.setDELETE_OSZ(deleteOsz);

                // LibraryManager.getInstance().sort();
                LibraryManager.getInstance().savetoCache(MainActivity.this);
            }
        }
    }

    public void checkNewSkins() {
        GlobalManager.getInstance().setInfo("Checking new skins...");

        final ArrayList<String> skins = new ArrayList<>();

        // Scanning skin directory
        final File skinDir = new File(Config.getSkinTopPath());

        if (skinDir.exists() && skinDir.isDirectory()) {
            final File[] files = FileUtils.listFiles(skinDir, ".osk");

            for (final File file : files) {
                ZipFile zip = new ZipFile(file);
                if(zip.isValidZipFile()) {
                    skins.add(file.getPath());
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
                ZipFile zip = new ZipFile(file);
                if(zip.isValidZipFile()) {
                    skins.add(file.getPath());
                }
            }
        }

        if (skins.size() > 0) {
            ToastLogger.showText(StringTable.format(
                    R.string.message_skin_importing_several,
                    skins.size()), false);

            for (final String skin : skins) {
                if (FileUtils.extractZip(skin, Config.getSkinTopPath())) {
                    String folderName = skin.substring(0, skin.length() - 4);
                    // We have imported the skin!
                    ToastLogger.showText(
                            StringTable.format(R.string.message_lib_imported, folderName),
                            true);
                    Config.addSkin(folderName.substring(folderName.lastIndexOf("/") + 1), skin);
                }
            }
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public FirebaseAnalytics getAnalytics() {
        return analytics;
    }

    public PowerManager.WakeLock getWakeLock() {
        return wakeLock;
    }

    public static boolean isActivityVisible() {
        return activityVisible;
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);
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
            } catch (IOException e) {
            }
        }
        onBeginBindService();
    }

    public void onCreateNotifyReceiver() {
        if (filter == null) {
            //过滤器创建
            filter = new IntentFilter();
            filter.addAction("Notify_cancel");
        }

        //按钮广播监听
        onNotifyButtonClick = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("Notify_cancel")) {
                    songService.stop();
                    GlobalManager.getInstance().getMainScene().exit();
                }
            }
        };
        registerReceiver(onNotifyButtonClick, filter);
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
            if (Build.VERSION.SDK_INT > 10) {
                onCreateNotifyReceiver();
            }
        }
        GlobalManager.getInstance().setSongService(songService);
        GlobalManager.getInstance().setSaveServiceObject(saveServiceObject);
    }

    @Override
    protected void onStart() {
//        this.enableAccelerometerSensor(this);
        if (getIntent().getAction() != null
                && getIntent().getAction().equals(Intent.ACTION_VIEW)) {
            if (ContentResolver.SCHEME_FILE.equals(getIntent().getData().getScheme())) {
                beatmapToAdd = getIntent().getData().getPath();
            }
            if (BuildConfig.DEBUG) {
                System.out.println(getIntent());
                System.out.println(getIntent().getData().getEncodedPath());
            }
        }
        super.onStart();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (this.mEngine == null) {
            return;
        }
        activityVisible = true;
        if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getGameScene() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            GlobalManager.getInstance().getEngine().getTextureManager().reloadTextures();
        }
        if (GlobalManager.getInstance().getMainScene() != null) {
            if (songService != null && Build.VERSION.SDK_INT > 10) {
                if (songService.hideNotifyPanel()) {
                    if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
                    GlobalManager.getInstance().getMainScene().loadBeatmapInfo();
                    GlobalManager.getInstance().getMainScene().loadTimeingPoints(false);
                    GlobalManager.getInstance().getMainScene().progressBar.setTime(songService.getLength());
                    GlobalManager.getInstance().getMainScene().progressBar.setPassedTime(songService.getPosition());
                    GlobalManager.getInstance().getMainScene().musicControl(MainScene.MusicOption.SYNC);
                }
            }
        }
    }
    
    @Override
    public void onPause() {
        super.onPause();
        activityVisible = false;
        if (this.mEngine == null) {
            return;
        }
        if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getGameScene() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            SpritePool.getInstance().purge();
            GlobalManager.getInstance().getGameScene().pause();
        }
        if (GlobalManager.getInstance().getMainScene() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getMainScene().beatmapInfo;
            if (songService != null && beatmapInfo != null && !songService.isGaming()/* && !songService.isSettingMenu()*/) {
                if (Build.VERSION.SDK_INT > 10) {
                    songService.showNotifyPanel();

                    if (wakeLock == null) {
                        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "osudroid:MainActivity");
                    }
                    wakeLock.acquire();

                    if (beatmapInfo.getArtistUnicode() != null && beatmapInfo.getTitleUnicode() != null) {
                        songService.updateTitleText(beatmapInfo.getTitleUnicode(), beatmapInfo.getArtistUnicode());
                    } else if (beatmapInfo.getArtist() != null && beatmapInfo.getTitle() != null) {
                        songService.updateTitleText(beatmapInfo.getTitle(), beatmapInfo.getArtist());
                    } else {
                        songService.updateTitleText("QAQ I cant load info", " ");
                    }
                    songService.updateCoverImage(beatmapInfo.getTrack(0).getBackground());
                    songService.updateStatus();
                } else {
                    songService.stop();
                }
            } else {
                if (songService != null) {
                    songService.pause();
                }
            }
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
        if (GlobalManager.getInstance().getEngine() != null
                && GlobalManager.getInstance().getGameScene() != null
                && !hasFocus
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            if (!GlobalManager.getInstance().getGameScene().isPaused()) {
                GlobalManager.getInstance().getGameScene().pause();
            }
        }
        if (hasFocus && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && Config.isHideNaviBar()) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
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

        if(autoclickerDialogShown) {
            return false;
        }

        if (event.getAction() != KeyEvent.ACTION_DOWN) {
            return super.onKeyDown(keyCode, event);
        }
        if (GlobalManager.getInstance().getEngine() == null) {
            return super.onKeyDown(keyCode, event);
        }

        if (event.getAction() == TouchEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_BACK && ActivityOverlay.onBackPress()) {
            return true;
        }

        if (GlobalManager.getInstance().getGameScene() != null
                && (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_MENU)
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            if (GlobalManager.getInstance().getGameScene().isPaused()) {
                GlobalManager.getInstance().getGameScene().resume();
            } else {
                GlobalManager.getInstance().getGameScene().pause();
            }
            return true;
        }
        if (GlobalManager.getInstance().getScoring() != null && keyCode == KeyEvent.KEYCODE_BACK
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getScoring().getScene()) {
            GlobalManager.getInstance().getScoring().replayMusic();
            GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getSongMenu().getScene());
            GlobalManager.getInstance().getSongMenu().updateScore();
            ResourceManager.getInstance().getSound("applause").stop();
            GlobalManager.getInstance().getScoring().setReplayID(-1);
            return true;
        }
        if ((keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_ENTER)
                && GlobalManager.getInstance().getEngine() != null
                && GlobalManager.getInstance().getSongMenu() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene()
                && GlobalManager.getInstance().getSongMenu().getScene().hasChildScene()) {
            if (FilterMenu.getInstance().getClass() == FilterMenu.class) {
                if (GlobalManager.getInstance().getSongMenu().getScene().getChildScene() == FilterMenu.getInstance()
                        .getScene()) {
                    if (keyCode == KeyEvent.KEYCODE_ENTER) {
                        InputManager.getInstance().toggleKeyboard();
                    }
                    FilterMenu.getInstance().hideMenu();
                }
            }

            /*if (GlobalManager.getInstance().getSongMenu().getScene().getChildScene() == PropsMenu.getInstance()
                    .getScene()) {
                PropsMenu.getInstance().saveChanges();
                if (keyCode == KeyEvent.KEYCODE_ENTER) {
                    InputManager.getInstance().toggleKeyboard();
                }
            }*/

            if (GlobalManager.getInstance().getSongMenu().getScene().getChildScene() == ModMenu.getInstance().getScene()) {
                ModMenu.getInstance().hide();
            }

            return true;
        }
        if (GlobalManager.getInstance().getSongMenu() != null && GlobalManager.getInstance().getEngine() != null
                && keyCode == KeyEvent.KEYCODE_MENU
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene()
                && GlobalManager.getInstance().getSongMenu().getScene().hasChildScene() == false) {
            GlobalManager.getInstance().getSongMenu().stopScroll(0);
            GlobalManager.getInstance().getSongMenu().showPropertiesMenu(null);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getSongMenu() != null &&
                    GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getSongMenu().getScene()) {

                //SongMenu 界面按返回按钮（系统按钮）
                GlobalManager.getInstance().getSongMenu().back();
            } else {

                if (GlobalManager.getInstance().getEngine().getScene() instanceof LoadingScreen.LoadingScene) {
                    return true;
                }

                GlobalManager.getInstance().getMainScene().showExitDialog();
            }
            return true;
        }

        if (InputManager.getInstance().isStarted()) {
            if (keyCode == KeyEvent.KEYCODE_DEL) {
                InputManager.getInstance().pop();
            } else if (keyCode != KeyEvent.KEYCODE_ENTER) {
                final char c = (char) event.getUnicodeChar();
                if (c != 0) {
                    InputManager.getInstance().append(c);
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void forcedExit() {
        if(GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            GlobalManager.getInstance().getGameScene().quit();
        }
        GlobalManager.getInstance().getEngine().setScene(GlobalManager.getInstance().getMainScene().getScene());
        GlobalManager.getInstance().getMainScene().exit();
    }

    private void initAccessibilityDetector() {
        ScheduledExecutorService scheduledExecutorService =
            Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService
            .scheduleAtFixedRate(() -> {
                AccessibilityManager manager = (AccessibilityManager)
                    getSystemService(Context.ACCESSIBILITY_SERVICE);
                List<AccessibilityServiceInfo> activeServices = new ArrayList<AccessibilityServiceInfo>(
                    manager.getEnabledAccessibilityServiceList(
                        AccessibilityServiceInfo.FEEDBACK_ALL_MASK));

                for(AccessibilityServiceInfo activeService : activeServices) {
                     int capabilities = activeService.getCapabilities();
                    if((AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES & capabilities)
                            == AccessibilityServiceInfo.CAPABILITY_CAN_PERFORM_GESTURES) {
                        if(!autoclickerDialogShown && activityVisible) {
                            runOnUiThread(() -> {
                                ConfirmDialogFragment dialog = new ConfirmDialogFragment()
                                    .setMessage(R.string.message_autoclicker_detected);
                                dialog.setOnDismissListener(() -> forcedExit());
                                dialog.showForResult(isAccepted -> forcedExit());
                            });
                            autoclickerDialogShown = true;
                        }
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
    }

    public long getVersionCode() {
        long versionCode = 0;
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(
                getPackageName(), 0);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = packageInfo.getLongVersionCode();
            }else {
                versionCode = packageInfo.versionCode;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Debug.e("PackageManager: " + e.getMessage(), e);
        }
        return versionCode;
    }

    public float getRefreshRate() {
        return ((WindowManager) getSystemService(Context.WINDOW_SERVICE))
            .getDefaultDisplay()
            .getRefreshRate();
    }

    private boolean checkPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()) {
            return true;
        }else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
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
