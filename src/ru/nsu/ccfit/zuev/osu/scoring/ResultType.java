package ru.nsu.ccfit.zuev.osu.scoring;

public enum ResultType {
    HIT300((byte) 4),
    HIT100((byte) 3),
    HIT50((byte) 2),
    MISS((byte) 1);

    private final byte id;

    ResultType(byte id) {
        this.id = id;
    }

    public byte getId() {
        return id;
    }
}
