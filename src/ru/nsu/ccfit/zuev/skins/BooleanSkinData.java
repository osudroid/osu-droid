package ru.nsu.ccfit.zuev.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import ru.nsu.ccfit.zuev.osu.datatypes.DefaultBoolean;

public class BooleanSkinData extends SkinData<Boolean> {
    public BooleanSkinData(String tag, boolean defaultValue) {
        super(tag, new DefaultBoolean(defaultValue));
    }

    public BooleanSkinData(String tag) {
        this(tag, new DefaultBoolean().getCurrentValue());
    }

    @Override
    public void setFromJson(@NonNull JSONObject data) {
        setCurrentValue(data.optBoolean(getTag(), getDefaultValue()));
    }
}
