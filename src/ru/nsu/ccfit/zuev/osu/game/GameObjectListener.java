package ru.nsu.ccfit.zuev.osu.game;

import android.graphics.PointF;

import com.osudroid.game.Cursor;
import com.reco1l.framework.Color4;
import com.rian.osu.gameplay.GameplayHitSampleInfo;

import java.util.BitSet;

public interface GameObjectListener {

    int SLIDER_START = 1, SLIDER_REPEAT = 2, SLIDER_END = 3, SLIDER_TICK = 4;

    void onCircleHit(int id, float accuracy, PointF pos, boolean endCombo, byte forcedScore, Color4 color);

    void onSliderHit(int id, int score, PointF judgementPos,
                     boolean endCombo, Color4 color, int type, boolean incrementCombo);

    void onSliderEnd(int id, int accuracy, BitSet tickSet);

    void onSpinnerStart(int id);

    void onSpinnerHit(int id, int score, boolean endCombo, int totalScore);

    void addObject(GameObject object);

    void removeObject(GameObject object);

    Cursor getCursor(int index);

    int getCursorsCount();

    void registerAccuracy(double acc);
    
    void updateAutoBasedPos(float pX, float pY);

    void onTrackingSliders(boolean isTrackingSliders);

    void onUpdatedAutoCursor(float pX, float pY);

    void playHitSamples(GameplayHitSampleInfo[] samples);
}