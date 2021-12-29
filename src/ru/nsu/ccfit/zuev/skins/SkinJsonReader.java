package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import com.edlplan.framework.utils.interfaces.Consumer;

import org.json.JSONArray;
import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public class SkinJsonReader extends SkinReader {
    private static final SkinJsonReader reader = new SkinJsonReader();

    private JSONObject currentData;
    private JSONObject currentComboColorData;
    private JSONObject currentSliderData;
    private JSONObject currentUtilsData;
    private JSONObject currentLayoutData;
    private JSONObject currentColorData;
    private JSONObject currentCursorData;

    private SkinJsonReader() {

    }

    public static SkinJsonReader getReader() {
        return reader;
    }

    public void supplyJson(JSONObject json) {
        currentData = json;
        loadSkin();
    }

    @Override
    protected void loadSkinBase() {
        load("ComboColor", currentData, (c) -> {
            currentComboColorData = c;
            loadComboColorSetting();
        });
        load("Slider", currentData, (c) -> {
            currentSliderData = c;
            loadSlider();
        });
        load("Utils", currentData, (c) -> {
            currentUtilsData = c;
            loadUtils();
        });
        load("Layout", currentData, (c) -> {
            currentLayoutData = c;
            loadLayout();
        });
        load("Color", currentData, (c) -> {
            currentColorData = c;
            loadColor();
        });
        load("Cursor", currentData, (c) -> {
            currentCursorData = c;
            loadCursor();
        });
    }

    @Override
    protected void loadComboColorSetting() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentComboColorData;
        skin.forceOverrideComboColor.setFromJson(data);
        skin.comboColor.clear();
        JSONArray array = data.optJSONArray("colors");
        if (array == null || array.length() == 0) {
            skin.comboColor.add(RGBColor.hex2Rgb(skin.DEFAULT_COLOR_HEX));
        } else {
            for (int i = 0; i < array.length(); i++) {
                String hex = array.optString(i, skin.DEFAULT_COLOR_HEX);
                skin.comboColor.add(RGBColor.hex2Rgb(hex));
            }
        }
    }

    @Override
    protected void loadSlider() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentSliderData;
        skin.sliderBodyWidth.setFromJson(data);
        skin.sliderBodyWidth.setFromJson(data);
        skin.sliderBodyBaseAlpha.setFromJson(data);
        skin.sliderHintWidth.setFromJson(data);
        skin.sliderHintShowMinLength.setFromJson(data);
        skin.sliderHintAlpha.setFromJson(data);
        skin.sliderFollowComboColor.setFromJson(data);
        skin.sliderHintEnable.setFromJson(data);
        skin.sliderBodyColor.setFromJson(data);
        skin.sliderBorderColor.setFromJson(data);
        skin.sliderHintColor.setFromJson(data);
    }

    @Override
    protected void loadUtils() {
        JSONObject data = currentUtilsData;
        OsuSkin skin = OsuSkin.get();
        skin.limitComboTextLength.setFromJson(data);
        skin.disableKiai.setFromJson(data);
        skin.comboTextScale.setFromJson(data);
    }

    @Override
    protected void loadLayout() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentLayoutData;
        skin.useNewLayout.setFromJson(data);
        JSONArray names = data.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            if (names.optString(i).equals(skin.useNewLayout.getTag())) {
                continue;
            }
            JSONObject layoutJSON = data.optJSONObject(names.optString(i));
            if (layoutJSON != null) {
                putLayout(names.optString(i), SkinLayout.load(layoutJSON));
            }
        }
    }

    @Override
    protected void loadColor() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentColorData;
        JSONArray names = data.names();
        if (names == null) return;
        for (int i = 0; i < names.length(); i++) {
            skin.colorData.put(names.optString(i), RGBColor.hex2Rgb(data.optString(names.optString(i))));
        }
    }

    @Override
    protected void loadCursor() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentCursorData;
        skin.rotateCursor.setFromJson(data);
    }

    public void load(String tag, @NonNull JSONObject data, Consumer<JSONObject> consumer) {
        JSONObject object = data.optJSONObject(tag);
        if (object == null) {
            object = new JSONObject();
        }
        consumer.consume(object);
    }
}

