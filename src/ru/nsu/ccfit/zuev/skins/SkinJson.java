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

    private final String DEFAULT_COLOR_HEX = "#FFFFFF";
    private final RGBColor DEFAULT_COLOR = RGBColor.hex2Rgb(DEFAULT_COLOR_HEX);
    private final DefaultBoolean forceOverrideComboColor = new DefaultBoolean(false);
    private final ArrayList<RGBColor> comboColor = new ArrayList<>();
    private final DefaultRGBColor sliderBorderColor = new DefaultRGBColor(DEFAULT_COLOR);
    private final DefaultBoolean sliderFollowComboColor = new DefaultBoolean(true);
    private final DefaultRGBColor sliderBodyColor = new DefaultRGBColor(DEFAULT_COLOR);
    private final DefaultFloat sliderBodyBaseAlpha = new DefaultFloat(0.7f);
    private final DefaultBoolean limitComboTextLength = new DefaultBoolean(false);
    private final DefaultFloat comboTextScale = new DefaultFloat(1f);
    private final DefaultBoolean disableKiai = new DefaultBoolean(false);
    private final DefaultFloat sliderBodyWidth = new DefaultFloat(61f);
    private final DefaultFloat sliderBorderWidth = new DefaultFloat(5.2f);
    private final DefaultBoolean sliderHintEnable = new DefaultBoolean(false);
    private final DefaultFloat sliderHintWidth = new DefaultFloat(3f);
    private final DefaultFloat sliderHintAlpha = new DefaultFloat();
    private final DefaultRGBColor sliderHintColor = new DefaultRGBColor(null);
    private final DefaultBoolean rotateCursor = new DefaultBoolean(false);
    private final int sliderType = SliderType.FLAT;
    private final DefaultFloat sliderHintShowMinLength = new DefaultFloat(300f);
    private final DefaultBoolean useNewLayout = new DefaultBoolean(false);
    private final HashMap<String, Layout> layoutData = new HashMap<>();
    private final HashMap<String, RGBColor> colorData = new HashMap<>();

    public static SkinJson get() {
        return skinJson;
    }

    private static void parseColor(@NonNull JSONObject data, String name, @NonNull DefaultRGBColor color) {
        String defaultHex = "";
        String hex = data.optString(name, defaultHex);
        color.setCurrentValue(hex.equals(defaultHex)? color.getDefaultValue() : RGBColor.hex2Rgb(hex));
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
            comboColor.add(RGBColor.hex2Rgb("#FFFFFF"));
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

    public Layout getLayout(String name) {
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
        rotateCursor.setCurrentValue(data.optBoolean("cursorRotate"));
    }

    public void loadFloatData(String name, @NonNull JSONObject data, @NonNull DefaultFloat float_) {
        float_.setCurrentValue((float) data.optDouble(name, float_.getDefaultValue()));
    }

    public void loadBooleanData(String name, @NonNull JSONObject data, @NonNull DefaultBoolean boolean_) {
        boolean_.setCurrentValue(data.optBoolean(name, boolean_.getDefaultValue()));
    }

    public void loadSlider(@NonNull JSONObject data) {
        loadFloatData("sliderBodyWidth", data, sliderBodyWidth);
        loadFloatData("sliderBorderWidth", data, sliderBorderWidth);
        loadFloatData("sliderBodyBaseAlpha", data, sliderBodyBaseAlpha);
        loadFloatData("sliderHintWidth", data, sliderHintWidth);
        loadFloatData("sliderHintShowMinLength", data, sliderHintShowMinLength);
        loadFloatData("sliderHintAlpha", data, sliderHintAlpha);

        loadBooleanData("sliderFollowComboColor", data, sliderFollowComboColor);
        loadBooleanData("sliderHintEnable", data, sliderHintEnable);

        parseColor(data, "sliderBodyColor", sliderBodyColor);
        parseColor(data, "sliderBorderColor", sliderBorderColor);
        parseColor(data, "sliderHintColor", sliderHintColor);
        parseColor(data, "sliderHintColor", sliderHintColor);
    }

    public void loadColor(@NonNull JSONObject jsonObject) {
        JSONArray names = jsonObject.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            colorData.put(names.optString(i), RGBColor.hex2Rgb(jsonObject.optString(names.optString(i))));
        }
    }

    public void loadLayout(JSONObject jsonObject) {
        loadBooleanData("useNewLayout", jsonObject, useNewLayout);
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
        layoutData.put(name, Layout.load(object));
    }

    public void load(String tag, @NonNull JSONObject data, Consumer<JSONObject> consumer) {
        JSONObject object = data.optJSONObject(tag);
        if (object == null) {
            object = new JSONObject();
        }
        consumer.consume(object);
    }

    public void loadComboColorSetting(@NonNull JSONObject data) {
        loadBooleanData("forceOverride", data, forceOverrideComboColor);
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
        loadBooleanData("limitComboTextLength", data, limitComboTextLength);
        loadBooleanData("disableKiai", data, disableKiai);
        loadFloatData("comboTextScale", data, comboTextScale);
    }

    public static class SliderType {
        public static final int FLAT = 1;
        public static final int STABLE = 2;
    }

    public static class Layout {

        public JSONObject property;

        public float width, height, xOffset, yOffset;

        public float scale = 1;

        @NonNull
        public static Layout load(@NonNull JSONObject object) {
            Layout layout = new Layout();
            layout.property = object;
            layout.width = (float) object.optDouble("w", -1);
            layout.height = (float) object.optDouble("h", -1);
            layout.xOffset = (float) object.optDouble("x", 0);
            layout.yOffset = (float) object.optDouble("y", 0);
            layout.scale = (float) object.optDouble("scale", -1);
            return layout;
        }

        public void baseApply(@NonNull Sprite entity) {
            entity.setPosition(xOffset, yOffset);
            if (scale != -1) {
                entity.setScale(scale);
            }
            if (width != -1) {
                entity.setWidth(width);
            }
            if (height != -1) {
                entity.setHeight(height);
            }
        }

    }
}
