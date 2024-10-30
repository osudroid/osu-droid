package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.util.DisplayMetrics;

import androidx.preference.PreferenceManager;

import com.edlplan.framework.math.FMath;
import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.reco1l.osu.multiplayer.Multiplayer;
import com.reco1l.osu.playfield.ProgressIndicatorType;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.anddev.andengine.util.Debug;

import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.scoring.BeatmapLeaderboardScoringMode;

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
        noChangeDimInBreaks,
        dimHitObjects,
        forceMaxRefreshRate;

    private static int RES_WIDTH,
        RES_HEIGHT,
        errorMeter,
        spinnerStyle,
        metronomeSwitch,
        progressIndicatorType;
    
    private static float soundVolume,
        bgmVolume,
        offset,
        backgroundBrightness,
        scaleMultiplier,
        playfieldSize,
        cursorSize;

    private static DifficultyAlgorithm difficultyAlgorithm;

    private static BeatmapLeaderboardScoringMode beatmapLeaderboardScoringMode;

    private static Map<String, String> skins;

    private static RGBColor[] comboColors;
    private static Context context;

    public static void loadConfig(final Context context) {
        Config.context = context;
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        // graphics
        useCustomSkins = prefs.getBoolean("skin", false);
        useCustomSounds = prefs.getBoolean("beatmapSounds", true);
        comboburst = prefs.getBoolean("comboburst", false);
        corovans = prefs.getBoolean("images", false);
        showFPS = prefs.getBoolean("fps", true);
        showAverageOffset = prefs.getBoolean("averageOffset", true);
        showUnstableRate = prefs.getBoolean("unstableRate", true);
        errorMeter = Integer.parseInt(prefs.getString("errormeter", "0"));
        spinnerStyle = Integer.parseInt(prefs.getString("spinnerstyle", "1"));
        showFirstApproachCircle = prefs.getBoolean("showfirstapproachcircle", false);
        metronomeSwitch = Integer.parseInt(prefs.getString("metronomeswitch", "1"));
        showScoreboard = prefs.getBoolean("showscoreboard", true);
        enableStoryboard = prefs.getBoolean("enableStoryboard", false);
        videoEnabled = prefs.getBoolean("enableVideo", false);
        keepBackgroundAspectRatio = prefs.getBoolean("keepBackgroundAspectRatio", false);
        noChangeDimInBreaks = prefs.getBoolean("noChangeDimInBreaks", false);
        dimHitObjects = prefs.getBoolean("dimHitObjects", true);
        forceMaxRefreshRate = prefs.getBoolean("forceMaxRefreshRate", false);
        progressIndicatorType = Integer.parseInt(prefs.getString("progressIndicatorType", "0"));

        setSize();
        setPlayfieldSize(prefs.getInt("playfieldSize", 100) / 100f);

        shrinkPlayfieldDownwards = prefs.getBoolean("shrinkPlayfieldDownwards", true);
        animateFollowCircle = prefs.getBoolean("animateFollowCircle", true);
        animateComboText = prefs.getBoolean("animateComboText", true);
        snakingInSliders = prefs.getBoolean("snakingInSliders", true);

        try {
            offset = (int) FMath.clamp(prefs.getInt("offset", 0), -250, 250);
            backgroundBrightness = prefs.getInt("bgbrightness", 25) / 100f;
            soundVolume = prefs.getInt("soundvolume", 100) / 100f;
            bgmVolume = prefs.getInt("bgmvolume", 100) / 100f;
            cursorSize = prefs.getInt("cursorSize", 50) / 100f;
        }catch(RuntimeException e) { // use valid integer since this makes the game crash on android m
            prefs.edit()
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
        corePath = prefs.getString("corePath", defaultCorePath);
        if (corePath.length() == 0) {
            corePath = defaultCorePath;
        }
        if (corePath.charAt(corePath.length() - 1) != '/') {
            corePath += "/";
        }
        scorePath = corePath + "Scores/";

        skinPath = prefs.getString("skinPath", corePath + "Skin/");
        if (skinPath.length() == 0) {
            skinPath = corePath + "Skin/";
        }
        if (skinPath.charAt(skinPath.length() - 1) != '/') {
            skinPath += "/";
        }

        skinTopPath = prefs.getString("skinTopPath", skinPath);
        if (skinTopPath.length() == 0) {
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
        if (beatmapPath.length() == 0) {
            beatmapPath = corePath + "Songs/";
        }
        if (beatmapPath.charAt(beatmapPath.length() - 1) != '/') {
            beatmapPath += "/";
        }
        deleteUnsupportedVideos = prefs.getBoolean("deleteUnsupportedVideos", true);

        // other
        playMusicPreview = prefs.getBoolean("musicpreview", true);
        showCursor = prefs.getBoolean("showcursor", false);
        hideNaviBar = prefs.getBoolean("hidenavibar", false);
        enablePP = false;//prefs.getBoolean("enablePP",true);
        fixFrameOffset = prefs.getBoolean("fixFrameOffset", true);
        removeSliderLock = prefs.getBoolean("removeSliderLock", false);
        displayScoreStatistics = prefs.getBoolean("displayScoreStatistics", false);
        hideReplayMarquee = prefs.getBoolean("hideReplayMarquee", false);
        hideInGameUI = prefs.getBoolean("hideInGameUI", false);
        receiveAnnouncements = prefs.getBoolean("receiveAnnouncements", true);
        safeBeatmapBg = prefs.getBoolean("safebeatmapbg", false);
        displayRealTimePPCounter = prefs.getBoolean("displayRealTimePPCounter", false);
        difficultyAlgorithm = DifficultyAlgorithm.droid;

        // Multiplayer
        useNightcoreOnMultiplayer = prefs.getBoolean("player_nightcore", false);
        submitScoreOnMultiplayer = prefs.getBoolean("player_submitScore", true);

        if(receiveAnnouncements) {
            FirebaseMessaging.getInstance().subscribeToTopic("announcements");
        }else {
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
    }

    public static void loadOnlineConfig(final Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        onlineUsername = prefs.getString("onlineUsername", "");
        onlinePassword = prefs.getString("onlinePassword", null);
        stayOnline = prefs.getBoolean("stayOnline", false);
        loadAvatar = prefs.getBoolean("loadAvatar",false);
        beatmapLeaderboardScoringMode = BeatmapLeaderboardScoringMode.parse(Integer.parseInt(prefs.getString("beatmapLeaderboardScoringMode", "0")));
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

    public static BeatmapLeaderboardScoringMode getBeatmapLeaderboardScoringMode() {
        return beatmapLeaderboardScoringMode;
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

    public static boolean isDimHitObjects() {
        return dimHitObjects;
    }

    public static boolean isForceMaxRefreshRate() {
        return forceMaxRefreshRate;
    }

    @ProgressIndicatorType
    public static int getProgressIndicatorType() {
        return progressIndicatorType;
    }
}