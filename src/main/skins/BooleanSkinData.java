package main.skins;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import main.osu.datatypes.DefaultBoolean;

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
