package ru.nsu.ccfit.zuev.osu.datatypes;

public class DefaultData<T> {
    private final T defaultValue;
    private T currentValue;

    public DefaultData(T defaultValue) {
        this.defaultValue = defaultValue;
        this.currentValue = defaultValue;
    }

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(T currentValue) {
        this.currentValue = currentValue;
    }
}
