package com.rian.difficultycalculator.beatmap;

import com.rian.difficultycalculator.beatmap.hitobject.HitCircle;
import com.rian.difficultycalculator.beatmap.hitobject.HitObject;
import com.rian.difficultycalculator.beatmap.hitobject.Slider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A manager for hit objects of a beatmap.
 */
public class BeatmapHitObjectsManager {
    /**
     * All objects in this beatmap.
     */
    private final ArrayList<HitObject> objects = new ArrayList<>();

    /**
     * The amount of circles in this beatmap.
     */
    private int circleCount;

    /**
     * The amount of sliders in this beatmap.
     */
    private int sliderCount;

    /**
     * The amount of spinners in this beatmap.
     */
    private int spinnerCount;

    public BeatmapHitObjectsManager() {}

    /**
     * Copy constructor.
     *
     * @param source The source to copy from.
     */
    private BeatmapHitObjectsManager(BeatmapHitObjectsManager source) {
        circleCount = source.circleCount;
        sliderCount = source.sliderCount;
        spinnerCount = source.spinnerCount;

        for (HitObject object : source.objects) {
            objects.add(object.deepClone());
        }
    }

    /**
     * Adds hit objects to this beatmap.
     *
     * @param objects The hit objects to add.
     */
    public void add(Iterable<HitObject> objects) {
        for (HitObject object : objects) {
            add(object);
        }
    }

    /**
     * Adds a hit object to this beatmap.
     *
     * @param object The hit object to add.
     */
    public void add(HitObject object) {
        // Objects may be out of order *only* if a user has manually edited an .osu file.
        // Unfortunately there are "ranked" maps in this state (example: https://osu.ppy.sh/s/594828).
        // Finding index is used to guarantee that the parsing order of hit objects with equal start times is maintained (stably-sorted).
        objects.add(findInsertionIndex(object.getStartTime()), object);

        if (object instanceof HitCircle) {
            ++circleCount;
        } else if (object instanceof Slider) {
            ++sliderCount;
        } else {
            ++spinnerCount;
        }
    }

    /**
     * Removes a hit object from this beatmap.
     *
     * @param object The hit object to remove.
     * @return Whether the hit object was successfully removed.
     */
    public boolean remove(HitObject object) {
        return objects.remove(object);
    }

    /**
     * Removes a hit object from this beatmap at a given index.
     *
     * @param index The index of the hit object to remove.
     * @return The hit object that was removed, <code>null</code> if no hit objects were removed.
     */
    public HitObject remove(int index) {
        if (index < 0 || index > objects.size() - 1) {
            return null;
        }

        HitObject object = objects.remove(index);

        if (object instanceof HitCircle) {
            --circleCount;
        } else if (object instanceof Slider) {
            --sliderCount;
        } else {
            --spinnerCount;
        }

        return object;
    }

    /**
     * Clears all hit objects from this beatmap.
     */
    public void clear() {
        objects.clear();
        circleCount = 0;
        sliderCount = 0;
        spinnerCount = 0;
    }

    /**
     * Gets the list of hit objects in this beatmap.
     */
    public List<HitObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    /**
     * Deep clones this hit object manager.
     *
     * @return The deep cloned instance of this manager.
     */
    public BeatmapHitObjectsManager deepClone() {
        return new BeatmapHitObjectsManager(this);
    }

    /**
     * Gets the amount of circles in this beatmap.
     */
    public int getCircleCount() {
        return circleCount;
    }

    /**
     * Gets the amount of sliders in this beatmap.
     */
    public int getSliderCount() {
        return sliderCount;
    }

    /**
     * Gets the amount of spinners in this beatmap.
     */
    public int getSpinnerCount() {
        return spinnerCount;
    }

    /**
     * Finds the insertion index of a hit object in a given time.
     *
     * @param startTime The start time of the hit object.
     */
    private int findInsertionIndex(double startTime) {
        if (objects.size() == 0 || startTime < objects.get(0).getStartTime()) {
            return 0;
        }

        if (startTime >= objects.get(objects.size() - 1).getStartTime()) {
            return objects.size();
        }

        int l = 0;
        int r = objects.size() - 2;

        while (l <= r) {
            int pivot = l + ((r - l) >> 1);
            HitObject object = objects.get(pivot);
            double objectStartTime = object.getStartTime();

            if (objectStartTime < startTime) {
                l = pivot + 1;
            } else if (objectStartTime > startTime) {
                r = pivot - 1;
            } else {
                return pivot;
            }
        }

        return l;
    }
}
