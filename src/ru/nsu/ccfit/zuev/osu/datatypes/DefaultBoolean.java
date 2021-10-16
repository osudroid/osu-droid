package ru.nsu.ccfit.zuev.osu.datatypes;

public class DefaultBoolean extends DefaultData<Boolean> {
    public DefaultBoolean(Boolean defaultValue) {
        super(defaultValue);
    }

    public DefaultBoolean() {
        super();
    }

    @Override
    protected Boolean instanceDefaultValue() {
        return false;
    }
}
