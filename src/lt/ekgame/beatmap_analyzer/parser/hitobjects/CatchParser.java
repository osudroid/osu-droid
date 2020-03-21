package lt.ekgame.beatmap_analyzer.parser.hitobjects;

import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapDifficulties;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapEditorState;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapGenerals;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapMetadata;
import lt.ekgame.beatmap_analyzer.beatmap.BreakPeriod;
import lt.ekgame.beatmap_analyzer.beatmap.TimingPoint;
import lt.ekgame.beatmap_analyzer.beatmap.ctb.CatchObject;

public class CatchParser extends HitObjectParser<CatchObject> {

    @Override
    public CatchObject parse(String line) {
        return null;
    }

    @Override
    public Beatmap buildBeatmap(BeatmapGenerals generals, BeatmapEditorState editorState,
                                BeatmapMetadata metadata, BeatmapDifficulties difficulties, List<BreakPeriod> breaks,
                                List<TimingPoint> timingPoints, List<String> rawObjects) {
        return null;
    }
}
