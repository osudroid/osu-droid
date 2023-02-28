package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.math.Vector2;
import com.rian.difficultycalculator.utils.GameMode;

/**
 * Represents a hit object.
 */
public abstract class HitObject {
    /**
     * The radius of hit objects (i.e. the radius of a circle) relative to osu!standard.
     */
    public static final float OBJECT_RADIUS = 64;

    /**
     * The time at which this hit object starts, in milliseconds.
     */
    protected double startTime;

    /**
     * The position of this hit object.
     */
    protected Vector2 position;

    /**
     * The end position of this hit object.
     */
    protected Vector2 endPosition;

    /**
     * The stack height of this hit object.
     */
    protected int stackHeight;

    /**
     * The rimu! scale of this hit object with respect to osu!standard scale metric.
     */
    private float rimuScale;

    /**
     * The osu!standard scale of this hit object.
     */
    private float standardScale;

    /**
     * @param startTime The time at which this hit object starts, in milliseconds.
     * @param position  The position of the hit object relative to the play field.
     */
    public HitObject(double startTime, Vector2 position) {
        this.startTime = startTime;
        this.position = position;
        endPosition = position;
    }

    /**
     * @param startTime The time at which this hit object starts, in milliseconds.
     * @param x         The X position of the hit object relative to the play field.
     * @param y         The Y position of the hit object relative to the play field.
     */
    public HitObject(double startTime, double x, double y) {
        this(startTime, new Vector2(x, y));
    }

    /**
     * Gets the rimu! scale of this hit object with respect to osu!standard scale metric.
     */
    public float getRimuScale() {
        return rimuScale;
    }

    /**
     * Sets the rimu! scale of this hit object.
     *
     * @param rimuScale The new rimu! scale.
     */
    public void setRimuScale(float rimuScale) {
        this.rimuScale = rimuScale;
    }

    /**
     * Gets the osu!standard scale of this hit object.
     */
    public float getStandardScale() {
        return standardScale;
    }

    /**
     * Sets the osu!standard scale of this hit object.
     *
     * @param standardScale The new osu!standard scale.
     */
    public void setStandardScale(float standardScale) {
        this.standardScale = standardScale;
    }

    /**
     * Gets the time at which this hit object starts, in milliseconds.
     */
    public double getStartTime() {
        return startTime;
    }

    /**
     * Gets the stack height of this hit object.
     */
    public int getStackHeight() {
        return stackHeight;
    }

    /**
     * Sets the stack height of this hit object.
     *
     * @param stackHeight The new stack height.
     */
    public void setStackHeight(int stackHeight) {
        this.stackHeight = stackHeight;
    }

    /**
     * Gets the position of this hit object.
     */
    public Vector2 getPosition() {
        return position;
    }

    /**
     * Gets the end position of this hit object.
     */
    public Vector2 getEndPosition() {
        return endPosition;
    }

    /**
     * Gets the radius of this hit object.
     *
     * @param mode The game mode to calculate for.
     */
    public double getRadius(GameMode mode) {
        double radius = OBJECT_RADIUS;


        switch (mode) {
            case rimu:
                radius *= rimuScale;
            case standard:
                radius *= standardScale;
        }

        return radius;
    }

    /**
     * Gets the stack offset vector of this hit object.
     *
     * @param mode The game mode to calculate for.
     */
    public Vector2 getStackOffset(GameMode mode) {
        double coordinate = stackHeight;

        switch (mode) {
            case rimu:
                coordinate *= rimuScale * -4;
                break;
            case standard:
                coordinate *= standardScale * -6.4;
        }

        return new Vector2(coordinate);
    }

    /**
     * Gets the stacked position of this hit object.
     *
     * @param mode The game mode to calculate for.
     */
    public Vector2 getStackedPosition(GameMode mode) {
        return evaluateStackedPosition(position, mode);
    }

    /**
     * Gets the stacked end position of this slider.
     *
     * @param mode The game mode to calculate for.
     */
    public Vector2 getStackedEndPosition(GameMode mode) {
        return evaluateStackedPosition(endPosition, mode);
    }

    /**
     * Evaluates a stacked position relative to this hit object.
     *
     * @param position The position to evaluate.
     * @param mode The game mode to calculate for.
     * @return The evaluated stacked position.
     */
    protected Vector2 evaluateStackedPosition(Vector2 position, GameMode mode) {
        return position.add(getStackOffset(mode));
    }

    /**
     * Deep clones this hit object.
     *
     * This method only deep clones fields that may be changed during difficulty calculation.
     *
     * @return The deep cloned hit object.
     */
    public HitObject deepClone() {
        return null;
    }
}
