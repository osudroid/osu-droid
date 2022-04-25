package ru.nsu.ccfit.zuev.skins;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class SkinManager {
    private static SkinManager instance = new SkinManager();
    private static Map<String, Integer> frameCount = new HashMap<String, Integer>();
    private static Map<String, Integer> stdframeCount = new HashMap<String, Integer>();
    private static boolean skinEnabled = true;

    static {
        stdframeCount.put("sliderb", 10);
        stdframeCount.put("followpoint", 1);
        stdframeCount.put("scorebar-colour", 4);
        stdframeCount.put("play-skip", 1);
        stdframeCount.put("sliderfollowcircle", 1);
        frameCount.putAll(stdframeCount);
    }

    private final RGBColor sliderColor = new RGBColor(1, 1, 1);
    private String skinname = "";

    private SkinManager() {

    }

    public static SkinManager getInstance() {
        return instance;
    }

    public static boolean isSkinEnabled() {
        return skinEnabled;
    }

    public static void setSkinEnabled(final boolean skinEnabled) {
        SkinManager.skinEnabled = skinEnabled;
    }

    public static int getFrames(final String texname) {
        if (frameCount.containsKey(texname) == false) {
            return 0;
        }
        return frameCount.get(texname);
    }

    public static void setFrames(final String texname, final int frames) {
        frameCount.put(texname, frames);
    }

    public RGBColor getSliderColor() {
        return sliderColor;
    }

    public void presetFrameCount() {
        stdframeCount.put("sliderb", 10);
        stdframeCount.put("followpoint", 1);
        stdframeCount.put("scorebar-colour", 4);
        stdframeCount.put("play-skip", 1);
        stdframeCount.put("sliderfollowcircle", 1);

        for (final String s : stdframeCount.keySet()) {
            final int fcount = ResourceManager.getInstance().getFrameCount(s);
            if (fcount >= 0) {
                stdframeCount.put(s, fcount);
            }
        }
        frameCount.clear();
        frameCount.putAll(stdframeCount);
    }

    public void loadBeatmapSkin(final String beatmapFolder) {
        skinEnabled = true;
        if (skinname.equals(beatmapFolder)) {
            return;
        }
        clearSkin();
        skinname = beatmapFolder;
        final File folderFile = new File(beatmapFolder);
        File[] folderFiles = FileUtils.listFiles(folderFile, new String[]{
            ".wav", ".mp3", ".ogg", ".png", ".jpg"});
        for (final File f : folderFiles) {
            if (!f.isFile()) {
                continue;
            }
            if (Config.isUseCustomSounds()
                    && (f.getName().toLowerCase().matches(".*[.]wav")
                    || f.getName().toLowerCase().matches(".*[.]mp3")
                    || f.getName().toLowerCase().matches(".*[.]ogg"))
                && f.length() >= 1024) {
                ResourceManager.getInstance().loadCustomSound(f);
            } else if (Config.isUseCustomSkins()
                    && (f.getName().toLowerCase().matches(".*[.]png")
                    || f.getName().toLowerCase().matches(".*[.]jpg"))) {
                ResourceManager.getInstance().loadCustomTexture(f);
            }

        }

        if (!Config.isUseCustomSkins()) return;

        for (final String s : frameCount.keySet()) {
            final int fcount = ResourceManager.getInstance().getFrameCount(s);
            if (fcount >= 0) {
                frameCount.put(s, fcount);
            }
        }
    }

    public void clearSkin() {
        if (skinname.equals("")) {
            return;
        }
        skinname = "";
        frameCount.put("sliderb", stdframeCount.get("sliderb"));
        frameCount.put("followpoint", stdframeCount.get("followpoint"));
        frameCount.put("scorebar-colour", stdframeCount.get("scorebar-colour"));
        frameCount.put("play-skip", stdframeCount.get("play-skip"));
        frameCount.put("sliderfollowcircle",
                stdframeCount.get("sliderfollowcircle"));
        ResourceManager.getInstance().clearCustomResources();
    }
}
