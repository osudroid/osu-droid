package com.dgsrz.tpdifficulty.tp;

/**
 * Created by Fuuko on 2015/5/30.
 */
public enum DifficultyType {
    Speed(0),
    Aim(1);
    private int value = 0;

    DifficultyType(int value) {
        this.value = value;
    }

    public int value() {
        return this.value;
    }
}
