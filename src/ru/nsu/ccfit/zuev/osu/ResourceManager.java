package ru.nsu.ccfit.zuev.osu;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;

import com.dgsrz.bancho.security.SecurityUtils;

import org.anddev.andengine.engine.Engine;
import org.anddev.andengine.opengl.font.Font;
import org.anddev.andengine.opengl.font.FontFactory;
import org.anddev.andengine.opengl.font.StrokeFont;
import org.anddev.andengine.opengl.texture.TextureOptions;
import org.anddev.andengine.opengl.texture.atlas.bitmap.BitmapTextureAtlas;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.AssetBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.atlas.bitmap.source.FileBitmapTextureAtlasSource;
import org.anddev.andengine.opengl.texture.region.TextureRegion;
import org.anddev.andengine.opengl.texture.region.TextureRegionFactory;
import org.anddev.andengine.util.Debug;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.nsu.ccfit.zuev.audio.BassSoundProvider;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;
import ru.nsu.ccfit.zuev.osu.helper.QualityAssetBitmapSource;
import ru.nsu.ccfit.zuev.osu.helper.QualityFileBitmapSource;
import ru.nsu.ccfit.zuev.osu.helper.ScaledBitmapSource;
import ru.nsu.ccfit.zuev.skins.OsuSkin;
import ru.nsu.ccfit.zuev.skins.SkinJsonReader;
import ru.nsu.ccfit.zuev.skins.SkinManager;

public class ResourceManager {
    private static ResourceManager mgr = new ResourceManager();
    private final Map<String, Font> fonts = new HashMap<String, Font>();
    private final Map<String, TextureRegion> textures = new HashMap<String, TextureRegion>();
    private final Map<String, BassSoundProvider> sounds = new HashMap<String, BassSoundProvider>();
    private final Map<String, BassSoundProvider> customSounds = new HashMap<String, BassSoundProvider>();
    private final Map<String, TextureRegion> customTextures = new HashMap<String, TextureRegion>();
    private final Map<String, Integer> customFrameCount = new HashMap<String, Integer>();
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
        loadFont("font", null, 28, Color.WHITE);
        loadStrokeFont("strokeFont", null, 36, Color.BLACK, Color.WHITE);
        loadFont("CaptionFont", null, 35, Color.WHITE);

        if (!folder.endsWith("/"))
            folder = folder + "/";

        loadCustomSkin(folder);

        loadTexture("::track", "gfx/hitcircle.png", false,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        loadTexture("::track2", "gfx/slidertrack.png", false);
        loadTexture("::trackborder", "gfx/sliderborder.png", false,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        loadTexture("ranking_enabled", "ranking_enabled.png", false);
        loadTexture("ranking_disabled", "ranking_disabled.png", false);
        loadTexture("flashlight_cursor", "flashlight_cursor.png", false, TextureOptions.BILINEAR_PREMULTIPLYALPHA);

        if (textures.containsKey("lighting") == false)
            textures.put("lighting", null);
//		textures.put("fail-background", null);
//		textures.put("pause-overlay", null);
    }

    public void loadCustomSkin(String folder) {

        if (!folder.endsWith("/")) folder += "/";

        File[] skinFiles = null;
        File skinFolder = null;
        if (folder != null) {
            skinFolder = new File(folder);
            if (!skinFolder.exists()) {
                skinFolder = null;
            } else {
                skinFiles = FileUtils.listFiles(skinFolder);
            }
        }
        if (skinFiles != null) {
            JSONObject skinjson = null;
            File skinJson = new File(folder, "skin.json");
            if (skinJson.exists()) {
                try {
                    skinjson = new JSONObject(OsuSkin.readFull(skinJson));
                } catch (Exception e) {
                    e.printStackTrace();
                    skinjson = null;
                }
            }
            if (skinjson == null) skinjson = new JSONObject();
            SkinJsonReader.getReader().supplyJson(skinjson);
        }
        final Map<String, File> availableFiles = new HashMap<String, File>();
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

        customFrameCount.clear();

        try {
            for (final String s : context.getAssets().list("gfx")) {
                final String name = s.substring(0, s.length() - 4);
                if (Config.isCorovans() == false) {
                    if (name.equals("count1") || name.equals("count2")
                            || name.equals("count3") || name.equals("go")
                            || name.equals("ready")) {
                        continue;
                    }
                }
                if (availableFiles.containsKey(name)) {
                    loadTexture(name, availableFiles.get(name).getPath(), true);
                    if (Character.isDigit(name.charAt(name.length() - 1))) {
                        noticeFrameCount(name);
                    }
                } else {
                    loadTexture(name, "gfx/" + s, false);
                }
            }
            if (availableFiles.containsKey("scorebar-kidanger")) {
                loadTexture("scorebar-kidanger", availableFiles.get("scorebar-kidanger").getPath(), true);
                loadTexture("scorebar-kidanger2",
                        availableFiles.get(
                                availableFiles.containsKey("scorebar-kidanger2") ? "scorebar-kidanger2" : "scorebar-kidanger"
                        ).getPath(), true);
            }
            if (availableFiles.containsKey("comboburst"))
                loadTexture("comboburst", availableFiles.get("comboburst").getPath(), true);
            else unloadTexture("comboburst");
            for (int i = 0; i < 10; i++) {
                String textureName = "comboburst-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "play-skip-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "menu-back-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "scorebar-colour-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            //
            for (int i = 0; i < 60; i++) {
                String textureName = "hit0-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "hit50-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "hit100-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "hit100k-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "hit300-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "hit300k-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            for (int i = 0; i < 60; i++) {
                String textureName = "hit300g-" + i;
                if (availableFiles.containsKey(textureName))
                    loadTexture(textureName, availableFiles.get(textureName).getPath(), true);
                else unloadTexture(textureName);
            }
            //
        } catch (final IOException e) {
            Debug.e("Resources: " + e.getMessage(), e);
        }

        SkinManager.getInstance().presetFrameCount();

        try {
            // TODO: buggy?
            for (final String s : context.getAssets().list("sfx")) {
                final String name = s.substring(0, s.length() - 4);
                if (availableFiles.containsKey(name)) {
                    loadSound(name, availableFiles.get(name).getPath(), true);
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

        loadTexture("::track", "gfx/hitcircle.png", false,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        loadTexture("::track2", "gfx/slidertrack.png", false);
        loadTexture("::trackborder", "gfx/sliderborder.png", false,
                TextureOptions.BILINEAR_PREMULTIPLYALPHA);
        loadTexture("ranking_button", "ranking_button.png", false);
        loadTexture("ranking_enabled", "ranking_enabled.png", false);
        loadTexture("ranking_disabled", "ranking_disabled.png", false);
        if (textures.containsKey("lighting") == false)
            textures.put("lighting", null);
//		textures.put("fail-background", null);
//		textures.put("pause-overlay", null);
    }

    private void noticeFrameCount(final String name) {
        String resnameWN;
        if (name.contains("-") == false) {
            resnameWN = name.substring(0, name.length() - 1);
        } else {
            resnameWN = name.substring(0, name.lastIndexOf('-'));
        }
        int frameNum;
        try {
            frameNum = Integer.parseInt(name.substring(resnameWN.length()));
        } catch (final NumberFormatException e) {
            return;
        }
        if (frameNum < 0) {
            frameNum *= -1;
        }
        if (customFrameCount.containsKey(resnameWN) == false
                || customFrameCount.get(resnameWN) < frameNum) {
            customFrameCount.put(resnameWN, frameNum);
        }
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
                            + file, size, true, color1, 2 / Config.getTextureQuality(),
                    color2);
        }
        engine.getTextureManager().loadTexture(texture);
        engine.getFontManager().loadFont(font);
        fonts.put(resname, font);
        return font;
    }

    public Font getFont(final String resname) {
        if (fonts.containsKey(resname) == false) {
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
            engine.getTextureManager().unloadTexture(
                    textures.get("::background").getTexture());
        }
        if (file == null) {
            return null;
        }
        int tw = 16, th = 16;
        TextureRegion region;
        final ScaledBitmapSource source = new ScaledBitmapSource(new File(file));
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }
        while (tw < source.getWidth()) {
            tw *= 2;
        }
        while (th < source.getHeight()) {
            th *= 2;
        }
        if (source.preload() == false) {
            textures.put("::background", textures.get("menu-background"));
            return textures.get("::background");
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(tw, th,
                TextureOptions.BILINEAR);
        region = TextureRegionFactory
                .createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put("::background", region);
        return region;
    }

    public TextureRegion loadTexture(final String resname, final String file,
                                     final boolean external, final TextureOptions opt, Engine engine) {
        int tw = 4, th = 4;
        TextureRegion region;
        if (external) {
            final File texFile = new File(file);
            if (texFile.exists() == false) {
                return textures.values().iterator().next();
            }
            final QualityFileBitmapSource source = new QualityFileBitmapSource(
                    texFile);
            if (source.getWidth() == 0 || source.getHeight() == 0) {
                return null;
            }
            while (tw < source.getWidth()) {
                tw *= 2;
            }
            while (th < source.getHeight()) {
                th *= 2;
            }

            int errorCount = 0;
            while (source.preload() == false && errorCount < 3) {
                errorCount++;
            }
            if (errorCount >= 3) {
                return null;
            }
            final BitmapTextureAtlas tex = new BitmapTextureAtlas(tw, th, opt);
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0,
                    false);
            engine.getTextureManager().loadTexture(tex);
            textures.put(resname, region);
        } else {
            final QualityAssetBitmapSource source;

            try {
                source = new QualityAssetBitmapSource(
                        context, file);
            } catch (NullPointerException e) {
                return textures.values().iterator().next();
            }

            if (source.getWidth() == 0 || source.getHeight() == 0) {
                return null;
            }
            while (tw < source.getWidth()) {
                tw *= 2;
            }
            while (th < source.getHeight()) {
                th *= 2;
            }
            int errorCount = 0;
            while (source.preload() == false && errorCount < 3) {
                errorCount++;
            }
            if (errorCount >= 3) {
                return null;
            }
            final BitmapTextureAtlas tex = new BitmapTextureAtlas(tw, th, opt);
            region = TextureRegionFactory.createFromSource(tex, source, 0, 0,
                    false);
            engine.getTextureManager().loadTexture(tex);
            textures.put(resname, region);
        }

        if (region.getWidth() > 1) {
            region.setWidth(region.getWidth() - 1);
        }
        if (region.getHeight() > 1) {
            region.setHeight(region.getHeight() - 1);
        }


        return region;
    }

    public TextureRegion loadHighQualityAsset(final String resname,
                                              final String file) {
        int tw = 16, th = 16;
        TextureRegion region;

        final AssetBitmapTextureAtlasSource source = new AssetBitmapTextureAtlasSource(
                context, file);
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }
        while (tw < source.getWidth()) {
            tw *= 2;
        }
        while (th < source.getHeight()) {
            th *= 2;
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(tw, th,
                TextureOptions.BILINEAR);
        region = TextureRegionFactory
                .createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put(resname, region);
        //region.setWidth(region.getWidth() - 1);
        //region.setHeight(region.getHeight() - 1);

        return region;
    }

    public TextureRegion loadHighQualityFile(final String resname,
                                             final File file) {
        int tw = 16, th = 16;
        TextureRegion region;

        final FileBitmapTextureAtlasSource source = new FileBitmapTextureAtlasSource(file);
        if (source.getWidth() == 0 || source.getHeight() == 0) {
            return null;
        }
        while (tw < source.getWidth()) {
            tw *= 2;
        }
        while (th < source.getHeight()) {
            th *= 2;
        }
        final BitmapTextureAtlas tex = new BitmapTextureAtlas(tw, th,
                TextureOptions.BILINEAR);
        region = TextureRegionFactory
                .createFromSource(tex, source, 0, 0, false);
        engine.getTextureManager().loadTexture(tex);
        textures.put(resname, region);
        region.setWidth(region.getWidth() - 1);
        region.setHeight(region.getHeight() - 1);
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

    public TextureRegion getTexture(final String resname) {
        if (SkinManager.isSkinEnabled() && customTextures.containsKey(resname)) {
            return customTextures.get(resname);
        }
        if (textures.containsKey(resname) == false) {
            Debug.i("Loading texture: " + resname);
            return loadTexture(resname, "gfx/" + resname + ".png", false);
        }
        return textures.get(resname);
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

    public BassSoundProvider getSound(final String resname) {
        return sounds.get(resname);
    }

    public void loadCustomSound(final File file) {
        BassSoundProvider snd = new BassSoundProvider();
        String resName = file.getName();
        resName = resName.substring(0, resName.length() - 4);
        if (resName.length() == 0) {
            return;
        }
        Pattern pattern = Pattern.compile("([^\\d\\.]+)");
        Matcher matcher = pattern.matcher(resName);
        if (matcher.find()) {
            String setName = matcher.group(1);
            if (!sounds.containsKey(setName)) {
                // 剔除未知的音频文件
                return;
            }
        }
        try {
            snd.prepare(file.getPath());
        } catch (final Exception e) {
            Debug.e("ResourceManager.loadCustomSound: " + e.getMessage(), e);
            return;
        }

        customSounds.put(resName, snd);
    }

    public BassSoundProvider getCustomSound(final String resname, final int set) {
        if (!SkinManager.isSkinEnabled()) {
            return getSound(resname);
        }
        if (set >= 2) {
            String fullName = resname + String.valueOf(set);
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

        if (Character.isDigit(resname.charAt(resname.length() - 1))) {

            String resnameWN;
            if (resname.contains("-") == false) {
                resnameWN = resname.substring(0, resname.length() - 1);
            } else {
                resnameWN = resname.substring(0, resname.lastIndexOf('-'));
            }

            if (textures.containsKey(resname) == false
                    && SkinManager.getFrames(resnameWN) == 0) {
                return;
            }

            if (textures.containsKey(resnameWN)
                    || textures.containsKey(resnameWN + "-0")
                    || textures.containsKey(resnameWN + "0")) {
                int frameNum = Integer.parseInt(resname.substring(resnameWN
                        .length()));
                if (frameNum < 0) {
                    frameNum *= -1;
                }
                if (customFrameCount.containsKey(resnameWN) == false
                        || customFrameCount.get(resnameWN) < frameNum) {
                    customFrameCount.put(resnameWN, frameNum);
                }
            }
        } else if (textures.containsKey(resname) == false) {

            if (textures.containsKey(resname + "-0") || textures.containsKey(resname + "0")) {
                if (textures.containsKey(resname + "0"))
                    delimiter = "";
                if (SkinManager.getFrames(resname) != 0) {
                    customFrameCount.put(resname, 1);
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
        if (source.preload() == false) {
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
                    textures.get(name).getTexture());
            textures.remove(name);
            Debug.i("Texture \"" + name + "\"unloaded");
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
            if (customTextures.get(s).getTexture().isLoadedToHardware()) {
                engine.getTextureManager().unloadTexture(
                        customTextures.get(s).getTexture());
                // engine.getTextureManager().loadTexture(textures.get(s).getTexture());
            }
        }
        customTextures.clear();
        customSounds.clear();
        customFrameCount.clear();
    }

    public int getFrameCount(final String texname) {
        if (customFrameCount.containsKey(texname) == false) {
            return -1;
        } else {
            return customFrameCount.get(texname);
        }
    }

    public void checkSpinnerTextures() {
        final String[] names = {"spinner-background", "spinner-circle",
                "spinner-metre", "spinner-approachcircle", "spinner-spin"};
        for (final String s : names) {
            if (textures != null
                    && textures.get(s) != null
                    && textures.get(s).getTexture() != null
                    && !textures.get(s).getTexture().isLoadedToHardware()) {
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
            if (textures != null
                    && textures.get(s) != null
                    && textures.get(s).getTexture() != null
                    && !textures.get(s).getTexture().isLoadedToHardware()) {
                engine.getTextureManager().reloadTextures();
                break;
            }
        }
    }

}
