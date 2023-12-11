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
import com.reco1l.legacy.Multiplayer;
import net.margaritov.preference.colorpicker.ColorPickerPreference;
import org.anddev.andengine.util.Debug;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Config {

    private static String corePath, defaultCorePath, beatmapPath, cachePath, skinPath, skinTopPath, scorePath, localUsername, onlineUsername, onlinePassword, onlineDeviceID;

    private static boolean DELETE_OSZ, SCAN_DOWNLOAD, deleteUnimportedBeatmaps, showFirstApproachCircle, comboburst, useCustomSkins, useCustomSounds, corovans, showFPS, complexAnimations, snakingInSliders, playMusicPreview, showCursor, shrinkPlayfieldDownwards, hideNaviBar, showScoreboard, enablePP, enableExtension, loadAvatar, stayOnline, syncMusic, burstEffects, hitLighting, useDither, useParticles, useCustomComboColors, forceRomanized, fixFrameOffset, removeSliderLock, calculateSliderPathInGameStart, displayScoreStatistics, hideReplayMarquee, hideInGameUI, receiveAnnouncements, enableStoryboard, safeBeatmapBg, trianglesAnimation, displayRealTimePPCounter, useNightcoreOnMultiplayer, videoEnabled, deleteUnsupportedVideos, submitScoreOnMultiplayer, keepBackgroundAspectRatio, noChangeDimInBreaks;

    private static int RES_WIDTH, RES_HEIGHT, errorMeter, spinnerStyle, backgroundQuality, metronomeSwitch;

    private static float soundVolume, bgmVolume, offset, backgroundBrightness, scaleMultiplier, playfieldSize, cursorSize;

    private static Map<String, String> skins;

    private static RGBColor[] comboColors;

    private static Context context;

    public static void loadConfig(final Context context) {
        Config.context = context;
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String s;
        // graphics
        s = prefs.getString("background", "2");
        backgroundQuality = Integer.parseInt(s);
        useCustomSkins = prefs.getBoolean("skin", false);
        useCustomSounds = prefs.getBoolean("beatmapSounds", true);
        comboburst = prefs.getBoolean("comboburst", false);
        corovans = prefs.getBoolean("images", false);
        showFPS = prefs.getBoolean("fps", false);
        errorMeter = Integer.parseInt(prefs.getString("errormeter", "0"));
        spinnerStyle = Integer.parseInt(prefs.getString("spinnerstyle", "0"));
        showFirstApproachCircle = prefs.getBoolean("showfirstapproachcircle", false);
        metronomeSwitch = Integer.parseInt(prefs.getString("metronomeswitch", "1"));
        showScoreboard = prefs.getBoolean("showscoreboard", true);
        enableStoryboard = prefs.getBoolean("enableStoryboard", false);
        trianglesAnimation = prefs.getBoolean("trianglesAnimation", true);
        videoEnabled = prefs.getBoolean("enableVideo", false);
        keepBackgroundAspectRatio = prefs.getBoolean("keepBackgroundAspectRatio", false);
        noChangeDimInBreaks = prefs.getBoolean("noChangeDimInBreaks", false);

        setSize();
        setPlayfieldSize(prefs.getInt("playfieldSize", 100) / 100f);

        shrinkPlayfieldDownwards = prefs.getBoolean("shrinkPlayfieldDownwards", true);
        complexAnimations = prefs.getBoolean("complexanimations", true);
        snakingInSliders = prefs.getBoolean("snakingInSliders", true);

        try {
            offset = (int) FMath.clamp(prefs.getInt("offset", 0), -250, 250);
            backgroundBrightness = prefs.getInt("bgbrightness", 25) / 100f;
            soundVolume = prefs.getInt("soundvolume", 100) / 100f;
            bgmVolume = prefs.getInt("bgmvolume", 100) / 100f;
            cursorSize = prefs.getInt("cursorSize", 50) / 100f;
        } catch (RuntimeException e) { // use valid integer since this makes the game crash on android m
            prefs.edit().putInt("offset", 0).putInt("bgbrightness", 25).putInt("soundvolume", 100).putInt("bgmvolume", 100).putInt("cursorSize", 50).commit();
            Config.loadConfig(context);
            return;
        }

        //advanced
        defaultCorePath = Environment.getExternalStorageDirectory() + "/osu!droid/";
        corePath = prefs.getString("corePath", defaultCorePath);
        if (corePath.isEmpty()) {
            corePath = defaultCorePath;
        }
        if (corePath.charAt(corePath.length() - 1) != '/') {
            corePath += "/";
        }
        scorePath = corePath + "Scores/";

        skinPath = prefs.getString("skinPath", corePath + "Skin/");
        if (skinPath.isEmpty()) {
            skinPath = corePath + "Skin/";
        }
        if (skinPath.charAt(skinPath.length() - 1) != '/') {
            skinPath += "/";
        }

        skinTopPath = prefs.getString("skinTopPath", skinPath);
        if (skinTopPath.isEmpty()) {
            skinTopPath = skinPath;
        }
        if (skinTopPath.charAt(skinTopPath.length() - 1) != '/') {
            skinTopPath += "/";
        }

        syncMusic = prefs.getBoolean("syncMusic", syncMusic);
        enableExtension = false;// prefs.getBoolean("enableExtension", false);
        cachePath = context.getCacheDir().getPath();
        burstEffects = prefs.getBoolean("bursts", burstEffects);
        hitLighting = prefs.getBoolean("hitlighting", hitLighting);
        useDither = prefs.getBoolean("dither", useDither);
        useParticles = prefs.getBoolean("particles", useParticles);
        useCustomComboColors = prefs.getBoolean("useCustomColors", useCustomComboColors);
        comboColors = new RGBColor[4];
        for (int i = 1; i <= 4; i++) {
            comboColors[i - 1] = RGBColor.hex2Rgb(ColorPickerPreference.convertToRGB(prefs.getInt("combo" + i, 0xff000000)));
        }

        // beatmaps
        DELETE_OSZ = prefs.getBoolean("deleteosz", true);
        SCAN_DOWNLOAD = prefs.getBoolean("scandownload", false);
        deleteUnimportedBeatmaps = prefs.getBoolean("deleteUnimportedBeatmaps", false);
        forceRomanized = prefs.getBoolean("forceromanized", false);
        beatmapPath = prefs.getString("directory", corePath + "Songs/");
        if (beatmapPath.isEmpty()) {
            beatmapPath = corePath + "Songs/";
        }
        if (beatmapPath.charAt(beatmapPath.length() - 1) != '/') {
            beatmapPath += "/";
        }
        deleteUnsupportedVideos = prefs.getBoolean("deleteUnsupportedVideos", true);

        // other
        playMusicPreview = prefs.getBoolean("musicpreview", true);
        localUsername = prefs.getString("playername", "");
        showCursor = prefs.getBoolean("showcursor", false);
        hideNaviBar = prefs.getBoolean("hidenavibar", false);
        enablePP = false;//prefs.getBoolean("enablePP",true);
        fixFrameOffset = prefs.getBoolean("fixFrameOffset", true);
        removeSliderLock = prefs.getBoolean("removeSliderLock", false);
        calculateSliderPathInGameStart = prefs.getBoolean("calculateSliderPathInGameStart", false);
        displayScoreStatistics = prefs.getBoolean("displayScoreStatistics", false);
        hideReplayMarquee = prefs.getBoolean("hideReplayMarquee", false);
        hideInGameUI = prefs.getBoolean("hideInGameUI", false);
        receiveAnnouncements = prefs.getBoolean("receiveAnnouncements", true);
        safeBeatmapBg = prefs.getBoolean("safebeatmapbg", false);
        displayRealTimePPCounter = prefs.getBoolean("displayRealTimePPCounter", false);

        // Multiplayer
        useNightcoreOnMultiplayer = prefs.getBoolean("player_nightcore", false);
        submitScoreOnMultiplayer = prefs.getBoolean("player_submitScore", true);

        if (receiveAnnouncements) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements");
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("announcements");
        }

        //Init
        onlineDeviceID = prefs.getString("installID", null);
        if (onlineDeviceID == null) {
            onlineDeviceID = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
            Editor editor = prefs.edit();
            editor.putString("installID", onlineDeviceID);
            editor.putString("corePath", corePath);
            editor.putString("skinTopPath", skinTopPath);
            editor.putString("skinPath", skinPath);
            editor.commit();
        }

        loadOnlineConfig(context);
        FavoriteLibrary.get().load();
    }

    public static void loadOnlineConfig(final Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        onlineUsername = prefs.getString("onlineUsername", "");
        onlinePassword = prefs.getString("onlinePassword", null);
        stayOnline = prefs.getBoolean("stayOnline", false);
        loadAvatar = prefs.getBoolean("loadAvatar", false);
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

    public static boolean isEnableExtension() {
        return enableExtension;
    }

    public static boolean isShowFPS() {
        return showFPS;
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

    public static float getSoundVolume() {
        return soundVolume;
    }

    public static float getBgmVolume() {
        return bgmVolume;
    }

    public static float getOffset() {
        return offset;
    }

    public static int getBackgroundQuality() {
        return backgroundQuality;
    }

    public static void setBackgroundQuality(final int backgroundQuality) {
        Config.backgroundQuality = backgroundQuality;
    }

    public static String getCorePath() {
        return corePath;
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

    public static int getRES_HEIGHT() {
        return RES_HEIGHT;
    }

    public static boolean isDELETE_OSZ() {
        return DELETE_OSZ;
    }

    public static boolean isSCAN_DOWNLOAD() {
        return SCAN_DOWNLOAD;
    }

    public static boolean isDeleteUnimportedBeatmaps() {
        return deleteUnimportedBeatmaps;
    }

    public static boolean isUseCustomSkins() {
        return useCustomSkins;
    }

    public static boolean isUseCustomSounds() {
        return useCustomSounds;
    }

    public static float getBackgroundBrightness() {
        return backgroundBrightness;
    }

    public static void setBackgroundBrightness(final float backgroundBrightness) {
        Config.backgroundBrightness = backgroundBrightness;
    }

    public static boolean isComplexAnimations() {
        return complexAnimations;
    }

    public static boolean isSnakingInSliders() {
        return snakingInSliders;
    }

    public static boolean isPlayMusicPreview() {
        return playMusicPreview;
    }

    public static String getLocalUsername() {
        return localUsername;
    }

    public static boolean isShowCursor() {
        return showCursor;
    }

    public static float getScaleMultiplier() {
        return scaleMultiplier;
    }

    public static void setScaleMultiplier(final float scaleMultiplier) {
        Config.scaleMultiplier = scaleMultiplier;
    }

    public static String getOnlineUsername() {
        return onlineUsername;
    }

    public static String getOnlinePassword() {
        return onlinePassword;
    }

    public static boolean isStayOnline() {
        return stayOnline && BuildType.hasOnlineAccess();
    }

    public static boolean getLoadAvatar() {
        return loadAvatar;
    }

    public static String getOnlineDeviceID() {
        return onlineDeviceID;
    }

    public static boolean isSyncMusic() {
        return syncMusic;
    }

    public static String getCachePath() {
        return cachePath;
    }

    public static boolean isBurstEffects() {
        return burstEffects;
    }

    public static boolean isHitLighting() {
        return hitLighting;
    }

    public static boolean isUseDither() {
        return useDither;
    }

    public static boolean isUseParticles() {
        return useParticles;
    }

    public static String getSkinPath() {
        return skinPath;
    }

    public static String getSkinTopPath() {
        return skinTopPath;
    }

    public static boolean isHideNaviBar() {
        return hideNaviBar;
    }

    public static String getScorePath() {
        return scorePath;
    }

    public static boolean isUseCustomComboColors() {
        return useCustomComboColors;
    }

    public static RGBColor[] getComboColors() {
        return comboColors;
    }

    public static int getErrorMeter() {
        return errorMeter;
    }

    public static int getSpinnerStyle() {
        return spinnerStyle;
    }

    public static boolean isShowFirstApproachCircle() {
        return showFirstApproachCircle;
    }

    public static int getMetronomeSwitch() {
        return metronomeSwitch;
    }

    public static boolean isComboburst() {
        return comboburst;
    }

    public static boolean isForceRomanized() {
        return forceRomanized;
    }

    public static float getCursorSize() {
        return cursorSize;
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

    public static boolean isHideReplayMarquee() {
        return hideReplayMarquee;
    }

    public static boolean isHideInGameUI() {
        return hideInGameUI;
    }

    public static boolean isSafeBeatmapBg() {
        return safeBeatmapBg;
    }

    public static boolean isTrianglesAnimation() {
        return trianglesAnimation;
    }

    public static String getDefaultCorePath() {
        return defaultCorePath;
    }

    public static void loadSkins() {
        File[] folders = FileUtils.listFiles(new File(skinTopPath), file -> file.isDirectory() && !file.getName().startsWith("."));
        skins = new HashMap<>();
        for (File folder : folders) {
            skins.put(folder.getName(), folder.getPath());
            Debug.i("skins: " + folder.getName() + " - " + folder.getPath());
        }
    }

    public static Map<String, String> getSkins() {
        return skins;
    }

    public static void addSkin(String name, String path) {
        if (skins == null) {
            skins = new HashMap<>();
        }
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

    public static boolean isKeepBackgroundAspectRatio() {
        return keepBackgroundAspectRatio;
    }

    public static boolean isNoChangeDimInBreaks() {
        return noChangeDimInBreaks;
    }

}