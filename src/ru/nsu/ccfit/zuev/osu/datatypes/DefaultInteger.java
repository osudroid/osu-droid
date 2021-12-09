package ru.nsu.ccfit.zuev.osu.datatypes;

public class DefaultInteger extends DefaultData<Integer> {
    public DefaultInteger(Integer defaultValue) {
        super(defaultValue);
    }

    public DefaultInteger() {
        super();
    }

    @Override
    protected Integer instanceDefaultValue() {
        return 0;
    }
}
