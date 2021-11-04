package ru.nsu.ccfit.zuev.osu;

import com.edlplan.framework.utils.interfaces.Consumer;

import okio.BufferedSource;
import okio.Okio;

import org.anddev.andengine.entity.sprite.Sprite;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class SkinJson {

    private static SkinJson skinJson = new SkinJson();
    private boolean forceOverrideComboColor = false;
    private ArrayList<RGBColor> comboColor;
    private RGBColor sliderBorderColor = null;
    private boolean sliderFollowComboColor = true;
    private RGBColor sliderBodyColor;
    private float sliderBodyBaseAlpha = 0.7f;
    private boolean limitComboTextLength = false;
    private float comboTextScale = 1;
    private boolean disableKiai = false;
    private float sliderBodyWidth = 61;
    private float sliderBorderWidth = 5.2f;
    private int sliderType = 1;
    private boolean sliderHintEnable = false;
    private float sliderHintWidth = 3;
    private float sliderHintAlpha = 0;
    private RGBColor sliderHintColor;
    private float sliderHintShowMinLength = 300;
    private boolean useNewLayout = false;
    private HashMap<String, Layout> layoutData = new HashMap<>();
    private HashMap<String, RGBColor> colorData = new HashMap<>();

    public static SkinJson get() {
        return skinJson;
    }

    private static RGBColor parseColor(JSONObject data, String name, RGBColor fallback) {
        return data.optString(name, null) == null ?
                fallback : RGBColor.hex2Rgb(data.optString(name));
    }

    public static String readFull(File file) throws IOException {
        BufferedSource source = Okio.buffer(Okio.source(file));
        String result = source.readUtf8();
        source.close();
        return result;
    }

    public float getComboTextScale() {
        return comboTextScale;
    }

    public boolean isUseNewLayout() {
        return useNewLayout;
    }

    public boolean isSliderHintEnable() {
        return sliderHintEnable;
    }

    public float getSliderHintAlpha() {
        return sliderHintAlpha;
    }

    public float getSliderHintWidth() {
        return sliderHintWidth;
    }

    public RGBColor getSliderHintColor() {
        return sliderHintColor;
    }

    public float getSliderHintShowMinLength() {
        return sliderHintShowMinLength;
    }

    public float getSliderBodyWidth() {
        return sliderBodyWidth;
    }

    public float getSliderBorderWidth() {
        return sliderBorderWidth;
    }

    public boolean isDisableKiai() {
        return disableKiai;
    }

    public boolean isLimitComboTextLength() {
        return limitComboTextLength;
    }

    public float getSliderBodyBaseAlpha() {
        return sliderBodyBaseAlpha;
    }

    public boolean isForceOverrideComboColor() {
        return forceOverrideComboColor;
    }

    public ArrayList<RGBColor> getComboColor() {
        if (comboColor == null) {
            comboColor = new ArrayList<>();
            comboColor.add(RGBColor.hex2Rgb("#FFFFFF"));
        }
        return comboColor;
    }

    public boolean isForceOverrideSliderBorderColor() {
        return sliderBorderColor != null;
    }

    public RGBColor getSliderBorderColor() {
        return sliderBorderColor;
    }

    public boolean isSliderFollowComboColor() {
        return sliderFollowComboColor;
    }

    public RGBColor getSliderBodyColor() {
        return sliderBodyColor;
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
    }

    public void loadSlider(JSONObject data) {
        sliderBodyWidth = (float) data.optDouble("sliderBodyWidth", 61);
        sliderBorderWidth = (float) data.optDouble("sliderBorderWidth", 5.2f);

        sliderBodyBaseAlpha = (float) data.optDouble("sliderBodyBaseAlpha", 0.7f);

        sliderFollowComboColor = data.optBoolean("sliderFollowComboColor", true);
        sliderBodyColor = data.optString("sliderBodyColor", null) == null ?
                RGBColor.hex2Rgb("#FFFFFF") : RGBColor.hex2Rgb(data.optString("sliderBodyColor"));

        sliderBorderColor = data.optString("sliderBorderColor", null) == null ?
                null : RGBColor.hex2Rgb(data.optString("sliderBorderColor"));

        sliderHintEnable = data.optBoolean("sliderHintEnable", false);
        sliderHintAlpha = (float) data.optDouble("sliderHintAlpha", 0.3f);
        sliderHintColor = parseColor(data, "sliderHintColor", null);
        sliderHintWidth = (float) data.optDouble("sliderHintWidth", 3);
        sliderHintShowMinLength = (float) data.optDouble("sliderHintShowMinLength", 300);
    }

    public void loadColor(JSONObject jsonObject) {
        JSONArray names = jsonObject.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            colorData.put(names.optString(i), RGBColor.hex2Rgb(jsonObject.optString(names.optString(i))));
        }
    }

    public void loadLayout(JSONObject jsonObject) {
        useNewLayout = jsonObject.optBoolean("useNewLayout", false);
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

    public void load(String tag, JSONObject data, Consumer<JSONObject> consumer) {
        JSONObject object = data.optJSONObject(tag);
        if (object == null) {
            object = new JSONObject();
        }
        consumer.consume(object);
    }

    public void loadComboColorSetting(JSONObject data) {
        forceOverrideComboColor = data.optBoolean("forceOverride", false);
        comboColor = new ArrayList<>();
        JSONArray array = data.optJSONArray("colors");
        if (array == null || array.length() == 0) {
            comboColor.add(RGBColor.hex2Rgb("#FFFFFF"));
        } else {
            for (int i = 0; i < array.length(); i++) {
                comboColor.add(RGBColor.hex2Rgb(array.optString(i, "#FFFFFF")));
            }
        }
    }

    public void loadUtils(JSONObject data) {
        limitComboTextLength = data.optBoolean("limitComboTextLength", false);
        disableKiai = data.optBoolean("disableKiai", false);
        comboTextScale = (float) data.optDouble("comboTextScale", 1);
    }

    public static class SliderType {
        public static final int FLAT = 1;
        public static final int STABLE = 2;
    }

    public static class Layout {

        public JSONObject property;

        public float width, height, xOffset, yOffset;

        public float scale = 1;

        public static Layout load(JSONObject object) {
            Layout layout = new Layout();
            layout.property = object;
            layout.width = (float) object.optDouble("w", -1);
            layout.height = (float) object.optDouble("h", -1);
            layout.xOffset = (float) object.optDouble("x", 0);
            layout.yOffset = (float) object.optDouble("y", 0);
            layout.scale = (float) object.optDouble("scale", -1);
            return layout;
        }

        public void baseApply(Sprite entity) {
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
