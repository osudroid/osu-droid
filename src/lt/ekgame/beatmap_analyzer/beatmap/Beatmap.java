package lt.ekgame.beatmap_analyzer.beatmap;

import java.util.List;
import java.util.ListIterator;

import lt.ekgame.beatmap_analyzer.Gamemode;
import lt.ekgame.beatmap_analyzer.difficulty.Difficulty;
import lt.ekgame.beatmap_analyzer.difficulty.DifficultyCalculator;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public abstract class Beatmap {

    protected BeatmapGenerals generals;
    protected BeatmapEditorState editorState;
    protected BeatmapMetadata metadata;
    protected BeatmapDifficulties difficulties;

    protected List<BreakPeriod> breaks;
    protected List<TimingPoint> timingPoints;

    protected Beatmap(BeatmapGenerals generals, BeatmapEditorState editorState,
                      BeatmapMetadata metadata, BeatmapDifficulties difficulties,
                      List<BreakPeriod> breaks, List<TimingPoint> timingPoints) {
        this.generals = generals;
        this.editorState = editorState;
        this.metadata = metadata;
        this.difficulties = difficulties;
        this.breaks = breaks;
        this.timingPoints = timingPoints;
    }

    protected void finalizeObjects(List<? extends HitObject> objects) {
        ListIterator<TimingPoint> timingIterator = timingPoints.listIterator();
        ListIterator<? extends HitObject> objectIterator = objects.listIterator();

        // find first parent point
        TimingPoint parent = null;
        while (parent == null || parent.isInherited())
            parent = timingIterator.next();

        while (true) {
            TimingPoint current = timingIterator.hasNext() ? timingIterator.next() : null;
            TimingPoint previous = timingPoints.get(timingIterator.previousIndex() - (current == null ? 0 : 1));
            if (!previous.isInherited()) parent = previous;

            while (objectIterator.hasNext()) {
                HitObject object = objectIterator.next();
                if (current == null || object.getStartTime() < current.getTimestamp()) {
                    object.finalize(previous, parent, this);
                } else {
                    objectIterator.previous();
                    break;
                }
            }

            if (current == null) break;
        }
    }

    public abstract Gamemode getGamemode();

    public abstract DifficultyCalculator getDifficultyCalculator();

    public abstract Difficulty getDifficulty(Mods mods);

    public abstract Difficulty getDifficulty();

    public abstract int getMaxCombo();

    public abstract int getObjectCount();

    public BeatmapGenerals getGenerals() {
        return generals;
    }

    public BeatmapEditorState getEditorState() {
        return editorState;
    }

    public BeatmapMetadata getMetadata() {
        return metadata;
    }

    public BeatmapDifficulties getDifficultySettings() {
        return difficulties;
    }

    public List<BreakPeriod> getBreaks() {
        return breaks;
    }

    public List<TimingPoint> getTimingPoints() {
        return timingPoints;
    }
}
