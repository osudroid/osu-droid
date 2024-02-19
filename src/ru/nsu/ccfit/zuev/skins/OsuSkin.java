package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import okio.BufferedSource;
import okio.Okio;
import ru.nsu.ccfit.zuev.osu.RGBColor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class OsuSkin {
    private static final OsuSkin skinJson = new OsuSkin();

    protected final FloatSkinData comboTextScale = new FloatSkinData("comboTextScale", 1f);
    protected final FloatSkinData sliderBodyWidth = new FloatSkinData("sliderBodyWidth", 61f);
    protected final FloatSkinData sliderBorderWidth = new FloatSkinData("sliderBorderWidth", 5.2f);
    protected final FloatSkinData hitCircleOverlap = new FloatSkinData("hitCircleOverlap", -2);

    protected final BooleanSkinData limitComboTextLength = new BooleanSkinData("limitComboTextLength");
    protected final BooleanSkinData disableKiai = new BooleanSkinData("disableKiai");
    protected final BooleanSkinData sliderFollowComboColor = new BooleanSkinData("sliderFollowComboColor", true);
    protected final BooleanSkinData useNewLayout = new BooleanSkinData("useNewLayout");
    protected final BooleanSkinData forceOverrideComboColor = new BooleanSkinData("forceOverride");
    protected final BooleanSkinData rotateCursor = new BooleanSkinData("rotateCursor", true);

    protected final String DEFAULT_COLOR_HEX = "#FFFFFF";
    protected final ArrayList<RGBColor> comboColor = new ArrayList<>();

    protected final ColorSkinData sliderBorderColor = new ColorSkinData("sliderBorderColor", DEFAULT_COLOR_HEX);
    protected final ColorSkinData sliderBodyColor = new ColorSkinData("sliderBodyColor", DEFAULT_COLOR_HEX);

    protected final StringSkinData hitCirclePrefix = new StringSkinData("hitCirclePrefix", "default");
    protected final StringSkinData scorePrefix = new StringSkinData("scorePrefix", "score");
    protected final StringSkinData comboPrefix = new StringSkinData("comboPrefix", "score");

    protected final HashMap<String, SkinLayout> layoutData = new HashMap<>();
    protected final HashMap<String, RGBColor> colorData = new HashMap<>();

    public static OsuSkin get() {
        return skinJson;
    }

    @NonNull
    public static String readFull(File file) throws IOException {
        BufferedSource source = Okio.buffer(Okio.source(file));
        String result = source.readUtf8();
        source.close();
        return result;
    }

    public boolean isRotateCursor() {
        return rotateCursor.getCurrentValue();
    }

    public float getComboTextScale() {
        return comboTextScale.getCurrentValue();
    }

    public boolean isUseNewLayout() {
        return useNewLayout.getCurrentValue();
    }

    public float getSliderBodyWidth() {
        return sliderBodyWidth.getCurrentValue();
    }

    public float getSliderBorderWidth() {
        return sliderBorderWidth.getCurrentValue();
    }

    public boolean isDisableKiai() {
        return disableKiai.getCurrentValue();
    }

    public boolean isLimitComboTextLength() {
        return limitComboTextLength.getCurrentValue();
    }

    public boolean isForceOverrideComboColor() {
        return forceOverrideComboColor.getCurrentValue();
    }

    public ArrayList<RGBColor> getComboColor() {
        if (comboColor.isEmpty()) {
            comboColor.add(RGBColor.hex2Rgb(DEFAULT_COLOR_HEX));
        }
        return comboColor;
    }

    public boolean isForceOverrideSliderBorderColor() {
        return !sliderBorderColor.currentIsDefault();
    }

    public RGBColor getSliderBorderColor() {
        return sliderBorderColor.getCurrentValue();
    }

    public boolean isSliderFollowComboColor() {
        return sliderFollowComboColor.getCurrentValue();
    }

    public RGBColor getSliderBodyColor() {
        return sliderBodyColor.getCurrentValue();
    }

    public SkinLayout getLayout(String name) {
        return layoutData.get(name);
    }

    public RGBColor getColor(String name, RGBColor fallback) {
        RGBColor color = colorData.get(name);
        return color == null ? fallback : color;
    }

    public StringSkinData getHitCirclePrefix() {
        return hitCirclePrefix;
    }

    public StringSkinData getScorePrefix() {
        return scorePrefix;
    }

    public StringSkinData getComboPrefix() {
        return comboPrefix;
    }

    public float getHitCircleOverlap() {
        return hitCircleOverlap.getCurrentValue();
    }

    public void reset() {
        layoutData.clear();
        colorData.clear();
    }
}
