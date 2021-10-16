package ru.nsu.ccfit.zuev.osu.datatypes;

public interface IDefaultableData<T> {
    T getDefaultValue();
    T getCurrentValue();
    void setCurrentValue(T currentValue);
}
