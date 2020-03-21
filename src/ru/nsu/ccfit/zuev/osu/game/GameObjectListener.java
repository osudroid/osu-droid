package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import java.util.BitSet;

import ru.nsu.ccfit.zuev.osu.RGBColor;

public interface GameObjectListener {

    int SLIDER_START = 1, SLIDER_REPEAT = 2, SLIDER_END = 3, SLIDER_TICK = 4;

    public void onCircleHit(int id, float accuracy, PointF pos, boolean endCombo, byte forcedScore, RGBColor color);

    public void onSliderHit(int id, int score, PointF start, PointF end,
                            boolean endCombo, RGBColor color, int type);

    public void onSliderEnd(int id, int accuracy, BitSet tickSet);

    public void onSpinnerHit(int id, int score, boolean endCombo, int totalScore);

    public void playSound(String name, int sampleSet, int addition);

    public void stopSound(String name);

    public void addObject(GameObject object);

    public void removeObject(GameObject object);

    public void addPassiveObject(GameObject object);

    public void removePassiveObject(GameObject object);

    public PointF getMousePos(int index);

    public boolean isMouseDown(int index);

    public boolean isMousePressed(GameObject object, int index);

    double downFrameOffset(int index);

    public int getCursorsCount();

    public void registerAccuracy(float acc);
}
