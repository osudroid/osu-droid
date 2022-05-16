package ru.nsu.ccfit.zuev.osu;

import java.io.Serializable;

public class Video implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String path;
    private final int startTime;
    private final int xOffset;
    private final int yOffset;

    public Video(String path, int startTime, int xOffset, int yOffset) {
        this.path = path;
        this.startTime = startTime;
        this.xOffset = xOffset;
        this.yOffset = yOffset;
    }

    public String getPath() {
        return path;
    }

    public int getStartTime() {
        return startTime;
    }

    public int getxOffset() {
        return xOffset;
    }

    public int getyOffset() {
        return yOffset;
    }
}
