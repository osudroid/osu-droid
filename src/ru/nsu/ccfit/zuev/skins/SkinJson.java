package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import com.edlplan.framework.utils.interfaces.Consumer;

import okio.BufferedSource;
import okio.Okio;
import ru.nsu.ccfit.zuev.osu.RGBColor;
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultBoolean;
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultFloat;
import ru.nsu.ccfit.zuev.osu.datatypes.DefaultRGBColor;

import org.anddev.andengine.entity.sprite.Sprite;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SkinJson {
    private static final SkinJson skinJson = new SkinJson();

    private final FloatSkinData comboTextScale = new FloatSkinData("comboTextScale", 1f);
    private final FloatSkinData sliderHintWidth = new FloatSkinData("sliderHintWidth", 3f);
    private final FloatSkinData sliderBodyWidth = new FloatSkinData("sliderBodyWidth", 61f);
    private final FloatSkinData sliderBorderWidth = new FloatSkinData("sliderBorderWidth", 5.2f);
    private final FloatSkinData sliderBodyBaseAlpha = new FloatSkinData("sliderBodyBaseAlpha", 0.7f);
    private final FloatSkinData sliderHintAlpha = new FloatSkinData("sliderHintAlpha");
    private final FloatSkinData sliderHintShowMinLength = new FloatSkinData("sliderHintShowMinLength", 300f);

    private final BooleanSkinData limitComboTextLength = new BooleanSkinData("limitComboTextLength");
    private final BooleanSkinData disableKiai = new BooleanSkinData("disableKiai");
    private final BooleanSkinData sliderHintEnable = new BooleanSkinData("sliderHintEnable");
    private final BooleanSkinData sliderFollowComboColor = new BooleanSkinData("sliderFollowComboColor", true);
    private final BooleanSkinData useNewLayout = new BooleanSkinData("useNewLayout");
    private final BooleanSkinData forceOverrideComboColor = new BooleanSkinData("forceOverrideComboColor");
    private final BooleanSkinData rotateCursor = new BooleanSkinData("rotateCursor", true);

    private final String DEFAULT_COLOR_HEX = "#FFFFFF";
    private final RGBColor DEFAULT_COLOR = RGBColor.hex2Rgb(DEFAULT_COLOR_HEX);
    private final ArrayList<RGBColor> comboColor = new ArrayList<>();

    private final ColorSkinData sliderBorderColor = new ColorSkinData("sliderBorderColor", DEFAULT_COLOR);
    private final ColorSkinData sliderBodyColor = new ColorSkinData("sliderBodyColor", DEFAULT_COLOR);
    private final ColorSkinData sliderHintColor = new ColorSkinData("sliderHintColor", DEFAULT_COLOR);

    private final SkinSliderType skinSliderType = SkinSliderType.FLAT;

    private final HashMap<String, SkinLayout> layoutData = new HashMap<>();
    private final HashMap<String, RGBColor> colorData = new HashMap<>();

    public static SkinJson get() {
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

    public boolean isSliderHintEnable() {
        return sliderHintEnable.getCurrentValue();
    }

    public float getSliderHintAlpha() {
        return sliderHintAlpha.getCurrentValue();
    }

    public float getSliderHintWidth() {
        return sliderHintWidth.getCurrentValue();
    }

    public RGBColor getSliderHintColor() {
        return sliderHintColor.getCurrentValue();
    }

    public float getSliderHintShowMinLength() {
        return sliderHintShowMinLength.getCurrentValue();
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

    public float getSliderBodyBaseAlpha() {
        return sliderBodyBaseAlpha.getCurrentValue();
    }

    public boolean isForceOverrideComboColor() {
        return forceOverrideComboColor.getCurrentValue();
    }

    public ArrayList<RGBColor> getComboColor() {
        if (comboColor.isEmpty()) {
            comboColor.add(DEFAULT_COLOR);
        }
        return comboColor;
    }

    public boolean isForceOverrideSliderBorderColor() {
        return sliderBorderColor.getCurrentValue() != sliderBorderColor.getDefaultValue();
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

    public void reset() {
        layoutData.clear();
        colorData.clear();
    }

    public void loadSkinJson(JSONObject json) {
        reset();
        load("ComboColor", json, this::loadComboColorSetting);
        load("Slider", json, this::loadSlider);
        load("Utils", json, this::loadUtils);
        load("Layout", json, this::loadLayout);
        load("Color", json, this::loadColor);
        load("Cursor", json, this::loadCursor);
    }

    public void loadCursor(@NonNull JSONObject data) {
        rotateCursor.setFromJson(data);
    }

    public void loadSlider(@NonNull JSONObject data) {
        sliderBodyWidth.setFromJson(data);
        sliderBodyWidth.setFromJson(data);
        sliderBodyBaseAlpha.setFromJson(data);
        sliderHintWidth.setFromJson(data);
        sliderHintShowMinLength.setFromJson(data);
        sliderHintAlpha.setFromJson(data);
        sliderFollowComboColor.setFromJson(data);
        sliderHintEnable.setFromJson(data);
        sliderBodyColor.setFromJson(data);
        sliderBorderColor.setFromJson(data);
        sliderHintColor.setFromJson(data);
    }

    public void loadColor(@NonNull JSONObject jsonObject) {
        JSONArray names = jsonObject.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            colorData.put(names.optString(i), RGBColor.hex2Rgb(jsonObject.optString(names.optString(i))));
        }
    }

    public void loadLayout(JSONObject jsonObject) {
        useNewLayout.setFromJson(jsonObject);
        JSONArray names = jsonObject.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            if (names.optString(i).equals("useNewLayout")) {
                continue;
            }
            putLayout(names.optString(i), jsonObject.optJSONObject(names.optString(i)));
        }
    }

    public void putLayout(String name, JSONObject object) {
        layoutData.put(name, SkinLayout.load(object));
    }

    public void load(String tag, @NonNull JSONObject data, Consumer<JSONObject> consumer) {
        JSONObject object = data.optJSONObject(tag);
        if (object == null) {
            object = new JSONObject();
        }
        consumer.consume(object);
    }

    public void loadComboColorSetting(@NonNull JSONObject data) {
        forceOverrideComboColor.setFromJson(data);
        comboColor.clear();
        JSONArray array = data.optJSONArray("colors");
        if (array == null || array.length() == 0) {
            comboColor.add(DEFAULT_COLOR);
        } else {
            for (int i = 0; i < array.length(); i++) {
                comboColor.add(RGBColor.hex2Rgb(array.optString(i, DEFAULT_COLOR_HEX)));
            }
        }
    }

    public void loadUtils(@NonNull JSONObject data) {
        limitComboTextLength.setFromJson(data);
        disableKiai.setFromJson(data);
        comboTextScale.setFromJson(data);
    }
}
