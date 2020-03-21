package lt.ekgame.beatmap_analyzer.beatmap.taiko;

import lt.ekgame.beatmap_analyzer.beatmap.HitObject;
import lt.ekgame.beatmap_analyzer.utils.Vec2;

public abstract class TaikoObject extends HitObject {

    public TaikoObject(Vec2 position, int startTime, int endTime, int hitSound) {
        super(position, startTime, endTime, hitSound);
    }
}
