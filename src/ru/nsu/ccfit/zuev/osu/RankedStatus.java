package ru.nsu.ccfit.zuev.osu;

public enum RankedStatus {
    ranked,
    approved,
    qualified,
    loved,
    pending,
    workInProgress,
    graveyard;

    public static RankedStatus valueOf(int value) {
        return switch (value) {
            case 1 -> ranked;
            case 2 -> approved;
            case 3 -> qualified;
            case 4 -> loved;
            case 0 -> pending;
            case -1 -> workInProgress;
            case -2 -> graveyard;
            default -> throw new IllegalArgumentException("Invalid value: " + value);
        };
    }
}
