package ru.nsu.ccfit.zuev.osu.scoring;


import java.util.HashMap;


public enum TouchType{
    DOWN((byte) 0),
    MOVE((byte) 1),
    UP((byte) 2);

    private static final HashMap<Byte, TouchType> byID = new HashMap<>();

    static {
        for (TouchType v: TouchType.values()) {
            byID.put(v.getId(), v);
        }
    }

    public static TouchType getByID(byte id) {
        return byID.get(id);
    }

    private final byte id;

    TouchType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }

}
