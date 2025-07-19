package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import com.edlplan.framework.utils.interfaces.Consumer;
import com.osudroid.ui.v2.hud.HUDSkinData;
import com.reco1l.andengine.ui.Theme;
import com.reco1l.framework.Color4;
import com.reco1l.framework.HexComposition;

import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

public class SkinJsonReader extends SkinReader {
    private static final SkinJsonReader reader = new SkinJsonReader();

    private JSONObject currentData;
    private JSONObject currentComboColorData;
    private JSONObject currentSliderData;
    private JSONObject currentUtilsData;
    private JSONObject currentLayoutData;
    private JSONObject currentColorData;
    private JSONObject currentCursorData;
    private JSONObject currentFontsData;
    private JSONObject currentThemeData;

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
        load("Fonts", currentData, (c) -> {
            currentFontsData = c;
            loadFonts();
        });
        loadArray("HUD", currentData, (json) -> {
            OsuSkin.get().hudSkinData = json == null
                    ? HUDSkinData.Default
                    : HUDSkinData.readFromJSON(json);
        });
        load("Theme", currentData, (c) -> {
            currentThemeData = c;
            loadTheme();
        });
    }

    @Override
    protected void loadFonts()
    {
        var skin = OsuSkin.get();

        skin.hitCirclePrefix.setFromJson(currentFontsData);
        skin.hitCircleOverlap.setFromJson(currentFontsData);
        skin.scorePrefix.setFromJson(currentFontsData);
        skin.scoreOverlap.setFromJson(currentFontsData);
        skin.comboPrefix.setFromJson(currentFontsData);
        skin.comboOverlap.setFromJson(currentFontsData);
    }

    @Override
    protected void loadComboColorSetting() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentComboColorData;
        skin.forceOverrideComboColor.setFromJson(data);
        skin.comboColor.clear();
        JSONArray array = data.optJSONArray("colors");
        if (array == null || array.length() == 0) {
            skin.comboColor.add(new Color4(skin.DEFAULT_COLOR_HEX, HexComposition.RRGGBB));
        } else {
            for (int i = 0; i < array.length(); i++) {
                String hex = array.optString(i, skin.DEFAULT_COLOR_HEX);

                try {
                    skin.comboColor.add(new Color4(hex, HexComposition.RRGGBB));
                } catch (NumberFormatException ignored) {}
            }

            // If no valid colors were found, use the default color
            if (skin.comboColor.isEmpty()) {
                skin.comboColor.add(new Color4(skin.DEFAULT_COLOR_HEX, HexComposition.RRGGBB));
            }
        }
    }

    @Override
    protected void loadSlider() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentSliderData;
        skin.sliderBodyWidth.setFromJson(data);
        skin.sliderBorderWidth.setFromJson(data);
        skin.sliderBodyBaseAlpha.setFromJson(data);
        skin.sliderHintWidth.setFromJson(data);
        skin.sliderHintShowMinLength.setFromJson(data);
        skin.sliderHintAlpha.setFromJson(data);
        skin.sliderBallFlip.setFromJson(data);
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
        skin.animationFramerate.setFromJson(data);
        skin.layeredHitSounds.setFromJson(data);
        skin.spinnerFrequencyModulate.setFromJson(data);
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
            try {
                skin.colorData.put(names.optString(i), new Color4(data.optString(names.optString(i)), HexComposition.RRGGBB));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    protected void loadCursor() {
        OsuSkin skin = OsuSkin.get();
        JSONObject data = currentCursorData;
        skin.rotateCursor.setFromJson(data);
        skin.rotateCursorTrail.setFromJson(data);
    }

    @Override
    protected void loadTheme() {
        var accentColor =
            new Color4(currentThemeData.optString("accentColor", "#C2CAFF"), HexComposition.RRGGBB);

        Theme.Companion.setCurrent(new Theme(accentColor));
    }

    public void load(String tag, @NonNull JSONObject data, Consumer<JSONObject> consumer) {
        JSONObject object = data.optJSONObject(tag);
        if (object == null) {
            object = new JSONObject();
        }
        consumer.consume(object);
    }

    public void loadArray(String tag, @NonNull JSONObject data, Consumer<@Nullable JSONArray> consumer) {
        consumer.consume(data.optJSONArray(tag));
    }

    public JSONObject getCurrentData() {
        return currentData;
    }

    public JSONObject setCurrentData(JSONObject currentData) {
        return this.currentData = currentData;
    }
}

