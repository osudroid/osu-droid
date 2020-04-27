package ru.nsu.ccfit.zuev.osu;

import android.Manifest;
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
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.edlplan.framework.utils.functionality.SmartIterator;
import com.edlplan.ui.ActivityOverlay;
import com.tencent.bugly.Bugly;
import com.umeng.analytics.MobclickAgent;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.engine.camera.SmoothCamera;
import org.anddev.andengine.engine.options.EngineOptions;
import org.anddev.andengine.engine.options.resolutionpolicy.RatioResolutionPolicy;
import org.anddev.andengine.entity.scene.Scene;
import org.anddev.andengine.entity.util.FPSLogger;
import org.anddev.andengine.extension.input.touch.controller.MultiTouch;
import org.anddev.andengine.extension.input.touch.controller.MultiTouchController;
import org.anddev.andengine.input.touch.TouchEvent;
import org.anddev.andengine.opengl.view.RenderSurfaceView;
import org.anddev.andengine.sensor.accelerometer.AccelerometerData;
import org.anddev.andengine.sensor.accelerometer.IAccelerometerListener;
import org.anddev.andengine.ui.activity.BaseGameActivity;
import org.anddev.andengine.util.Debug;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import ru.nsu.ccfit.zuev.audio.BassAudioPlayer;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SaveServiceObject;
import ru.nsu.ccfit.zuev.audio.serviceAudio.SongService;
import ru.nsu.ccfit.zuev.osu.async.AsyncTaskLoader;
import ru.nsu.ccfit.zuev.osu.async.OsuAsyncCallback;
import ru.nsu.ccfit.zuev.osu.async.SyncTaskManager;
import ru.nsu.ccfit.zuev.osu.game.SpritePool;
import ru.nsu.ccfit.zuev.osu.helper.InputManager;
import ru.nsu.ccfit.zuev.osu.helper.StringTable;
import ru.nsu.ccfit.zuev.osu.menu.FilterMenu;
import ru.nsu.ccfit.zuev.osu.menu.LoadingScreen;
import ru.nsu.ccfit.zuev.osu.menu.ModMenu;
import ru.nsu.ccfit.zuev.osu.menu.SplashScene;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.osuplus.R;

public class MainActivity extends BaseGameActivity implements IAccelerometerListener {

    public static SongService songService;
    public ServiceConnection connection;
    public BroadcastReceiver onNotifyButtonClick;
    private PowerManager.WakeLock wakeLock = null;
    private String beatmapToAdd = null;
    private SaveServiceObject saveServiceObject;
    private IntentFilter filter;
    private boolean willReplay = false;

    @Override
    public Engine onLoadEngine() {
        GlobalManager.getInstance().setMainActivity(this);
        initialBuglySDK();
        checkPermissions();
        Config.loadConfig(this);
        StringTable.setContext(this);
        ToastLogger.init(this);
        SyncTaskManager.getInstance().init(this);
        InputManager.setContext(this);
        OnlineManager.getInstance().Init(getApplicationContext());
        setupOsuDroidDirectory();
        initialWakeLock();

        final EngineOptions opt = loadEngineOptions();
        final Engine engine = new Engine(opt);
        setupMultiTouchFeature(engine);
        GlobalManager.getInstance().setEngine(engine);
        return engine;
    }

    private void setupMultiTouchFeature(Engine engine) {
        if (MultiTouch.isSupported(this)) {
            engine.setTouchController(new MultiTouchController());
        } else {
            ToastLogger.showText(
                    StringTable.get(R.string.message_error_multitouch),
                    false);
        }
    }

    private EngineOptions loadEngineOptions() {
        return new EngineOptions(
                true, null,
                new RatioResolutionPolicy(Config.getRES_WIDTH(), Config.getRES_HEIGHT()),
                new SmoothCamera(0, 0,
                        Config.getRES_WIDTH(), Config.getRES_HEIGHT(),
                        0, 1800, 1)
        ){{
            setNeedsMusic(true);
            setNeedsSound(true);
            getRenderOptions().disableExtensionVertexBufferObjects();
            getTouchOptions().enableRunOnUpdateThread();
        }};
    }

    private void initialWakeLock() {
        final PowerManager manager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = manager != null ? manager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
                "osudroid:osu") : null;
    }

    private void initialBuglySDK() {
        // 初始化BuglySDK
        Bugly.init(getApplicationContext(), "d1e89e4311", false);
    }

    private void setupOsuDroidDirectory() {
        checkSongsDir();
        checkSkinDir();
    }

    private void checkSongsDir() {
        File dir = new File(Config.getBeatmapPath());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                dir = resetSongsDir();
            }
            createNomediaFile(dir);
        }
    }

    // Creating Osu/Skin directory if it doesn't exist
    private void checkSkinDir() {
        final File skinDir = new File(Config.getCorePath() + "/Skin");
        if (!skinDir.exists()) {
            skinDir.mkdirs();
        }
    }

    private File resetSongsDir() {
        Config.setBeatmapPath(Config.getCorePath() + "Songs/");
        File dir = new File(Config.getBeatmapPath());
        if (!(dir.exists() || dir.mkdirs())) {
            ToastLogger.showText(StringTable.format(
                    R.string.message_error_createdir, dir.getPath()),
                    true);
            // Songs文件夹创建失败，检查权限后重试
            checkPermissions();
            if (!(dir.exists() || dir.mkdirs())) {
                // 完全无法创建文件夹时直接抛出异常，退出
                // TODO: 在一个新的Activity中显示错误信息
                throw new RuntimeException("Unable to start the game");
            }
        } else {
            final SharedPreferences prefs = PreferenceManager
                    .getDefaultSharedPreferences(this);
            final Editor editor = prefs.edit();
            editor.putString("directory", dir.getPath());
            editor.commit();
        }
        return dir;
    }

    private void createNomediaFile(File dir) {
        try {
            new File(dir.getParentFile(), ".nomedia").createNewFile();
        } catch (final IOException e) {
            Debug.e("LibraryManager: " + e.getMessage(), e);
        }
    }

    private void initPreferences() {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        initialOfflineUserName(prefs);

        // TODO 确认是否是无效的代码
        /*if (!prefs.getBoolean("qualitySet", false)) {
            // 这里又是啥迷惑行为
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
        }*/
        /*if (!prefs.getBoolean("onlineSet", false)) {

            Editor editor = prefs.edit();
            editor.putBoolean("onlineSet", true);
            editor.commit();
            //TODO removed auto registration at first launch
            *//*OnlineInitializer initializer = new OnlineInitializer(this);
			initializer.createInitDialog();*//*
        }*/
    }

    private void initialOfflineUserName(SharedPreferences prefs) {
        if (prefs.getString("playername", "").equals("")) {
            final Editor editor = prefs.edit();
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
                    (dialog, whichButton) -> {
                        final String value = input.getText().toString();
                        editor.putString("playername", value);
                        editor.commit();
                    });

            alert.show();
        }
    }

    public void onLoadResources() {
        ResourceManager.getInstance().Init(mEngine, this);
        ResourceManager.getInstance().loadHighQualityAsset("logo", "logo.png");
        ResourceManager.getInstance().loadHighQualityAsset("play", "play.png");
        ResourceManager.getInstance().loadHighQualityAsset("exit", "exit.png");
        ResourceManager.getInstance().loadHighQualityAsset("options", "options.png");
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
        ResourceManager.getInstance().loadFont("font", null, 28, Color.WHITE);
        ResourceManager.getInstance().loadFont("smallFont", null, 21, Color.WHITE);
        ResourceManager.getInstance().loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE);

        BassAudioPlayer.initDevice();
    }

    public Scene onLoadScene() {
        if (BuildConfig.DEBUG){
            GlobalManager.getInstance().getEngine().registerUpdateHandler(new FPSLogger(1f));
        }
        return new SplashScene().getScene();
    }

    public void onLoadComplete() {
        new AsyncTaskLoader().execute(new OsuAsyncCallback() {
            public void run() {
                GlobalManager.getInstance().init();
                GlobalManager.getInstance().setLoadingProgress(50);
                checkBeatmapAndReplay();
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
                if (willReplay) {
                    GlobalManager.getInstance().getMainScene().watchReplay(beatmapToAdd);
                    willReplay = false;
                }
            }
        });
    }

    @SuppressLint("ResourceType")
    @Override
    protected void onSetContentView() {
        this.mRenderSurfaceView = new RenderSurfaceView(this);
        this.mRenderSurfaceView.setEGLConfigChooser(true);
        this.mRenderSurfaceView.setRenderer(this.mEngine);

        RelativeLayout layout = new RelativeLayout(this);
        layout.setBackgroundColor(Color.argb(255, 0, 0, 0));
        layout.addView(
                mRenderSurfaceView,
                new RelativeLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ) {{
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
    }

    public void checkBeatmapAndReplay() {
        GlobalManager.getInstance().setInfo("Checking new maps...");
        final File mainDir = new File(Config.getCorePath());
        if (beatmapToAdd != null) {
            File file = new File(beatmapToAdd);
            if (file.getName().endsWith(".osz")) {
                ToastLogger.showText(
                        StringTable.get(R.string.message_lib_importing),
                        false);
                loadBeatmapToAdd(beatmapToAdd);
            } else if (file.getName().endsWith(".odr")) {
                willReplay = true;
            }
        } else if (mainDir.exists() && mainDir.isDirectory()) {
            checkNewBeatmaps(mainDir);
        }
    }

    private void checkNewBeatmaps(File mainDir) {
        final List<String> beatmaps = searchAllOszToImport(mainDir);

        if (beatmaps.size() > 0) {
            ToastLogger.showText(StringTable.format(
                    R.string.message_lib_importing_several,
                    beatmaps.size()), false);
            for (final String s : beatmaps) {
                if (OSZParser.parseOSZ(MainActivity.this, s, true)) {
                    String folderName = s.substring(0, s.length() - 4);
                    // We have imported the beatmap!
                    ToastLogger.showText(
                            StringTable.format(R.string.message_lib_imported, folderName),
                            true);
                }
            }
            LibraryManager.getInstance().sort();
            LibraryManager.getInstance().savetoCache(MainActivity.this);
        }
    }

    @NotNull
    private List<String> searchAllOszToImport(File mainDir) {
        List<String> beatmaps = new ArrayList<>(searchFiles(mainDir, ".*\\.(osz|zip)"));

        File beatmapDir = new File(Config.getBeatmapPath());
        if (beatmapDir.exists() && beatmapDir.isDirectory()) {
            beatmaps.addAll(searchFiles(beatmapDir, ".*\\.(osz|zip)"));
        }

        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (Config.isSCAN_DOWNLOAD() && downloadDir.exists() && downloadDir.isDirectory()) {
            beatmaps.addAll(searchFiles(downloadDir, ".*\\.(osz)"));
        }
        return beatmaps;
    }

    private List<String> searchFiles(File dir, String reg) {
        return SmartIterator.ofArray(dir.listFiles(f -> f.isFile() && f.getName().matches(reg)))
        .applyFunction(File::getAbsolutePath)
        .collectAllAsList();
    }

    private void loadBeatmapToAdd(String beatmapToAdd) {
        if (OSZParser.parseOSZ(MainActivity.this, beatmapToAdd, true)) {
            String folderName = beatmapToAdd.substring(0, beatmapToAdd.length() - 4);
            // We have imported the beatmap!
            ToastLogger.showText(
                    StringTable.format(R.string.message_lib_imported, folderName),
                    true);
        }
        LibraryManager.getInstance().sort();
        LibraryManager.getInstance().savetoCache(MainActivity.this);
    }

    @Override
    protected void onCreate(Bundle pSavedInstanceState) {
        super.onCreate(pSavedInstanceState);

        if (BuildConfig.DEBUG) {
            exportDebugLog();
        }
        if (Config.isHideNaviBar()) {
            setUiOptions();
        }
        onBeginBindService();
    }

    private void exportDebugLog() {
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

    private void setUiOptions() {
        int newUiOptions = this.getWindow().getDecorView().getSystemUiVisibility();

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        // Immersive mode: Backward compatible to KitKat.
        // Note that this flag doesn't do anything by itself, it only augments the behavior
        // of HIDE_NAVIGATION and FLAG_FULLSCREEN.  For the purposes of this sample
        // all three flags are being toggled together.
        // Note that there are two immersive mode UI flags, one of which is referred to as "sticky".
        // Sticky immersive mode differs in that it makes the navigation and status bars
        // semi-transparent, and the UI flag does not get cleared when the user interacts with
        // the screen.
        if (Build.VERSION.SDK_INT >= 19) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
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
        if (GlobalManager.getInstance().getSkinNow() != null) {
            if (GlobalManager.getInstance().getSkinNow() != Config.getSkinPath()) {
                GlobalManager.getInstance().setSkinNow(Config.getSkinPath());
                ToastLogger.showText(StringTable.get(R.string.message_loading_skin), true);
                ResourceManager.getInstance().loadCustomSkin(Config.getSkinPath());
            }
        }
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
        MobclickAgent.onResume(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (GlobalManager.getInstance().getEngine() != null && GlobalManager.getInstance().getGameScene() != null
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            SpritePool.getInstance().purge();
            GlobalManager.getInstance().getGameScene().pause();
        }
        if (GlobalManager.getInstance().getMainScene() != null) {
            BeatmapInfo beatmapInfo = GlobalManager.getInstance().getMainScene().beatmapInfo;
            if (songService != null && beatmapInfo != null && !songService.isGaming() && !songService.isSettingMenu()) {
                songService.showNotifyPanel();

                if (wakeLock != null) {
                    wakeLock.acquire(10*60*1000L /*10 minutes*/);
                }

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
                if (songService != null) {
                    songService.pause();
                }
            }
        }
        MobclickAgent.onPause(this);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if (GlobalManager.getInstance().getEngine() != null
                && GlobalManager.getInstance().getGameScene() != null
                && !hasFocus
                && GlobalManager.getInstance().getEngine().getScene() == GlobalManager.getInstance().getGameScene().getScene()) {
            if (!GlobalManager.getInstance().getGameScene().isPaused()) {
                GlobalManager.getInstance().getGameScene().pause();
            }
        }
    }

//    @Override
//    public void onBackPressed() {
//        MainManager.getInstance().getMainScene().exit();
//    }

    @Override
    public void onAccelerometerChanged(final AccelerometerData arg0) {
        if (GlobalManager.getInstance().getCamera().getRotation() == 0 && arg0.getY() < -5) {
            GlobalManager.getInstance().getCamera().setRotation(180);
        } else if (GlobalManager.getInstance().getCamera().getRotation() == 180 && arg0.getY() > 5) {
            GlobalManager.getInstance().getCamera().setRotation(0);
        }
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        /*boolean superDown = super.onKeyDown(keyCode, event);
        if (superDown) {
            return superDown;
        }*/

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

                GlobalManager.getInstance().getMainScene().exit();
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
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

    private void checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Need permission to access osu!droid directory");
                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission
                                .WRITE_EXTERNAL_STORAGE}, 2333);
                    }
                });
                builder.show();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2333);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 2333: {
                for (int i = 0; i < permissions.length; i++) {
                    String permission = permissions[i];
                    if (Manifest.permission.WRITE_EXTERNAL_STORAGE.equals(permission)) {
                        if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                            boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, permission);
                            if (!showRationale) {// user denied flagging NEVER ASK AGAIN
                                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                                builder.setMessage("Game won't run without storage permission!\nPlease enable it manually.");
                                builder.setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, 2334);
                                    }
                                });
                                builder.setNegativeButton(R.string.dialog_cancel, null);
                                builder.show();

                            } else {
                                // user denied WITHOUT never ask again
                                // game will exit by exception
                            }
                        }
                    }
                }
            }
        }
    }
}
