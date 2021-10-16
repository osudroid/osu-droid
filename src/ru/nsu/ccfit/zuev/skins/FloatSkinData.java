package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.datatypes.DefaultFloat;

public class FloatSkinData extends SkinData<Float> {
    public FloatSkinData(String tag, float number) {
        super(tag, new DefaultFloat(number));
    }

    public FloatSkinData(String tag) {
        this(tag, new DefaultFloat().getCurrentValue());
    }

    @Override
    public void setFromJson(@NonNull JSONObject data) {
        setCurrentValue((float) data.optDouble(getTag(), getDefaultValue()));
    }
}
