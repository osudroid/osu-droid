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

    public T getDefaultValue() {
        return defaultValue;
    }

    public T getCurrentValue() {
        return currentValue;
    }

    public void setCurrentValue(T currentValue) {
        this.currentValue = currentValue;
    }

    protected abstract T instanceDefaultValue();

}
