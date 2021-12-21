package ru.nsu.ccfit.zuev.osu.datatypes;

public class DefaultFloat extends DefaultData<Float> {
    public DefaultFloat(Float defaultValue) {
        super(defaultValue);
    }

    public DefaultFloat() {
        super();
    }

    @Override
    protected Float instanceDefaultValue() {
        return 0f;
    }
}
