package com.rian.difficultycalculator.beatmap.hitobject;

import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHead;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderHitObject;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderRepeat;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderTail;
import com.rian.difficultycalculator.beatmap.hitobject.sliderobject.SliderTick;
import com.rian.difficultycalculator.beatmap.timings.DifficultyControlPoint;
import com.rian.difficultycalculator.beatmap.timings.TimingControlPoint;
import com.rian.difficultycalculator.math.MathUtils;
import com.rian.difficultycalculator.math.Vector2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a slider.
 */
public class Slider extends HitObjectWithDuration {
    public static final int legacyLastTickOffset = 36;

    /**
     * The repetition amount of this slider.
     * <br><br>
     * Note that 1 repetition means no repeats (1 loop).
     */
    protected final int repeatCount;

    /**
     * The path of this slider.
     */
    protected final SliderPath path;

    /**
     * The nested hit objects of the slider.
     * <br><br>
     * Consists of head circle (slider head), slider ticks, repeat points, and tail circle (slider end).
     */
    protected final ArrayList<SliderHitObject> nestedHitObjects = new ArrayList<>();

    /**
     * The velocity of this slider.
     */
    protected final double velocity;

    /**
     * The head of the slider.
     */
    protected final SliderHead head;

    /**
     * The tail of the slider.
     */
    protected final SliderTail tail;

    /**
     * The position of the cursor at the point of completion of this slider if it was hit
     * with as few movements as possible. This is set and used by difficulty calculation.
     */
    protected Vector2 lazyEndPosition;

    /**
     * The distance travelled by the cursor upon completion of this slider if it was hit
     * with as few movements as possible. This is set and used by difficulty calculation.
     */
    protected float lazyTravelDistance;

    /**
     * The time taken by the cursor upon completion of this slider if it was hit with
     * as few movements as possible. This is set and used by difficulty calculation.
     */
    protected double lazyTravelTime;

    /**
     * The duration of one span of this slider.
     */
    protected double spanDuration;

    /**
     * @param startTime              The time at which this slider starts, in milliseconds.
     * @param x                      The X position of this slider relative to the play field.
     * @param y                      The Y position of this slider relative to the play field.
     * @param timingControlPoint     The timing control point this slider is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider is under effect on.
     * @param repeatCount            The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     * @param path                   The path of this slider.
     * @param tickRate               The tick rate of the beatmap containing this slider.
     * @param tickDistanceMultiplier The multiplier for calculating slider ticks.
     * @param generateTicks          Whether to generate ticks for this slider.
     */
    public Slider(double startTime, float x, float y, TimingControlPoint timingControlPoint,
                  DifficultyControlPoint difficultyControlPoint, int repeatCount, SliderPath path,
                  double sliderVelocity, double tickRate, double tickDistanceMultiplier, boolean generateTicks) {
        this(startTime, new Vector2(x, y), timingControlPoint, difficultyControlPoint, repeatCount,
                path, sliderVelocity, tickRate, tickDistanceMultiplier, generateTicks);
    }

    /**
     * @param startTime              The time at which this slider starts, in milliseconds.
     * @param position               The position of the slider relative to the play field.
     * @param timingControlPoint     The timing control point this slider is under effect on.
     * @param difficultyControlPoint The difficulty control point this slider is under effect on.
     * @param repeatCount            The repetition amount of this slider. Note that 1 repetition means no repeats (1 loop).
     * @param path                   The path of this slider.
     * @param sliderVelocity         The slider velocity of the beatmap containing this slider.
     * @param tickRate               The tick rate of the beatmap containing this slider.
     * @param tickDistanceMultiplier The multiplier for calculating slider ticks.
     * @param generateTicks          Whether to generate ticks for this slider.
     */
    public Slider(double startTime, Vector2 position,
                  TimingControlPoint timingControlPoint, DifficultyControlPoint difficultyControlPoint,
                  int repeatCount, SliderPath path, double sliderVelocity, double tickRate,
                  double tickDistanceMultiplier, boolean generateTicks) {
        // Temporarily set end time to start time. It will be evaluated later.
        super(startTime, startTime, position);

        this.repeatCount = repeatCount;
        this.path = path;

        double scoringDistance = 100 * sliderVelocity * difficultyControlPoint.speedMultiplier;
        velocity = scoringDistance / timingControlPoint.msPerBeat;

        endTime = startTime + repeatCount * path.expectedDistance / velocity;
        endPosition = position.add(path.positionAt(repeatCount % 2));

        spanDuration = getDuration() / repeatCount;

        head = new SliderHead(startTime, position);
        nestedHitObjects.add(head);

        // A very lenient maximum length of a slider for ticks to be generated.
        // This exists for edge cases such as /b/1573664 where the beatmap has been edited by the user, and should never be reached in normal usage.
        double maxLength = 100000;
        double length = Math.min(maxLength, path.expectedDistance);
        double tickDistance = MathUtils.clamp(scoringDistance / tickRate * tickDistanceMultiplier, 0, length);

        if (tickDistance != 0 && generateTicks) {
            double minDistanceFromEnd = velocity * 10;

            for (int span = 0; span < repeatCount; ++span) {
                double spanStartTime = startTime + span * spanDuration;
                boolean reversed = span % 2 == 1;

                ArrayList<SliderTick> sliderTicks = new ArrayList<>();

                for (double d = tickDistance; d <= length; d += tickDistance) {
                    if (d >= length - minDistanceFromEnd) {
                        break;
                    }

                    // Always generate ticks from the start of the path rather than the span to ensure
                    // that ticks in repeat spans are positioned identically to those in non-repeat spans
                    double distanceProgress = d / length;
                    double timeProgress = reversed ? 1 - distanceProgress : distanceProgress;

                    Vector2 tickPosition = position.add(path.positionAt(distanceProgress));

                    sliderTicks.add(new SliderTick(spanStartTime + timeProgress * spanDuration, tickPosition, span, spanStartTime));
                }

                // For repeat spans, ticks are returned in reverse-StartTime order.
                if (reversed) {
                    Collections.reverse(sliderTicks);
                }

                nestedHitObjects.addAll(sliderTicks);

                if (span < repeatCount - 1) {
                    Vector2 repeatPosition = position.add(path.positionAt((span + 1) % 2));
                    nestedHitObjects.add(new SliderRepeat(spanStartTime + spanDuration, repeatPosition, span, spanStartTime));
                }
            }
        }

        // Okay, I'll level with you. I made a mistake. It was 2007.
        // Times were simpler. osu! was but in its infancy and sliders were a new concept.
        // A hack was made, which has unfortunately lived through until this day.
        //
        // This legacy tick is used for some calculations and judgements where audio output is not required.
        // Generally we are keeping this around just for difficulty compatibility.
        // Optimistically we do not want to ever use this for anything user-facing going forwards.
        int finalSpanIndex = repeatCount - 1;
        double finalSpanStartTime = startTime + finalSpanIndex * spanDuration;
        double finalSpanEndTime = Math.max(
                startTime + getDuration() / 2,
                finalSpanStartTime + spanDuration - Slider.legacyLastTickOffset
        );

        tail = new SliderTail(finalSpanEndTime, endPosition, finalSpanIndex, finalSpanStartTime);
        nestedHitObjects.add(tail);
        Collections.sort(nestedHitObjects, (o1, o2) -> (int) (o1.startTime - o2.startTime));
    }

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private Slider(Slider source) {
        super(source);

        repeatCount = source.repeatCount;
        endPosition = new Vector2(source.endPosition.x, source.endPosition.y);
        path = source.path.deepClone();
        velocity = source.velocity;
        head = source.head.deepClone();
        tail = source.tail.deepClone();
        lazyEndPosition = source.lazyEndPosition != null ? new Vector2(source.lazyEndPosition.x, source.lazyEndPosition.y) : null;
        lazyTravelDistance = source.lazyTravelDistance;
        lazyTravelTime = source.lazyTravelTime;
        spanDuration = source.spanDuration;

        nestedHitObjects.add(head);

        for (SliderHitObject object : source.nestedHitObjects.subList(1, source.nestedHitObjects.size() - 1)) {
            nestedHitObjects.add((SliderHitObject) object.deepClone());
        }

        nestedHitObjects.add(tail);
        Collections.sort(nestedHitObjects, (a, b) -> Double.compare(a.startTime, b.startTime));
    }

    @Override
    public Slider deepClone() {
        return new Slider(this);
    }

    /**
     * Gets the repetition amount of this slider.
     * <br><br>
     * Note that 1 repetition means no repeats (1 loop).
     */
    public int getRepeatCount() {
        return repeatCount;
    }

    /**
     * Gets the path of this slider.
     */
    public SliderPath getPath() {
        return path;
    }

    @Override
    public void setScale(float scale) {
        super.setScale(scale);

        for (SliderHitObject object : nestedHitObjects) {
            object.setScale(scale);
        }
    }

    /**
     * Gets the nested hit objects of this slider.
     */
    public List<SliderHitObject> getNestedHitObjects() {
        return Collections.unmodifiableList(nestedHitObjects);
    }

    /**
     * Gets the position of the cursor at the point of completion of this slider if it was hit
     * with as few movements as possible. This is set and used by difficulty calculation.
     */
    public Vector2 getLazyEndPosition() {
        return lazyEndPosition;
    }

    /**
     * Gets the distance travelled by the cursor upon completion of this slider if it was hit
     * with as few movements as possible. This is set and used by difficulty calculation.
     */
    public double getLazyTravelDistance() {
        return lazyTravelDistance;
    }

    /**
     * Gets the time taken by the cursor upon completion of this slider if it was hit with
     * as few movements as possible. This is set and used by difficulty calculation.
     */
    public double getLazyTravelTime() {
        return lazyTravelTime;
    }

    /**
     * Gets the velocity of this slider.
     */
    public double getVelocity() {
        return velocity;
    }

    /**
     * Gets the duration of one span of this slider.
     */
    public double getSpanDuration() {
        return spanDuration;
    }
}
