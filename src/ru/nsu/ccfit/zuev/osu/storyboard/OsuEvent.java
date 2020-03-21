package ru.nsu.ccfit.zuev.osu.storyboard;

import java.util.ArrayList;

/**
 * Created by dgsrz on 16/9/16.
 */
public class OsuEvent {

    public Command command;
    public int ease;
    public long startTime;
    public long endTime;
    public float[] params;
    public ArrayList<OsuEvent> subEvents;//for command L and T
    public String triggerType;//for command T
    public int loopCount;
    public String P;

    public OsuEvent() {
    }
}
