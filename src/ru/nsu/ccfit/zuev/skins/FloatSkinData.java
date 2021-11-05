package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.datatypes.DefaultFloat;

public class FloatSkinData extends SkinData<Float> {
    public FloatSkinData(String tag, float defaultValue) {
        super(tag, new DefaultFloat(defaultValue));
    }

    public FloatSkinData(String tag) {
        this(tag, new DefaultFloat().getCurrentValue());
    }

    @Override
    public void setFromJson(@NonNull JSONObject data) {
        setCurrentValue((float) data.optDouble(getTag(), getDefaultValue()));
    }
}
