package lt.ekgame.beatmap_analyzer.parser.hitobjects;

import java.util.ArrayList;
import java.util.List;

import lt.ekgame.beatmap_analyzer.beatmap.Beatmap;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapDifficulties;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapEditorState;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapGenerals;
import lt.ekgame.beatmap_analyzer.beatmap.BeatmapMetadata;
import lt.ekgame.beatmap_analyzer.beatmap.BreakPeriod;
import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.beatmap.TimingPoint;

public abstract class HitObjectParser<T extends HitObject> {

    public abstract T parse(String line);

    public List<T> parse(List<String> lines) {
        List<T> l = new ArrayList<T>(lines.size());
        for (int i = 0; i < lines.size(); i++) {
            l.add(i, parse(lines.get(i)));
        }


        return l;
        //lines.stream().map(this::parse)
        //	.collect(Collectors.toList());
    }

    public abstract Beatmap buildBeatmap(BeatmapGenerals generals, BeatmapEditorState editorState,
                                         BeatmapMetadata metadata, BeatmapDifficulties difficulties, List<BreakPeriod> breaks,
                                         List<TimingPoint> timingPoints, List<String> rawObjects);

}
