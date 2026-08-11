package ru.nsu.ccfit.zuev.osu;

import static kotlin.collections.ArraysKt.any;
import static kotlin.collections.ArraysKt.filter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.osudroid.resources.FontStore;
import com.osudroid.resources.SoundStore;
import com.osudroid.resources.TextureStore;
import com.osudroid.resources.skin.AnimatableFrameMatch;
import com.osudroid.resources.skin.SkinTextureRules;
import com.osudroid.ui.skinning.StringSkinData;
import com.osudroid.ui.skinning.IniReader;
import com.osudroid.ui.skinning.SkinConverter;
import com.reco1l.andengine.UIEngine;
import org.andengine.engine.Engine;
import org.andengine.opengl.font.Font;
import org.andengine.opengl.font.StrokeFont;
import org.andengine.opengl.texture.TextureOptions;
import org.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.andengine.opengl.texture.region.TextureRegion;
import org.andengine.opengl.texture.region.TextureRegionFactory;
import org.andengine.util.debug.Debug;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinJsonReader;
import ru.nsu.ccfit.zuev.skins.BeatmapSkinManager;

public class ResourceManager {

    /**
     * The textures that shouldn't fallback to the default skin if they're not present in the skin folder.
     */
    private static final String[] OPTIONAL_TEXTURES = SkinTextureRules.OPTIONAL_TEXTURES;

    /**
     * The textures that can be animated.
     */
    private static final String[] ANIMATABLE_TEXTURES = SkinTextureRules.ANIMATABLE_TEXTURES;

    private final static ResourceManager mgr = new ResourceManager();

    // These caches are written from background loading threads (e.g. beatmap skin loading during
    // gameplay start, see BeatmapSkinManager) while being read concurrently from the update/GL thread
    // during rendering - the stores are ConcurrentHashMap-backed internally specifically to stay safe
    // under that access pattern.
    private final FontStore fontStore = new FontStore();
    private final SoundStore soundStore = new SoundStore();
    private final SoundStore customSoundStore = new SoundStore();
    private final TextureStore textureStore = new TextureStore();
    private final TextureStore customTextureStore = new TextureStore();

    private Engine engine;
    private Context context;

    private ResourceManager() {
    }

    public static ResourceManager getInstance() {
        return mgr;
    }

    public Engine getEngine() {
        return engine;
    }

    public void Init(final Engine engine, final Context context) {
        this.engine = engine;
        this.context = context;

        fontStore.init(engine, context);
        soundStore.init(context);
        customSoundStore.init(context);
        textureStore.init(engine, context);
        customTextureStore.init(engine, context);

        fontStore.clear();
        textureStore.clear();
        soundStore.clear();

        customSoundStore.clear();
        customTextureStore.clear();

        initSecurityUtils();
    }

    public void loadSkin(String folder) {
        loadFont("smallFont", null, 21, Color.WHITE);
        loadFont("middleFont", null, 24, Color.WHITE);
        loadFont("bigFont", null, 36, Color.WHITE);
        loadFont("font", null, 28, Color.WHITE);
        loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE);
        loadFont("CaptionFont", null, 35, Color.WHITE);

        if (!folder.endsWith("/"))
            folder = folder + "/";

        loadCustomSkin(folder);

        loadTexture("defaultapproachcircle", "gfx/approachcircle.png", false);
        loadTexture("ranking_enabled_score", "ranking_enabled_score.png", false);
        loadTexture("ranking_enabled_pp", "ranking_enabled_pp.png", false);
        loadTexture("ranking_disabled", "ranking_disabled.png", false);
        loadTexture("flashlight_cursor", "flashlight_cursor.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

        textureStore.markAbsent("lighting");

        UIEngine.getCurrent().onSkinChange();
    }

    public void loadCustomSkin(String folder) {

        if (!folder.endsWith("/")) folder += "/";

        File[] skinFiles = null;
        File skinFolder = new File(folder);
        if (!skinFolder.exists()) {
            skinFolder = null;
        } else {
            skinFiles = FileUtils.listFiles(skinFolder);
        }
        if (skinFiles != null) {
            JSONObject skinjson = null;
            File jsonFile = new File(folder, "skin.json");
            if (jsonFile.exists()) {
                try {
                    skinjson = new JSONObject(OsuSkin.readFull(jsonFile));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                var iniFile = new File(folder, "skin.ini");

                if (iniFile.exists()) {
                    GlobalManager.getInstance().setInfo("Reading skin.ini...");

                    try (var ini = new IniReader(iniFile)) {
                        skinjson = SkinConverter.convertToJson(ini);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    SkinConverter.ensureOptionalTexture(new File(folder, "sliderendcircle.png"));
                    SkinConverter.ensureOptionalTexture(new File(folder, "sliderendcircleoverlay.png"));

                    SkinConverter.ensureTexture(new File(folder, "selection-mods.png"));
                    SkinConverter.ensureTexture(new File(folder, "selection-random.png"));
                    SkinConverter.ensureTexture(new File(folder, "selection-options.png"));

                    skinFiles = FileUtils.listFiles(skinFolder);
                }
            }
            if (skinjson == null) skinjson = new JSONObject();
            SkinJsonReader.getReader().supplyJson(skinjson);
        }
        final Map<String, File> availableFiles = new HashMap<>();
        if (skinFiles != null) {
            for (final File f : skinFiles) {
                if (f.isFile()) {
                    if (f.getName().startsWith("comboburst")
                            && (f.getName().endsWith(".wav") || f.getName().endsWith(".mp3"))) {
                        continue;
                    }
                    if (f.getName().length() < 5) {
                        continue;
                    }
                    if (f.length() == 0) {
                        continue;
                    }
                    final String filename = f.getName().substring(0, f.getName().length() - 4);
                    availableFiles.put(filename, f);
                    //if ((filename.startsWith("hit0") || filename.startsWith("hit50") || filename.startsWith("hit100") || filename.startsWith("hit300"))){
                    //    availableFiles.put(filename + "-0", f);
                    //}

                    if (filename.equals("hitcircle")) {
                        if (!availableFiles.containsKey("sliderstartcircle")) {
                            availableFiles.put("sliderstartcircle", f);
                        }
                        if (!availableFiles.containsKey("sliderendcircle")) {
                            availableFiles.put("sliderendcircle", f);
                        }
                    }
                    if (filename.equals("hitcircleoverlay")) {
                        if (!availableFiles.containsKey("sliderstartcircleoverlay")) {
                            availableFiles.put("sliderstartcircleoverlay", f);
                        }
                        if (!availableFiles.containsKey("sliderendcircleoverlay")) {
                            availableFiles.put("sliderendcircleoverlay", f);
                        }
                    }
                }
            }
        }

        // Removing loaded animatable textures from the previous skin.
        for (var key : textureStore.getKeys().toArray(new String[0])) {
            if (any(ANIMATABLE_TEXTURES, key::startsWith)) {
                unloadTexture(key);
            }
        }

        textureStore.clearFrameCounts();
        customTextureStore.clearFrameCounts();

        try {

            String[] availableAnimatableFilenames = filter(availableFiles.keySet().toArray(new String[0]), f -> any(ANIMATABLE_TEXTURES, f::startsWith)).toArray(new String[0]);

            boolean isDefaultSkin = Objects.equals(folder, Config.getSkinTopPath());

            for (var assetName : Objects.requireNonNull(context.getAssets().list("gfx"))) {

                var textureName = assetName.substring(0, assetName.length() - 4);

                // Animatable textures are managed separately unless they're not present in the skin folder.
                var skip = false;
                for (var animatableTexture : ANIMATABLE_TEXTURES) {
                    if (textureName.startsWith(animatableTexture) && any(availableAnimatableFilenames, f -> f.startsWith(animatableTexture))) {
                        skip = true;
                        break;
                    }
                }
                if (skip) {
                    continue;
                }

                if (availableFiles.containsKey(textureName)) {
                    loadTexture(textureName, Objects.requireNonNull(availableFiles.get(textureName)).getPath(), true);
                } else {
                    if (!isDefaultSkin && any(OPTIONAL_TEXTURES, textureName::startsWith)) {
                        unloadTexture(textureName);
                    } else {
                        loadTexture(textureName, "gfx/" + assetName, false);
                        parseFrameIndex(textureName, false, false);
                    }
                }
            }

            if (availableFiles.containsKey("scorebar-kidanger")) {
                loadTexture("scorebar-kidanger", Objects.requireNonNull(availableFiles.get("scorebar-kidanger")).getPath(), true);
                loadTexture("scorebar-kidanger2", Objects.requireNonNull(availableFiles.get(availableFiles.containsKey("scorebar-kidanger2") ? "scorebar-kidanger2" : "scorebar-kidanger")).getPath(), true);
            }

            if (availableFiles.containsKey("comboburst")) {
                loadTexture("comboburst", Objects.requireNonNull(availableFiles.get("comboburst")).getPath(), true);
            } else {
                unloadTexture("comboburst");
            }

            for (int i = 0; i < 10; i++) {
                String textureName = "comboburst-" + i;
                if (availableFiles.containsKey(textureName)) {
                    File file = availableFiles.get(textureName);
                    if (file != null) {
                        loadTexture(textureName, file.getPath(), true);
                    } else {
                        unloadTexture(textureName);
                    }
                } else {
                    unloadTexture(textureName);
                }
            }

            for (var filename : availableAnimatableFilenames) {

                var file = availableFiles.get(filename);
                if (file != null) {
                    loadTexture(filename, file.getPath(), true);
                    parseFrameIndex(filename, false, false);
                } else {
                    unloadTexture(filename);
                }
            }

        } catch (final IOException e) {
            Debug.e("Resources: " + e.getMessage(), e);
        }

        try {
            // TODO: buggy?
            for (final String s : Objects.requireNonNull(context.getAssets().list("sfx"))) {
                final String name = s.substring(0, s.length() - 4);
                if (availableFiles.containsKey(name)) {
                    loadSound(name, Objects.requireNonNull(availableFiles.get(name)).getPath(), true);
                } else {
                    loadSound(name, "sfx/" + s, false);
                }
            }
            if (skinFolder != null) {
                loadSound("comboburst", folder + "comboburst.wav", true);
                for (int i = 0; i < 10; i++) {
                    loadSound("comboburst-" + i, folder + "comboburst-" + i + ".wav", true);
                }
            }
        } catch (final IOException e) {
            Debug.e("Resources: " + e.getMessage(), e);
        }

        loadTexture("ranking_button", "ranking_button.png", false);
        loadTexture("ranking_enabled_score", "ranking_enabled_score.png", false);
        loadTexture("ranking_enabled_pp", "ranking_enabled_pp.png", false);
        loadTexture("ranking_disabled", "ranking_disabled.png", false);
        loadTexture("selection-approved", "selection-approved.png", false);
        loadTexture("selection-loved", "selection-loved.png", false);
        loadTexture("selection-question", "selection-question.png", false);
        loadTexture("selection-ranked", "selection-ranked.png", false);
        textureStore.markAbsent("lighting");
    }

    /**
     * Parses the frame count from the filename and updates the customFrameCount map.
     *
     * @param filename The filename, this shouldn't contain the file extension.
     * @param checkFirstFrameExists Whether to check if the first frame is loaded or not,
     *                              if this is set to true and the first frame is not
     *                              loaded, the frame count will not be parsed.
     * @param isBeatmapSkin Whether the frame is from a beatmap skin or not.
     *
     * @return The frame index parsed from the filename, or -1 if the frame count could not be parsed.
     */
    private int parseFrameIndex(String filename, boolean checkFirstFrameExists, boolean isBeatmapSkin) {

        // If match is null, the filename does not belong to any animatable texture.
        AnimatableFrameMatch match = SkinTextureRules.matchAnimatableFrame(filename);

        String textureName = match != null ? match.getTextureName() : filename;
        int frameIndex = match != null ? match.getFrameIndex() : 0;

        var skinTextureStore = isBeatmapSkin ? customTextureStore : textureStore;

        if (match == null || checkFirstFrameExists
                && !skinTextureStore.containsKey(textureName)
                && !skinTextureStore.containsKey(textureName + "-0")
                && !skinTextureStore.containsKey(textureName + "0")) {
            skinTextureStore.removeFrameCount(textureName);
            return -1;
        }

        skinTextureStore.growFrameCount(textureName, frameIndex + 1);

        if (BuildConfig.DEBUG) {
            Log.v("ResourceManager", "Parsed frame index: " + frameIndex + " from " + filename);
        }

        return frameIndex;
    }

    public Font loadFont(final String resname, final String file, final int size,
                         final int color) {
        return fontStore.loadFont(resname, file, size, color);
    }

    public StrokeFont loadStrokeFont(final String resname, final String file,
                                     final int size, final int color1, final int color2) {
        return fontStore.loadStrokeFont(resname, file, size, color1, color2);
    }

    public Font getFont(final String resname) {
        return fontStore.getOrLoadDefault(resname);
    }

    public TextureRegion loadTexture(final String resname, final String file,
                                     final boolean external, final TextureOptions opt) {
        return textureStore.load(resname, file, external, opt);
    }

    public TextureRegion loadTexture(final String resname, final String file,
                                     final boolean external) {
        return textureStore.load(resname, file, external, TextureOptions.BILINEAR);
    }

    public TextureRegion loadBackground(final String file) {
        if (textureStore.containsKey("::background")) {
            // Deliberately not textureStore.unload(name) here, which would also remove the map
            // entry - the original code only unloads the GL texture at this point and leaves the
            // (now stale, about to be overwritten below) region in the map, since getTexture/
            // getTextureIfLoaded("::background") is read externally (GameScene, ScoringScene,
            // GameLoaderScene, RoomScene, LobbyScene) and must keep seeing *something* until this
            // method's own textureStore.put(...) calls below replace it.
            engine.getTextureManager().unloadTexture(Objects.requireNonNull(textureStore.get("::background")).getTexture());
        }
        if (file == null) {
            return textureStore.get("menu-background");
        }

        final QualityFileBitmapSource source = new QualityFileBitmapSource(new File(file));
        if (source.getWidth() == 0 || source.getHeight() == 0 || !source.preload()) {
            TextureRegion fallback = textureStore.get("menu-background");
            textureStore.put("::background", fallback);
            return fallback;
        }

        final BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        TextureRegion region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textureStore.put("::background", region);
        return region;
    }

    public TextureRegion loadHighQualityAsset(final String resname,
                                              final String file) {
        return textureStore.loadHighQualityAsset(resname, file);
    }

    public TextureRegion loadHighQualityFile(final String resname, final File file) {
        return textureStore.loadHighQualityFile(resname, file);
    }

    public void loadHighQualityFileUnderFolder(File folder) {
        File[] files = FileUtils.listFiles(folder, new String[]{
            ".png", ".jpg", ".bmp"});
        for (File file : files) {
            if (file.isDirectory()) {
                loadHighQualityFileUnderFolder(file);
            } else {
                Log.i("texture", "load: " + file.getPath());
                loadHighQualityFile(file.getPath(), file);
            }
        }
    }

    public TextureRegion getTextureWithPrefix(StringSkinData prefix, String name)
    {
        var defaultName = prefix.getDefaultValue() + "-" + name;
        if (BeatmapSkinManager.isSkinEnabled() && customTextureStore.containsKey(defaultName)) {
            return customTextureStore.get(defaultName);
        }

        var customName = prefix.getCurrentValue() + "-" + name;

        if (!textureStore.containsKey(customName)) {
            loadTexture(customName, Config.getSkinPath() + customName.replace("\\", "") + ".png", true);
        }

        TextureRegion custom = textureStore.get(customName);
        if (custom != null) {
            return custom;
        }
        return textureStore.get(defaultName);
    }

    public TextureRegion getTexture(final String resname) {
        if (BeatmapSkinManager.isSkinEnabled() && customTextureStore.containsKey(resname)) {
            return customTextureStore.get(resname);
        }
        if (!textureStore.containsKey(resname)) {
            Debug.i("Loading texture: " + resname);

            return loadTexture(resname, "gfx/" + resname + ".png", false);
        }
        return textureStore.get(resname);
    }

    public TextureRegion getAvatarTextureIfLoaded(final String avatarURL) {
        var region = getTextureIfLoaded(MD5Calculator.getStringMD5(avatarURL));

        if (region == null) {
            region = getTextureIfLoaded(MD5Calculator.getStringMD5(OnlineManager.defaultAvatarURL));
        }

        return region;
    }

    public TextureRegion getProfileBannerTextureIfLoaded(final String bannerURL) {
        if (bannerURL == null || bannerURL.length() == 0) {
            return null;
        }

        return getTextureIfLoaded(MD5Calculator.getStringMD5(bannerURL));
    }

    public TextureRegion getTextureIfLoaded(final String resname) {
        if (textureStore.containsKey(resname)) {
            return textureStore.get(resname);
        }
        return null;
    }

    public boolean isTextureLoaded(final String resname) {
        return textureStore.containsKey(resname);
    }

    public BassSoundProvider loadSound(final String resname, final String file,
                                       final boolean external) {
        return soundStore.load(resname, file, external);
    }

    public BassSoundProvider getSound(final String name) {
        return getSound(name, true);
    }

    public BassSoundProvider getSound(final String name, final boolean defaultToEmpty) {
        var sound = soundStore.get(name);

        if (sound == null && defaultToEmpty) {
            return BassSoundProvider.EMPTY;
        }

        return sound;
    }

    public void loadCustomSound(final File file) {
        BassSoundProvider snd = new BassSoundProvider();
        String resName = file.getName();
        resName = resName.substring(0, resName.length() - 4);
        if (resName.length() == 0) {
            return;
        }
        Pattern pattern = Pattern.compile("([^\\d.]+)");
        Matcher matcher = pattern.matcher(resName);
        if (matcher.find()) {
            String setName = matcher.group(1);
            if (!soundStore.containsKey(setName)) {
                // 剔除未知的音频文件
                return;
            }
        }
        try {
            if (!snd.prepare(file.getPath())) {
                return;
            }
        } catch (final Exception e) {
            Debug.e("ResourceManager.loadCustomSound: " + e.getMessage(), e);
            return;
        }

        customSoundStore.put(resName, snd);
    }

    public BassSoundProvider getCustomSound(final String name, final boolean defaultToEmpty) {
        if (BeatmapSkinManager.isSkinEnabled() && customSoundStore.containsKey(name)) {
            return customSoundStore.get(name);
        }

        return getSound(name, defaultToEmpty);
    }

    public BassSoundProvider getCustomSound(final String resname, final int set) {
        if (!BeatmapSkinManager.isSkinEnabled()) {
            return getSound(resname);
        }
        if (set >= 2) {
            String fullName = resname + set;
            if (customSoundStore.containsKey(fullName)) {
                return customSoundStore.get(fullName);
            } else {
                return soundStore.get(resname);
            }
        }
        if (customSoundStore.containsKey(resname)) {
            return customSoundStore.get(resname);
        }

        return soundStore.get(resname);
    }

    public void loadCustomTexture(final File file) {
        String resname = file.getName();
        resname = resname.substring(0, resname.length() - 4).toLowerCase();
        boolean multiframe = false;

        String delimiter = "-";

        if (parseFrameIndex(resname, true, true) < 0 && !textureStore.containsKey(resname)) {
            if (textureStore.containsKey(resname + "-0") || textureStore.containsKey(resname + "0")) {
                if (textureStore.containsKey(resname + "0")) {
                    delimiter = "";
                }
                multiframe = true;
            } else {
                return;
            }
        }
        QualityFileBitmapSource source = new QualityFileBitmapSource(file);

        if (!source.preload()) {
            return;
        }
        BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        TextureRegion region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        if (multiframe) {
            int i = 0;
            while (textureStore.containsKey(resname + delimiter + i)) {
                customTextureStore.put(resname + delimiter + i, region);
                i++;
            }
        } else {
            customTextureStore.put(resname, region);

            if (resname.equals("hitcircle")) {
                if (!customTextureStore.containsKey("sliderstartcircle")) {
                    customTextureStore.put("sliderstartcircle", region);
                }

                if (!customTextureStore.containsKey("sliderendcircle")) {
                    customTextureStore.put("sliderendcircle", region);
                }
            }

            if (resname.equals("hitcircleoverlay")) {
                if (!customTextureStore.containsKey("sliderstartcircleoverlay")) {
                    customTextureStore.put("sliderstartcircleoverlay", region);
                }

                if (!customTextureStore.containsKey("sliderendcircleoverlay")) {
                    customTextureStore.put("sliderendcircleoverlay", region);
                }
            }
        }
    }

    public void unloadTexture(final String name) {
        textureStore.unload(name);
    }

    public void unloadTexture(TextureRegion texture) {
        textureStore.unload(texture);
    }

    public void initSecurityUtils() {
        SecurityUtils.getAppSignature(context, context.getPackageName());
    }

    public void clearCustomResources() {
        for (final BassSoundProvider s : customSoundStore.getValues()) {
            s.free();
        }
        for (final String s : customTextureStore.getKeys()) {
            TextureRegion tex = customTextureStore.get(s);
            if (tex != null && tex.getTexture() != null && tex.getTexture().isLoadedToHardware()) {
                engine.getTextureManager().unloadTexture(tex.getTexture());
            }
        }
        customTextureStore.clear();
        customSoundStore.clear();
    }

    public int getFrameCount(final String texname) {
        if (BeatmapSkinManager.isSkinEnabled()) {
            int custom = customTextureStore.getFrameCount(texname);
            if (custom != -1) {
                return custom;
            }
        }

        return textureStore.getFrameCount(texname);
    }

    public void checkSpinnerTextures() {
        final String[] names = {"spinner-background", "spinner-circle",
                "spinner-metre", "spinner-approachcircle", "spinner-spin"};
        for (final String s : names) {
            TextureRegion tex = textureStore.get(s);
            if (tex != null && tex.getTexture() != null && !tex.getTexture().isLoadedToHardware()) {
                engine.getTextureManager().onReload();
                break;
            }
        }
    }

    public void checkEvoSpinnerTextures() {
        final String[] names = {
                "spinner-bottom",
                "spinner-top",
                "spinner-glow",
                "spinner-middle",
                "spinner-middle2",
                "spinner-spin",
                "spinner-clear"
        };
        for (final String s : names) {
            TextureRegion tex = textureStore.get(s);
            if (tex != null && tex.getTexture() != null && !tex.getTexture().isLoadedToHardware()) {
                engine.getTextureManager().onReload();
                break;
            }
        }
    }

}
