package lt.ekgame.beatmap_analyzer.beatmap.taiko;

import java.util.List;

import lt.ekgame.beatmap_analyzer.Gamemode;
import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapDifficulties;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapEditorState;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapGenerals;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapMetadata;
import lt.ekgame.beatmap_analyzer.beatmap.BreakPeriod;
import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.beatmap.TimingPoint;
import lt.ekgame.beatmap_analyzer.difficulty.TaikoDifficulty;
import lt.ekgame.beatmap_analyzer.difficulty.TaikoDifficultyCalculator;
import lt.ekgame.beatmap_analyzer.utils.Mods;

public class TaikoBeatmap extends Beatmap {

    private List<TaikoObject> hitObjects;

    public TaikoBeatmap(BeatmapGenerals generals, BeatmapEditorState editorState, BeatmapMetadata metadata,
                        BeatmapDifficulties difficulties, List<BreakPeriod> breaks, List<TimingPoint> timingPoints,
                        List<TaikoObject> hitObjects) {
        super(generals, editorState, metadata, difficulties, breaks, timingPoints);
        this.hitObjects = hitObjects;
        finalizeObjects(hitObjects);
    }

    @Override
    public Gamemode getGamemode() {
        return Gamemode.TAIKO;
    }

    @Override
    public int getMaxCombo() {
        int c = 0;
        for (HitObject o : hitObjects) {
            if (o instanceof TaikoCircle) c++;
        }


        return c;
        //(int) hitObjects.stream().filter(o->o instanceof TaikoCircle).count();
    }

    public List<TaikoObject> getHitObjects() {
        return hitObjects;
    }

    @Override
    public int getObjectCount() {
        return hitObjects.size();
    }

    @Override
    public TaikoDifficultyCalculator getDifficultyCalculator() {
        return new TaikoDifficultyCalculator();
    }

    @Override
    public TaikoDifficulty getDifficulty(Mods mods) {
        return getDifficultyCalculator().calculate(mods, this);
    }

    @Override
    public TaikoDifficulty getDifficulty() {
        return getDifficulty(Mods.NOMOD);
    }
}
