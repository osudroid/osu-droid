package ru.nsu.ccfit.zuev.osu;

import static kotlin.collections.ArraysKt.any;
import static kotlin.collections.ArraysKt.filter;
import static kotlin.collections.ArraysKt.joinToString;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;

import com.reco1l.osu.skinning.IniReader;
import com.reco1l.osu.skinning.SkinConverter;
import com.reco1l.andengine.texture.BlankTextureRegion;
import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;
import org.anddev.andengine.opengl.font.StrokeFont;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.util.Debug;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import kotlin.text.MatchResult;
import kotlin.text.Regex;
import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.MD5Calculator;
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource;
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource;
import ru.nsu.ccfit.zuev.osu.online.OnlineManager;
import ru.nsu.ccfit.zuev.osuplus.BuildConfig;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinJsonReader;
import ru.nsu.ccfit.zuev.skins.SkinManager;
import ru.nsu.ccfit.zuev.skins.StringSkinData;

public class ResourceManager {

    /**
     * The textures that shouldn't fallback to the default skin if they're not present in the skin folder.
     */
    private static final String[] OPTIONAL_TEXTURES = {
        "scorebar-marker",
        "scorebar-ki",
        "scorebar-kidanger",
        "scorebar-kidanger2",
    };

    /**
     * The textures that can be animated.
     */
    private static final String[] ANIMATABLE_TEXTURES = {
        "followpoint-",
        "hit0-",
        "hit100-",
        "hit100k-",
        "hit300-",
        "hit300g-",
        "hit300k-",
        "hit50-",
        "menu-back-",
        "play-skip-",
        "scorebar-colour-",
        "sliderb",
        "sliderfollowcircle-"
    };

    /**
     * <h2>Explanation</h2>
     * <p>
     * The first capturing group will refer to the texture's base name. The name may contain one or more hyphens/dashes
     * in the name (e.g <code>menu-back</code>), but it should never end with a hyphen/dash.
     * </p>
     * <p>
     * The second capturing group will refer to the frame index. A hyphen/dash may be present before the frame index
     * (e.g., <code>menu-back-0</code> (with hyphen) or <code>sliderb0</code> (without hyphen)).
     * </p>
     */
    private static final Regex ANIMATABLE_TEXTURE_REGEX = new Regex("^(" + joinToString(ANIMATABLE_TEXTURES, "|", "", "", -1, "", null) + ")(\\d+)$");


    private final static ResourceManager mgr = new ResourceManager();
    private final Map<String, Font> fonts = new HashMap<>();
    private final Map<String, TextureRegion> textures = new HashMap<>();
    private final Map<String, BassSoundProvider> sounds = new HashMap<>();
    private final Map<String, BassSoundProvider> customSounds = new HashMap<>();
    private final Map<String, TextureRegion> customTextures = new HashMap<>();
    private final Map<String, Integer> customFrameCount = new HashMap<>();
    private final BassSoundProvider emptySound = new BassSoundProvider();
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

        fonts.clear();
        textures.clear();
        sounds.clear();

        customSounds.clear();
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

        loadTexture("ranking_enabled", "ranking_enabled.png", false);
        loadTexture("ranking_disabled", "ranking_disabled.png", false);
        loadTexture("flashlight_cursor", "flashlight_cursor.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

        if (!textures.containsKey("lighting"))
            textures.put("lighting", null);
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
                        parseFrameIndex(textureName, false);
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
                if (availableFiles.containsKey(textureName)) { // No idea if this is still needed
                    File file = availableFiles.get(textureName);
                    if (file != null) {
                        loadTexture(textureName, file.getPath(), true);
                    } else {
                        unloadTexture(textureName);
                    }
                }
            }

            for (var filename : availableAnimatableFilenames) {

                var file = availableFiles.get(filename);
                if (file != null) {
                    loadTexture(filename, file.getPath(), true);
                    parseFrameIndex(filename, false);
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
        loadTexture("ranking_enabled", "ranking_enabled.png", false);
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
     *
     * @return The frame index parsed from the filename, or -1 if the frame count could not be parsed.
     */
    private int parseFrameIndex(String filename, boolean checkFirstFrameExists) {

        String textureName = filename;
        int frameIndex;

        MatchResult result = ANIMATABLE_TEXTURE_REGEX.matchEntire(filename);

        // If result is null, the filename does not match the regex pattern.
        if (result != null) {
            List<String> values = result.getGroupValues();

            textureName = values.get(1);
            if (textureName.endsWith("-")) {
                textureName = textureName.substring(0, textureName.length() - 1);
            }

            frameIndex = Integer.parseInt(values.get(2));
        } else {
            customFrameCount.remove(textureName);
            return -1;
        }

        if (checkFirstFrameExists && !textures.containsKey(textureName) && !textures.containsKey(textureName + "-0") && !textures.containsKey(textureName + "0")) {
            customFrameCount.remove(textureName);
            return -1;
        }

        if (!customFrameCount.containsKey(textureName) || Objects.requireNonNull(customFrameCount.get(textureName)) < frameIndex + 1) {
            customFrameCount.put(textureName, frameIndex + 1);
        }

        if (BuildConfig.DEBUG) {
            Log.v("ResourceManager", "Parsed frame index: " + frameIndex + " from " + filename);
        }

        return frameIndex;
    }

    public Font loadFont(final String resname, final String file, int size,
                         final int color) {
        size /= Config.getTextureQuality();
        final BitmapTextureAtlas texture = new BitmapTextureAtlas(512, 512,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        Font font;
        if (file == null) {
            font = new Font(texture, Typeface.create(Typeface.DEFAULT,
                    Typeface.NORMAL), size, true, color);
        } else {
            font = FontFactory.createFromAsset(texture, context, "fonts/"
                    + file, size, true, color);
        }
        engine.getTextureManager().loadTexture(texture);
        engine.getFontManager().loadFont(font);
        fonts.put(resname, font);
        return font;
    }

    public StrokeFont loadStrokeFont(final String resname, final String file,
                                     int size, final int color1, final int color2) {
        size /= Config.getTextureQuality();
        final BitmapTextureAtlas texture = new BitmapTextureAtlas(512, 256,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        StrokeFont font;
        if (file == null) {
            font = new StrokeFont(texture, Typeface.create(Typeface.DEFAULT,
                    Typeface.NORMAL), size, true, color1,
                    Config.getTextureQuality() == 1 ? 2 : 0.75f, color2);
        } else {
            font = FontFactory.createStrokeFromAsset(texture, context, "fonts/"
                            + file, size, true, color1, (float) 2 / Config.getTextureQuality(),
                    color2);
        }
        engine.getTextureManager().loadTexture(texture);
        engine.getFontManager().loadFont(font);
        fonts.put(resname, font);
        return font;
    }

    public Font getFont(final String resname) {
        if (!fonts.containsKey(resname)) {
            loadFont(resname, null, 35, Color.WHITE);
        }
        return fonts.get(resname);
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
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
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

            final BitmapTextureAtlas tex = new BitmapTextureAtlas(source.getWidth(), source.getHeight(), opt);
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
            final BitmapTextureAtlas tex = new BitmapTextureAtlas(source.getWidth(), source.getHeight(), opt);
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

        final BitmapTextureAtlas tex = new BitmapTextureAtlas(source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put(resname, region);

        return region;
    }

    public TextureRegion loadHighQualityFile(final String resname,
                                             final File file) {
        TextureRegion region;

        final QualityFileBitmapSource source = new QualityFileBitmapSource(file);
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(source.getWidth(), source.getHeight(), TextureOptions.BILINEAR);
        region = TextureRegionFactory.createFromSource(tex, source, 0, 0, false);
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
        if (SkinManager.isSkinEnabled() && customTextures.containsKey(defaultName)) {
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
        if (SkinManager.isSkinEnabled() && customTextures.containsKey(resname)) {
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
        BassSoundProvider snd = new BassSoundProvider();
        if (external) {
            //若是来自储存文件
            try {
                if (!snd.prepare(file)) {
                    // 外部文件加载失败尝试自带皮肤
                    String shortName = file.substring(file.lastIndexOf("/") + 1);
                    if (!snd.prepare(context.getAssets(), "sfx/" + shortName)) {
                        return null;
                    }
                }
            } catch (final Exception e) {
                Debug.e("ResourceManager.loadSoundFromExternal: " + e.getMessage(), e);
                return null;
            }
        } else {
            //若是没有自定义音效，则使用自带音效
            try {
                if (!snd.prepare(context.getAssets(), file)) {
                    return null;
                }
            } catch (final Exception e) {
                Debug.e("ResourceManager.loadSound: " + e.getMessage(), e);
                return null;
            }
        }

        sounds.put(resname, snd);

        return snd;
    }

    public BassSoundProvider getSound(final String name) {
        return getSound(name, true);
    }

    public BassSoundProvider getSound(final String name, final boolean defaultToEmpty) {
        var sound = sounds.get(name);

        if (sound == null && defaultToEmpty) {
            return emptySound;
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
            if (!sounds.containsKey(setName)) {
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

        customSounds.put(resName, snd);
    }

    public BassSoundProvider getCustomSound(final String name, final boolean defaultToEmpty) {
        if (SkinManager.isSkinEnabled() && customSounds.containsKey(name)) {
            return customSounds.get(name);
        }

        return getSound(name, defaultToEmpty);
    }

    public BassSoundProvider getCustomSound(final String resname, final int set) {
        if (!SkinManager.isSkinEnabled()) {
            return getSound(resname);
        }
        if (set >= 2) {
            String fullName = resname + set;
            if (customSounds.containsKey(fullName)) {
                return customSounds.get(fullName);
            } else {
                return sounds.get(resname);
            }
        }
        if (customSounds.containsKey(resname)) {
            return customSounds.get(resname);
        }

        return sounds.get(resname);
    }

    public void loadCustomTexture(final File file) {
        String resname = file.getName();
        resname = resname.substring(0, resname.length() - 4).toLowerCase();
        boolean multiframe = false;

        String delimiter = "-";

        if (parseFrameIndex(resname, true) < 0 && !textures.containsKey(resname)) {
            if (textures.containsKey(resname + "-0") || textures.containsKey(resname + "0")) {
                if (textures.containsKey(resname + "0")) {
                    delimiter = "";
                }
                multiframe = true;
            } else {
                return;
            }
        }
        int tw = 16, th = 16;
        final QualityFileBitmapSource source = new QualityFileBitmapSource(file);
        while (tw < source.getWidth()) {
            tw *= 2;
        }
        while (th < source.getHeight()) {
            th *= 2;
        }
        if (!source.preload()) {
            return;
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(tw, th,
                TextureOptions.BILINEAR);
        final TextureRegion region = TextureRegionFactory.createFromSource(tex,
                source, 0, 0, false);
        // engine.getTextureManager().unloadTexture(textures.get(resname).getTexture());
        engine.getTextureManager().loadTexture(tex);
        if (region.getWidth() > 1) {
            region.setWidth(region.getWidth() - 1);
        }
        if (region.getHeight() > 1) {
            region.setHeight(region.getHeight() - 1);
        }
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

        for (var entry : textures.entrySet()) {
            if (entry.getValue() == texture) {
                toRemove.add(entry.getKey());
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
        for (final BassSoundProvider s : customSounds.values()) {
            s.free();
        }
        final Set<String> texnames = customTextures.keySet();
        for (final String s : texnames) {
            TextureRegion tex = customTextures.get(s);
            if (tex != null && tex.getTexture() != null && tex.getTexture().isLoadedToHardware()) {
                engine.getTextureManager().unloadTexture(tex.getTexture());
                // engine.getTextureManager().loadTexture(textures.get(s).getTexture());
            }
        }
        customTextures.clear();
        customSounds.clear();
        customFrameCount.clear();
    }

    public int getFrameCount(final String texname) {
        if (!customFrameCount.containsKey(texname)) {
            return -1;
        } else {
            return Objects.requireNonNull(customFrameCount.get(texname));
        }
    }

    public void checkSpinnerTextures() {
        final String[] names = {"spinner-background", "spinner-circle",
                "spinner-metre", "spinner-approachcircle", "spinner-spin"};
        for (final String s : names) {
            TextureRegion tex = textures.get(s);
            if (tex != null && tex.getTexture() != null && !tex.getTexture().isLoadedToHardware()) {
                engine.getTextureManager().reloadTextures();
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
                engine.getTextureManager().reloadTextures();
                break;
            }
        }
    }

}
