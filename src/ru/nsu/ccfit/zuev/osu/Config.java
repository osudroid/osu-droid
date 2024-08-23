package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import com.edlplan.favorite.FavoriteLibrary;
import com.edlplan.framework.math.FMath;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.reco1l.osu.multiplayer.Multiplayer;
import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.anddev.andengine.util.Debug;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class Config {
    private static String corePath,
        defaultCorePath,
        beatmapPath,
        cachePath,
        skinPath,
        skinTopPath,
        scorePath,
        onlineUsername,
        onlinePassword,
        onlineDeviceID;

    private static boolean DELETE_OSZ,
        SCAN_DOWNLOAD,
        deleteUnimportedBeatmaps,
        showFirstApproachCircle,
        comboburst,
        useCustomSkins,
        useCustomSounds,
        corovans,
        showFPS,
        showAverageOffset,
        showUnstableRate,
        animateFollowCircle,
        animateComboText,
        snakingInSliders,
        playMusicPreview,
        showCursor,
        shrinkPlayfieldDownwards,
        hideNaviBar,
        showScoreboard,
        enablePP,
        enableExtension,
        loadAvatar,
        stayOnline,
        syncMusic,
        burstEffects,
        hitLighting,
        useParticles,
        useCustomComboColors,
        forceRomanized,
        fixFrameOffset,
        removeSliderLock,
        calculateSliderPathInGameStart,
        displayScoreStatistics,
        hideReplayMarquee,
        hideInGameUI,
        receiveAnnouncements,
        enableStoryboard,
        safeBeatmapBg,
        displayRealTimePPCounter,
        useNightcoreOnMultiplayer,
        videoEnabled,
        deleteUnsupportedVideos,
        submitScoreOnMultiplayer,
        keepBackgroundAspectRatio,
        noChangeDimInBreaks;

    private static int RES_WIDTH,
        RES_HEIGHT,
        errorMeter,
        spinnerStyle,
        metronomeSwitch;
    
    private static float soundVolume,
        bgmVolume,
        offset,
        backgroundBrightness,
        scaleMultiplier,
        playfieldSize,
        cursorSize;

    private static DifficultyAlgorithm difficultyAlgorithm;

    private static Map<String, String> skins;

    private static RGBColor[] comboColors;
    private static Context context;


    /**
     * Shared preferences for the application.
     */
    private static SharedPreferences preferences;


    public static void loadConfig(final Context context) {
        Config.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        // graphics
        useCustomSkins = preferences.getBoolean("skin", false);
        useCustomSounds = preferences.getBoolean("beatmapSounds", true);
        comboburst = preferences.getBoolean("comboburst", false);
        corovans = preferences.getBoolean("images", false);
        showFPS = preferences.getBoolean("fps", true);
        showAverageOffset = preferences.getBoolean("averageOffset", true);
        showUnstableRate = preferences.getBoolean("unstableRate", true);
        errorMeter = Integer.parseInt(preferences.getString("errormeter", "0"));
        spinnerStyle = Integer.parseInt(preferences.getString("spinnerstyle", "0"));
        showFirstApproachCircle = preferences.getBoolean("showfirstapproachcircle", false);
        metronomeSwitch = Integer.parseInt(preferences.getString("metronomeswitch", "1"));
        showScoreboard = preferences.getBoolean("showscoreboard", true);
        enableStoryboard = preferences.getBoolean("enableStoryboard", false);
        videoEnabled = preferences.getBoolean("enableVideo", false);
        keepBackgroundAspectRatio = preferences.getBoolean("keepBackgroundAspectRatio", false);
        noChangeDimInBreaks = preferences.getBoolean("noChangeDimInBreaks", false);

        setSize();
        setPlayfieldSize(preferences.getInt("playfieldSize", 100) / 100f);

        shrinkPlayfieldDownwards = preferences.getBoolean("shrinkPlayfieldDownwards", true);
        animateFollowCircle = preferences.getBoolean("animateFollowCircle", true);
        animateComboText = preferences.getBoolean("animateComboText", true);
        snakingInSliders = preferences.getBoolean("snakingInSliders", true);

        try {
            offset = (int) FMath.clamp(preferences.getInt("offset", 0), -250, 250);
            backgroundBrightness = preferences.getInt("bgbrightness", 25) / 100f;
            soundVolume = preferences.getInt("soundvolume", 100) / 100f;
            bgmVolume = preferences.getInt("bgmvolume", 100) / 100f;
            cursorSize = preferences.getInt("cursorSize", 50) / 100f;
        }catch(RuntimeException e) { // use valid integer since this makes the game crash on android m
            preferences.edit()
                .putInt("offset", 0)
                .putInt("bgbrightness", 25)
                .putInt("soundvolume", 100)
                .putInt("bgmvolume", 100)
                .putInt("cursorSize", 50)
                .commit();
            Config.loadConfig(context);
            return;
        }

        //advanced
        defaultCorePath = Environment.getExternalStorageDirectory() + "/osu!droid/";
        corePath = preferences.getString("corePath", defaultCorePath);
        if (corePath.length() == 0) {
            corePath = defaultCorePath;
        }
        if (corePath.charAt(corePath.length() - 1) != '/') {
            corePath += "/";
        }
        scorePath = corePath + "Scores/";

        skinPath = preferences.getString("skinPath", corePath + "Skin/");
        if (skinPath.length() == 0) {
            skinPath = corePath + "Skin/";
        }
        if (skinPath.charAt(skinPath.length() - 1) != '/') {
            skinPath += "/";
        }

        skinTopPath = preferences.getString("skinTopPath", skinPath);
        if (skinTopPath.length() == 0) {
            skinTopPath = skinPath;
        }
        if (skinTopPath.charAt(skinTopPath.length() - 1) != '/') {
            skinTopPath += "/";
        }

        syncMusic = preferences.getBoolean("syncMusic", syncMusic);
        enableExtension = false;// prefs.getBoolean("enableExtension", false);
        cachePath = context.getCacheDir().getPath();
        burstEffects = preferences.getBoolean("bursts", burstEffects);
        hitLighting = preferences.getBoolean("hitlighting", hitLighting);
        useParticles = preferences.getBoolean("particles", useParticles);
        useCustomComboColors = preferences.getBoolean("useCustomColors", useCustomComboColors);
        comboColors = new RGBColor[4];
        for (int i = 1; i <= 4; i++) {
            comboColors[i - 1] = RGBColor.hex2Rgb(ColorPickerPreference.convertToRGB(preferences.getInt("combo" + i, 0xff000000)));
        }

        // beatmaps
        DELETE_OSZ = preferences.getBoolean("deleteosz", true);
        SCAN_DOWNLOAD = preferences.getBoolean("scandownload", false);
        deleteUnimportedBeatmaps = preferences.getBoolean("deleteUnimportedBeatmaps", false);
        forceRomanized = preferences.getBoolean("forceromanized", false);
        beatmapPath = preferences.getString("directory", corePath + "Songs/");
        if (beatmapPath.length() == 0) {
            beatmapPath = corePath + "Songs/";
        }
        if (beatmapPath.charAt(beatmapPath.length() - 1) != '/') {
            beatmapPath += "/";
        }
        deleteUnsupportedVideos = preferences.getBoolean("deleteUnsupportedVideos", true);

        // other
        playMusicPreview = preferences.getBoolean("musicpreview", true);
        showCursor = preferences.getBoolean("showcursor", false);
        hideNaviBar = preferences.getBoolean("hidenavibar", false);
        enablePP = false;//prefs.getBoolean("enablePP",true);
        fixFrameOffset = preferences.getBoolean("fixFrameOffset", true);
        removeSliderLock = preferences.getBoolean("removeSliderLock", false);
        calculateSliderPathInGameStart = preferences.getBoolean("calculateSliderPathInGameStart", false);
        displayScoreStatistics = preferences.getBoolean("displayScoreStatistics", false);
        hideReplayMarquee = preferences.getBoolean("hideReplayMarquee", false);
        hideInGameUI = preferences.getBoolean("hideInGameUI", false);
        receiveAnnouncements = preferences.getBoolean("receiveAnnouncements", true);
        safeBeatmapBg = preferences.getBoolean("safebeatmapbg", false);
        displayRealTimePPCounter = preferences.getBoolean("displayRealTimePPCounter", false);
        difficultyAlgorithm = DifficultyAlgorithm.droid;

        // Multiplayer
        useNightcoreOnMultiplayer = preferences.getBoolean("player_nightcore", false);
        submitScoreOnMultiplayer = preferences.getBoolean("player_submitScore", true);

        if(receiveAnnouncements) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements");
        }else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements"); 
        }

        //Init
        onlineDeviceID = preferences.getString("installID", null);
        if (onlineDeviceID == null) {
            onlineDeviceID = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            Editor editor = preferences.edit();
            editor.putString("installID", onlineDeviceID);
            editor.putString("corePath", corePath);
            editor.putString("skinTopPath", skinTopPath);
            editor.putString("skinPath", skinPath);
            editor.commit();
        }

        loadOnlineConfig();
        FavoriteLibrary.get().load();
    }

    public static void loadOnlineConfig() {
        onlineUsername = preferences.getString("onlineUsername", "");
        onlinePassword = preferences.getString("onlinePassword", null);
        stayOnline = preferences.getBoolean("stayOnline", false);
        loadAvatar = preferences.getBoolean("loadAvatar",false);
    }

    public static void setSize() {
        final DisplayMetrics dm = new DisplayMetrics();
        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(dm);

        int width = Math.max(dm.widthPixels, dm.heightPixels), height = Math.min(dm.widthPixels, dm.heightPixels);
        //int width = dm.widthPixels, height =  dm.heightPixels;
        setSize(width, height);
        //ToastLogger.showText("width=" + dm.widthPixels + " height=" + dm.heightPixels, true);
        Debug.i("width=" + dm.widthPixels + " height=" + dm.heightPixels);
    }

    public static void setSize(int width, int height) {
        RES_WIDTH = 1280;
        RES_HEIGHT = 1280 * height / width;
    }

    public static boolean isEnableStoryboard() {
        return backgroundBrightness > 0.02 && enableStoryboard;
    }

    public static void setEnableStoryboard(boolean enableStoryboard) {
        Config.enableStoryboard = enableStoryboard;
    }

    public static boolean isFixFrameOffset() {
        return fixFrameOffset;
    }

    public static boolean isRemoveSliderLock() {
        //noinspection DataFlowIssue
        return Multiplayer.isConnected() ? Multiplayer.room.getGameplaySettings().isRemoveSliderLock() : removeSliderLock;
    }

    public static boolean isCalculateSliderPathInGameStart() {
        return calculateSliderPathInGameStart;
    }

    public static boolean isDisplayScoreStatistics() {
        return displayScoreStatistics;
    }

    public static boolean isDisplayRealTimePPCounter() {
        return displayRealTimePPCounter;
    }

    public static DifficultyAlgorithm getDifficultyAlgorithm() {
        return difficultyAlgorithm;
    }

    public static void setDifficultyAlgorithm(DifficultyAlgorithm algorithm) {
        Config.difficultyAlgorithm = algorithm;
    }

    public static boolean isEnableExtension() {
        return enableExtension;
    }

    public static void setEnableExtension(boolean enableExtension) {
        Config.enableExtension = enableExtension;
    }

    public static boolean isShowFPS() {
        return showFPS;
    }

    public static void setShowFPS(final boolean showFPS) {
        Config.showFPS = showFPS;
    }

    public static boolean isShowAverageOffset() {
        return showAverageOffset;
    }

    public static void setShowAverageOffset(final boolean showAverageOffset) {
        Config.showAverageOffset = showAverageOffset;
    }

    public static boolean isShowUnstableRate() {
        return showUnstableRate;
    }

    public static void setShowUnstableRate(final boolean showUnstableRate) {
        Config.showUnstableRate = showUnstableRate;
    }

    public static boolean isShowScoreboard() {
        return showScoreboard;
    }

    public static void setShowScoreboard(final boolean showScoreboard) {
        Config.showScoreboard = showScoreboard;
    }

    public static boolean isCorovans() {
        return corovans;
    }

    public static void setCorovans(final boolean corovans) {
        Config.corovans = corovans;
    }

    public static float getSoundVolume() {
        return soundVolume;
    }

    public static void setSoundVolume(final float volume) {
        Config.soundVolume = volume;
    }

    public static float getBgmVolume() {
        return bgmVolume;
    }

    public static void setBgmVolume(float bgmVolume) {
        Config.bgmVolume = bgmVolume;
    }

    public static float getOffset() {
        return offset;
    }

    public static void setOffset(final float offset) {
        Config.offset = offset;
    }

    public static String getCorePath() {
        return corePath;
    }

    public static void setCorePath(final String corePath) {
        Config.corePath = corePath;
    }

    public static String getBeatmapPath() {
        return beatmapPath;
    }

    public static void setBeatmapPath(final String path) {
        beatmapPath = path;
    }

    public static int getRES_WIDTH() {
        return RES_WIDTH;
    }

    public static void setRES_WIDTH(final int rES_WIDTH) {
        RES_WIDTH = rES_WIDTH;
    }

    public static int getRES_HEIGHT() {
        return RES_HEIGHT;
    }

    public static void setRES_HEIGHT(final int rES_HEIGHT) {
        RES_HEIGHT = rES_HEIGHT;
    }

    public static boolean isDELETE_OSZ() {
        return DELETE_OSZ;
    }

    public static void setDELETE_OSZ(final boolean dELETE_OSZ) {
        DELETE_OSZ = dELETE_OSZ;
    }

    public static boolean isSCAN_DOWNLOAD() {
        return SCAN_DOWNLOAD;
    }

    public static void setSCAN_DOWNLOAD(final boolean sCAN_DOWNLOAD) {
        SCAN_DOWNLOAD = sCAN_DOWNLOAD;
    }

    public static boolean isDeleteUnimportedBeatmaps() {
        return deleteUnimportedBeatmaps;
    }

    public static void setDeleteUnimportedBeatmaps(boolean deleteUnimportedBeatmaps) {
        Config.deleteUnimportedBeatmaps = deleteUnimportedBeatmaps;
    }

    public static boolean isUseCustomSkins() {
        return useCustomSkins;
    }

    public static void setUseCustomSkins(final boolean useCustomSkins) {
        Config.useCustomSkins = useCustomSkins;
    }

    public static boolean isUseCustomSounds() {
        return useCustomSounds;
    }

    public static void setUseCustomSounds(boolean useCustomSounds) {
        Config.useCustomSounds = useCustomSounds;
    }

    public static int getTextureQuality() {
        return 1;
    }

    public static float getBackgroundBrightness() {
        return backgroundBrightness;
    }

    public static void setBackgroundBrightness(final float backgroundBrightness) {
        Config.backgroundBrightness = backgroundBrightness;
    }

    public static boolean isAnimateFollowCircle() {
        return animateFollowCircle;
    }

    public static boolean isAnimateComboText() {
        return animateComboText;
    }


    public static boolean isSnakingInSliders()
    {
        return snakingInSliders;
    }

    public static boolean isPlayMusicPreview() {
        return playMusicPreview;
    }

    public static void setPlayMusicPreview(final boolean playMusicPreview) {
        Config.playMusicPreview = playMusicPreview;
    }

    public static boolean isShowCursor() {
        return showCursor;
    }

    public static void setShowCursor(final boolean showCursor) {
        Config.showCursor = showCursor;
    }

    public static float getScaleMultiplier() {
        return scaleMultiplier;
    }

    public static void setScaleMultiplier(final float scaleMultiplier) {
        Config.scaleMultiplier = scaleMultiplier;
    }

    public static String getOnlineUsername() {
        return !onlineUsername.isEmpty() ? onlineUsername : "Guest";
    }

    public static void setOnlineUsername(String onlineUsername) {
        Config.onlineUsername = onlineUsername;
    }

    public static String getOnlinePassword() {
        return onlinePassword;
    }

    public static void setOnlinePassword(String onlinePassword) {
        Config.onlinePassword = onlinePassword;
    }

    public static boolean isStayOnline() {
        return stayOnline && BuildType.hasOnlineAccess();
    }

    public static void setStayOnline(boolean stayOnline) {
        Config.stayOnline = stayOnline;
    }

    public static boolean getLoadAvatar() {
        return loadAvatar;
    }

    public static void setLoadAvatar(boolean loadAvatar) {
        Config.loadAvatar = loadAvatar;
    }

    public static String getOnlineDeviceID() {
        return onlineDeviceID;
    }

    public static boolean isSyncMusic() {
        return syncMusic;
    }

    public static void setSyncMusic(boolean syncMusic) {
        Config.syncMusic = syncMusic;
    }

    public static String getCachePath() {
        return cachePath;
    }

    public static void setCachePath(String cachePath) {
        Config.cachePath = cachePath;
    }

    public static boolean isBurstEffects() {
        return burstEffects;
    }

    public static void setBurstEffects(boolean burstEffects) {
        Config.burstEffects = burstEffects;
    }

    public static boolean isHitLighting() {
        return hitLighting;
    }

    public static void setHitLighting(boolean hitLighting) {
        Config.hitLighting = hitLighting;
    }

    public static boolean isUseParticles() {
        return useParticles;
    }

    public static void setUseParticles(boolean useParticles) {
        Config.useParticles = useParticles;
    }

    public static String getSkinPath() {
        return skinPath;
    }

    public static void setSkinPath(String skinPath) {
        Config.skinPath = skinPath;
    }

    public static String getSkinTopPath() {
        return skinTopPath;
    }

    public static void setSkinTopPath(String skinTopPath) {
        Config.skinTopPath = skinTopPath;
    }

    public static boolean isHideNaviBar() {
        return hideNaviBar;
    }

    public static void setHideNaviBar(boolean hideNaviBar) {
        Config.hideNaviBar = hideNaviBar;
    }

    public static boolean isEnablePP() {
        return enablePP;
    }

    public static void setEnablePP(boolean enablePP) {
        Config.enablePP = enablePP;
    }

    public static String getScorePath() {
        return scorePath;
    }

    public static void setScorePath(String scorePath) {
        Config.scorePath = scorePath;
    }

    public static boolean isUseCustomComboColors() {
        return useCustomComboColors;
    }

    public static void setUseCustomComboColors(boolean useCustomComboColors) {
        Config.useCustomComboColors = useCustomComboColors;
    }

    public static RGBColor[] getComboColors() {
        return comboColors;
    }

    public static int getErrorMeter() {
        return errorMeter;
    }

    public static void setErrorMeter(int errorMeter) {
        Config.errorMeter = errorMeter;
    }

    public static int getSpinnerStyle() {
        return spinnerStyle;
    }

    public static void setSpinnerStyle(int spinnerStyle) {
        Config.spinnerStyle = spinnerStyle;
    }

    public static boolean isShowFirstApproachCircle() {
        return showFirstApproachCircle;
    }

    public static void setShowFirstApproachCircle(boolean showFirstApproachCircle) {
        Config.showFirstApproachCircle = showFirstApproachCircle;
    }

    public static int getMetronomeSwitch() {
        return metronomeSwitch;
    }

    public static void setMetronomeSwitch(int metronomeSwitch) {
        Config.metronomeSwitch = metronomeSwitch;
    }

    public static boolean isComboburst() {
        return comboburst;
    }

    public static void setComboburst(boolean comboburst) {
        Config.comboburst = comboburst;
    }

    public static boolean isForceRomanized() {
        return forceRomanized;
    }

    public static void setForceRomanized(boolean forceRomanized) {
        Config.forceRomanized = forceRomanized;
    }

    public static float getCursorSize() {
        return cursorSize;
    }

    public static void setCursorSize(final float cursorSize) {
        Config.cursorSize = cursorSize;
    }

    public static float getPlayfieldSize() {
        return playfieldSize;
    }

    public static void setPlayfieldSize(final float playfieldSize) {
        Config.playfieldSize = playfieldSize;
    }

    public static boolean isShrinkPlayfieldDownwards() {
        return shrinkPlayfieldDownwards;
    }

    public static void setShrinkPlayfieldDownwards(boolean shrinkPlayfieldDownwards) {
        Config.shrinkPlayfieldDownwards = shrinkPlayfieldDownwards;
    }

    public static boolean isHideReplayMarquee() {
        return hideReplayMarquee;
    }

    public static void setHideReplayMarquee(boolean hideReplayMarquee) {
        Config.hideReplayMarquee = hideReplayMarquee;
    }

    public static boolean isHideInGameUI() {
        return hideInGameUI;
    }

    public static void setHideInGameUI(boolean hideInGameUI) {
        Config.hideInGameUI = hideInGameUI;
    }

    public static boolean isReceiveAnnouncements() {
        return receiveAnnouncements;
    }

    public static void setReceiveAnnouncements(boolean receiveAnnouncements) {
        Config.receiveAnnouncements = receiveAnnouncements;
    }

    public static boolean isSafeBeatmapBg() {
        return safeBeatmapBg;
    }

    public static void setSafeBeatmapBg(boolean safeBeatmapBg) {
        Config.safeBeatmapBg = safeBeatmapBg;
    }

    public static boolean isTrianglesAnimation() {
        return false;
    }

    public static String getDefaultCorePath() {
        return defaultCorePath;
    }

    public static void loadSkins() {
        File[] folders = FileUtils.listFiles(new File(skinTopPath), file -> file.isDirectory() && !file.getName().startsWith("."));
        skins = new HashMap<>();
        for(File folder : folders) {
            skins.put(folder.getName(), folder.getPath());
            Debug.i("skins: " + folder.getName() + " - " + folder.getPath());
        }
    }

    public static Map<String, String> getSkins(){
        return skins;
    }

    public static void addSkin(String name, String path) {
        if(skins == null) skins = new HashMap<>();
        skins.put(name, path);
    }

    public static boolean isUseNightcoreOnMultiplayer() {
        return useNightcoreOnMultiplayer;
    }

    public static void setUseNightcoreOnMultiplayer(boolean value) {
        useNightcoreOnMultiplayer = value;
    }

    public static boolean isVideoEnabled() {
        return videoEnabled;
    }

    public static void setVideoEnabled(boolean value) {
        videoEnabled = value;
    }

    public static boolean isDeleteUnsupportedVideos() {
        return deleteUnsupportedVideos;
    }

    public static boolean isSubmitScoreOnMultiplayer() {
        return submitScoreOnMultiplayer;
    }

    public static void setSubmitScoreOnMultiplayer(boolean submitScoreOnMultiplayer) {
        Config.submitScoreOnMultiplayer = submitScoreOnMultiplayer;
    }

    public static boolean isKeepBackgroundAspectRatio() {
        return keepBackgroundAspectRatio;
    }

    public static boolean isNoChangeDimInBreaks() {
        return noChangeDimInBreaks;
    }


    public static boolean get(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    public static int get(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    public static float get(String key, float defaultValue) {
        return preferences.getFloat(key, defaultValue);
    }

    public static String get(String key, String defaultValue) {
        return preferences.getString(key, defaultValue);
    }

    public static Set<String> get(String key, Set<String> defaultValue) {
        return preferences.getStringSet(key, defaultValue);
    }

    public static void set(String key, boolean value) {
        preferences.edit().putBoolean(key, value).apply();
    }

    public static void set(String key, int value) {
        preferences.edit().putInt(key, value).apply();
    }

    public static void set(String key, String value) {
        preferences.edit().putString(key, value).apply();
    }

    public static void set(String key, float value) {
        preferences.edit().putFloat(key, value).apply();
    }

    public static void set(String key, Set<String> value) {
        preferences.edit().putStringSet(key, value).apply();
    }
}