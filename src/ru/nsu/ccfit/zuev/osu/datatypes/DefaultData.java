package ru.nsu.ccfit.zuev.osu.datatypes;

public abstract class DefaultData<T> implements IDefaultableData<T> {
    private final T defaultValue;
    private T currentValue;

    public DefaultData(T defaultValue) {
        this.defaultValue = defaultValue;
        setCurrentValue(defaultValue);
    }

    public DefaultData() {
        this.defaultValue = instanceDefaultValue();
        setCurrentValue(defaultValue);
    }

    @Override
    public T getDefaultValue() {
        return defaultValue;
    }

    @Override
    public T getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(T currentValue) {
        this.currentValue = currentValue;
    }

    @Override
    public boolean currentIsDefault() {
        return currentValue == defaultValue;
    }

    protected abstract T instanceDefaultValue();
}
