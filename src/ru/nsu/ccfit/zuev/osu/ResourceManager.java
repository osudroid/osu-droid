package ru.nsu.ccfit.zuev.osu;

import static kotlin.collections.ArraysKt.any;
import static kotlin.collections.ArraysKt.filter;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;

import com.osudroid.resources.FontStore;
import com.osudroid.resources.SoundStore;
import com.osudroid.resources.skin.AnimatableFrameMatch;
import com.osudroid.resources.skin.SkinTextureRules;
import com.osudroid.ui.skinning.StringSkinData;
import com.osudroid.ui.skinning.IniReader;
import com.osudroid.ui.skinning.SkinConverter;
import com.reco1l.andengine.UIEngine;
import com.reco1l.andengine.texture.BlankTextureRegion;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource;
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
    // during rendering. Plain HashMaps corrupt under that access pattern, so these must stay synchronized.
    // (Collections.synchronizedMap rather than ConcurrentHashMap because "textures" intentionally stores
    // null values as tombstones, e.g. the "lighting" entry below.)
    private final FontStore fontStore = new FontStore();
    private final SoundStore soundStore = new SoundStore();
    private final SoundStore customSoundStore = new SoundStore();

    private final Map<String, Integer> frameCount = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, TextureRegion> textures = Collections.synchronizedMap(new HashMap<>());

    private final Map<String, Integer> customFrameCount = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, TextureRegion> customTextures = Collections.synchronizedMap(new HashMap<>());

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

        fontStore.clear();
        textures.clear();
        soundStore.clear();
        frameCount.clear();

        customSoundStore.clear();
        customTextures.clear();
        customFrameCount.clear();

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

        if (!textures.containsKey("lighting"))
            textures.put("lighting", null);

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

        // Removing loaded animatable textures from the previous skin. Usage of toArray() is necessary to avoid ConcurrentModificationException.
        for (var key : textures.keySet().toArray(new String[0])) {
            if (any(ANIMATABLE_TEXTURES, key::startsWith)) {
                unloadTexture(key);
            }
        }

        frameCount.clear();
        customFrameCount.clear();

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
        if (!textures.containsKey("lighting"))
            textures.put("lighting", null);
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

        var skinTextures = isBeatmapSkin ? customTextures : textures;
        var skinFrameCount = isBeatmapSkin ? customFrameCount : frameCount;

        if (match == null || checkFirstFrameExists
                && !skinTextures.containsKey(textureName)
                && !skinTextures.containsKey(textureName + "-0")
                && !skinTextures.containsKey(textureName + "0")) {
            skinFrameCount.remove(textureName);
            return -1;
        }

        //noinspection DataFlowIssue
        if (!skinFrameCount.containsKey(textureName) || skinFrameCount.get(textureName) < frameIndex + 1) {
            skinFrameCount.put(textureName, frameIndex + 1);
        }

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
        return loadTexture(resname, file, external, opt, this.engine);
    }

    public TextureRegion loadTexture(final String resname, final String file,
                                     final boolean external) {
        return loadTexture(resname, file, external, TextureOptions.BILINEAR, this.engine);
    }

    public TextureRegion loadTexture(final String resname, final String file,
                                     final boolean external, Engine engine) {
        return loadTexture(resname, file, external, TextureOptions.BILINEAR, engine);
    }

    public TextureRegion loadBackground(final String file) {
        return loadBackground(file, this.engine);
    }

    public TextureRegion loadBackground(final String file, Engine engine) {
        if (textures.containsKey("::background")) {
            engine.getTextureManager().unloadTexture(Objects.requireNonNull(textures.get("::background")).getTexture());
        }
        if (file == null) {
            return textures.get("menu-background");
        }
        TextureRegion region;
        final QualityFileBitmapSource source = new QualityFileBitmapSource(new File(file));
        if (source.getWidth() == 0 || source.getHeight() == 0 || !source.preload()) {
            textures.put("::background", textures.get("menu-background"));
            return textures.get("::background");
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put("::background", region);
        return region;
    }

    public TextureRegion loadTexture(final String resname, final String file,
                                     final boolean external, final TextureOptions opt, Engine engine) {
        TextureRegion region;
        if (external) {
            var texFile = new File(file);
            var isHDTexture = false;

            if (!texFile.exists()) {

                var dotIndex = file.lastIndexOf('.');

                texFile = new File(file.substring(0, dotIndex) + "@2x" + file.substring(dotIndex));
                isHDTexture = texFile.exists();

                if (!isHDTexture) {
                    return new BlankTextureRegion();
                }
            }
            final QualityFileBitmapSource source = new QualityFileBitmapSource(texFile, isHDTexture ? 2 : 1);

            if (source.getWidth() == 0 || source.getHeight() == 0 || !source.preload()) {
                return null;
            }

            final BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), opt);
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
            engine.getTextureManager().loadTexture(tex);
            textures.put(resname, region);
        } else {
            final QualityAssetBitmapSource source;

            try {
                source = new QualityAssetBitmapSource(context, file);
            } catch (NullPointerException e) {
                return new BlankTextureRegion();
            }

            if (source.getWidth() == 0 || source.getHeight() == 0 || !source.preload()) {
                return null;
            }
            final BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), opt);
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
            engine.getTextureManager().loadTexture(tex);
            textures.put(resname, region);
        }

        return region;
    }

    public TextureRegion loadHighQualityAsset(final String resname,
                                              final String file) {
        TextureRegion region;

        final QualityAssetBitmapSource source = new QualityAssetBitmapSource(context, file);
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }

        final BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put(resname, region);

        return region;
    }

    public TextureRegion loadHighQualityFile(final String resname, final File file) {
        QualityFileBitmapSource source = new QualityFileBitmapSource(file);
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }
        BitmapTextureAtlas tex = new BitmapTextureAtlas(engine.getTextureManager(), source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        TextureRegion region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put(resname, region);
        return region;
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
        if (BeatmapSkinManager.isSkinEnabled() && customTextures.containsKey(defaultName)) {
            return customTextures.get(defaultName);
        }

        var customName = prefix.getCurrentValue() + "-" + name;

        if (!textures.containsKey(customName)) {
            loadTexture(customName, Config.getSkinPath() + customName.replace("\\", "") + ".png", true);
        }

        if (textures.get(customName) != null) {
            return textures.get(customName);
        }
        return textures.get(defaultName);
    }

    public TextureRegion getTexture(final String resname) {
        if (BeatmapSkinManager.isSkinEnabled() && customTextures.containsKey(resname)) {
            return customTextures.get(resname);
        }
        if (!textures.containsKey(resname)) {
            Debug.i("Loading texture: " + resname);

            return loadTexture(resname, "gfx/" + resname + ".png", false);
        }
        return textures.get(resname);
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
        if (textures.containsKey(resname)/*
         * &&
         * textures.get(resname).getTexture().
         * isLoadedToHardware()
         */) {
            return textures.get(resname);
        }
        return null;
    }

    public boolean isTextureLoaded(final String resname) {
        return textures.containsKey(resname);
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

        if (parseFrameIndex(resname, true, true) < 0 && !textures.containsKey(resname)) {
            if (textures.containsKey(resname + "-0") || textures.containsKey(resname + "0")) {
                if (textures.containsKey(resname + "0")) {
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
            while (textures.containsKey(resname + delimiter + i)) {
                customTextures.put(resname + delimiter + i, region);
                i++;
            }
        } else {
            customTextures.put(resname, region);

            if (resname.equals("hitcircle")) {
                if (!customTextures.containsKey("sliderstartcircle")) {
                    customTextures.put("sliderstartcircle", region);
                }

                if (!customTextures.containsKey("sliderendcircle")) {
                    customTextures.put("sliderendcircle", region);
                }
            }

            if (resname.equals("hitcircleoverlay")) {
                if (!customTextures.containsKey("sliderstartcircleoverlay")) {
                    customTextures.put("sliderstartcircleoverlay", region);
                }

                if (!customTextures.containsKey("sliderendcircleoverlay")) {
                    customTextures.put("sliderendcircleoverlay", region);
                }
            }
        }
    }

    public void unloadTexture(final String name) {
        if (textures.get(name) != null) {
            engine.getTextureManager().unloadTexture(
                    Objects.requireNonNull(textures.get(name)).getTexture());
            textures.remove(name);
            Debug.i("Texture \"" + name + "\"unloaded");
        }
    }

    public void unloadTexture(TextureRegion texture) {
        engine.getTextureManager().unloadTexture(texture.getTexture());

        List<String> toRemove = new ArrayList<>();

        // Manual iteration over a synchronizedMap's view isn't guarded by the map's own lock,
        // so it must be wrapped explicitly to avoid a ConcurrentModificationException if a
        // background thread mutates "textures" (e.g. skin loading) at the same time.
        synchronized (textures) {
            for (var entry : textures.entrySet()) {
                if (entry.getValue() == texture) {
                    toRemove.add(entry.getKey());
                }
            }
        }

        for (var key : toRemove) {
            textures.remove(key);
        }
    }

    public void initSecurityUtils() {
        SecurityUtils.getAppSignature(context, context.getPackageName());
    }

    public void clearCustomResources() {
        for (final BassSoundProvider s : customSoundStore.getValues()) {
            s.free();
        }
        synchronized (customTextures) {
            for (final String s : customTextures.keySet()) {
                TextureRegion tex = customTextures.get(s);
                if (tex != null && tex.getTexture() != null && tex.getTexture().isLoadedToHardware()) {
                    engine.getTextureManager().unloadTexture(tex.getTexture());
                }
            }
        }
        customTextures.clear();
        customSoundStore.clear();
        customFrameCount.clear();
    }

    public int getFrameCount(final String texname) {

        boolean isCustom = BeatmapSkinManager.isSkinEnabled() && customFrameCount.containsKey(texname);

        if (isCustom) {
            //noinspection DataFlowIssue
            return customFrameCount.get(texname);
        }

        if (!frameCount.containsKey(texname)) {
            return -1;
        }

        //noinspection DataFlowIssue
        return frameCount.get(texname);
    }

    public void checkSpinnerTextures() {
        final String[] names = {"spinner-background", "spinner-circle",
                "spinner-metre", "spinner-approachcircle", "spinner-spin"};
        for (final String s : names) {
            TextureRegion tex = textures.get(s);
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
            TextureRegion tex = textures.get(s);
            if (tex != null && tex.getTexture() != null && !tex.getTexture().isLoadedToHardware()) {
                engine.getTextureManager().onReload();
                break;
            }
        }
    }

}
