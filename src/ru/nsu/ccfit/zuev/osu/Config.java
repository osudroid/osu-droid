package ru.nsu.ccfit.zuev.osu;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;

import com.edlplan.favorite.FavoriteLibrary;

import net.margaritov.preference.colorpicker.ColorPickerPreference;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.util.Debug;

import java.util.UUID;

public class Config {
    private static boolean DELETE_OSZ = false;
    private static boolean SCAN_DOWNLOAD = false;
    private static int RES_WIDTH = 1280;
    private static int RES_HEIGHT = 720;
    private static String corePath = Environment.getExternalStorageDirectory() + "/osu!droid/";
    private static String beatmapPath = corePath + "Songs/";
    private static String cachePath = corePath;
    private static String skinPath = corePath + "Skin/";
    private static String skinTopPath = skinPath;
    private static String scorePath = corePath + "Scores/";
    private static String APIKey = "";
    private static int errorMeter = 0;
    private static int spinnerStyle = 0;
    private static boolean showFirstApproachCircle = false;
    private static boolean comboburst = false;
    private static int backgroundQuality = 1;
    private static boolean useCustomSkins = false;
    private static boolean useCustomSounds = true;
    private static boolean corovans = true;
    private static float soundVolume = 1;
    private static float bgmVolume = 1;
    private static float offset = 0;
    private static int skipOffset = 0;
    private static boolean doubleSound = true;
    private static boolean showFPS = false;
    private static int textureQuality = 1;
    private static int metronomeSwitch = 1;
    private static boolean useNativePlayer = true;
    private static float backgroundBrightness = 1;
    private static int vbrOffset = 0;
    private static int oggOffset = 0;
    private static int pauseOffset = 0;
    private static boolean sliderBorders = true;
    private static boolean complexAnimations = true;
    private static boolean multitouch = true;
    private static boolean playMusicPreview = false;
    private static String localUsername = "";
    private static boolean showCursor = false;
    private static boolean accurateSlider = true;
    private static float scaleMultiplier = 0;
    private static boolean hideNaviBar = false;
    private static boolean showScoreboard = true;
    private static boolean enablePP = true;
    private static boolean enableExtension = false;

    private static String onlineUsername = "Pesets";
    private static String onlinePassword = null;
    private static String onlineDeviceID = null;
    private static boolean stayOnline = true;

    private static boolean syncMusic = true;
    private static boolean saveReplays = true;
    private static boolean burstEffects = false;
    private static boolean hitLighting = false;
    private static boolean useDither = true;
    private static boolean useParticles = false;
    private static boolean useCustomComboColors = false;
    private static RGBColor[] comboColors;
    private static boolean forceRomanized = false;

    private static boolean fixFrameOffset = true;
    private static boolean removeSliderLock = false;

    private static float cursorSize = 1;

    private static boolean useSuperSlider = true;

    private static boolean enableStoryboard = false;

    private static Context context;

    public static void loadConfig(final Context context) {
        Config.context = context;
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);
        String s;
        // graphics
        s = prefs.getString("background", "2");
        backgroundQuality = Integer.parseInt(s);
        useCustomSkins = prefs.getBoolean("skin", false);
        useCustomSounds = prefs.getBoolean("sound", true);
        comboburst = prefs.getBoolean("comboburst", false);
        corovans = prefs.getBoolean("images", false);
        showFPS = prefs.getBoolean("fps", false);
        textureQuality = prefs.getBoolean("lowtextures", false) ? 2 : 1;
        errorMeter = Integer.parseInt(prefs.getString("errormeter", "0"));
        spinnerStyle = Integer.parseInt(prefs.getString("spinnerstyle", "0"));
        showFirstApproachCircle = prefs.getBoolean("showfirstapproachcircle", false);
        metronomeSwitch = Integer.parseInt(prefs.getString("metronomeswitch", "1"));
        showScoreboard = prefs.getBoolean("showscoreboard", true);
        enableStoryboard = prefs.getBoolean("enableStoryboard", false);

        setSize();

        setBackgroundBrightness(Integer.parseInt(prefs.getString(
                "bgbrightness", "25")) / 100f);
        sliderBorders = prefs.getBoolean("sliderborders", true);
        complexAnimations = prefs.getBoolean("complexanimations", true);
        accurateSlider = true;//prefs.getBoolean("demoSpline", true);

        useSuperSlider = prefs.getBoolean("superSlider", false);

        // sound
        s = prefs.getString("soundvolume", "100");
        soundVolume = 1;
        try {
            final int vol = Integer.parseInt(s);
            if (vol >= 0 && vol <= 100) {
                soundVolume = vol / 100f;
            }
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid volume!");
        }
        // music
        s = prefs.getString("bgmvolume", "100");
        bgmVolume = 1;
        try {
            final int vol = Integer.parseInt(s);
            if (vol >= 0 && vol <= 100) {
                bgmVolume = vol / 100f;
            }
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid volume!");
        }
        s = prefs.getString("offset", "0");
        offset = 0;
        try {
            final int off = Integer.parseInt(s);
            offset = (int) (Math.signum(off) * Math.min(250, Math.abs(off)));
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid offset!");
        }
        s = prefs.getString("skipoffset", "0");
        skipOffset = 0;
        try {
            final int off = Integer.parseInt(s);
            skipOffset = off;
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid offset!");
        }
        s = prefs.getString("vbroffset", "50");
        vbrOffset = 0;
        try {
            final int off = Integer.parseInt(s);
            vbrOffset = off;
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid offset!");
        }
        s = prefs.getString("oggoffset", "0");
        oggOffset = 0;
        try {
            final int off = Integer.parseInt(s);
            oggOffset = off;
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid offset!");
        }
        s = prefs.getString("pauseoffset", "0");
        pauseOffset = 0;
        try {
            final int off = Integer.parseInt(s);
            pauseOffset = off;
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid offset!");
        }
        s = prefs.getString("cursorSize", "50");
        cursorSize = 1;
        try {
            final int csize = Integer.parseInt(s);
            if (csize >= 25 && csize <= 300) {
                cursorSize = csize / 100f;
            }
        } catch (final NumberFormatException e) {
            Debug.e("loadConfig: " + s + " is not a valid size!");
        }
        doubleSound = prefs.getBoolean("doublesound", true);
        useNativePlayer = prefs.getBoolean("nativeplayer", true);
        // beatmaps
        DELETE_OSZ = prefs.getBoolean("deleteosz", true);
        SCAN_DOWNLOAD = prefs.getBoolean("scandownload", false);
        forceRomanized = prefs.getBoolean("forceromanized", false);
        beatmapPath = prefs.getString("directory", corePath + "Songs/");
        if (beatmapPath.length() == 0) {
            beatmapPath = corePath + "Songs/";
        }
        if (beatmapPath.charAt(beatmapPath.length() - 1) != '/') {
            beatmapPath += "/";
        }
        // other
        playMusicPreview = prefs.getBoolean("musicpreview", true);
        localUsername = prefs.getString("playername", "");
        showCursor = prefs.getBoolean("showcursor", false);
        hideNaviBar = prefs.getBoolean("hidenavibar", false);
        enablePP = false;//prefs.getBoolean("enablePP",true);

        fixFrameOffset = prefs.getBoolean("fixFrameOffset", true);
        removeSliderLock = prefs.getBoolean("removeSliderLock", true);
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

        //advanced
        corePath = prefs.getString("corePath", corePath);
        skinTopPath = prefs.getString("skinTopPath", skinTopPath);
        skinPath = prefs.getString("skinPath", skinPath);
        syncMusic = prefs.getBoolean("syncMusic", syncMusic);
        if (prefs.getBoolean("lowDelay", true)) {
            Engine.INPUT_PAUSE_DURATION = 0;
        } else {
            Engine.INPUT_PAUSE_DURATION = 20;
        }
        enableExtension = false;// prefs.getBoolean("enableExtension", false);
        cachePath = context.getCacheDir().getPath();
        saveReplays = prefs.getBoolean("saveReplays", true);
        burstEffects = prefs.getBoolean("bursts", burstEffects);
        hitLighting = prefs.getBoolean("hitlighting", hitLighting);
        useDither = prefs.getBoolean("dither", useDither);
        useParticles = prefs.getBoolean("particles", useParticles);
        useCustomComboColors = prefs.getBoolean("useCustomColors", useCustomComboColors);
        comboColors = new RGBColor[4];
        for (int i = 1; i <= 4; i++) {
            comboColors[i - 1] = RGBColor.hex2Rgb(ColorPickerPreference.convertToRGB(prefs.getInt("combo" + i, 0xff000000)));
        }

        FavoriteLibrary.get().load();
    }

    public static void loadOnlineConfig(final Context context) {
        final SharedPreferences prefs = PreferenceManager
                .getDefaultSharedPreferences(context);

        APIKey = prefs.getString("APIKey", "");
        onlineUsername = prefs.getString("onlineUsername", "");
        onlinePassword = prefs.getString("onlinePassword", null);
        stayOnline = prefs.getBoolean("stayOnline", true);
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

    public static boolean isUseSuperSlider() {
        return useSuperSlider;
    }

    public static boolean isFixFrameOffset() {
        return fixFrameOffset;
    }

    public static boolean isRemoveSliderLock() {
        return removeSliderLock;
    }

    public static boolean isEnableExtension() {
        return enableExtension;
    }

    public static void setEnableExtension(boolean enableExtension) {
        Config.enableExtension = enableExtension;
    }

    public static int getSkipOffset() {
        return skipOffset;
    }

    public static void setSkipOffset(final int skipOffset) {
        Config.skipOffset = skipOffset;
    }

    public static boolean isShowFPS() {
        return showFPS;
    }

    public static void setShowFPS(final boolean showFPS) {
        Config.showFPS = showFPS;
    }

    public static boolean isShowScoreboard() {
        return showScoreboard;
    }

    public static void setShowScoreboard(final boolean showScoreboard) {
        Config.showScoreboard = showScoreboard;
    }

    public static boolean isDoubleSound() {
        return doubleSound;
    }

    public static void setDoubleSound(final boolean doubleSound) {
        Config.doubleSound = doubleSound;
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

    public static int getBackgroundQuality() {
        return backgroundQuality;
    }

    public static void setBackgroundQuality(final int backgroundQuality) {
        Config.backgroundQuality = backgroundQuality;
    }

    public static String getCorePath() {
        return corePath;
    }

    public static void setCorePath(final String scorePath) {
        Config.corePath = scorePath;
    }

    public static String getAPIKey() {
        return APIKey;
    }

    public static void setAPIKey(final String APIKey) {
        Config.APIKey = APIKey;
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

    public static void setTextureQuality(final int textureQuality) {
        Config.textureQuality = textureQuality;
    }

    public static boolean isUseNativePlayer() {
        return useNativePlayer;
    }

    public static void setUseNativePlayer(final boolean useNativePlayer) {
        Config.useNativePlayer = useNativePlayer;
    }

    public static float getBackgroundBrightness() {
        return backgroundBrightness;
    }

    public static void setBackgroundBrightness(final float backgroundBrightness) {
        Config.backgroundBrightness = backgroundBrightness;
    }

    public static int getVbrOffset() {
        return vbrOffset;
    }

    public static void setVbrOffset(final int vbrOffect) {
        Config.vbrOffset = vbrOffect;
    }

    public static boolean isSliderBorders() {
        return sliderBorders;
    }

    public static void setSliderBorders(final boolean sliderBorders) {
        Config.sliderBorders = sliderBorders;
    }

    public static boolean isComplexAnimations() {
        return complexAnimations;
    }

    public static void setComplexAnimations(final boolean complexAnimations) {
        Config.complexAnimations = complexAnimations;
    }

    public static boolean isMultitouch() {
        return multitouch;
    }

    public static void setMultitouch(final boolean multitouch) {
        Config.multitouch = multitouch;
    }

    public static int getOggOffset() {
        return oggOffset;
    }

    public static void setOggOffset(final int oggOffset) {
        Config.oggOffset = oggOffset;
    }

    public static int getPauseOffset() {
        return pauseOffset;
    }

    public static void setPauseOffset(final int pauseOffset) {
        Config.pauseOffset = pauseOffset;
    }

    public static boolean isPlayMusicPreview() {
        return playMusicPreview;
    }

    public static void setPlayMusicPreview(final boolean playMusicPreview) {
        Config.playMusicPreview = playMusicPreview;
    }

    public static String getLocalUsername() {
        return localUsername;
    }

    public static void setLocalUsername(final String localUsername) {
        Config.localUsername = localUsername;
    }

    public static boolean isShowCursor() {
        return showCursor;
    }

    public static void setShowCursor(final boolean showCursor) {
        Config.showCursor = showCursor;
    }

    public static boolean isAccurateSlider() {
        return accurateSlider;
    }

    public static void setAccurateSlider(final boolean accurateSlider) {
        Config.accurateSlider = accurateSlider;
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
        return stayOnline;
    }

    public static void setStayOnline(boolean stayOnline) {
        Config.stayOnline = stayOnline;
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

    public static boolean isSaveReplays() {
        return true;
    }

    public static void setSaveReplays(boolean saveReplays) {
        Config.saveReplays = saveReplays;
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

    public static boolean isUseDither() {
        return useDither;
    }

    public static void setUseDither(boolean useDither) {
        Config.useDither = useDither;
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

    public static void setCursorSize() {
        Config.cursorSize = cursorSize;
    }
}
