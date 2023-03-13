package com.rian.difficultycalculator.beatmap.timings;

/**
 * Represents a control point that changes speed multiplier.
 */
public class DifficultyControlPoint extends ControlPoint {
    /**
     * The slider speed multiplier of this control point.
     */
    public final double speedMultiplier;

    /**
     * Whether or not slider ticks should be generated at this control point.
     * <br>
     * This exists for backwards compatibility with maps that abuse NaN slider velocity behavior on osu!stable (e.g. /b/2628991).
     */
    public final boolean generateTicks;

    /**
     * @param time            The time at which this control point takes effect, in milliseconds.
     * @param speedMultiplier The slider speed multiplier of this control point.
     * @param generateTicks   Whether or not slider ticks should be generated at this control point.<br>This exists for backwards compatibility with maps that abuse NaN slider velocity behavior on osu!stable (e.g. /b/2628991).
     */
    public DifficultyControlPoint(int time, double speedMultiplier, boolean generateTicks) {
        super(time);

        this.speedMultiplier = speedMultiplier;
        this.generateTicks = generateTicks;
    }

    @Override
    public boolean isRedundant(ControlPoint existing) {
        return existing instanceof DifficultyControlPoint &&
                speedMultiplier == ((DifficultyControlPoint) existing).speedMultiplier &&
                generateTicks == ((DifficultyControlPoint) existing).generateTicks;
    }
}
