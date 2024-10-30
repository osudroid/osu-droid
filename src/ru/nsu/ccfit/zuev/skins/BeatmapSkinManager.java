package ru.nsu.ccfit.zuev.skins;

import java.io.File;

import ru.nsu.ccfit.zuev.osu.Config;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.ResourceManager;
import ru.nsu.ccfit.zuev.osu.helper.FileUtils;

public class BeatmapSkinManager {
    private static BeatmapSkinManager instance = new BeatmapSkinManager();
    private static boolean skinEnabled = true;

    private final RGBColor sliderColor = new RGBColor(1, 1, 1);
    private String skinname = "";

    private BeatmapSkinManager() {

    }

    public static BeatmapSkinManager getInstance() {
        return instance;
    }

    public static boolean isSkinEnabled() {
        return skinEnabled;
    }

    public static void setSkinEnabled(final boolean skinEnabled) {
        BeatmapSkinManager.skinEnabled = skinEnabled;
    }

    public RGBColor getSliderColor() {
        return sliderColor;
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
    }

    public void clearSkin() {
        if (skinname.isEmpty()) {
            return;
        }
        skinname = "";
        ResourceManager.getInstance().clearCustomResources();
    }
}
