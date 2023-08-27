package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;

/**
 * Represents a hit circle.
 */
public class HitCircle extends HitObject {
    /**
     * @param startTime The start time of this hit circle, in milliseconds.
     * @param position  The position of this hit circle relative to the play field.
     */
    public HitCircle(double startTime, Vector2 position) {
        super(startTime, position);
    }

    /**
     * @param startTime The start time of this hit circle, in milliseconds.
     * @param x         The X position of this hit circle relative to the play field.
     * @param y         The Y position of this hit circle relative to the play field.
     */
    public HitCircle(double startTime, float x, float y) {
        super(startTime, new Vector2(x, y));
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private HitCircle(HitCircle source) {
        super(source);
    }

    @Override
    public HitCircle deepClone() {
        return new HitCircle(this);
    }
}
